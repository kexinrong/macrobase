package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.conf.MacroBaseDefaults;
import macrobase.datamodel.Datum;
import macrobase.ingest.CSVIngester;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SmoothingParam {
    public MacroBaseConf conf;
    public Metrics metrics;
    public int windowSize = 1;
    public int slideSize = 1;
    public long binSize;
    public TimeDatumStream dataStream;
    public List<Datum> currWindow;
    public String name;
    protected long windowRange;
    protected double thresh;
    public long runtimeMS = 0;
    public int numPoints;
    public int updateInterval = 0;
    public int pointsChecked = 0;
    public int numUpdates = 0;
    private int timeColumn;
    protected BatchSlidingWindowTransform swTransform;
    protected double minObj;


    public SmoothingParam(MacroBaseConf conf, long windowRange, long binSize,
                          double thresh) throws Exception {
        this.conf = conf;
        this.windowRange = windowRange;
        this.binSize = binSize;
        this.thresh = thresh;
        // Ingest
        CSVIngester ingester = new CSVIngester(conf);
        List<Datum> data = ingester.getStream().drain();
        timeColumn = conf.getInt(MacroBaseConf.TIME_COLUMN, MacroBaseDefaults.TIME_COLUMN);
        dataStream = new TimeDatumStream(data, timeColumn);
        List<Datum> window = dataStream.drainDuration(windowRange);
        // Bin
        if (binSize > 0) {
            MacroBaseConf binConf = new MacroBaseConf();
            binConf.set(MacroBaseConf.TIME_WINDOW, binSize);
            binConf.set(MacroBaseConf.TIME_COLUMN, 0);
            binConf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
            Stopwatch watch = Stopwatch.createStarted();
            BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(binConf, binSize);
            sw.consume(window);
            sw.shutdown();
            List<Datum> panes = sw.getStream().drain();
            runtimeMS = watch.elapsed(TimeUnit.MICROSECONDS);
            this.currWindow = panes;
            metrics = new Metrics(panes);
        } else {
            this.currWindow = window;
            metrics = new Metrics(window);
        }
        numPoints = window.size();
    }

    protected List<Datum> transform(int w) throws ConfigurationException {
        conf.set(MacroBaseConf.TIME_WINDOW, w * binSize);
        swTransform = new BatchSlidingWindowTransform(conf, binSize);
        swTransform.consume(currWindow);
        swTransform.shutdown();
        return swTransform.getStream().drain();
    }

    protected void binarySearch(int head, int tail) throws ConfigurationException  {
        while (head < tail) {
            int w = (head + tail) / 2;
            List<Datum> windows = transform(w);
            double kurtosis = metrics.kurtosis(windows);
            if (kurtosis >= metrics.originalKurtosis) {
                double smoothness = metrics.smoothness(windows);
                if (smoothness < minObj) {
                    windowSize = w;
                    minObj = smoothness;
                }
                head = w + 1;
            } else {
                tail = w - 1;
            }
            pointsChecked += 1;
        }
    }

    public void findRangeSlide() throws Exception {};

    public void updateWindow(long interval) throws Exception {};
}
