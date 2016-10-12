package macrobase.util.asap;
import macrobase.datamodel.Datum;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Metrics {
    private static double ZSCORE_THRESH = 2;
    private Kurtosis kurtosis;
    private Variance variance;
    private Mean mean;
    public double originalKurtosis;

    public Metrics() {
        kurtosis = new Kurtosis();
        variance = new Variance();
        mean = new Mean();
    }

    public Metrics(List<Datum> data) {
        this();
        originalKurtosis = kurtosis(data);
    }

    public void updateKurtosis(List<Datum> data) {
        originalKurtosis = kurtosis(data);
    }

    private double[] diffs(double[] values, boolean isAbs) {
        double[] slopes = new double[values.length - 1];
        for (int i = 1; i < values.length; i++) {
            if (isAbs) {
                slopes[i - 1] = Math.abs(values[i] - values[i - 1]);
            } else {
                slopes[i - 1] = values[i] - values[i - 1];
            }
                    }
        return slopes;
    }

    private double[] stripDatum(List<Datum> data) {
        assert(data.get(0).metrics().getDimension() == 2);
        double[] values = new double[data.size()];
        for (int i = 0; i < data.size(); i ++) {
            values[i] = data.get(i).metrics().getEntry(1);
        }
        return values;
    }

    public double variance(List<Datum> data) {
        return variance.evaluate(stripDatum(data));
    }

    public double mean(List<Datum> data) {
        return mean.evaluate(stripDatum(data));
    }

    public double smoothness(List<Datum> data) {
        double[] slopes = diffs(stripDatum(data), false);
        return Math.sqrt(variance.evaluate(slopes));
    }

    public double kurtosis(List<Datum> data) {
        return kurtosis.evaluate(stripDatum(data));
    }

    public double[] zscores(List<Datum> data) {
        double[] values = stripDatum(data);
        double std = Math.sqrt(variance.evaluate(values));
        double m = mean.evaluate(values);
        double[] zscores = new double[data.size()];
        for (int i = 0; i < values.length; i ++) {
            zscores[i] = (values[i] - m) / std;
        }
        return zscores;
    }

    public List<Pair<Integer, Double>> getOutliers(List<Datum> data) {
        List<Pair<Integer, Double>> outliers = new ArrayList<>();
        double[] zscores = zscores(data);
        for (int i = 0; i < zscores.length; i ++) {
            if (zscores[i] > Metrics.ZSCORE_THRESH || zscores[i] < -Metrics.ZSCORE_THRESH)
                outliers.add(new Pair<>(i, zscores[i]));
        }
        return outliers;
    }
}
