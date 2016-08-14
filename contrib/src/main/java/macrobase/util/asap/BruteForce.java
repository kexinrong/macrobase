package macrobase.util.asap;

import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;

public class BruteForce extends SmoothingParam {
    private BatchSlidingWindowTransform swTransform;

    public BruteForce(MacroBaseConf conf, int windowRange,
                      int binSize, double thresh) throws Exception {
        super(conf, windowRange, binSize, thresh);
    }

    @Override
    public void findRangeSlide() throws Exception {
        int maxWindow = windowRange / binSize / 3;

        double minVariance = 100;
        for (int w = 1; w < maxWindow; w ++) {
            conf.set(MacroBaseConf.TIME_WINDOW, w);
            for (int s = 1; s < w; s ++) {
                swTransform = new BatchSlidingWindowTransform(conf, s * binSize);
                swTransform.consume(data);
                List<Datum> windows = swTransform.getStream().drain();
                double var = metrics.smoothness(windows, s);
                double kurtosis = metrics.kurtosis(windows);
                double recall = metrics.recall(windows, w, s);
                if (recall > thresh && var < minVariance) {
                    windowSize = w;
                    slideSize = s;
                }
            }
        }
    }
}
