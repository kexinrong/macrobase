package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import jdk.nashorn.internal.runtime.ECMAException;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.DoubleArray;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BruteForce extends SmoothingParam {
    public int stepSize = 1;
    private FileWriter fw;

    public BruteForce(MacroBaseConf conf, long windowRange,
                      long binSize, double thresh, boolean preAggregate) throws Exception {
        super(conf, windowRange, binSize, thresh, preAggregate);
    }

    @Override
    public void findRangeSlide() throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        metrics.updateKurtosis(currWindow);

        minObj = Double.MAX_VALUE;
        int N = currWindow.size();
        windowSize = 1;
        for (int w = 2; w < N / maxWindow + 1; w += stepSize) {
            List<Datum> windows = transform(w);
            double kurtosis = metrics.kurtosis(windows);
            double smoothness = metrics.smoothness(windows);
            if (kurtosis >= thresh * metrics.originalKurtosis && smoothness < minObj) {
                minObj = smoothness;
                windowSize = w;
            }
            pointsChecked += 1;
        }
        runtimeMS += sw.elapsed(TimeUnit.MICROSECONDS);
    }

    public void paramSweep() throws Exception {
        fw = new FileWriter(new File("param_sweep.csv"));
        fw.write(String.format("window, std, kurtosis, kurtosis * w^4, var * w^2, mean, acf\n"));
        FastACF acf = new FastACF(maxWindow);
        acf.evaluate(currWindow);
        int N = currWindow.size();
        for (int w = 1; w <  N / maxWindow + 1; w ++) {
            List<Datum> windows = transform(w);
            double std = metrics.smoothness(windows);
            double var = metrics.variance(windows);
            double mean = metrics.mean(windows);
            double kurtosis = metrics.kurtosis(windows);
            fw.write(String.format("%d,%f,%f,%f,%f,%f,%f\n", w, std, kurtosis,
                    kurtosis * w * w * w * w, var * w * w, mean, acf.correlations[w]));
        }
        fw.close();
    }

    public void updateWindow(long interval) throws ConfigurationException {
        List<Datum> data = dataStream.drainDuration(interval);
        if (updateInterval == 0) {
            updateInterval = data.size();
        }
        numUpdates += 1;
        numPoints += data.size();
        if (preAggregate) { // Pixel-aware aggregate
            conf.set(MacroBaseConf.TIME_WINDOW, binSize);
            BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, binSize);
            Stopwatch watch = Stopwatch.createStarted();
            sw.consume(data);
            sw.shutdown();
            List<Datum> newPanes = sw.getStream().drain();
            currWindow.addAll(newPanes);
            currWindow.subList(0, newPanes.size()).clear();
            runtimeMS += watch.elapsed(TimeUnit.MICROSECONDS);
        } else {
            currWindow.addAll(data);
            currWindow.subList(0, data.size()).clear();
        }
    }
}
