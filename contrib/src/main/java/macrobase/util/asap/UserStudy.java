package macrobase.util.asap;

import macrobase.datamodel.Datum;

import java.io.PrintWriter;
import java.util.Arrays;

public class UserStudy extends Experiment {
    private BruteForce oversmooth;
    private BruteForce PAA;

    public UserStudy(int datasetID, int resolution, double thresh) throws Exception {
        super(datasetID);
        long windowRange = DataSources.WINDOW_RANGES.get(datasetID);
        long binSize = roundBinSize(windowRange, resolution);
        grid = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        asap = new ASAP(conf, windowRange, binSize, thresh, true);
        oversmooth = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        oversmooth.name = "Oversmooth";
        PAA = new BruteForce(conf, windowRange, DataSources.INTERVALS.get(datasetID), thresh, false);
        PAA.name = "PAA";
    }

    public static void generatePlots(UserStudy exp) throws Exception {
        // ASAP, target resolution 800
        System.gc();
        exp.asap.findRangeSlide();
        computeWindow(exportConf, exp.asap, true);

        // Oversmooth with window size that is 1/4 of the number of points
        exp.oversmooth.windowSize = exp.oversmooth.numPoints / 4;
        computeWindow(exportConf, exp.oversmooth, true);

        // PAA with 100 points
        exp.PAA.windowSize = exp.PAA.numPoints / 100;
        exp.PAA.slideSize = exp.PAA.windowSize;
        computeWindow(exportConf, exp.PAA, true);
    }

    public static void main(String[] args) throws Exception {
        int resolution = Integer.parseInt(args[0]);
        datasetID = Integer.parseInt(args[1]);
        result = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/results/%d_userstudy.txt",
                        datasetID), "UTF-8");
        plot = new PrintWriter(
                String.format("contrib/src/main/java/macrobase/util/asap/plots/%d_userstudy.txt",
                        datasetID), "UTF-8");

        UserStudy exp = new UserStudy(datasetID, resolution, 1);
        // Raw series
        plot.println("Original");
        plot.println(String.format("%d %d %d", exp.grid.binSize, 1, 1));
        for (Datum d : exp.grid.currWindow) {
            plot.println(String.format("%f,%f", d.metrics().getEntry(0), d.metrics().getEntry(1)));
        }

        generatePlots(exp);

        result.close();
        plot.close();
    }
}
