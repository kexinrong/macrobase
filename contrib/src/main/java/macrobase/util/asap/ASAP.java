package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ASAP extends SmoothingParam {
    private static int KURT_THRESH = 5;
    private FastACF acf = new FastACF(maxWindow);
    private int N;

    public ASAP(MacroBaseConf conf, long windowRange, long binSize, double thresh, boolean preAggregate) throws Exception {
        super(conf, windowRange, binSize, thresh, preAggregate);
        Stopwatch sw = Stopwatch.createStarted();
        metrics.updateKurtosis(currWindow);
        if (metrics.originalKurtosis < KURT_THRESH) {
            acf.evaluate(currWindow);
        }
        runtimeMS += sw.elapsed(TimeUnit.MICROSECONDS);
        name = "ASAP";
    }

    private void checkLastWindow() throws ConfigurationException {
        if (windowSize == 1) {
            return;
        }
        // Check window from last frame
        pointsChecked += 1;
        List<Datum> windows = transform(windowSize);
        double kurtosis = metrics.kurtosis(windows);
        double smoothness = metrics.smoothness(windows);
        if (kurtosis >= metrics.originalKurtosis) {
            minObj = smoothness;
            return;
        } else {
            windowSize = 1;
        }
    }

    private boolean roughnessGreaterThanOpt(int w) {
        return Math.sqrt(1 - acf.correlations[w]) * windowSize > Math.sqrt(1 - acf.correlations[windowSize]) * w;
    }

    private int updateLB(int lowerBoundWindow, int w) {
        return (int) Math.round(Math.max(w * Math.sqrt((acf.maxACF - 1) / (acf.correlations[w] - 1)), lowerBoundWindow));
    }

    @Override
    public void findRangeSlide() throws ConfigurationException {
        Stopwatch sw = Stopwatch.createStarted();
        N = currWindow.size();

        minObj = Double.MAX_VALUE;
        int lowerBoundWindow = 1;
        checkLastWindow();
        if (windowSize > 1) {
            lowerBoundWindow = updateLB(lowerBoundWindow, windowSize);
        }
        int largestFeasible = -1;
        if (metrics.originalKurtosis < KURT_THRESH) {
            int j = acf.peaks.size() - 1;
            for (int i = j; i >= 0; i --) {
                int w = acf.peaks.get(i);
                if (w == windowSize) {
                    continue;
                } else if (w < lowerBoundWindow || w == 1) {
                    break;
                } else if (roughnessGreaterThanOpt(w)) {
                    continue;
                }
                List<Datum> windows = transform(w);
                double kurtosis = metrics.kurtosis(windows);
                double smoothness = metrics.smoothness(windows);
                if ((kurtosis + 3) >= thresh * (metrics.originalKurtosis + 3)) {
                    if (smoothness < minObj) {
                        minObj = smoothness;
                        windowSize = w;
                    }
                    if (largestFeasible < 0) { largestFeasible = i; }
                    lowerBoundWindow = updateLB(lowerBoundWindow, w);
                }
                pointsChecked += 1;
            }
        }
        // Binary search from max period to largest permitted window size
        int tail = N / maxWindow;
        if (largestFeasible >= 0) {
            if (largestFeasible < acf.peaks.size() - 2) { tail = acf.peaks.get(largestFeasible + 1); }
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
            metrics.updateKurtosis(currWindow);
            if (metrics.originalKurtosis < KURT_THRESH) {
                acf.evaluate(currWindow);
            }
            runtimeMS += watch.elapsed(TimeUnit.MICROSECONDS);
        } else {
            currWindow.addAll(data);
            currWindow.subList(0, data.size()).clear();
        }
    }
}
