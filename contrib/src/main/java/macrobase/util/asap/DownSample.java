package macrobase.util.asap;

import com.google.common.collect.ImmutableMap;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DownSample extends Experiment {

    private static Map<Integer, Integer> GRID_RAW = ImmutableMap.<Integer, Integer>builder()
            .put(19, 1)
            .put(42, 1)
            .put(22, 703)
            .put(24, 2015)
            .put(36, 423)
            .put(15, 4896)
            .build();

    private static Map<Integer, Integer> ASAP_RAW = ImmutableMap.<Integer, Integer>builder()
            .put(19, 1)
            .put(42, 1)
            .put(22, 702)
            .put(24, 2009)
            .put(36, 207)
            .put(15, 4896)
            .build();

    //private static ArrayList<Integer> Datasets = new ArrayList<Integer>(Arrays.asList(42, 15, 19, 22, 24, 36));
    private static ArrayList<Integer> Datasets = new ArrayList<Integer>(Arrays.asList(36));
    private static ArrayList<Integer> resolutions = new ArrayList<Integer>(
            Arrays.asList(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000));
    //private static ArrayList<Integer> resolutions = new ArrayList<Integer>(
    //        Arrays.asList(2500));


    public DownSample(int datasetID, double thresh) throws Exception {
        super(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        grid = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        grid.name = "GridRaw";
        //grid.binSize = ;
        System.out.println(grid.currWindow.size());
    }

    private static double getRoughness(SmoothingParam s, int ratio) throws ConfigurationException {
        // SMA
        conf.set(MacroBaseConf.TIME_WINDOW, s.windowSize * s.binSize);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, s.slideSize * s.binSize);
        sw.consume(s.currWindow);
        sw.shutdown();
        List<Datum> windows = sw.getStream().drain();
        // PAA downsample according to ratio
        conf.set(MacroBaseConf.TIME_WINDOW, ratio * s.binSize);
        sw = new BatchSlidingWindowTransform(conf, ratio * s.binSize);
        sw.consume(windows);
        sw.shutdown();
        List<Datum> downSampled = sw.getStream().drain();
        //List<Datum> downSampled = new ArrayList<>();
        //for (int i = 0; i < windows.size() / ratio; i ++)
        //    downSampled.add(windows.get(i * ratio));
        if (true) {
            plot.println(s.name);
            plot.println(String.format("%d %d %d", s.binSize, ratio * s.binSize, ratio * s.binSize));
            for (Datum d : downSampled) {
                plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
            }
        }
        System.out.println(s.windowSize);
        System.out.println(s.slideSize);
        System.out.println(s.binSize);
        System.out.println(downSampled.size());
        System.out.println(s.metrics.kurtosis(downSampled));
        return s.metrics.smoothness(downSampled);
    }

    public static void downSample(DownSample exp, int datasetID) throws Exception {
        for (int resolution : resolutions) {
            // Grid Raw
            //int ratio = (exp.grid.currWindow.size() - GRID_RAW.get(datasetID)) / resolution;
            exp.grid.binSize = DataSources.INTERVALS.get(datasetID);
            int ratio = (exp.grid.currWindow.size() - GRID_RAW.get(datasetID)) / resolution;
            exp.grid.name = String.format("GridRaw%d", resolution);
            exp.grid.windowSize = GRID_RAW.get(datasetID);
            exp.grid.slideSize = 1;
            double roughness = getRoughness(exp.grid, ratio);
            result.println(resolution);
            result.println("GridRaw");
            result.println(String.format("var: %f", roughness));

            // ASAP Raw
            ratio = (exp.grid.currWindow.size() - ASAP_RAW.get(datasetID)) / resolution;
            exp.grid.name = String.format("asapRaw%d", resolution);
            exp.grid.windowSize = ASAP_RAW.get(datasetID);
            roughness = getRoughness(exp.grid, ratio);
            result.println("asapRaw");
            result.println(String.format("var: %f", roughness));
        }
    }

    public static void main(String[] args) throws Exception {
        for (int d : Datasets) {
            System.out.println(d);
            result = new PrintWriter(
                    String.format("contrib/src/main/java/macrobase/util/asap/results/%d_pixelcomp.txt", d), "UTF-8");
            plot = new PrintWriter(
                    String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_pixelcomp.txt", d), "UTF-8");
            DownSample exp = new DownSample(d, 1);
            downSample(exp, d);
            result.close();
            plot.close();
        }
    }
}
