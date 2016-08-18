package macrobase.util.asap;

import macrobase.analysis.transform.IncrementalSlidingWindowTransform;
import macrobase.analysis.transform.aggregate.BatchWindowAvg;
import macrobase.analysis.transform.aggregate.IncrementalWindowAvg;
import macrobase.analysis.transform.aggregate.IncrementalWindowSum;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.ArrayList;
import java.util.List;

public class PeriodSMA {
    public int windowSize;
    public int binSize;
    public int paneSize;
    private IncrementalSlidingWindowTransform sw;
    private MacroBaseConf conf;
    private List<Datum> rawData = new ArrayList<>();
    private List<Datum> paneValues = new ArrayList<>();
    private List<Datum> windowValues = new ArrayList<>();

    public PeriodSMA(MacroBaseConf conf, List<Datum> data, int binSize) throws ConfigurationException {
        this.conf = conf;
        rawData = data;
        this.binSize = binSize;
    }

    public void updatePane(int paneSize) throws ConfigurationException {
        conf.set(MacroBaseConf.TIME_WINDOW, paneSize * binSize);
        sw = new IncrementalSlidingWindowTransform(conf, binSize);
        sw.consume(rawData);
        paneValues = sw.getStream().drain();
        for (Datum d : paneValues) {
            windowValues.add(new Datum(d));
        }
        windowSize = paneSize;
        this.paneSize = paneSize;
    }

    public void updateRange(int range) throws ConfigurationException {
        if (range == windowSize)
            return;
        int w = range / paneSize;
        windowValues.clear();
        BatchWindowAvg avg = new BatchWindowAvg(conf);
        for (int i = 0; i < paneValues.size() - (w - 1) * paneSize; i ++) {
            List<Datum> data = new ArrayList<>();
            for (int j = 0; j < w; j ++) {
                data.add(paneValues.get(i + j * paneSize));
            }
            windowValues.add(avg.aggregate(data));
        }
        this.windowSize = range;
    }

    public void updateSlide(int slide) {
        windowValues.clear();
        for (int i = 0; i < paneValues.size(); i += slide) {
            windowValues.add(new Datum(paneValues.get(i)));
        }
    }

    public List<Datum> getWindows() {
        return windowValues;
    }

}
