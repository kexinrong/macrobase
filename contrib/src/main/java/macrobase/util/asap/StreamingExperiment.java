package macrobase.util.asap;

import macrobase.analysis.transform.aggregate.AggregateConf;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.io.PrintWriter;

public class StreamingExperiment extends Experiment {
    public StreamingExperiment(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID, resolution, thresh);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%s_stream.txt",
                        DataSources.TABLE_NAMES.get(datasetID)), "UTF-8");
        plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%s_stream.txt",
                        DataSources.TABLE_NAMES.get(datasetID)), "UTF-8");
    }

    public void run(SmoothingParam s) throws Exception {
        while (s.dataStream.remaining() > 0) {
            System.out.println(s.dataStream.remaining());
            s.findRangeSlide();
            export(s);
            s.updateWindow(s.binSize);
        }
    }

    public void export(SmoothingParam s) throws Exception {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.TIME_COLUMN, 0);
        conf.set(AggregateConf.AGGREGATE_TYPE, AggregateConf.AggregateType.AVG);
        // Raw series
        plot.println("Original");
        plot.println(String.format("%d %d %d", s.binSize, 1, 1));
        for (Datum d : s.currWindow) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
        // Compute window and output to file
        computeWindow(conf, s);
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        StreamingExperiment exp = new StreamingExperiment(datasetID, resolution, 0.6);
        exp.run(bf);
        exp.run(asapRaw);
        exp.run(asap);
        result.close();
        plot.close();
    }
}
