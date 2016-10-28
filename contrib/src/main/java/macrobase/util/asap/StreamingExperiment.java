package macrobase.util.asap;


import macrobase.datamodel.Datum;

import java.io.PrintWriter;
import java.util.Arrays;

public class StreamingExperiment extends Experiment {
    public StreamingExperiment(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID, resolution, thresh);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_stream.txt",
                        datasetID, "UTF-8"));
        plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_stream.txt",
                        datasetID, "UTF-8"));
        // Raw series
        plot.println("Original");
        plot.println(String.format("%d %d %d", grid.binSize, 1, 1));
        for (Datum d : grid.currWindow) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }
    }

    public void run(SmoothingParam s, long duration) throws Exception {
        while (s.dataStream.remaining() > 0) {
            s.findRangeSlide();
            s.updateWindow(duration);
        }
        computeWindow(conf, s, false);
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        int interval_in_sec = Integer.parseInt(args[2]);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        StreamingExperiment exp = new StreamingExperiment(datasetID, resolution, 1);
        for (int s : Arrays.asList(1, 2, 5, 10)) {
            exp.grid = new BruteForce(conf, windowRange, binSize, 1, s);
            exp.run(exp.grid, interval_in_sec * 1000L);
        }
        exp.run(exp.asap, interval_in_sec * 1000L);
        result.close();
        plot.close();
    }
}
