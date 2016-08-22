package macrobase.util;

import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import macrobase.util.asap.ASAP;
import macrobase.util.asap.BruteForce;
import macrobase.util.asap.DataSources;
import macrobase.util.asap.SmoothingParam;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Experiment {
    protected static BruteForce bf;
    protected static ASAP asapRaw;
    protected static ASAP asap;
    protected static MacroBaseConf conf;
    protected static int datasetID;

    public Experiment(int datasetID, int resolution, double thresh) throws Exception {
        this.datasetID = datasetID;
        conf = getConf(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        int binSize = (int)(windowRange / resolution);
        bf = new BruteForce(conf, windowRange, binSize, thresh);
        asapRaw = new ASAP(conf, windowRange, binSize, thresh, false);
        asap = new ASAP(conf, windowRange, binSize, thresh, true);
    }

    protected static MacroBaseConf getConf(int datasetID) {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.CSV_INPUT_FILE,
                String.format("contrib/src/test/resources/data/%s.csv", DataSources.TABLE_NAMES.get(datasetID)));
        conf.set(MacroBaseConf.ATTRIBUTES, new ArrayList<>());
        conf.set(MacroBaseConf.METRICS, DataSources.COLUMN_NAMES.get(datasetID));
        conf.set(MacroBaseConf.TIME_COLUMN, 0);
        conf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        return conf;
    }

    protected void computeWindow(MacroBaseConf conf, SmoothingParam s,
                               PrintWriter result, PrintWriter plot) throws ConfigurationException {
        conf.set(MacroBaseConf.TIME_WINDOW, s.windowSize * s.binSize);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, s.slideSize * s.binSize);
        sw.consume(s.currWindow);
        List<Datum> windows = sw.getStream().drain();
        double var = s.metrics.smoothness(windows, s.slideSize);
        double recall = s.metrics.recall(windows, s.windowSize, s.slideSize);
        // Output to file
        result.println(s.name);
        result.println(String.format("%d %d %f %f", s.windowSize, s.slideSize, var, recall));
        result.println(String.format("%d %d %d", s.numPoints, s.pointsChecked, s.runtimeMS));
        plot.println(s.name);
        plot.println(String.format("%d %d %d", s.binSize, s.windowSize, s.slideSize));
        for (Datum d : windows) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
    }
}
