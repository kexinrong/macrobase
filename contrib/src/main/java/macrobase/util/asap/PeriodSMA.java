package macrobase.util.asap;

import macrobase.analysis.transform.aggregate.IncrementalWindowSum;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.ArrayList;
import java.util.List;

public class PeriodSMA {
    public int windowSize;
    public int slideSize;
    public int paneSize;
    private IncrementalWindowSum windowSum;
    List<Datum> rawData = new ArrayList<>();
    List<Datum> panesValues = new ArrayList<>();
    List<Datum> windowValues = new ArrayList<>();

    public PeriodSMA(MacroBaseConf conf, List<Datum> data) throws ConfigurationException {
        rawData = data;
        windowSum = new IncrementalWindowSum(conf);
    }


    public void updatePeriod(int period) {
        /*panesValues.clear();
        int i = 0;
        while (i < rawData.size()) {
            int j = i;
            double sum = 0;
            while (j < rawData.size() &&) {
                sum += rawData.get(j);
                j ++;
            }
            panesValues.add(sum / (j - i));
            i = j;
        }
        this.paneSize = paneSize;*/
    }

    public void updateRange(int range) {

    }

    public void updateSlide(int slide) {

    }

}
