package macrobase.pipeline;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import macrobase.analysis.pipeline.BasicBatchedPipeline;
import macrobase.analysis.pipeline.BasicContextualBatchedPipeline;
import macrobase.analysis.result.AnalysisResult;
import macrobase.conf.MacroBaseConf;
import macrobase.ingest.CSVIngester;
import macrobase.ingest.result.ColumnValue;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.*;

public class BasicContextualBatchPipelineTest {
    private static final Logger log = LoggerFactory.getLogger(BasicContextualBatchPipelineTest.class);

    /*
        N.B. These tests could use considerable love.
             Right now, they basically just catch changed behavior
             in our core analysis pipelines.
     */

    @Test
    public void testContextualEnabled() throws Exception {
    	 MacroBaseConf conf = new MacroBaseConf()
                 .set(MacroBaseConf.TARGET_PERCENTILE, 0.99) // analysis
                 .set(MacroBaseConf.USE_PERCENTILE, true)
                 .set(MacroBaseConf.MIN_OI_RATIO, .01)
                 .set(MacroBaseConf.MIN_SUPPORT, .01)
                 .set(MacroBaseConf.RANDOM_SEED, 0)
                 .set(MacroBaseConf.ATTRIBUTES, Lists.newArrayList()) // loader
                 .set(MacroBaseConf.LOW_METRICS, Lists.newArrayList())
                 .set(MacroBaseConf.HIGH_METRICS, Lists.newArrayList("A"))
                 .set(MacroBaseConf.AUXILIARY_ATTRIBUTES, "")
                 .set(MacroBaseConf.DATA_LOADER_TYPE, MacroBaseConf.DataIngesterType.CSV_LOADER)
                 .set(MacroBaseConf.CSV_INPUT_FILE, "src/test/resources/data/simpleContextual.csv")
                .set(MacroBaseConf.CONTEXTUAL_ENABLED,false)
                .set(MacroBaseConf.CONTEXTUAL_DOUBLE_ATTRIBUTES, Lists.newArrayList())
                .set(MacroBaseConf.CONTEXTUAL_DISCRETE_ATTRIBUTES, Lists.newArrayList("C1","C2"))
                .set(MacroBaseConf.CONTEXTUAL_DENSECONTEXTTAU, 0.4)
                .set(MacroBaseConf.CONTEXTUAL_NUMINTERVALS, 10)
                .set(MacroBaseConf.CONTEXTUAL_OUTPUT_FILE,"temp.txt");
        conf.loadSystemProperties();
        conf.sanityCheckBatch();

        BasicContextualBatchedPipeline pipeline = new BasicContextualBatchedPipeline(conf);
        assertEquals(pipeline.next(),null);
        assertEquals(pipeline.hasNext(),false);
       
        
    }
    
    @Test
    public void testContextualMADAnalyzer() throws Exception {
    	 MacroBaseConf conf = new MacroBaseConf()
                 .set(MacroBaseConf.TARGET_PERCENTILE, 0.99) // analysis
                 .set(MacroBaseConf.USE_PERCENTILE, true)
                 .set(MacroBaseConf.MIN_OI_RATIO, .01)
                 .set(MacroBaseConf.MIN_SUPPORT, .01)
                 .set(MacroBaseConf.RANDOM_SEED, 0)
                 .set(MacroBaseConf.ATTRIBUTES, Lists.newArrayList()) // loader
                 .set(MacroBaseConf.LOW_METRICS, Lists.newArrayList())
                 .set(MacroBaseConf.HIGH_METRICS, Lists.newArrayList("A"))
                 .set(MacroBaseConf.AUXILIARY_ATTRIBUTES, "")
                 .set(MacroBaseConf.DATA_LOADER_TYPE, MacroBaseConf.DataIngesterType.CSV_LOADER)
                 .set(MacroBaseConf.CSV_INPUT_FILE, "src/test/resources/data/simpleContextual.csv")
                .set(MacroBaseConf.CONTEXTUAL_ENABLED,true)
                .set(MacroBaseConf.CONTEXTUAL_DOUBLE_ATTRIBUTES, Lists.newArrayList())
                .set(MacroBaseConf.CONTEXTUAL_DISCRETE_ATTRIBUTES, Lists.newArrayList("C1","C2"))
                .set(MacroBaseConf.CONTEXTUAL_DENSECONTEXTTAU, 0.4)
                .set(MacroBaseConf.CONTEXTUAL_NUMINTERVALS, 10)
                .set(MacroBaseConf.CONTEXTUAL_OUTPUT_FILE,"temp.txt");
        conf.loadSystemProperties();
        conf.sanityCheckBatch();

        BasicContextualBatchedPipeline pipeline = new BasicContextualBatchedPipeline(conf);
        if(pipeline.hasNext()){
        	AnalysisResult ar = pipeline.next();
        	assertEquals(0, ar.getItemSets().size());
        }

       
     
    }
    @Test
    public void testContextualAPI() throws Exception {
    	 MacroBaseConf conf = new MacroBaseConf()
                 .set(MacroBaseConf.TARGET_PERCENTILE, 0.99) // analysis
                 .set(MacroBaseConf.USE_PERCENTILE, true)
                 .set(MacroBaseConf.MIN_OI_RATIO, .01)
                 .set(MacroBaseConf.MIN_SUPPORT, .01)
                 .set(MacroBaseConf.RANDOM_SEED, 0)
                 .set(MacroBaseConf.ATTRIBUTES, Lists.newArrayList()) // loader
                 .set(MacroBaseConf.LOW_METRICS, Lists.newArrayList())
                 .set(MacroBaseConf.HIGH_METRICS, Lists.newArrayList("A"))
                 .set(MacroBaseConf.AUXILIARY_ATTRIBUTES, "")
                 .set(MacroBaseConf.DATA_LOADER_TYPE, MacroBaseConf.DataIngesterType.CSV_LOADER)
                 .set(MacroBaseConf.CSV_INPUT_FILE, "src/test/resources/data/simpleContextual.csv")
                .set(MacroBaseConf.CONTEXTUAL_ENABLED,true)
                .set(MacroBaseConf.CONTEXTUAL_DOUBLE_ATTRIBUTES, Lists.newArrayList())
                .set(MacroBaseConf.CONTEXTUAL_DISCRETE_ATTRIBUTES, Lists.newArrayList("C1","C2"))
                .set(MacroBaseConf.CONTEXTUAL_DENSECONTEXTTAU, 0.4)
                .set(MacroBaseConf.CONTEXTUAL_NUMINTERVALS, 10)
                .set(MacroBaseConf.CONTEXTUAL_API, "findContextsGivenOutlierPredicate")
                .set(MacroBaseConf.CONTEXTUAL_API_OUTLIER_PREDICATES,"C2 = b2")
                .set(MacroBaseConf.CONTEXTUAL_OUTPUT_FILE,"temp.txt");
        conf.loadSystemProperties();
        conf.sanityCheckBatch();

        BasicContextualBatchedPipeline pipeline = new BasicContextualBatchedPipeline(conf);
        if(pipeline.hasNext()){
        	AnalysisResult ar = pipeline.next();
        	assertEquals(0, ar.getItemSets().size());
        }

       
     
    }
}
