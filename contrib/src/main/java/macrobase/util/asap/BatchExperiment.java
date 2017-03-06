package macrobase.util.asap;

import macrobase.datamodel.Datum;

import java.io.PrintWriter;
import java.util.Arrays;

public class BatchExperiment extends Experiment {
    private static int resolution;

    public BatchExperiment(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID, resolution, thresh);
    }

    public static void ASAP_VS_Grid(BatchExperiment exp) throws Exception {
        conf = getConf(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        /*for (int s : Arrays.asList(10, 2, 1)) {
            System.gc();
            BruteForce bf = new BruteForce(conf, windowRange, binSize, 1, true);
            bf.stepSize = s;
            bf.name = String.format("Grid%d", s);
            bf.runtimeMS = 0;
            bf.findRangeSlide();
            computeWindow(exportConf, bf, true);
        }*/
        BruteForce bf = new BruteForce(conf, windowRange, binSize, 1, true);
        bf.name = "Oversmooth";
        System.out.println(binSize);
        bf.windowSize = 56 * 5;
        computeWindow(exportConf, bf, true);

        System.gc();
        exp.asap.findRangeSlide();
        exp.asap.windowSize = 56;
        computeWindow(exportConf, exp.asap, true);

        /*System.gc();
        exp.bs.findRangeSlide();
        computeWindow(exportConf, exp.bs, true);*/
    }

    public static void paramSweep(BatchExperiment exp) throws Exception {
        exp.grid.paramSweep();
    }

    public static void main(String[] args) throws Exception {
        resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_batch.txt",
                        datasetID), "UTF-8");
        plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_batch.txt",
                        datasetID), "UTF-8");

        BatchExperiment exp = new BatchExperiment(datasetID, resolution, 1);
        // Raw series
        plot.println("Original");
        plot.println(String.format("%d %d %d", exp.grid.binSize, 1, 1));
        for (Datum d : exp.grid.currWindow) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }

        ASAP_VS_Grid(exp);
        //paramSweep(exp);

        result.close();
        plot.close();
    }
}
