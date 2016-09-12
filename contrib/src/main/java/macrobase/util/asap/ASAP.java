package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ASAP extends SmoothingParam {
    private RunningACF acf;
    private BatchSlidingWindowTransform swTransform;
    private boolean usePeriod;

    public ASAP(MacroBaseConf conf, long windowRange,
                long binSize, double thresh, boolean usePeriod) throws Exception {
        super(conf, windowRange, binSize, thresh);
        this.usePeriod = usePeriod;
        name = "ASAP (no period)";
        if (usePeriod) {
            acf = new RunningACF(currWindow);
            name = "ASAP";
        }
    }

    private int findRange() throws ConfigurationException {
        int period = 1;
        int w = 2;
        if (usePeriod) {
            period = acf.period;
            w = period;
        }
        int maxWindow = (int)(windowRange / binSize / 10);
        double recall = 1;
        double minVariance = Double.MAX_VALUE;
        int maxWindowSize = 1;
        pointsChecked = 0;
        while (w <= maxWindow) {
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            double var = metrics.smoothness(windows, 1);
            recall = metrics.weightedRecall(windows, w, 1);
            if (recall > thresh && var < minVariance) {
                minVariance = var;
                maxWindowSize = w;
            }
            pointsChecked += 1;
            //if (recall == 0)
            //    break;
            w += period;
        }
        return maxWindowSize;
    }

    private int findSlide() throws ConfigurationException {
        int s = 1;
        double recall = 1;
        conf.set(MacroBaseConf.TIME_WINDOW, windowSize * binSize);
        while (recall > thresh && s < windowSize) {
            s ++;
            swTransform = new BatchSlidingWindowTransform(conf, s * binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            recall = metrics.weightedRecall(windows, windowSize, s);
            pointsChecked += 1;
        }
        if (recall < thresh)
            s -= 1;
        return s;
    }

    public void findRangeSlide() throws ConfigurationException {
        Stopwatch sw = Stopwatch.createStarted();
        windowSize = findRange();
        slideSize = findSlide();
        runtimeMS = sw.elapsed(TimeUnit.MILLISECONDS);
    }


    public void updateWindow(long interval) throws ConfigurationException {
        List<Datum> data = dataStream.drainDuration(interval);
        numPoints = data.size();
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(conf, binSize);
        sw.consume(data);
        sw.shutdown();
        List<Datum> panes = sw.getStream().drain();
        List<Datum> expiredPanes = currWindow.subList(0, panes.size());
        currWindow.addAll(panes);
        currWindow.remove(expiredPanes);

        Stopwatch watch = Stopwatch.createStarted();
        if (usePeriod)
            acf.update(expiredPanes, panes);
        runtimeMS += watch.elapsed(TimeUnit.MILLISECONDS);
    }
}
