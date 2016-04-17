package macrobase.analysis.pipeline;

import macrobase.analysis.classify.BatchingPercentileClassifier;
import macrobase.analysis.classify.OutlierClassifier;

import macrobase.analysis.result.AnalysisResult;
import macrobase.analysis.summary.BatchSummarizer;
import macrobase.analysis.summary.Summary;
import macrobase.analysis.transform.FeatureTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.ingest.DataIngester;
import macrobase.ingest.TimedBatchIngest;
import macrobase.analysis.transform.BatchScoreFeatureTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.SQLException;


public class BasicBatchedPipeline extends OneShotPipeline {
    private static final Logger log = LoggerFactory.getLogger(BasicBatchedPipeline.class);

    public BasicBatchedPipeline(MacroBaseConf conf) throws ConfigurationException, SQLException, IOException {
        super(conf);
        conf.sanityCheckBatch();
    }


    @Override
    AnalysisResult run() throws SQLException, IOException, ConfigurationException {
        long startMs = System.currentTimeMillis();
        DataIngester ingester = conf.constructIngester();
        TimedBatchIngest batchIngest = new TimedBatchIngest(ingester);
        FeatureTransform featureTransform = new BatchScoreFeatureTransform(conf, batchIngest, conf.getTransformType());
        OutlierClassifier outlierClassifier = new BatchingPercentileClassifier(conf, featureTransform);
        BatchSummarizer summarizer = new BatchSummarizer(conf, outlierClassifier);

     

        Summary result = summarizer.next();

        final long endMs = System.currentTimeMillis();
        final long loadMs = batchIngest.getFinishTimeMs() - startMs;
        final long totalMs = endMs - batchIngest.getFinishTimeMs();
        final long summarizeMs = result.getCreationTimeMs();
        final long executeMs = totalMs - result.getCreationTimeMs();

        log.info("took {}ms ({} tuples/sec)",
                 totalMs,
                 (result.getNumInliers()+result.getNumOutliers())/(double)totalMs*1000);

        return new AnalysisResult(result.getNumOutliers(),
                                  result.getNumInliers(),
                                  loadMs,
                                  executeMs,
                                  summarizeMs,
                                  result.getItemsets());
    }
}
