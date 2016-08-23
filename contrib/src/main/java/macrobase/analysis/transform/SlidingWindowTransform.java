package macrobase.analysis.transform;

import macrobase.analysis.pipeline.stream.MBStream;
import macrobase.datamodel.Datum;
import macrobase.util.asap.TimeDatumStream;

import java.util.ArrayList;
import java.util.List;

public abstract class SlidingWindowTransform extends FeatureTransform {
    protected long windowSize;
    protected long slideSize;
    protected int timeColumn;
    protected long windowStart = -1;

    protected TimeDatumStream output;
    protected List<Datum> currWindow = new ArrayList<>();

    protected boolean datumInRange(Datum d, long start, long size) {
        return d.getTime(timeColumn) - start < size;
    }
}
