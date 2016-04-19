package macrobase.analysis.transform;

import macrobase.analysis.stats.BatchTrainScore;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import macrobase.diagnostics.JsonUtils;
import macrobase.diagnostics.MetricsAndDensity;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BatchScoreFeatureTransform extends BatchTransform {
    protected BatchTrainScore batchTrainScore;
    protected MacroBaseConf conf;

    public BatchScoreFeatureTransform(MacroBaseConf conf, Iterator<Datum> input, MacroBaseConf.TransformType transformType)
            throws ConfigurationException {
        super(input);
        this.batchTrainScore = conf.constructTransform(transformType);
        this.conf = conf;
    }

    public BatchScoreFeatureTransform(MacroBaseConf conf, Iterator<Datum> input) throws ConfigurationException {
        this(conf, input, conf.getTransformType());
    }

    @Override
    protected List<Datum> transform(List<Datum> data) {
        batchTrainScore.train(data);
        List<Datum> results = new ArrayList<>(data.size());
        List<MetricsAndDensity> metricsAndDensities = null;
        boolean dumpScores = false;
        String scoreFile = conf.getString(MacroBaseConf.SCORE_DUMP_FILE_CONFIG_PARAM, "");
        if (scoreFile != "") {
            metricsAndDensities = new ArrayList<>(data.size());
            dumpScores = true;
        }
        for(Datum d : data) {
            double score = batchTrainScore.score(d);
            results.add(new Datum(d, batchTrainScore.score(d)));
            if (dumpScores) {
                metricsAndDensities.add(new MetricsAndDensity(d.getMetrics(), score));
            }
        }
        if (dumpScores) {
            try {
                JsonUtils.dumpAsJson(metricsAndDensities, scoreFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return results;
    }
}
