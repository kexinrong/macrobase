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

    public ASAP(MacroBaseConf conf, long windowRange, long binSize, double thresh) throws Exception {
        super(conf, windowRange, binSize, thresh);
        acf = new RunningACF(currWindow);
        name = "ASAP";
    }

    private int findRange() throws ConfigurationException {
        double recall = 1;
        int maxWindowSize = 1;
        pointsChecked = 0;
        int head = 0;
        int tail = acf.peaks.size();
        while (head < tail) {
            int mid = (head + tail) / 2;
            int w = acf.peaks.get(mid);
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            recall = metrics.weightedRecall(windows, w, 1);
            if (recall >= thresh) {
                maxWindowSize = w;
                head = mid + 1;
            } else {
                tail = mid - 1;
            }
            pointsChecked += 1;
        }
        return maxWindowSize;
    }

    public void findRangeSlide() throws ConfigurationException {
        Stopwatch sw = Stopwatch.createStarted();
        windowSize = findRange();
        slideSize = 1;
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
        acf.update(expiredPanes, panes);
        runtimeMS += watch.elapsed(TimeUnit.MILLISECONDS);
    }
}
