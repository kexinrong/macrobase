package macrobase.analysis.transform.aggregate;

import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

public class IncrementalWindowAvg extends IncrementalWindowAggregate {
    private int length = 0;

    public IncrementalWindowAvg(MacroBaseConf conf) throws ConfigurationException {
        super(conf);
    }

    public Datum updateWindow(List<Datum> new_data, List<Datum> old_data) {
        if (new_data.isEmpty() && old_data.isEmpty())
            return currWindow;

        RealVector results;
        int newLength = length + new_data.size() - old_data.size();
        if (currWindow != null) {
            results = new ArrayRealVector(currWindow.metrics());
            if (length != newLength) {
                for (int i = 0; i < dim; i ++) {
                    results.setEntry(i, results.getEntry(i) * length / newLength);
                }
            }

        } else {
            dim = new_data.get(0).metrics().getDimension();
            results = new ArrayRealVector(dim);
        }
        // Add new data
        for (Datum d : new_data) {
            RealVector metrics = d.metrics();
            for (int i = 0; i < dim; i ++) {
                if (timeColumn != null && i == timeColumn)
                    continue;
                results.setEntry(i, results.getEntry(i) + metrics.getEntry(i) / newLength);
            }
        }
        // Remove old data
        for (Datum d : old_data) {
            RealVector metrics = d.metrics();
            for (int i = 0; i < dim; i ++) {
                if (timeColumn != null && i == timeColumn)
                    continue;
                results.setEntry(i, results.getEntry(i) - metrics.getEntry(i) / newLength);
            }
        }
        length = newLength;
        currWindow = new Datum(new ArrayList<>(), results);
        return currWindow;
    }
}
