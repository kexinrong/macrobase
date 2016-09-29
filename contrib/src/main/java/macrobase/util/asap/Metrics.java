package macrobase.util.asap;
import macrobase.datamodel.Datum;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Metrics {
    private static double ZSCORE_THRESH = 1.8;
    private Kurtosis kurtosis;
    private Variance variance;
    private Mean mean;
    private List<Pair<Integer, Double>> originalOutliers;
    private double originalOutlyingness;

    public Metrics() {
        kurtosis = new Kurtosis();
        variance = new Variance();
        mean = new Mean();
    }

    public Metrics(List<Datum> data) {
        this();
        originalOutliers = getOutliers(data);
        originalOutlyingness = outlyingness(originalOutliers);
    }

    private double[] consecutiveSlops(double[] values, int dist) {
        double[] slopes = new double[values.length - 1];
        for (int i = 1; i < values.length; i++) {
            slopes[i - 1] = (values[i] - values[i - 1]) / dist;
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

    public double smoothness(List<Datum> data, int dist) {
        double[] slopes = consecutiveSlops(stripDatum(data), dist);
        //double[] slopes = consecutiveSlops(zscores(data), dist);
        return variance.evaluate(slopes);
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

    public double recall(List<Datum> data, int range, int slide) {
        if (originalOutliers.size() == 0 || data.size() == 0)
            return 0;

        List<Pair<Integer, Double>> aggregateOutliers = getOutliers(data);
        double preserved = 0;
        for (Pair<Integer, Double> o : originalOutliers) {
            for (Pair<Integer, Double> a : aggregateOutliers) {
                if (o.getFirst() >= a.getFirst() * slide && o.getFirst() - a.getFirst() * slide < range) {
                    preserved += 1;
                    break;
                }
            }
        }
        return preserved / originalOutliers.size();
    }

    public double outlyingness(List<Pair<Integer, Double>> outliers) {
        double outlyingness = 0;
        for (Pair<Integer, Double> o : outliers) {
            //outlyingness += (Math.abs(o.getSecond()) - ZSCORE_THRESH) * (Math.abs(o.getSecond()) - ZSCORE_THRESH);
            outlyingness += (Math.abs(o.getSecond()) - ZSCORE_THRESH);
        }
        return outlyingness;
    }


    public double weightedRecall(List<Datum> data, int range, int slide) {
        if (originalOutliers.size() == 0 || data.size() == 0)
            return 0;

        /*double preservedWeights = 0;
        double totalWeights = 0;
        List<Pair<Integer, Double>> aggregateOutliers = getOutliers(data);
        for (Pair<Integer, Double> o : originalOutliers) {
            totalWeights += (Math.abs(o.getSecond()) - ZSCORE_THRESH) * (Math.abs(o.getSecond()) - ZSCORE_THRESH);
            for (Pair<Integer, Double> a : aggregateOutliers) {
                if (o.getFirst() >= a.getFirst() * slide && o.getFirst() - a.getFirst() * slide < range) {
                    preservedWeights += (Math.abs(a.getSecond()) - ZSCORE_THRESH) * (Math.abs(a.getSecond()) - ZSCORE_THRESH);
                    break;
                }
            }
        }*/
        List<Pair<Integer, Double>> aggregateOutliers = getOutliers(data);
        double preservedOutlyingness = outlyingness(aggregateOutliers);

        return preservedOutlyingness / originalOutlyingness;
    }

    public double retainedOutlyingness(List<Datum> data, int range, int slide) {
        if (originalOutliers.size() == 0 || data.size() == 0)
            return 0;

        double deltas = 0;
        double[] zscores = zscores(data);
        int j = 0;
        int ts = range / 2;
        for (int i = 0; i < originalOutliers.size(); i ++) {
            int originalTS = originalOutliers.get(i).getFirst();
            double z_i = originalOutliers.get(i).getSecond();
            while (j < data.size() && ts < originalTS) {
                j += 1;
                ts += slide;
            }
            if (j == 0 || j == zscores.length || ts == originalTS) {
                if (j == zscores.length) {
                    deltas += z_i - zscores[j - 1];
                } else {
                    deltas += z_i - zscores[j];
                }
            } else {
                double new_z_i = zscores[j - 1] + (zscores[j] - zscores[j - 1]) *
                        ((originalTS - ts + slide) / slide);
                deltas += z_i - new_z_i;
            }
        }
        return deltas / originalOutliers.size();
    }
}
