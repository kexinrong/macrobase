package macrobase.analysis.transform;

import macrobase.analysis.pipeline.stream.MBStream;
import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.analysis.transform.aggregate.BatchWindowAggregate;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.conf.MacroBaseDefaults;
import macrobase.datamodel.Datum;
import macrobase.util.asap.TimeDatumStream;

import java.util.List;

public class MinMax extends SlidingWindowTransform {
    private BatchWindowAggregate windowAggregate;

    public MinMax(MacroBaseConf conf) throws ConfigurationException {
        this.timeColumn = conf.getInt(MacroBaseConf.TIME_COLUMN, MacroBaseDefaults.TIME_COLUMN);
        this.windowSize = conf.getLong(MacroBaseConf.TIME_WINDOW, MacroBaseDefaults.TIME_WINDOW);
        this.slideSize = windowSize;
        output = new TimeDatumStream(timeColumn);
    }

    private void slideWindow() {
        int i = 0;
        while (i < currWindow.size() && datumInRange(currWindow.get(i), windowStart, 0)) { i++; }
        currWindow.subList(0, i).clear();
    }

    private void aggregateWindow() {
        slideWindow();
        if (currWindow.size() > 0) {
            Datum min = new Datum(currWindow.get(0));
            Datum max = new Datum(currWindow.get(0));
            for (Datum d : currWindow) {
                if (d.metrics().getEntry(1 - timeColumn) > max.metrics().getEntry(1 - timeColumn)) {
                    max = d;
                }
                if (d.metrics().getEntry(1 - timeColumn) < min.metrics().getEntry(1 - timeColumn)) {
                    min = d;
                }
            }
            if (max.getTime(timeColumn) < min.getTime(timeColumn)) {
                output.add(max);
                output.add(min);
            } else {
                output.add(min);
                output.add(max);
            }
        }

        windowStart += this.slideSize;
    }

    @Override
    public void consume(List<Datum> data) {
        if (data.isEmpty())
            return;
        if (windowStart < 0)
            windowStart = data.get(0).getTime(timeColumn);

        for (Datum d: data) {
            while (!datumInRange(d, windowStart, windowSize)) {
                aggregateWindow();
            }
            currWindow.add(d);
        }
    }

    @Override
    public MBStream<Datum> getStream() { return output; }

    @Override
    public void initialize() throws Exception {}

    @Override
    public void shutdown() { aggregateWindow(); }
}
