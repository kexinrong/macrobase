package macrobase.util.asap;

import macrobase.datamodel.Datum;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class RunningACF extends ACF {
    public RunningVar runningVar;
    public LinkedList<Double> data = new LinkedList<>();
    public int length;
    public int maxLag;
    private double[] y0ykSum;
    private double[] y0ykMul;

    public RunningACF(List<Datum> datum) {
        data = new LinkedList<>();
        for (Datum d : datum) {
            data.add(d.metrics().getEntry(1));
        }

        runningVar = new RunningVar(data);
        length = data.size();
        maxLag = length / 3;
        y0ykSum = new double[maxLag];
        y0ykMul = new double[maxLag];
        correlations = new double[maxLag];

        for (int lag = 1; lag < maxLag; lag++) {
            for (int i = 0; i < length - lag; i ++) {
                y0ykSum[lag - 1] += data.get(i + lag) + data.get(i);
                y0ykMul[lag - 1] += data.get(i + lag) * data.get(i);
            }
            correlations[lag] = getCorrelation(lag);
        }
        findPeriod();
    }

    private double getCorrelation(int lag) {
        return (y0ykMul[lag - 1] - runningVar.mean * y0ykSum[lag - 1] +
                (length - lag) * runningVar.mean * runningVar.mean) / length / runningVar.variance;
    }

    public void update(List<Datum> old_datum, List<Datum> new_datum) {
        assert(old_datum.size() == new_datum.size());
        List<Double> old_data = stripDatum(old_datum);
        List<Double> new_data = stripDatum(new_datum);
        runningVar.update(old_data, new_data);

        for (int lag = 1; lag < maxLag; lag ++) {
            for (int i = 0; i < new_data.size(); i++) {
                y0ykSum[lag - 1] += data.get(length - lag + i) + new_data.get(i) -
                        data.get(lag + i) - old_data.get(i);
                y0ykMul[lag - 1] += data.get(length - lag + i) * new_data.get(i) -
                        data.get(lag + i) * old_data.get(i);
            }
            correlations[lag] = getCorrelation(lag);
        }

        for (int i = 0; i < new_data.size(); i ++) { data.remove(); }
        data.addAll(new_data);
        findPeriod();
    }
}


class RunningVar {
    public int length = 0;
    public double variance = 0;
    public double mean = 0;
    private double powerSum = 0;
    private double sum = 0;

    public RunningVar(List<Double> data) {
        update(new ArrayList<>(), data);
    }

    private double powerSum(List<Double> data) {
        double s = 0;
        for (double d: data) { s += d * d; }
        return s;
    }

    private double sum(List<Double> data) {
        double s = 0;
        for (double d: data) { s += d; }
        return s;
    }

    private double avg(double s) { return s / length; }

    public void update(List<Double> old_data, List<Double> new_data) {
        length += new_data.size() - old_data.size();
        sum += sum(new_data) - sum(old_data);
        powerSum += powerSum(new_data) - powerSum(old_data);
        if (length == 0) {
            mean = 0;
            variance = 0;
        } else {
            mean = avg(sum);
            variance = avg(powerSum) - mean * mean;
        }
    }
}

