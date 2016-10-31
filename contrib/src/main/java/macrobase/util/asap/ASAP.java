package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ASAP extends SmoothingParam {
    private static double ACF_THRESH = 0.3;
    private FastACF acf = new FastACF(maxWindow);
    private double variance;
    private int N;

    public ASAP(MacroBaseConf conf, long windowRange, long binSize, double thresh, boolean preAggregate) throws Exception {
        super(conf, windowRange, binSize, thresh, preAggregate);
        Stopwatch sw = Stopwatch.createStarted();
        acf.evaluate(currWindow);
        runtimeMS += sw.elapsed(TimeUnit.MICROSECONDS);
        name = "ASAP";
    }

    private double estimate_smoothness(int w) {
        return Math.sqrt(2 * variance - 2 * N * variance * acf.correlations[w] / (N - w)) / w;
    }

    private int getLowerBoundWindow(double maxACF) throws ConfigurationException {
        if (windowSize == 1) {
            return 1;
        }
        // Check window from last frame
        pointsChecked += 1;
        List<Datum> windows = transform(windowSize);
        double kurtosis = metrics.kurtosis(windows);
        double smoothness = metrics.smoothness(windows);
        if (kurtosis >= metrics.originalKurtosis) {
            minObj = smoothness;
            return (int)Math.round(windowSize * Math.sqrt((maxACF - 1) / (acf.correlations[windowSize] - 1)));
        } else {
            windowSize = 1;
        }
        return 1;
    }

    @Override
    public void findRangeSlide() throws ConfigurationException {
        Stopwatch sw = Stopwatch.createStarted();
        metrics.updateKurtosis(currWindow);
        variance = metrics.variance(currWindow);
        N = currWindow.size();

        minObj = Double.MAX_VALUE;
        int len = acf.peaks.size();
        int j = len - 1;
        int w = acf.peaks.get(j);
        while (j > 0 && acf.correlations[w] < ACF_THRESH) {
            j -= 1;
            w = acf.peaks.get(j);
        }
        double maxACF = 0;
        for (int i = 0; i <= j; i ++) {
            w = acf.peaks.get(i);
            if (w == 1) {
                continue;
            }
            maxACF = Math.max(maxACF, acf.correlations[w]);
        }
        int lowerBoundWindow = getLowerBoundWindow(maxACF);
        int largestFeasible = -1;
        for (int i = j; i >= 0; i --) {
            w = acf.peaks.get(i);
            if (w == windowSize || w < lowerBoundWindow || w == 1) {
                break;
            } else if (estimate_smoothness(w) > minObj) {
                continue;
            }
            List<Datum> windows = transform(w);
            double kurtosis = metrics.kurtosis(windows);
            double smoothness = metrics.smoothness(windows);
            if (kurtosis >= thresh * metrics.originalKurtosis) {
                if (smoothness < minObj) {
                    minObj = smoothness;
                    windowSize = w;
                }
                if (largestFeasible < 0) { largestFeasible = i; }
                lowerBoundWindow = (int)Math.round(Math.max(w * Math.sqrt((maxACF - 1) / (acf.correlations[w] - 1)), lowerBoundWindow));
            }
            pointsChecked += 1;
        }

        // Binary search from max period to largest permitted window size
        int tail = N / maxWindow;
        if (largestFeasible > 0) {
            if (largestFeasible != j && largestFeasible < len - 1) { tail = acf.peaks.get(largestFeasible + 1); }
            lowerBoundWindow = Math.max(lowerBoundWindow, acf.peaks.get(largestFeasible) + 1);
        }
        binarySearch(lowerBoundWindow, tail);

        runtimeMS += sw.elapsed(TimeUnit.MICROSECONDS);
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
            acf.evaluate(currWindow);
            runtimeMS += watch.elapsed(TimeUnit.MICROSECONDS);
        } else {
            currWindow.addAll(data);
            currWindow.subList(0, data.size()).clear();
        }
    }
}
