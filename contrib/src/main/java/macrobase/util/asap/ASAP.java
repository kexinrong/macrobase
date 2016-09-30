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
    private double variance;
    private int N;

    public ASAP(MacroBaseConf conf, long windowRange, long binSize, double thresh) throws Exception {
        super(conf, windowRange, binSize, thresh);
        acf = new RunningACF(currWindow);
        name = "ASAP";
        N = currWindow.size();
        variance = metrics.variance(currWindow);
    }

    private double getRecall(int window_size) throws ConfigurationException {
        conf.set(MacroBaseConf.TIME_WINDOW, window_size * binSize);
        swTransform = new BatchSlidingWindowTransform(conf, binSize);
        swTransform.consume(currWindow);
        swTransform.shutdown();
        List<Datum> windows = swTransform.getStream().drain();
        return metrics.weightedRecall(windows, window_size, 1);
    }

    private int findRange() throws ConfigurationException {
        int maxWindowSize = 1;
        pointsChecked = 0;
        int head = 0;
        int tail = acf.peaks.size();
        double[] recalls = new double[acf.peaks.size()];
        while (head < tail) {
            int mid = (head + tail) / 2;
            int w = acf.peaks.get(mid);
            if (recalls[mid] == 0) {
                recalls[mid] = getRecall(w);
                pointsChecked += 1;
            }
            if (mid > 0 && recalls[mid - 1] == 0) {
                recalls[mid - 1] = getRecall(acf.peaks.get(mid - 1));
                pointsChecked += 1;
            }
            if (mid < acf.peaks.size() - 1 && recalls[mid + 1] == 0) {
                recalls[mid + 1] = getRecall(acf.peaks.get(mid + 1));
                pointsChecked += 1;
            }
            if (mid == 0) {
                if (recalls[mid] > recalls[mid + 1]) {
                    maxWindowSize = w;
                } else {
                    maxWindowSize = acf.peaks.get(mid + 1);
                }
                break;
            } else if (mid == acf.peaks.size() - 1) {
                if (recalls[mid] > recalls[mid - 1]) {
                    maxWindowSize = w;
                } else {
                    maxWindowSize = acf.peaks.get(mid - 1);
                }
                break;
            }
            if (recalls[mid] > recalls[mid + 1] && recalls[mid] > recalls[mid - 1]) {
                maxWindowSize = w;
                break;
            } else if (recalls[mid] > recalls[mid + 1] && recalls[mid] < recalls[mid - 1]) {
                tail = mid - 1;
            } else {
                head = mid + 1;
            }
        }
        /*double maxRecall = 0;
        for (int i = 0; i < acf.peaks.size(); i ++) {
            int w = acf.peaks.get(i);
            conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
            swTransform = new BatchSlidingWindowTransform(conf, binSize);
            swTransform.consume(currWindow);
            swTransform.shutdown();
            List<Datum> windows = swTransform.getStream().drain();
            double recall = metrics.weightedRecall(windows, w, 1);
            if (recall >= maxRecall) {
                maxRecall = recall;
                maxWindowSize = w;
            }
            pointsChecked += 1;
        }*/
        if (head == tail) {
            maxWindowSize = acf.peaks.get(head);
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
