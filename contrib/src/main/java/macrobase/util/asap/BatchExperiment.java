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

    public static void ASAP_VS_Grid() throws Exception {
        asap.findRangeSlide();
        computeWindow(exportConf, asap);
        for (int s : Arrays.asList(1, 2, 5, 10)) {
            grid.stepSize = s;
            grid.findRangeSlide();
            computeWindow(exportConf, grid);
        }
    }
    public static void runPeaks() throws Exception {
        FastACF acf = new FastACF();
        acf.evaluate(asap.currWindow);
        for (int i = 0; i < acf.peaks.size(); i ++) {
            asap.windowSize = acf.peaks.get(i);
            asap.name = String.format("Manual%d", asap.windowSize);
            computeWindow(exportConf, asap);
        }
    }

    public static void paramSweep() throws Exception {
        grid.paramSweep();
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        BatchExperiment exp = new BatchExperiment(datasetID, resolution, 0.95);
        exportRaw();

        ASAP_VS_Grid();
        //runPeaks();
        //paramSweep();

        result.close();
        plot.close();
    }
}
