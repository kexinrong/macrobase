package macrobase.util.asap;

import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Experiment {
    protected static BruteForce grid;
    protected static ASAP asap;
    protected static MacroBaseConf conf;
    protected static MacroBaseConf exportConf;
    protected static int datasetID;
    protected static PrintWriter result;
    protected static PrintWriter plot;

    public Experiment(int datasetID, int resolution, double thresh, boolean isStream) throws Exception {
        this.datasetID = datasetID;
        conf = getConf(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        grid = new BruteForce(conf, windowRange, binSize, thresh, 1, isStream);
        asap = new ASAP(conf, windowRange, binSize, thresh, isStream);

        exportConf = new MacroBaseConf();
        exportConf.set(MacroBaseConf.TIME_COLUMN, 0);
        exportConf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
    }

    public static long roundBinSize(long windowRange, int resolution) {
        long binSize = windowRange / resolution;
        // Round to the nearest multilples of 10 min
        if (binSize > 600000) {
            binSize = (binSize / 600000 + 1) * 600000;
        } else if (binSize > 1000) {
            binSize = (binSize / 1000 + 1) * 1000;
        }
        return binSize;
    }

    protected static MacroBaseConf getConf(int datasetID) {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.CSV_INPUT_FILE,
                String.format("contrib/src/test/resources/data/%s.csv", DataSources.TABLE_NAMES.get(datasetID)));
        conf.set(MacroBaseConf.ATTRIBUTES, new ArrayList<>());
        conf.set(MacroBaseConf.METRICS, DataSources.COLUMN_NAMES.get(datasetID));
        conf.set(MacroBaseConf.TIME_COLUMN, 0);
        conf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        if (DataSources.TIME_FORMATS.containsKey(datasetID)) {
            conf.set(MacroBaseConf.TIME_FORMAT, DataSources.TIME_FORMATS.get(datasetID));
        }
        return conf;
    }

    protected static void exportRaw() {
        // Raw series
        plot.println("Original");
        plot.println(String.format("%d %d %d", grid.binSize, 1, 1));
        for (Datum d : grid.currWindow) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
    }

    protected static void computeWindow(MacroBaseConf conf, SmoothingParam s) throws ConfigurationException {
        conf.set(MacroBaseConf.TIME_WINDOW, s.windowSize * s.binSize);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, s.slideSize * s.binSize);
        sw.consume(s.currWindow);
        sw.shutdown();
        List<Datum> windows = sw.getStream().drain();
        double smoothness = s.metrics.smoothness(windows);
        double kurtosis = s.metrics.kurtosis(windows);
        // Output to file
        result.println(s.name);
        result.println(String.format("ws: %d, ss: %d, var: %f, kurtosis:%f",
                s.windowSize, s.slideSize, smoothness, kurtosis));
        result.println(String.format("#points: %d, #searches: %d, runtime: %d(micro sec), update interval: %d",
                s.numPoints, s.pointsChecked, s.runtimeMS, s.updateInterval));
        result.println();
        plot.println(s.name);
        plot.println(String.format("%d %d %d", s.binSize, s.windowSize, s.slideSize));
        for (Datum d : windows) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
    }
}
