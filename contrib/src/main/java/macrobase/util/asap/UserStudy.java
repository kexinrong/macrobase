package macrobase.util.asap;

import com.google.common.collect.ImmutableMap;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class UserStudy extends Experiment {
    private BruteForce oversmooth;
    private BruteForce PAA;
    private static int[] smooth14 = {65, 67, 40, 126};
    private static int[] smooth27 = {120, 82, 35, 168};
    private static int[] smooth36 = {67, 38, 20, 147};
    private static int[] smooth38 = {33, 52, 26, 128};
    private static int[] smooth42 = {19, 10, 5, 48};
    private static Map<Integer, int[]> smooth_ranges = ImmutableMap.<Integer, int[]>builder()
            .put(14, smooth14).put(27, smooth27).put(36, smooth36).put(38, smooth38)
            .put(42, smooth42).put(0, smooth27).build();

    public UserStudy(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        //grid = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        asap = new ASAP(conf, windowRange, binSize, thresh, true);
        oversmooth = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        oversmooth.name = "Oversmooth";
        PAA = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        PAA.name = "PAA";
        grid = new BruteForce(conf, windowRange, binSize, thresh, true);
        System.out.println(grid.currWindow.size());
    }

    public static void smoothnessPlots(UserStudy exp) throws Exception {
        // ASAP, target resolution 800
        exp.asap.findRangeSlide();
        exp.asap.name = "ASAP";
        computeWindow(exportConf, exp.asap, true);

        conf.set(MacroBaseConf.TIME_WINDOW, exp.asap.windowSize * exp.asap.binSize);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, exp.asap.slideSize * exp.asap.binSize);
        sw.consume(exp.asap.currWindow);
        sw.shutdown();
        List<Datum> windows = sw.getStream().drain();
        double roughness = exp.asap.metrics.smoothness(windows);
        System.out.println(exp.asap.metrics.originalKurtosis);

        exp.grid.windowSize = smooth_ranges.get(datasetID)[0];
        exp.grid.name = "1/2";
        computeWindow(exportConf, exp.grid, true);

        exp.grid.windowSize = smooth_ranges.get(datasetID)[1];
        //exp.grid.findWindowByRoughness(roughness * 4);
        exp.grid.name = "1/4";
        computeWindow(exportConf, exp.grid, true);

        exp.grid.windowSize = smooth_ranges.get(datasetID)[2];
        //exp.grid.findWindowByRoughness(roughness * 8);
        exp.grid.name = "1/8";
        computeWindow(exportConf, exp.grid, true);

        exp.grid.windowSize = smooth_ranges.get(datasetID)[3];
        //exp.grid.findWindowByRoughness(roughness / 2);
        exp.grid.name = "2x";
        computeWindow(exportConf, exp.grid, true);
    }

    public static void kurtosisPlots(UserStudy exp) throws Exception {
        // ASAP, target resolution 800
        exp.asap.findRangeSlide();
        exp.asap.name = "ASAP";
        computeWindow(exportConf, exp.asap, true);

        conf.set(MacroBaseConf.TIME_WINDOW, exp.asap.windowSize * exp.asap.binSize);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, exp.asap.slideSize * exp.asap.binSize);
        sw.consume(exp.asap.currWindow);
        sw.shutdown();
        List<Datum> windows = sw.getStream().drain();
        double kurtosis = exp.asap.metrics.kurtosis(windows);
        System.out.println(exp.asap.metrics.originalKurtosis);

        exp.grid.findWindowByKurtosis(0.5);
        exp.grid.name = "0.5x";
        computeWindow(exportConf, exp.grid, true);

        exp.grid.findWindowByKurtosis(0.75);
        exp.grid.name = "0.75x";
        computeWindow(exportConf, exp.grid, true);

        exp.grid.findWindowByKurtosis(1.5);
        exp.grid.name = "1.5x";
        computeWindow(exportConf, exp.grid, true);

        exp.grid.findWindowByKurtosis(2);
        exp.grid.name = "2x";
        computeWindow(exportConf, exp.grid, true);

    }

    public static void generatePlots(UserStudy exp) throws Exception {
        // ASAP, target resolution 800
        System.gc();
        exp.asap.findRangeSlide();
        computeWindow(exportConf, exp.asap, true);

        // Oversmooth with window size that is 1/4 of the number of points
        exp.oversmooth.windowSize = exp.oversmooth.numPoints / 4;
        computeWindow(exportConf, exp.oversmooth, true);

        // PAA with 100 points
        exp.PAA.windowSize = exp.PAA.numPoints / 100;
        exp.PAA.slideSize = exp.PAA.windowSize;
        computeWindow(exportConf, exp.PAA, true);

        // PAA with 800 points
        exp.PAA.windowSize = exp.PAA.numPoints / 800;
        exp.PAA.slideSize = exp.PAA.windowSize;
        computeWindow(exportConf, exp.PAA, true);
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_us_kurtrange.txt",
                        datasetID), "UTF-8");
        plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_us_kurt.txt",
                        datasetID), "UTF-8");

        UserStudy exp = new UserStudy(datasetID, resolution, 1);
        // Raw series
        plot.println("Original");
        plot.println(String.format("%d %d %d", exp.grid.binSize, 1, 1));
        for (Datum d : exp.grid.currWindow) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }

        //generatePlots(exp);
        //smoothnessPlots(exp);
        kurtosisPlots(exp);

        result.close();
        plot.close();
    }
}
