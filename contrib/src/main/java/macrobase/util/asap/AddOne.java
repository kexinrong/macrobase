package macrobase.util.asap;


import java.io.PrintWriter;

public class AddOne extends Experiment {
    private BruteForce gridRaw;
    private ASAP asapLazy;

    public AddOne(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID);

        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        System.gc();
        gridRaw = new BruteForce(conf, windowRange, -1, thresh, 1);
        gridRaw.name = "GridRaw";
        System.gc();
        grid = new BruteForce(conf, windowRange, binSize, thresh, 1);
        grid.name = "Grid+pixel";
        System.gc();
        asap = new ASAP(conf, windowRange, binSize, thresh);
        asapLazy = new ASAP(conf, windowRange, binSize, thresh);
        asapLazy.name = "ASAPLazy";
    }

    public static void run(SmoothingParam s, long duration) throws Exception {
        System.gc();
        while (s.dataStream.remaining() > 0) {
            s.findRangeSlide();
            s.updateWindow(duration);
        }
        if (s.binSize < 0) {
            s.binSize = DataSources.INTERVALS.get(datasetID);
        }
        computeWindow(exportConf, s, false);
    }


    public static void addOne(AddOne exp) throws Exception {
        long binSize = DataSources.INTERVALS.get(datasetID);
        // Grid search on raw time series
        run(exp.gridRaw, binSize);

        // Grid search on aggregated time series
        run(exp.grid, binSize);

        // ASAP on aggregated time series
        run(exp.asap, binSize);

        // ASAP on aggregated time series with on demand update
        run(exp.asapLazy, 24 * 60 * 60 * 1000L);
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_addone.txt",
                        datasetID), "UTF-8");

        AddOne exp = new AddOne(datasetID, resolution, 1);
        addOne(exp);

        result.close();
    }
}
