package edu.stanford.futuredata.macrobase.integration;

import edu.stanford.futuredata.macrobase.StreamingSummarizationTest;
import edu.stanford.futuredata.macrobase.analysis.summary.BatchSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.Explanation;
import edu.stanford.futuredata.macrobase.analysis.summary.IncrementalSummarizer;
import edu.stanford.futuredata.macrobase.analysis.summary.itemset.result.AttributeSet;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import edu.stanford.futuredata.macrobase.operator.WindowedOperator;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Compare the performancce of sliding window summarization with repeated batch summarization.
 * The incremental sliding window operator should be noticeably faster.
 */
public class StreamingSummarizationBenchmark {
    // Increase these numbers for more rigorous, slower performance testing
    static int n = 100000;
    static int k = 3;
    static int C = 4;
    static int d = 10;
    static double p = 0.005;
    static int eventIdx = 50000;
    static int eventEndIdx = 100000;
    static int windowSize = 50000;
    static int slideSize = 1000;

    public static void testWindowedPerformance() throws Exception {

        DataFrame df = StreamingSummarizationTest.generateAnomalyDataset(n, k, C, d, p, eventIdx, eventEndIdx);
        List<String> attributes = StreamingSummarizationTest.getAttributes(d, false);
        List<String> buggyAttributeValues = StreamingSummarizationTest.getAttributes(k, true);

        IncrementalSummarizer outlierSummarizer = new IncrementalSummarizer();
        outlierSummarizer.setAttributes(attributes);
        outlierSummarizer.setOutlierColumn("outlier");
        outlierSummarizer.setMinSupport(.3);
        WindowedOperator<Explanation> windowedSummarizer = new WindowedOperator<>(outlierSummarizer);
        windowedSummarizer.setWindowLength(windowSize);
        windowedSummarizer.setTimeColumn("time");
        windowedSummarizer.setSlideLength(slideSize);
        windowedSummarizer.initialize();

        BatchSummarizer bsumm = new BatchSummarizer();
        bsumm.setAttributes(attributes);
        bsumm.setOutlierColumn("outlier");
        bsumm.setMinSupport(.3);

        int miniBatchSize = slideSize;
        double totalStreamingTime = 0.0;
        double totalBatchTime = 0.0;

        double startTime = 0.0;
        while (startTime < n) {
            double endTime = startTime + miniBatchSize;
            double ls = startTime;
            DataFrame curBatch = df.filter(
                    "time",
                    (double t) -> t >= ls && t < endTime
            );
            long timerStart = System.currentTimeMillis();
            windowedSummarizer.process(curBatch);
            Explanation curExplanation = windowedSummarizer
                    .getResults()
                    .prune();
            long timerElapsed = System.currentTimeMillis() - timerStart;
            totalStreamingTime += timerElapsed;

            if (windowedSummarizer.getMaxWindowTime() > eventIdx
                    && windowedSummarizer.getMaxWindowTime() - windowSize < eventEndIdx) {
                //  make sure that the known anomalous attribute combination has the highest risk ratio
                AttributeSet topRankedExplanation = curExplanation.getItemsets().get(0);
                assertTrue(topRankedExplanation.getItems().values().containsAll(buggyAttributeValues));
            } else {
                // Otherwise make sure that the noisy explanations are all low-cardinality
                if (curExplanation.getItemsets().size() > 0) {
                    AttributeSet topRankedExplanation = curExplanation.getItemsets().get(0);
                    assertTrue(
                            topRankedExplanation.getNumRecords() < Math.max(0.0002 * n, 5)
                    );
                }
            }


            DataFrame curWindow = df.filter(
                    "time",
                    (double t) -> t >= (endTime - windowSize) && t < endTime
            );
            timerStart = System.currentTimeMillis();
            bsumm.process(curWindow);
            Explanation batchExplanation = bsumm.getResults();
            timerElapsed = System.currentTimeMillis() - timerStart;
            totalBatchTime += timerElapsed;

            startTime = endTime;
        }

        System.out.println("Streaming Time: "+totalStreamingTime);
        System.out.println("Batch Time: "+totalBatchTime);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 9) {
            n = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
            C = Integer.parseInt(args[2]);
            d = Integer.parseInt(args[3]);
            p = Double.parseDouble(args[4]);
            eventIdx = Integer.parseInt(args[5]);
            eventEndIdx = Integer.parseInt(args[6]);
            windowSize = Integer.parseInt(args[7]);
            slideSize = Integer.parseInt(args[8]);
        }

        testWindowedPerformance();
    }
}
