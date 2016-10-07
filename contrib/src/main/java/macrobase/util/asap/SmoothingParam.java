package macrobase.util.asap;

import com.sun.tools.javac.util.Pair;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.MacroBaseConf;
import macrobase.conf.MacroBaseDefaults;
import macrobase.datamodel.Datum;
import macrobase.ingest.CSVIngester;

import java.util.ArrayList;
import java.util.List;

public class SmoothingParam {
    public static int OUTLIER_THRESH = 4;
    public MacroBaseConf conf;
    public Metrics metrics;
    public int windowSize = 1;
    public int slideSize = 1;
    public long binSize;
    public TimeDatumStream dataStream;
    public List<Datum> currWindow;
    public List<Pair<Integer, Datum>> extremes = new ArrayList<>();
    public String name;
    protected long windowRange;
    protected double thresh;
    public long runtimeMS;
    public int numPoints;
    public int updateInterval = 0;
    public int pointsChecked = 0;
    private int timeColumn;

    public SmoothingParam(MacroBaseConf conf, long windowRange, long binSize, double thresh) throws Exception {
        this.conf = conf;
        this.windowRange = windowRange;
        this.binSize = binSize;
        this.thresh = thresh;
        // Ingest
        CSVIngester ingester = new CSVIngester(conf);
        List<Datum> data = ingester.getStream().drain();
        timeColumn = conf.getInt(MacroBaseConf.TIME_COLUMN, MacroBaseDefaults.TIME_COLUMN);
        dataStream = new TimeDatumStream(data, timeColumn);
        // Bin
        MacroBaseConf binConf = new MacroBaseConf();
        binConf.set(MacroBaseConf.TIME_WINDOW, binSize);
        binConf.set(MacroBaseConf.TIME_COLUMN, 0);
        binConf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(binConf, binSize);
        List<Datum> window = dataStream.drainDuration(windowRange);
        sw.consume(window);
        sw.shutdown();
        List<Datum> panes = sw.getStream().drain();
        this.currWindow = panes;
        //preFilter();
        metrics = new Metrics(panes);
        numPoints = window.size();
    }

    private void preFilter() {
        double[] zscores = new Metrics().zscores(currWindow);
        for (int i = 0; i < zscores.length; i ++) {
            if (zscores[i] > OUTLIER_THRESH) {
                extremes.add(new Pair<>(i, currWindow.get(i)));
                Datum d;
                if (i > 0) {
                    d = new Datum(currWindow.get(i - 1));
                } else {
                    d = new Datum(currWindow.get(i + 1));
                }
                d.metrics().setEntry(timeColumn, currWindow.get(i).getTime(timeColumn));
                currWindow.set(i, d);
            }
        }
    }

    public void findRangeSlide() throws Exception {};

    public void updateWindow(long interval) throws Exception {};
}
