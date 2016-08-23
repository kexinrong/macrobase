package macrobase.util.asap;

import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.MacroBaseConf;
import macrobase.conf.MacroBaseDefaults;
import macrobase.datamodel.Datum;
import macrobase.ingest.CSVIngester;

import java.util.List;

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
    public long runtimeMS;
    public int numPoints;
    public int pointsChecked = 0;

    public SmoothingParam(MacroBaseConf conf, long windowRange, long binSize, double thresh) throws Exception {
        this.conf = conf;
        this.windowRange = windowRange;
        this.binSize = binSize;
        this.thresh = thresh;
        // Ingest
        CSVIngester ingester = new CSVIngester(conf);
        List<Datum> data = ingester.getStream().drain();
        dataStream = new TimeDatumStream(data,
                conf.getInt(MacroBaseConf.TIME_COLUMN, MacroBaseDefaults.TIME_COLUMN));
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
        metrics = new Metrics(panes);
        numPoints = window.size();
    }

    public void findRangeSlide() throws Exception {};

    public void updateWindow(long interval) throws Exception {};
}