package macrobase.util.asap;


import java.io.PrintWriter;

public class RemoveOne extends Experiment {
    private ASAP asapNotLazy;
    private ASAP asapRaw;

    public RemoveOne(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID);

        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        System.gc();
        asapRaw = new ASAP(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        asapRaw.name = "ASAP-pixel";
        System.gc();
        grid = new BruteForce(conf, windowRange, binSize, thresh, true);
        grid.name = "ASAP-AC";
        System.gc();
        asap = new ASAP(conf, windowRange, binSize, thresh, true);
        System.gc();
        asapNotLazy = new ASAP(conf, windowRange, binSize, thresh, true);
        asapNotLazy.name = "ASAP-Lazy";
    }

    public static void run(SmoothingParam s, long duration) throws Exception {
        System.gc();
        while (s.dataStream.remaining() > 0) {
            s.findRangeSlide();
            s.updateWindow(duration);
        }
        computeWindow(exportConf, s, false);
    }


    public static void removeOne(RemoveOne exp) throws Exception {
        long binSize = DataSources.INTERVALS.get(datasetID);
        // ASAP on aggregated time series with on demand update
        run(exp.asap, 24 * 60 * 60 * 1000L);

        // ASAP without on demand update
        run(exp.asapNotLazy, binSize);

        // ASAP without pixel
        run(exp.asapRaw, 24 * 60 * 60 * 1000L);

        // ASAP without AC (grid on aggregated series)
        run(exp.grid, 24 * 60 * 60 * 1000L);
    }


    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_removeone.txt",
                        datasetID), "UTF-8");

        RemoveOne exp = new RemoveOne(datasetID, resolution, 1);
        removeOne(exp);

        result.close();
    }
}
