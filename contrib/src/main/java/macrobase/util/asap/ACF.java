package macrobase.util.asap;

import macrobase.datamodel.Datum;

import java.util.ArrayList;
import java.util.List;

public abstract class ACF {
    public int period;
    protected List<Datum> data;
    protected List<Integer> peaks;
    public double[] correlations;

    protected List<Double> stripDatum(List<Datum> datum) {
        List<Double> values = new ArrayList<>();
        for (Datum d : datum) {
            values.add(d.metrics().getEntry(1));
        }
        return values;
    }

    protected void findPeaks() {
        peaks = new ArrayList<>();
        int max_peak = 0;
        if (correlations.length > 1) {
            boolean positive = (correlations[1] > correlations[0]);
            for (int i = 2; i < correlations.length; i++) {
                if (!positive && correlations[i] > correlations[i - 1]) {
                    max_peak = i;
                    positive = !positive;
                } else if (positive && correlations[i] > correlations[max_peak]) {
                    max_peak = i;
                } else if (positive && correlations[i] < correlations[i - 1]) {
                    if (max_peak > 0) { peaks.add(max_peak); }
                    positive = !positive;
                }
            }
        }
        if (peaks.size() == 0) { peaks.add(1); }
    }
}