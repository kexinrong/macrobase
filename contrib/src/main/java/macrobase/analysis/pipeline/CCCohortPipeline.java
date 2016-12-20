package macrobase.analysis.pipeline;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import macrobase.analysis.classify.BatchingPercentileClassifier;
import macrobase.analysis.classify.OutlierClassifier;
import macrobase.analysis.pipeline.operator.MBGroupBy;
import macrobase.analysis.result.AnalysisResult;
import macrobase.analysis.result.OutlierClassificationResult;
import macrobase.analysis.summary.BatchSummarizer;
import macrobase.analysis.summary.Summarizer;
import macrobase.analysis.summary.Summary;
import macrobase.analysis.summary.itemset.result.ItemsetResult;
import macrobase.analysis.transform.BatchScoreFeatureTransform;
import macrobase.analysis.transform.FeatureTransform;
import macrobase.analysis.transform.LinearMetricNormalizerPerGroup;
import macrobase.analysis.transform.LowMetricTransform;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import macrobase.ingest.DataIngester;
import macrobase.analysis.stats.MAD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CCCohortPipeline extends BasePipeline {
    private static final Logger log = LoggerFactory.getLogger(CCCohortPipeline.class);
    public static final String PANTHEON_GROUP_BY_ATTRS = "pantheon.groupby.attributes";
    public static final List<String> DEFAULT_PANTHEON_GROUP_BY_ATTRS = Lists.newArrayList("cc");

    @Override
    public Pipeline initialize(MacroBaseConf conf) throws Exception {
        super.initialize(conf);
        return this;
    }

    private void exportOutliers(List<OutlierClassificationResult> results) throws Exception {
        PrintWriter writer = new PrintWriter("outliers.txt", "UTF-8");
        for (OutlierClassificationResult result : results) {
            if (result.isOutlier()) {
                Datum d = result.getDatum();
                writer.print(d.metrics());
                for (Integer a : d.attributes()) {
                    writer.print("," + conf.getEncoder().getAttribute(a).getValue());
                }
                writer.println();
            }
        }
        writer.close();
    }

    @Override
    public List<AnalysisResult> run() throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        DataIngester ingester = conf.constructIngester();
        FeatureTransform normalizer = new LinearMetricNormalizerPerGroup(
                conf, 0, Lists.newArrayList("is_cellular", "sender_name", "receiver_name"));
        normalizer.consume(ingester.getStream().drain());
        final long loadMs = sw.elapsed(TimeUnit.MILLISECONDS);
        List<Datum> data = normalizer.getStream().drain();
        LowMetricTransform lmt = new LowMetricTransform(Lists.newArrayList(0));
        lmt.consume(data);
        data = lmt.getStream().drain();
        System.gc();

        List<String> attrs = conf.getStringList(MacroBaseConf.ATTRIBUTES);
        List<String> metrics = Lists.newArrayList(conf.getStringList(MacroBaseConf.METRICS));

        List<String> groupByAttrs = conf.getStringList(PANTHEON_GROUP_BY_ATTRS, DEFAULT_PANTHEON_GROUP_BY_ATTRS);

        List<Integer> groupByIndices = new ArrayList<>();
        for(String a : groupByAttrs) {
            groupByIndices.add(attrs.indexOf(a));
        }

        Map<Integer, List<ItemsetResult>> commonItemsets = new HashMap<>();

        long totalMs = 0, summarizeMs = 0, executeMs = 0;
        double numOutliers = 0;
        double numInliers = 0;

        for(int metricNo = 0; metricNo < metrics.size(); ++metricNo) {

            //MBGroupBy gb = new MBGroupBy(groupByIndices,
            //        () -> new BatchScoreFeatureTransform(conf, new MAD(conf)));

            //gb.consume(data);

            OutlierClassifier oc = new BatchingPercentileClassifier(conf);
            //data = gb.getStream().drain();
            BatchScoreFeatureTransform bt = new BatchScoreFeatureTransform(conf, new MAD(conf));
            bt.consume(data);
            oc.consume(bt.getStream().drain());

            Summarizer bs = new BatchSummarizer(conf);
            List<OutlierClassificationResult> results = oc.getStream().drain();
            exportOutliers(results);
            bs.consume(results);
            final Summary result = bs.summarize().getStream().drain().get(0);

            totalMs += sw.elapsed(TimeUnit.MILLISECONDS) - loadMs;
            summarizeMs += result.getCreationTimeMs();
            executeMs += totalMs - result.getCreationTimeMs();

            numInliers += result.getNumInliers();
            numOutliers += result.getNumOutliers();

            log.info("dim {} took {}ms ({} tuples/sec)",
                    metricNo,
                    sw.elapsed(TimeUnit.MILLISECONDS) - loadMs,
                    (result.getNumInliers() + result.getNumOutliers()) / (double) totalMs * 1000);

            for (ItemsetResult r : result.getItemsets()) {
                List<ItemsetResult> matchingItemsets = commonItemsets.get(r.getItems().hashCode());

                if (matchingItemsets == null) {
                    matchingItemsets = new ArrayList<>();
                    commonItemsets.put(r.getItems().hashCode(), matchingItemsets);
                }

                matchingItemsets.add(r);
            }
        }

        List<ItemsetResult> combinedItemsets = new ArrayList<>();

        for(List<ItemsetResult> isrs : commonItemsets.values()) {
            double sumOutliers = 0, sumRecords = 0, sumRatios = 0;
            for (ItemsetResult item : isrs) {
                sumOutliers += item.getNumRecords() / item.getSupport();
                sumRecords += item.getNumRecords();
                sumRatios += item.getRatioToInliers();
            }

            ItemsetResult combined = new ItemsetResult(sumRecords / sumOutliers,
                    sumRecords / isrs.size(),
                    sumRatios / isrs.size(),
                    isrs.get(0).getItems());

            combinedItemsets.add(combined);
        }

        return Arrays.asList(new AnalysisResult(numOutliers,
                numInliers,
                loadMs,
                executeMs,
                summarizeMs,
                combinedItemsets));
    }
}
