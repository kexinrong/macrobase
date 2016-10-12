package macrobase.util.asap;


import java.io.PrintWriter;
import java.util.Arrays;

public class StreamingExperiment extends Experiment {
    public StreamingExperiment(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID, resolution, thresh, true);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_stream.txt",
                        datasetID, "UTF-8"));
        plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_stream.txt",
                        datasetID, "UTF-8"));
    }

    public void run(SmoothingParam s, long duration) throws Exception {
        while (s.dataStream.remaining() > 0) {
            s.findRangeSlide();
            s.updateWindow(duration);
        }
        computeWindow(conf, s);
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        int interval_in_sec = Integer.parseInt(args[2]);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        StreamingExperiment exp = new StreamingExperiment(datasetID, resolution, 0.7);
        exportRaw();
        for (int s : Arrays.asList(1, 2, 5, 10)) {
            grid = new BruteForce(conf, windowRange, binSize, 0.7, s, true);
            exp.run(grid, interval_in_sec * 1000L);
        }
        exp.run(asap, interval_in_sec * 1000L);
        result.close();
        plot.close();
    }
}
