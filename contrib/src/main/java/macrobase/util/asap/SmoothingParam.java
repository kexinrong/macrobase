package macrobase.util.asap;

import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import macrobase.ingest.CSVIngester;

import java.util.List;

public class SmoothingParam {
    public MacroBaseConf conf;
    public int windowSize;
    public int slideSize;
    public List<Datum> data;
    protected int windowRange;
    protected int binSize;
    protected double thresh;
    protected Metrics metrics;

    public SmoothingParam(MacroBaseConf conf, int windowRange, int binSize, double thresh) throws Exception {
        this.conf = conf;
        this.windowRange = windowRange;
        this.binSize = binSize;
        this.thresh = thresh;
        // Ingest
        CSVIngester ingester = new CSVIngester(conf);
        List<Datum> data = ingester.getStream().drain();
        // Bin
        MacroBaseConf binConf = new MacroBaseConf();
        binConf.set(MacroBaseConf.TIME_WINDOW, binSize);
        binConf.set(MacroBaseConf.TIME_COLUMN, 0);
        binConf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        BatchSlidingWindowTransform sw = new BatchSlidingWindowTransform(binConf, binSize);
        sw.consume(data);
        List<Datum> windows = sw.getStream().drain();
        this.data = windows;
        metrics = new Metrics(windows);
    }

    public void findRangeSlide() throws Exception {};
}
