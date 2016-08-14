package macrobase.util.asap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BatchExperiment {
    private static Map<Integer, String> TABLE_NAMES = ImmutableMap.of(
            1, "art_daily_jumpsdown", 2, "fridge_data");
    private static Map<Integer, Integer> WINDOW_RANGES = ImmutableMap.of(
            1, 14 * 24 * 3600 * 1000, 2, 60 * 24 * 3600 * 1000);
    private static BruteForce bf;
    private static int datasetID;
    private static MacroBaseConf conf;
    private static int binSize;

    public BatchExperiment(MacroBaseConf conf, int windowRange,
                           int binSize, double thresh) throws Exception {
        this.binSize = binSize;
        bf = new BruteForce(conf, windowRange, binSize, thresh);
    }

    private static MacroBaseConf getConf(int datasetID) {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.CSV_INPUT_FILE,
                String.format("contrib/src/test/resources/data/%s.csv", TABLE_NAMES.get(datasetID)));
        conf.set(MacroBaseConf.ATTRIBUTES, new ArrayList<>());
        conf.set(MacroBaseConf.METRICS, Lists.newArrayList("timestamp","value"));
        conf.set(MacroBaseConf.TIME_COLUMN, 0);
        conf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        return conf;
    }

    public void bruteForce() throws Exception {
        bf.findRangeSlide();
        export(bf.windowSize, bf.slideSize);
    }

    public void export(int windowSize, int slideSize) throws Exception {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.TIME_WINDOW, windowSize * binSize);
        conf.set(MacroBaseConf.TIME_COLUMN, 0);
        conf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, slideSize * binSize);
        sw.consume(bf.data);
        List<Datum> windows = sw.getStream().drain();
        // Output to file
        PrintWriter writer = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%s.txt", TABLE_NAMES.get(datasetID)),
                "UTF-8");
        writer.println(String.format("%d %d", windowSize, slideSize));
        for (Datum d : windows) {
            writer.println(String.format("%f %f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        datasetID = Integer.parseInt(args[0]);
        conf = getConf(datasetID);
        binSize = 3600000;
        BatchExperiment exp = new BatchExperiment(conf, WINDOW_RANGES.get(datasetID), binSize, 0.5);
        exp.bruteForce();
    }
}
