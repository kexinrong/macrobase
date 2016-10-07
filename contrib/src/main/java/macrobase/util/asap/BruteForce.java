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
    public int stepSize;
    private BatchSlidingWindowTransform swTransform;
    private FileWriter fw;

    public BruteForce(MacroBaseConf conf, long windowRange,
                      long binSize, double thresh, int stepSize) throws Exception {
        super(conf, windowRange, binSize, thresh);
        this.stepSize = stepSize;
        fw = new FileWriter(new File("param_sweep.csv"));
    }

    @Override
    public void findRangeSlide() throws Exception {
        name = String.format("Grid%d", stepSize);
        Stopwatch sw = Stopwatch.createStarted();
        int maxWindow = (int) (windowRange / binSize / 10);

        double minObj = Double.MAX_VALUE;
        double original_kurtosis = metrics.kurtosis(currWindow);
        pointsChecked = 0;
        for (int w = 1; w < maxWindow; w += stepSize) {
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            double kurtosis = metrics.kurtosis(windows);
            double smoothness = metrics.smoothness(windows);
            if (kurtosis > original_kurtosis && smoothness < minObj) {
                minObj = smoothness;
                windowSize = w;
            }

            pointsChecked += 1;
        }

        runtimeMS = sw.elapsed(TimeUnit.MICROSECONDS);
    }

    public void paramSweep() throws Exception {
        fw.write(String.format("window,recall,std,kurtosis,var\n"));
        RunningACF acf = new RunningACF(currWindow);
        int maxWindow = (int) (windowRange / binSize / 10);
        for (int w = 1; w < maxWindow; w ++) {
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            double recall = metrics.weightedRecall(windows, w, 1);
            double std = Math.sqrt(metrics.smoothness(windows));
            double var = metrics.variance(windows);
            double mean = metrics.mean(windows);
            double kurtosis = metrics.kurtosis(windows);
            fw.write(String.format("%d,%f,%f,%f,%f,%f\n", w, recall, std, kurtosis,
                    var * w * w, mean));
        }
        fw.close();
    }

    public void updateWindow(long interval) throws ConfigurationException {
        List<Datum> data = dataStream.drainDuration(interval);
        if (updateInterval == 0) {
            updateInterval = data.size();
        }
        numPoints += data.size();
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, binSize);
        sw.consume(data);
        sw.shutdown();
        List<Datum> newPanes = sw.getStream().drain();
        currWindow.addAll(newPanes);
        currWindow.remove(currWindow.subList(0, newPanes.size()));
    }
}
