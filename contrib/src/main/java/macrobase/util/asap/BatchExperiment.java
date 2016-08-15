package macrobase.util.asap;

import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BatchExperiment {
    private static BruteForce bf;
    private static ASAP asapRaw;
    private static ASAP asap;
    private static int datasetID;
    private static MacroBaseConf conf;
    private static int binSize;

    public BatchExperiment(MacroBaseConf conf, int windowRange,
                           int binSize, double thresh) throws Exception {
        this.binSize = binSize;
        bf = new BruteForce(conf, windowRange, binSize, thresh);
        asapRaw = new ASAP(conf, windowRange, binSize, thresh, false);
        asap = new ASAP(conf, windowRange, binSize, thresh, true);
    }

    private static MacroBaseConf getConf(int datasetID) {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.CSV_INPUT_FILE,
                String.format("contrib/src/test/resources/data/%s.csv", DataSources.TABLE_NAMES.get(datasetID)));
        conf.set(MacroBaseConf.ATTRIBUTES, new ArrayList<>());
        conf.set(MacroBaseConf.METRICS, DataSources.COLUMN_NAMES.get(datasetID));
        conf.set(MacroBaseConf.TIME_COLUMN, 0);
        conf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        return conf;
    }

    public void run() throws Exception {
        bf.findRangeSlide();
        asapRaw.findRangeSlide();
        asap.findRangeSlide();
    }

    private void computeWindow(MacroBaseConf conf, SmoothingParam s,
                               PrintWriter result, PrintWriter plot) throws ConfigurationException {
        conf.set(MacroBaseConf.TIME_WINDOW, s.windowSize * binSize);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, s.slideSize * binSize);
        sw.consume(s.data);
        List<Datum> windows = sw.getStream().drain();
        double var = s.metrics.smoothness(windows, 1);
        double recall = s.metrics.recall(windows, s.windowSize, s.slideSize);
        // Output to file
        result.println(s.name);
        result.println(String.format("%d %d %f %f", s.windowSize, s.slideSize, var, recall));
        result.println(String.format("%d %d", s.numPoints, s.runtimeMS));
        plot.println(s.name);
        plot.println(String.format("%d %d %d", binSize, s.windowSize, s.slideSize));
        for (Datum d : windows) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
    }

    public void export() throws Exception {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.TIME_COLUMN, 0);
        conf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);

        PrintWriter result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%s_batch.txt",
                        DataSources.TABLE_NAMES.get(datasetID)), "UTF-8");
        PrintWriter plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%s_batch.txt",
                        DataSources.TABLE_NAMES.get(datasetID)), "UTF-8");
        // Raw series
        plot.println("Original");
        plot.println(String.format("%d %d %d", binSize, 1, 1));
        for (Datum d : bf.data) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
        // Compute window and output to file
        computeWindow(conf, bf, result, plot);
        computeWindow(conf, asap, result, plot);
        computeWindow(conf, asapRaw, result, plot);

        result.close();
        plot.close();
    }

    public static void main(String[] args) throws Exception {
        datasetID = Integer.parseInt(args[0]);
        conf = getConf(datasetID);
        binSize = DataSources.BIN_SIZE.get(datasetID);
        BatchExperiment exp = new BatchExperiment(conf, DataSources.WINDOW_RANGES.get(datasetID), binSize, 0.5);
        exp.run();
        exp.export();
    }
}
