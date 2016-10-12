package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ASAP extends SmoothingParam {
    private FastACF acf = new FastACF();
    private BatchSlidingWindowTransform swTransform;
    private double variance;
    private int N;

    public ASAP(MacroBaseConf conf, long windowRange, long binSize, double thresh, boolean isStream) throws Exception {
        super(conf, windowRange, binSize, thresh, isStream);
        acf.evaluate(currWindow);
        name = "ASAP";
        N = currWindow.size();
        variance = metrics.variance(currWindow);
    }

    private double estimate_smoothness(int w) {
        return Math.sqrt((metrics.variance(currWindow.subList(0, N - w)) + metrics.variance(currWindow.subList(w, N))
                - 2 * N * variance * acf.correlations[w] / (N - w))) / w;
    }

    public void findRangeSlide() throws ConfigurationException {
        Stopwatch sw = Stopwatch.createStarted();
        double minObj = Double.MAX_VALUE;
        int len = acf.peaks.size();

        for (int i = 0; i < len; i ++) {
            int w = acf.peaks.get(len - 1 - i);
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
            if (kurtosis > metrics.originalKurtosis && smoothness < minObj) {
                minObj = smoothness;
                windowSize = w;
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
        variance = metrics.variance(currWindow);
        N = currWindow.size();
        metrics.updateKurtosis(currWindow);
        runtimeMS += watch.elapsed(TimeUnit.MICROSECONDS);
    }
}
