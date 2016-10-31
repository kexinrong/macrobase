package macrobase.util.asap;

import com.google.common.base.Stopwatch;
import jdk.nashorn.internal.runtime.ECMAException;
import macrobase.analysis.transform.BatchSlidingWindowTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.DoubleArray;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BinarySearch extends SmoothingParam {
    public BinarySearch(MacroBaseConf conf, long windowRange,
                      long binSize, double thresh, boolean preAggregate) throws Exception {
        super(conf, windowRange, binSize, thresh, preAggregate);
    }

    @Override
    public void findRangeSlide() throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        metrics.updateKurtosis(currWindow);
        name = String.format("BinarySearch");
        int maxWindow = (int) (windowRange / binSize / 10);

        minObj = Double.MAX_VALUE;
        windowSize = 1;
        binarySearch(1, maxWindow + 1);

        runtimeMS += sw.elapsed(TimeUnit.MICROSECONDS);
    }
}
