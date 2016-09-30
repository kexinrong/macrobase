package macrobase.util.asap;

import java.io.PrintWriter;
import java.util.Arrays;

public class BatchExperiment extends Experiment {

    public BatchExperiment(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID, resolution, thresh);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_batch.txt",
                        datasetID), "UTF-8");
        plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_batch.txt",
                        datasetID), "UTF-8");
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        BatchExperiment exp = new BatchExperiment(datasetID, resolution, 0.7);
        exportRaw();
        asap.findRangeSlide();
        computeWindow(exportConf, asap);
        for (int s : Arrays.asList(1, 2, 5, 10)) {
            grid.stepSize = s;
            grid.findRangeSlide();
            computeWindow(exportConf, grid);
        }
        result.close();
        plot.close();
    }
}
