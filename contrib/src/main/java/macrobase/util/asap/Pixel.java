package macrobase.util.asap;


import java.io.PrintWriter;

public class Pixel extends Experiment {
    private BruteForce gridRaw;
    private ASAP asapRaw;

    public Pixel(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID);

        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        System.gc();
        gridRaw = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        gridRaw.name = "GridRaw";
        System.gc();
        grid = new BruteForce(conf, windowRange, binSize, thresh, true);
        grid.name = "Grid";
        System.gc();
        asap = new ASAP(conf, windowRange, binSize, thresh, true);
        System.gc();
        asapRaw = new ASAP(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        asapRaw.name = "asapRaw";
    }

    public static void run(SmoothingParam s) throws Exception {
        System.gc();
        s.findRangeSlide();
        computeWindow(exportConf, s, false);
    }


    public static void pixel(Pixel exp) throws Exception {
        // Grid search on raw time series
        run(exp.gridRaw);

        // Grid search on aggregated time series
        run(exp.grid);

        // ASAP on raw time series
        run(exp.asapRaw);

        // ASAP on aggregated time series
        run(exp.asap);
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_pixel.txt",
                        datasetID), "UTF-8");

        Pixel exp = new Pixel(datasetID, resolution, 1);
        pixel(exp);

        result.close();
    }
}
