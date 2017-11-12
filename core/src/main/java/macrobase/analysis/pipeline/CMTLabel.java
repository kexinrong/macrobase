package macrobase.analysis.pipeline;

import com.google.common.collect.Lists;
import macrobase.analysis.classify.BatchingPercentileClassifier;
import macrobase.analysis.classify.OutlierClassifier;
import macrobase.analysis.result.OutlierClassificationResult;
import macrobase.analysis.transform.BatchScoreFeatureTransform;
import macrobase.analysis.transform.FeatureTransform;
import macrobase.analysis.transform.LowMetricTransform;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import macrobase.ingest.CSVIngester;

import java.io.PrintWriter;
import java.util.List;


public class CMTLabel {

    public static void genLabel() throws Exception {
        MacroBaseConf conf = new MacroBaseConf();
        conf.set(MacroBaseConf.CSV_INPUT_FILE, "/lfs/1/krong/cmt.csv");
        conf.set(MacroBaseConf.ATTRIBUTES, Lists.newArrayList("build_version",
                "app_version",
                "deviceid",
                "hardware_carrier",
                "state",
                "hardware_model"));
        conf.set(MacroBaseConf.METRICS, Lists.newArrayList(
                "data_count_minutes",
                "data_count_accel_samples",
                "data_count_netloc_samples",
                "data_count_gps_samples",
                "distance_mapmatched_km",
                "distance_gps_km",
                "battery_drain_rate_per_hour"));
        conf.set(MacroBaseConf.LOW_METRIC_TRANSFORM, Lists.newArrayList("data_count_minutes"));
        conf.set(MacroBaseConf.MCD_STOPPING_DELTA, 0.001);
        conf.set(MacroBaseConf.MCD_ALPHA, 0.5);
        conf.set(MacroBaseConf.TRANSFORM_TYPE, MacroBaseConf.TransformType.MCD);
        conf.set(MacroBaseConf.TARGET_PERCENTILE, 0.99);
        CSVIngester ingester = new CSVIngester(conf);

        List<Datum> data = ingester.getStream().drain();
        if(conf.isSet(MacroBaseConf.LOW_METRIC_TRANSFORM)) {
            LowMetricTransform lmt = new LowMetricTransform(conf);
            lmt.consume(data);
            data = lmt.getStream().drain();
        }
        PrintWriter writer = new PrintWriter("cmt-label-complex.txt");

        FeatureTransform ft = new BatchScoreFeatureTransform(conf);
        ft.consume(data);
        OutlierClassifier oc = new BatchingPercentileClassifier(conf);
        oc.consume(ft.getStream().drain());
        List<OutlierClassificationResult> labels = oc.getStream().drain();
        for (OutlierClassificationResult result : labels) {
            writer.println(result.isOutlier());
        }
        writer.close();

    }

    public static void main(String[] args) throws Exception {
        CMTLabel.genLabel();
    }

}

