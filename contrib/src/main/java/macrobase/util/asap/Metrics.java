package macrobase.util.asap;
import macrobase.datamodel.Datum;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import java.util.ArrayList;
import java.util.List;

public class Metrics {
    private static double ZSCORE_THRESH = 1.5;
    private Kurtosis kurtosis;
    private Variance variance;
    private Mean mean;
    private List<Integer> originalOutliers;

    public Metrics(List<Datum> data) {
        kurtosis = new Kurtosis();
        variance = new Variance();
        mean = new Mean();
        originalOutliers = getOutlierIndices(data);
    }

    private double[] consecutiveSlops(double[] values, int dist) {
        double[] slopes = new double[values.length - 1];
        for (int i = 1; i < values.length; i++) {
            slopes[i - 1] = (values[i] - values[i - 1]) / dist;
        }
        return slopes;
    }

    private double[] deltaOfDelta(double[] values) {
        double[] deltas = new double[values.length - 1];
        for (int i = 1; i < deltas.length; i ++) {
            deltas[i - 1] = values[i] - values[i - 1];
        }
        return deltas;
    }

    private double[] stripDatum(List<Datum> data) {
        assert(data.get(0).metrics().getDimension() == 2);
        double[] values = new double[data.size()];
        for (int i = 0; i < data.size(); i ++) {
            values[i] = data.get(i).metrics().getEntry(1);
        }
        return values;
    }

    public double smoothness(List<Datum> data, int dist) {
        double[] slopes = consecutiveSlops(stripDatum(data), dist);
        double[] deltas = deltaOfDelta(slopes);
        return variance.evaluate(deltas);
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

    public List<Integer> getOutlierIndices(List<Datum> data) {
        List<Integer> outliers = new ArrayList<>();
        double[] zscores = zscores(data);
        for (int i = 0; i < zscores.length; i ++) {
            if (zscores[i] > Metrics.ZSCORE_THRESH || zscores[i] < -Metrics.ZSCORE_THRESH)
                outliers.add(i);
        }
        return outliers;
    }

    public double recall(List<Datum> data, int range, int slide) {
        if (originalOutliers.size() == 0 || data.size() == 0)
            return 0;

        List<Integer> aggregateOutliers = getOutlierIndices(data);
        double preserved = 0;
        for (int i : originalOutliers) {
            for (int j : aggregateOutliers) {
                if (Math.abs(j * slide - i) < range) {
                    preserved += 1;
                    break;
                }
            }
        }
        return preserved / originalOutliers.size();
    }
}
