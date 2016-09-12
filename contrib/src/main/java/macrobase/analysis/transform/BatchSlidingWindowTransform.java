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

public class BatchSlidingWindowTransform extends SlidingWindowTransform {
    private BatchWindowAggregate windowAggregate;

    public BatchSlidingWindowTransform(MacroBaseConf conf, long slideSize) throws ConfigurationException {
        AggregateConf.AggregateType aggregateType = AggregateConf.getAggregateType(conf);
        this.windowAggregate = AggregateConf.constructBatchAggregate(conf, aggregateType);
        this.timeColumn = conf.getInt(MacroBaseConf.TIME_COLUMN, MacroBaseDefaults.TIME_COLUMN);
        this.slideSize = slideSize;
        this.windowSize = conf.getLong(MacroBaseConf.TIME_WINDOW, MacroBaseDefaults.TIME_WINDOW);
        output = new TimeDatumStream(timeColumn);
    }

    private void slideWindow() {
        int i = 0;
        while (i < currWindow.size() && datumInRange(currWindow.get(i), windowStart, 0)) { i++; }
        currWindow.subList(0, i).clear();
    }

    private void aggregateWindow() {
        slideWindow();
        Datum newWindow = windowAggregate.aggregate(currWindow);
        // Interpolate for empty windows
        if (currWindow.size() == 0) {
            newWindow = output.peek();
        }
        newWindow.metrics().setEntry(timeColumn, windowStart + windowSize / 2);
        output.add(newWindow);
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
