package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BruteForce extends SmoothingParam {
    private BatchSlidingWindowTransform swTransform;

    public BruteForce(MacroBaseConf conf, long windowRange,
                      int binSize, double thresh) throws Exception {
        super(conf, windowRange, binSize, thresh);
        name = "Brute Force";
    }

    @Override
    public void findRangeSlide() throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        int maxWindow = (int)(windowRange / binSize / 3);

        double minVariance = 100;
        for (int w = 1; w < maxWindow; w ++) {
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            for (int s = 1; s < w; s ++) {
                swTransform = new BatchSlidingWindowTransform(conf, s * binSize);
                swTransform.consume(currWindow);
                List<Datum> windows = swTransform.getStream().drain();
                double var = metrics.smoothness(windows, s);
                double recall = metrics.recall(windows, w, s);
                if (recall > thresh && var < minVariance) {
                    minVariance = var;
                    windowSize = w;
                    slideSize = s;
                }
                pointsChecked += 1;
            }
        }
        runtimeMS = sw.elapsed(TimeUnit.MILLISECONDS);
    }
}
