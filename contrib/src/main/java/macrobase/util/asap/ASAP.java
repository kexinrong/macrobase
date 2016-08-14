package macrobase.util.asap;

import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.List;

public class ASAP extends SmoothingParam {
    private ACF acf;
    private PeriodSMA sma;

    public ASAP(MacroBaseConf conf, int windowRange,
                int binSize, double thresh) throws Exception {
        super(conf, windowRange, binSize, thresh);
        //acf = new ACF(data);
        //sma = new PeriodSMA(conf, data);
    }

    private int findRange() {
        int period = acf.period;
        int r = 0;
        int s = binSize;
        while (r < windowRange / 3) {
            r += period;
            double kurtosis = metrics.kurtosis(data);
        }
        return 1;
    }

    private int findSlide() {
        return 1;
    }

    public void findRangeSlide() {
        windowSize = findRange();
        slideSize = findSlide();
    }
}
