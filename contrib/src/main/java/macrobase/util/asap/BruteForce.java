package macrobase.util.asap;

import com.google.common.base.Stopwatch;
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

    public BruteForce(MacroBaseConf conf, long windowRange,
                      long binSize, double thresh, int stepSize) throws Exception {
        super(conf, windowRange, binSize, thresh);
        this.stepSize = stepSize;
    }

    @Override
    public void findRangeSlide() throws Exception {
        name = String.format("Grid%d", stepSize);
        Stopwatch sw = Stopwatch.createStarted();
        int maxWindow = (int) (windowRange / binSize / 10);

        double minVar = Double.MAX_VALUE;
        pointsChecked = 0;
        if (stepSize > 0) {
            for (int w = 1; w < maxWindow; w += stepSize) {
                conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
                swTransform = new BatchSlidingWindowTransform(conf, binSize);
                swTransform.consume(currWindow);
                swTransform.shutdown();
                List<Datum> windows = swTransform.getStream().drain();
                double recall = metrics.weightedRecall(windows, w, 1);
                double var = metrics.smoothness(windows, 1);
                if (recall > thresh && var < minVar) {
                    minVar = var;
                    windowSize = w;
                }
                pointsChecked += 1;
            }
        } else {
            name = "BinarySearch";
            int head = 1;
            int tail = maxWindow + 1;
            while (head < tail) {
                int w = (head + tail) / 2;
                conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
                swTransform = new BatchSlidingWindowTransform(conf, binSize);
                swTransform.consume(currWindow);
                swTransform.shutdown();
                List<Datum> windows = swTransform.getStream().drain();
                double recall = metrics.weightedRecall(windows, w, 1);
                if (recall >= thresh ) {
                        windowSize = w;
                    head = w + 1;
                } else {
                    tail = w - 1;
                }
                pointsChecked += 1;
            }
        }

        runtimeMS = sw.elapsed(TimeUnit.MILLISECONDS);
    }

    public void updateWindow(long interval) throws ConfigurationException {
        List<Datum> data = dataStream.drainDuration(interval);
        numPoints = data.size();
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, binSize);
        sw.consume(data);
        sw.shutdown();
        List<Datum> newPanes = sw.getStream().drain();
        currWindow.addAll(newPanes);
        currWindow.remove(currWindow.subList(0, newPanes.size()));
    }
}
