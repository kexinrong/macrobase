package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ASAP extends SmoothingParam {
    private FastACF acf;
    private BatchSlidingWindowTransform swTransform;
    private double variance;
    private int N;

    public ASAP(MacroBaseConf conf, long windowRange, long binSize, double thresh) throws Exception {
        super(conf, windowRange, binSize, thresh);
        acf = new FastACF();
        acf.evaluate(currWindow);
        name = "ASAP";
        N = currWindow.size();
        variance = metrics.variance(currWindow);
    }

    private int findRange() throws ConfigurationException {
        int maxWindowSize = 1;
        pointsChecked = 0;
        double original_kurtosis = metrics.kurtosis(currWindow);
        double minObj = Double.MAX_VALUE;
        for (int i = 0; i < acf.peaks.size(); i ++) {
            int w = acf.peaks.get(acf.peaks.size() - 1 - i);
            if (Math.sqrt(2 * variance) / w * (1 - N * acf.correlations[w] / (N - w)) > minObj) {
                continue;
            }
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            double kurtosis = metrics.kurtosis(windows);
            double smoothness = metrics.smoothness(windows);
            if (kurtosis > original_kurtosis && smoothness < minObj) {
                minObj = smoothness;
                maxWindowSize = w;
            }
            pointsChecked += 1;

        }
        return maxWindowSize;
    }

    public void findRangeSlide() throws ConfigurationException {
        Stopwatch sw = Stopwatch.createStarted();
        windowSize = findRange();
        slideSize = 1;
        runtimeMS += sw.elapsed(TimeUnit.MICROSECONDS);
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

        Stopwatch watch = Stopwatch.createStarted();
        currWindow.addAll(panes);
        currWindow.remove(expiredPanes);
        acf.evaluate(currWindow);
        runtimeMS += watch.elapsed(TimeUnit.MICROSECONDS);
    }
}
