package edu.stanford.futuredata.macrobase.analysis.summary;

import edu.stanford.futuredata.macrobase.analysis.summary.itemset.result.AttributeSet;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import edu.stanford.futuredata.macrobase.datamodel.Schema;
import edu.stanford.futuredata.macrobase.ingest.CSVDataFrameLoader;
import edu.stanford.futuredata.macrobase.ingest.DataFrameLoader;
import edu.stanford.futuredata.macrobase.operator.WindowedOperator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by kexinrong on 11/12/17.
 */

/**
 * Compare the performancce of sliding window summarization with repeated batch summarization.
 * The incremental sliding window operator should be noticeably faster.
 */
public class SummarizerBench {
    // Increase these numbers for more rigorous, slower performance testing
    static int windowSize = 1000000;
    static int slideSize =  100000;
    private static DataFrame df;
    private static String outlierColumnName = "outlier";

    public static void ingest() throws Exception {
        Map<String, Schema.ColType> schema = new HashMap<>();
        schema.put("build_version", Schema.ColType.STRING);
        schema.put("app_version", Schema.ColType.STRING);
        schema.put("deviceid", Schema.ColType.STRING);
        schema.put("hardware_carrier", Schema.ColType.STRING);
        schema.put("state", Schema.ColType.DOUBLE);
        schema.put("hardware_model", Schema.ColType.STRING);
        schema.put("data_count_minutes", Schema.ColType.DOUBLE);
        schema.put("data_count_accel_samples", Schema.ColType.DOUBLE);
        schema.put("data_count_netloc_samples", Schema.ColType.DOUBLE);
        schema.put("data_count_gps_samples", Schema.ColType.DOUBLE);
        schema.put("distance_mapmatched_km", Schema.ColType.DOUBLE);
        schema.put("distance_gps_km", Schema.ColType.DOUBLE);
        schema.put("battery_drain_rate_per_hour", Schema.ColType.DOUBLE);
        DataFrameLoader loader = new CSVDataFrameLoader(
                "/data/pbailis/preagg/cmt.csv"
        ).setColumnTypes(schema);
        df = loader.load();
    }

    public static void addTSLabel() throws Exception {
        DataFrame output = df.copy();
        int len = df.getNumRows();
        double[] labelColumn = new double[len];
        double[] timeColumn = new double[len];

        File file = new File("cmt-label-complex.txt");
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        int i = 0;
        while ((line = bufferedReader.readLine()) != null) {
            timeColumn[i] = i;
            if (line.contains("false")) {
                labelColumn[i] = 0;
            } else {
                labelColumn[i] = 1;
            }
            i += 1;
        }
        fileReader.close();

        output.addDoubleColumn(outlierColumnName, labelColumn);
        output.addDoubleColumn("time", timeColumn);
        df = output;
    }

    public static void testWindowedPerformance() throws Exception {

        List<String> explanationAttributes = Arrays.asList(
                "build_version",
                "app_version",
                "deviceid",
                "hardware_carrier",
                "state",
                "hardware_model"
        );

        IncrementalSummarizer outlierSummarizer = new IncrementalSummarizer();
        outlierSummarizer.setAttributes(explanationAttributes);
        outlierSummarizer.setOutlierColumn(outlierColumnName);
        outlierSummarizer.setMinSupport(.01);
        WindowedOperator<Explanation> windowedSummarizer = new WindowedOperator<>(outlierSummarizer);
        windowedSummarizer.setWindowLength(windowSize);
        windowedSummarizer.setTimeColumn("time");
        windowedSummarizer.setSlideLength(slideSize);
        windowedSummarizer.initialize();

        BatchSummarizer bsumm = new BatchSummarizer();
        bsumm.setAttributes(explanationAttributes);
        bsumm.setOutlierColumn(outlierColumnName);
        bsumm.setMinSupport(.01);

        int miniBatchSize = slideSize;
        double totalStreamingTime = 0.0;
        double totalBatchTime = 0.0;

        double startTime = 0.0;
        int nRows = df.getNumRows();
        while (startTime < nRows) {
            double endTime = startTime + miniBatchSize;
            double ls = startTime;
            DataFrame curBatch = df.filter(
                    "time",
                    (double t) -> t >= ls && t < endTime
            );
            long timerStart = System.currentTimeMillis();
            windowedSummarizer.process(curBatch);
            if (endTime >= windowSize) {
                Explanation curExplanation = windowedSummarizer
                        .getResults()
                        .prune();
                long timerElapsed = System.currentTimeMillis() - timerStart;
                totalStreamingTime += timerElapsed;

                DataFrame curWindow = df.filter(
                        "time",
                        (double t) -> t >= (endTime - windowSize) && t < endTime
                );
                System.gc();
                timerStart = System.currentTimeMillis();
                bsumm.process(curWindow);
                Explanation batchExplanation = bsumm.getResults();
                timerElapsed = System.currentTimeMillis() - timerStart;
                totalBatchTime += timerElapsed;

                //  make sure that the known anomalous attribute combination has the highest risk ratio
//                if (curExplanation.getItemsets().size() > 0) {
//                    AttributeSet streamTopRankedExplanation = curExplanation.getItemsets().get(0);
//                    AttributeSet batchTopRankedExplanation = batchExplanation.getItemsets().get(0);
//                    System.out.println(startTime);
//                    System.out.println(streamTopRankedExplanation);
//                    System.out.println(batchTopRankedExplanation);
//                    System.out.println();
//                }
            } else {
                long timerElapsed = System.currentTimeMillis() - timerStart;
                totalStreamingTime += timerElapsed;
            }
            startTime = endTime;
            System.gc();
        }

        System.out.println("Streaming Time: "+totalStreamingTime);
        System.out.println("Batch Time: "+totalBatchTime);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            windowSize = Integer.parseInt(args[0]);
            slideSize = Integer.parseInt(args[1]);
        }
        ingest();
        addTSLabel();
        System.gc();
        testWindowedPerformance();
    }
}
