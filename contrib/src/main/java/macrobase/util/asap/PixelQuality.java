package macrobase.util.asap;

import java.io.PrintWriter;

public class PixelQuality extends Experiment {
    protected BruteForce gridRaw;

    public PixelQuality(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        grid = new BruteForce(conf, windowRange, binSize, thresh, true);
        grid.name = "Grid1";
        gridRaw = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        gridRaw.name = "GridRaw";
    }

    public static void computeOptimal(PixelQuality exp) throws Exception {
        System.gc();
        exp.grid.findRangeSlide();
        computeWindow(exportConf, exp.grid, true);

        System.gc();
        exp.gridRaw.findRangeSlide();
        computeWindow(exportConf, exp.gridRaw, true);
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_pixelquality.txt",
                        datasetID), "UTF-8");
        plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_pixelquality.txt",
                        datasetID), "UTF-8");

        PixelQuality exp = new PixelQuality(datasetID, resolution, 1);

        computeOptimal(exp);

        result.close();
        plot.close();
    }
}
