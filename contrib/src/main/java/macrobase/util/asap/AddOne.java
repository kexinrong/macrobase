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
        gridRaw = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        gridRaw.name = "GridRaw";
        System.gc();
        grid = new BruteForce(conf, windowRange, binSize, thresh, true);
        grid.name = "Grid+pixel";
        System.gc();
        asap = new ASAP(conf, windowRange, binSize, thresh, true);
        asapLazy = new ASAP(conf, windowRange, binSize, thresh, true);
        asapLazy.name = "ASAPLazy";
    }

    public static void run(SmoothingParam s, long duration) throws Exception {
        System.gc();
        int count = 0;
        while (s.dataStream.remaining() > 0) {
            if (!s.name.equals("GridRaw") || count < 20) {
                s.findRangeSlide();
                s.updateWindow(duration);
            } else {
                s.dataStream.drainDuration(duration);
            }
            count += 1;
        }
        result.println(count);
        computeWindow(exportConf, s, false);
    }


    public static void addOne(AddOne exp) throws Exception {
        // Grid search on raw time series
        run(exp.gridRaw, exp.gridRaw.binSize);

        // Grid search on aggregated time series
        run(exp.grid, exp.grid.binSize);

        // ASAP on aggregated time series
        run(exp.asap, exp.asap.binSize);

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
