package macrobase.util.asap;


import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.MinMax;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ASAPPAA extends Experiment {
    private static int resolution;
    private static long binSize;
    private BruteForce PAA;
    private BruteForce minmax;
    private static int N = 1000;
    private static int warmup = 50;
    private static PrintWriter fw;

    public ASAPPAA(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        binSize = roundBinSize(windowRange, resolution);
        PAA = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        minmax = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        fw = new PrintWriter(new FileOutputStream(new File("asap-paa-runtime.txt"), true));
    }

    private static long transform(SmoothingParam s) throws ConfigurationException {
        Stopwatch watch = Stopwatch.createStarted();
        MacroBaseConf binConf = new MacroBaseConf();
        binConf.set(MacroBaseConf.TIME_WINDOW, s.windowSize * s.binSize);
        binConf.set(MacroBaseConf.TIME_COLUMN, 0);
        binConf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(binConf, s.slideSize * s.binSize);
        sw.consume(s.currWindow);
        sw.shutdown();
        List<Datum> panes = sw.getStream().drain();
        long runtimeMS = watch.elapsed(TimeUnit.MICROSECONDS);
        return runtimeMS;
    }

    private static long minmax(SmoothingParam s) throws ConfigurationException {
        Stopwatch watch = Stopwatch.createStarted();
        MacroBaseConf binConf = new MacroBaseConf();
        binConf.set(MacroBaseConf.TIME_WINDOW, s.windowSize * s.binSize);
        binConf.set(MacroBaseConf.TIME_COLUMN, 0);
        binConf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        MinMax mm = new MinMax(binConf);
        mm.consume(s.currWindow);
        mm.shutdown();
        List<Datum> panes = mm.getStream().drain();
        long runtimeMS = watch.elapsed(TimeUnit.MICROSECONDS);
        return runtimeMS;
    }

    public static void ASAP_VS_PAA(ASAPPAA exp) throws Exception {
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);

        double asapTime = 0;
        for (int i = 0; i < N + warmup; i ++) {
            System.gc();
            exp.asap = new ASAP(conf, windowRange, binSize, 1, true);
            exp.asap.findRangeSlide();
            if (i < warmup) { continue; }
            asapTime += exp.asap.runtimeMS * 1.0 / N;
        }

        double paaTime = 0;
        exp.PAA.windowSize = exp.PAA.numPoints / resolution;
        exp.PAA.slideSize = exp.PAA.windowSize;
        for (int i = 0; i < N + warmup; i ++) {
            System.gc();
            if (i < warmup) {continue; }
            paaTime += transform(exp.PAA) * 1.0 / N;
        }

        double minmaxTime = 0;
        exp.minmax.windowSize = exp.minmax.numPoints / resolution;
        exp.minmax.slideSize = exp.minmax.windowSize;
        for (int i = 0; i < N + warmup; i ++) {
            System.gc();
            if (i < warmup) {continue; }
            minmaxTime += minmax(exp.minmax) * 1.0 / N;
        }

        fw.println(String.format("ASAP: %f", asapTime));
        fw.println(String.format("PAA: %f", paaTime));
        fw.println(String.format("MinMax: %f", minmaxTime));
    }


    public static void main(String[] args) throws Exception {
        resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);

        ASAPPAA exp = new ASAPPAA(datasetID, resolution, 1);
        fw.println(String.format("%d, %d", datasetID, resolution));
        ASAP_VS_PAA(exp);

        fw.close();
    }
}
