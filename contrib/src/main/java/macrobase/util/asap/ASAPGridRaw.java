package macrobase.util.asap;

import java.io.PrintWriter;

public class ASAPGridRaw extends Experiment {

    public ASAPGridRaw(int datasetID, double thresh) throws Exception {
        super(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        asap = new ASAP(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        asap.name = "asapRaw";
        grid = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        grid.name = "GridRaw";
    }

    public static void computeOptimal(ASAPGridRaw exp) throws Exception {
        System.gc();
        exp.grid.findRangeSlide();
        computeWindow(exportConf, exp.grid, false);

        System.gc();
        exp.asap.findRangeSlide();
        computeWindow(exportConf, exp.asap, false);
    }

    public static void main(String[] args) throws Exception {
        datasetID = Integer.parseInt(args[0]);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_pixelquality.txt",
                        datasetID), "UTF-8");
        //plot = new PrintWriter(
        //        String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_pixelquality.txt",
        //               datasetID), "UTF-8");

        ASAPGridRaw exp = new ASAPGridRaw(datasetID, 1);

        computeOptimal(exp);

        result.close();
        //plot.close();
    }
}
