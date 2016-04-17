package macrobase.analysis.pipeline;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import macrobase.analysis.classify.OutlierClassifier;
import macrobase.analysis.contextualoutlier.Context;
import macrobase.analysis.contextualoutlier.ContextualOutlierDetector;
import macrobase.analysis.result.AnalysisResult;
import macrobase.analysis.result.OutlierClassificationResult;
import macrobase.analysis.summary.BatchSummarizer;
import macrobase.analysis.summary.Summary;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.datamodel.Datum;
import macrobase.ingest.DataIngester;
import macrobase.util.IterUtils;

public class BasicContextualBatchedPipeline extends AbstractPipeline {

    private static final Logger log = LoggerFactory.getLogger(BasicContextualBatchedPipeline.class);

	public BasicContextualBatchedPipeline(MacroBaseConf conf) throws ConfigurationException {
		super(conf);
		// TODO Auto-generated constructor stub
	}

	
	private List<AnalysisResult> allARs;
	private int index = 0;
	
	private void run() throws ConfigurationException, SQLException, IOException{
		allARs = new ArrayList<AnalysisResult>();
		
		//here we invoke contextual outlier detection
		if(!contextualEnabled){
			log.info("Contextual Outlier Detection Not Enabled!");
			return;
		}
	
		long time1 = System.currentTimeMillis();
		
		//load the data
		DataIngester ingester = conf.constructIngester();
		List<Datum> data = Lists.newArrayList(ingester);
    	ContextualOutlierDetector contextualDetector = new ContextualOutlierDetector(conf);
    	Map<Context,OutlierClassifier> context2Outliers = null;
    	
    	long time2 = System.currentTimeMillis();
    	
    	//invoke different contextual outlier detection APIs
		if(contextualAPI.equals("findAllContextualOutliers")){
			context2Outliers = contextualDetector.searchContextualOutliers(data);
	    }else if(contextualAPI.equals("findContextsGivenOutlierPredicate")){
			context2Outliers = contextualDetector.searchContextGivenOutliers(data);
		}
	
		long time3 = System.currentTimeMillis();
		
		long loadMs = time2 - time1;
		long executeMs = time3 - time2;
		
		
		
		//summarize every contextual outliers found
		for(Context context: context2Outliers.keySet()){
			log.info("Context: " + context.print(conf.getEncoder()));
			OutlierClassifier outlierClassifier = context2Outliers.get(context);
			BatchSummarizer summarizer = new BatchSummarizer(conf, outlierClassifier);
	        Summary result = summarizer.next();
	        long summarizeMs = result.getCreationTimeMs();
	        AnalysisResult ar = new AnalysisResult(result.getNumOutliers(),
                    result.getNumInliers(),
                    loadMs,
                    executeMs,
                    summarizeMs,
                    result.getItemsets());
	        allARs.add(ar);
		}
	}
	
	
	
	@Override
	public AnalysisResult next() {
		if(allARs == null){
			try {
				run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(index < allARs.size()){
			AnalysisResult ar = allARs.get(index);
			index++;
			return ar;
		}else{
			return null;
		}
	}

	@Override
	public boolean hasNext() {
		if(allARs == null){
			try {
				run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(index < allARs.size()){
			return true;
		}else{
			return false;
		}
	}

}
