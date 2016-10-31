package macrobase.util.asap;


import java.io.PrintWriter;

public class StreamingExperiment extends Experiment {
    public StreamingExperiment(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID, resolution, thresh);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_stream.txt",
                        datasetID, "UTF-8"));
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
        StreamingExperiment exp = new StreamingExperiment(datasetID, resolution, 1);

        exp.run(exp.asap, interval_in_sec * 1000L);
        result.close();
    }
}
