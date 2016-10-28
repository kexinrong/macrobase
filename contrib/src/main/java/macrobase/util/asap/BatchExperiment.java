package macrobase.util.asap;

import macrobase.datamodel.Datum;

import java.io.PrintWriter;

public class BatchExperiment extends Experiment {
    protected BruteForce grid2;
    protected BruteForce grid10;


    public BatchExperiment(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID, resolution, thresh);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        grid2 = new BruteForce(conf, windowRange, binSize, thresh, 2);
        grid10 = new BruteForce(conf, windowRange, binSize, thresh, 10);
    }

    public static void ASAP_VS_Grid(BatchExperiment exp) throws Exception {
        System.gc();
        exp.grid.findRangeSlide();
        computeWindow(exportConf, exp.grid, true);

        System.gc();
        exp.grid2.findRangeSlide();
        computeWindow(exportConf, exp.grid2, true);

        System.gc();
        exp.grid10.findRangeSlide();
        computeWindow(exportConf, exp.grid10, true);

        System.gc();
        exp.asap.findRangeSlide();
        computeWindow(exportConf, exp.asap, true);

        System.gc();
        exp.bs.findRangeSlide();
        computeWindow(exportConf, exp.bs, true);
    }

    public static void paramSweep(BatchExperiment exp) throws Exception {
        exp.grid.paramSweep();
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
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
