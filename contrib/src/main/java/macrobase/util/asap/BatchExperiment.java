package macrobase.util.asap;

import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import macrobase.util.Experiment;

import java.io.PrintWriter;

public class BatchExperiment extends Experiment {
    public BatchExperiment(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID, resolution, thresh);
    }

    public void run() throws Exception {
        bf.findRangeSlide();
        asapRaw.findRangeSlide();
        asap.findRangeSlide();
    }

    public void export() throws Exception {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.TIME_COLUMN, 0);
        conf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);

        PrintWriter result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%s_batch.txt",
                        DataSources.TABLE_NAMES.get(datasetID)), "UTF-8");
        PrintWriter plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%s_batch.txt",
                        DataSources.TABLE_NAMES.get(datasetID)), "UTF-8");
        // Raw series
        plot.println("Original");
        plot.println(String.format("%d %d %d", bf.binSize, 1, 1));
        for (Datum d : bf.currWindow) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
        // Compute window and output to file
        computeWindow(conf, bf, result, plot);
        computeWindow(conf, asap, result, plot);
        computeWindow(conf, asapRaw, result, plot);

        result.close();
        plot.close();
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        BatchExperiment exp = new BatchExperiment(datasetID, resolution, 0.5);
        exp.run();
        exp.export();
    }
}
