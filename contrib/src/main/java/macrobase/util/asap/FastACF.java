package macrobase.util.asap;

import macrobase.datamodel.Datum;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.List;

public class FastACF extends ACF {
    private FastFourierTransformer fftTran = new FastFourierTransformer(DftNormalization.STANDARD);

    public FastACF() {}

    public void evaluate(List<Datum> data) {
        List<Double> metrics = stripDatum(data);
        int n = metrics.size();
        double m = new Metrics().mean(data);
        Double padding = Math.pow(2, 32 - Integer.numberOfLeadingZeros(2 * n - 1));
        double[] values = new double[padding.intValue()];
        // Pad with 0, zero mean data
        for (int i = 0; i < n; i++) { values[i] = metrics.get(i) - m; }
        Complex[] fft = fftTran.transform(values, TransformType.FORWARD);
        for (int i = 0; i < fft.length; i ++) {
            fft[i] = fft[i].multiply(fft[i].conjugate());
        }
        fft = fftTran.transform(fft, TransformType.INVERSE);

        int maxLag = n / 10;
        correlations = new double[maxLag];
        for (int i = 1; i < maxLag; i++) {
            correlations[i] = fft[i].getReal() / fft[0].getReal();
        }
        findPeaks();
    }
}
