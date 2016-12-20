package macrobase.analysis.transform;

import macrobase.analysis.pipeline.operator.MBGroupBy;
import macrobase.analysis.pipeline.stream.MBStream;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;

import java.util.ArrayList;
import java.util.List;

public class LinearMetricNormalizerPerGroup extends FeatureTransform {
    private int targetDim;
    private List<Integer> groupByIndices;

    private MBStream<Datum> output = new MBStream<>();


    public LinearMetricNormalizerPerGroup(
            MacroBaseConf conf, int targetDim, List<String> groupByAttrs) throws ConfigurationException {
        List<String> attrs = conf.getStringList(MacroBaseConf.ATTRIBUTES);
        groupByIndices = new ArrayList<>();
        for(String a : groupByAttrs) {
            groupByIndices.add(attrs.indexOf(a));
        }

        this.targetDim = targetDim;
    }

    @Override
    public void consume(List<Datum> data) throws Exception {
        FeatureTransform normalizer = new LinearMetricNormalizer(targetDim);

        MBGroupBy gb = new MBGroupBy(groupByIndices, () -> normalizer);
        gb.consume(data);
        output = gb.getStream();
    }

    @Override
    public MBStream<Datum> getStream() throws Exception  {
        return output;
    }

    @Override
    public void initialize() throws Exception {

    }

    @Override
    public void shutdown() throws Exception {

    }
}
