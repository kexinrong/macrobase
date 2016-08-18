package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ASAP extends SmoothingParam {
    private ACF acf;
    private PeriodSMA sma;
    private BatchSlidingWindowTransform swTransform;
    private boolean usePeriod;

    public ASAP(MacroBaseConf conf, int windowRange,
                int binSize, double thresh, boolean usePeriod) throws Exception {
        super(conf, windowRange, binSize, thresh);
        this.usePeriod = usePeriod;
        name = "ASAP";
        if (usePeriod) {
            acf = new ACF(data);
            name += "(no period)";
            sma = new PeriodSMA(conf, data, binSize);
        }
    }

    private int findRange() throws ConfigurationException {
        int period = 1;
        if (usePeriod) {
            period = acf.period;
            sma.updatePane(period);
        }
        int w = 0;
        double recall = 1;
        List<Datum> windows;
        while (recall > thresh && w < windowRange / binSize / 3) {
            w += period;
            if (usePeriod) {
                sma.updateRange(w);
                windows = sma.getWindows();
            } else {
                conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
                swTransform = new BatchSlidingWindowTransform(conf, binSize);
                swTransform.consume(data);
                windows = swTransform.getStream().drain();
            }
            recall = metrics.recall(windows, w, 1);
            System.out.println(String.format("%d %d %f %f", w, 1, recall, metrics.weightedRecall(windows, w, 1)));
        }
        if (recall < thresh && w > period)
            w -= period;
        return w;
    }

    private int findSlide() throws ConfigurationException {
        int s = 1;
        double recall = 1;
        List<Datum> windows;
        while (recall > thresh && s < windowSize) {
            if (usePeriod) {
                sma.updateSlide(s);
                windows = sma.getWindows();
            } else {
                swTransform = new BatchSlidingWindowTransform(conf, binSize);
                swTransform.consume(data);
                windows = swTransform.getStream().drain();
            }
            recall = metrics.recall(windows, windowSize, s);
            System.out.println(String.format("%d %d %f %f", windowSize, s, recall, metrics.weightedRecall(windows, windowSize, s)));
            s ++;
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
