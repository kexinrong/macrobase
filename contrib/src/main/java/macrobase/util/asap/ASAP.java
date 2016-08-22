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
                int binSize, double thresh, boolean usePeriod) throws Exception {
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
        if (usePeriod) {
            period = acf.period;
        }
        int w = period;
        int maxWindow = (int)(windowRange / binSize / 3);
        double recall = 1;
        List<Datum> windows;
        while (w <= maxWindow) {
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            windows = swTransform.getStream().drain();
            recall = metrics.recall(windows, w, 1);
            pointsChecked += 1;
            w += period;
        }
        if ((recall < thresh || w > maxWindow) && w > period)
            w -= period;
        return w;
    }

    private int findSlide() throws ConfigurationException {
        int s = 1;
        double recall = 1;
        List<Datum> windows;
        while (recall > thresh && s <= windowSize) {
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            windows = swTransform.getStream().drain();
            recall = metrics.recall(windows, windowSize, s);
            s ++;
            pointsChecked += 1;
        }
        if (recall < thresh && s > 1)
            s -= 1;
        return s;
    }

    public void findRangeSlide() throws ConfigurationException {
        Stopwatch sw = Stopwatch.createStarted();
        windowSize = findRange();
        slideSize = findSlide();
        runtimeMS = sw.elapsed(TimeUnit.MILLISECONDS);
    }
}
