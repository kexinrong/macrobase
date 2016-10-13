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
    private FastACF acf = new FastACF();
    private BatchSlidingWindowTransform swTransform;
    private double variance;
    private int N;

    public ASAP(MacroBaseConf conf, long windowRange, long binSize, double thresh, boolean isStream) throws Exception {
        super(conf, windowRange, binSize, thresh, isStream);
        acf.evaluate(currWindow);
        name = "ASAP";
    }

    private double estimate_smoothness(int w) {
        return Math.sqrt((metrics.variance(currWindow.subList(0, N - w)) + metrics.variance(currWindow.subList(w, N))
                - 2 * N * variance * acf.correlations[w] / (N - w))) / w;
    }

    @Override
    public void findRangeSlide() throws ConfigurationException {
        Stopwatch sw = Stopwatch.createStarted();
        metrics.updateKurtosis(currWindow);
        variance = metrics.variance(currWindow);
        N = currWindow.size();
        double minObj = Double.MAX_VALUE;
        int maxWindow = (int) (windowRange / binSize / 10);
        int len = acf.peaks.size();
        int j = len - 1;
        int w = acf.peaks.get(j);
        while (j > 0 && acf.correlations[w] < ACF_THRESH) {
            j -= 1;
            w = acf.peaks.get(j);
        }
        for (int i = j; i >= 0; i --) {
            w = acf.peaks.get(i);
            if (estimate_smoothness(w) > minObj) {
                continue;
            }
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            double kurtosis = metrics.kurtosis(windows);
            double smoothness = metrics.smoothness(windows);
            if (kurtosis >= metrics.originalKurtosis && smoothness < minObj) {
                minObj = smoothness;
                windowSize = w;
            }
            pointsChecked += 1;
        }
        // Binary search from max period to largest permitted window size
        int head = acf.peaks.get(j) + 1;
        int tail = maxWindow + 1;
        while (head < tail) {
            w = (head + tail) / 2;
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            double kurtosis = metrics.kurtosis(windows);
            if (kurtosis >= metrics.originalKurtosis ) {
                windowSize = w;
                head = w + 1;
            } else {
                tail = w - 1;
            }
            pointsChecked += 1;
        }


        if (isStream) {
            runtimeMS += sw.elapsed(TimeUnit.MICROSECONDS);
        } else {
            runtimeMS = sw.elapsed(TimeUnit.MICROSECONDS);
        }
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
        List<Datum> panes = sw.getStream().drain();
        List<Datum> expiredPanes = currWindow.subList(0, panes.size());

        currWindow.addAll(panes);
        currWindow.remove(expiredPanes);
        Stopwatch watch = Stopwatch.createStarted();
        acf.evaluate(currWindow);
        runtimeMS += watch.elapsed(TimeUnit.MICROSECONDS);
    }
}
