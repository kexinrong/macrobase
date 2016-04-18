package macrobase.analysis.contextualoutlier;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import macrobase.analysis.classify.OutlierClassifier;
import macrobase.analysis.classify.StaticThresholdClassifier;
import macrobase.analysis.result.OutlierClassificationResult;
import macrobase.analysis.stats.BatchTrainScore;
import macrobase.analysis.summary.itemset.FPGrowthEmerging;
import macrobase.analysis.summary.itemset.result.ItemsetResult;
import macrobase.analysis.summary.result.DatumWithScore;
import macrobase.analysis.transform.BatchScoreFeatureTransform;
import macrobase.analysis.transform.FeatureTransform;
import macrobase.conf.ConfigurationException;
import macrobase.conf.MacroBaseConf;
import macrobase.conf.MacroBaseDefaults;
import macrobase.datamodel.Datum;
import macrobase.ingest.DatumEncoder;
import macrobase.util.IterUtils;

public class ContextualOutlierDetector{
    private static final Logger log = LoggerFactory.getLogger(ContextualOutlierDetector.class);

    
    
    private MacroBaseConf conf;
    private List<String> contextualDiscreteAttributes;
    private List<String> contextualDoubleAttributes;
    private int totalContextualDimensions;
    
    
    Context globalContext;
    
    
    private double denseContextTau;
    private int numIntervals;
    private int maxPredicates;
    private ContextPruningOptions contextPruningOptions;
    private DatumEncoder encoder;
    //This is the outliers detected for every dense context
    //could've stored Context,OutlierDetector.BatchResult, but waste a lot of memory
    private Map<Context,OutlierClassifier> context2OutlierClassifier = new HashMap<Context,OutlierClassifier>();
    
    private PrintWriter contextualOut;
    
    public ContextualOutlierDetector(MacroBaseConf conf){
    	
    	this.conf = conf;
    	this.contextualDiscreteAttributes = conf.getStringList(MacroBaseConf.CONTEXTUAL_DISCRETE_ATTRIBUTES,MacroBaseDefaults.CONTEXTUAL_DISCRETE_ATTRIBUTES);
    	this.contextualDoubleAttributes = conf.getStringList(MacroBaseConf.CONTEXTUAL_DOUBLE_ATTRIBUTES,MacroBaseDefaults.CONTEXTUAL_DOUBLE_ATTRIBUTES);
    	this.denseContextTau = conf.getDouble(MacroBaseConf.CONTEXTUAL_DENSECONTEXTTAU, MacroBaseDefaults.CONTEXTUAL_DENSECONTEXTTAU);
    	this.numIntervals = conf.getInt(MacroBaseConf.CONTEXTUAL_NUMINTERVALS, MacroBaseDefaults.CONTEXTUAL_NUMINTERVALS);
    	this.maxPredicates = conf.getInt(MacroBaseConf.CONTEXTUAL_MAX_PREDICATES,MacroBaseDefaults.CONTEXTUAL_MAX_PREDICATES);
    	this.contextPruningOptions = new ContextPruningOptions(conf.getString(MacroBaseConf.CONTEXTUAL_PRUNING,MacroBaseDefaults.CONTEXTUAL_PRUNING));
    	this.totalContextualDimensions = contextualDiscreteAttributes.size() + contextualDoubleAttributes.size();
    	this.encoder = conf.getEncoder();
    	
    	String contextualOutputFile = conf.getString(MacroBaseConf.CONTEXTUAL_OUTPUT_FILE,MacroBaseDefaults.CONTEXTUAL_OUTPUT_FILE);
    	if(contextualOutputFile != null){
    		try {
				contextualOut = new PrintWriter(new FileWriter(contextualOutputFile));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	log.debug("There are {} contextualDiscreteAttributes, and {} contextualDoubleAttributes" , contextualDiscreteAttributes.size(),contextualDoubleAttributes.size());
    }
   
    
    
    /**
     * Interface 1: search all contextual outliers
     * @param data
     * @param zScore
     * @param encoder
     */
    public Map<Context,OutlierClassifier> searchContextualOutliers(List<Datum> data){
    	
    	Stopwatch sw = Stopwatch.createUnstarted();
    	
    	
    	log.debug("Find global context outliers on data num tuples: {} , MBs {} ",data.size());
    	sw.start();
    	
    	HashSet<Datum> sample = randomSampling(data);
        globalContext = new Context(sample);
    	ContextPruning.data = data;
    	ContextPruning.sample = sample;
    	ContextPruning.alpha = 0.05;
    	ContextPruning.contextPruningOptions = contextPruningOptions;
    	
        contextualOutlierDetection(data,globalContext);
    	
    	sw.stop();
    	long globalOutlierDetecionTime = sw.elapsed(TimeUnit.MILLISECONDS);
    	sw.reset();
    	log.debug("Done global context outlier remaining data size {} : (duration: {}ms)", data.size(),globalOutlierDetecionTime);
    	
    	
    	
    	List<LatticeNode> preLatticeNodes = new ArrayList<LatticeNode>();
    	List<LatticeNode> curLatticeNodes = new ArrayList<LatticeNode>();
    	for(int level = 1; level <= totalContextualDimensions; level++){
			
    		if(level > maxPredicates)
    			break;
    		
    		log.debug("Build {}-dimensional contexts on all attributes",level);
    		sw.start();
    		if(level == 1){
    			curLatticeNodes = buildOneDimensionalLatticeNodes(data);
        	}else{
        		curLatticeNodes = levelUpLattice(preLatticeNodes, data);	
        	}
    		sw.stop();
    		long latticeNodesBuildTimeCurLevel = sw.elapsed(TimeUnit.MILLISECONDS);
    		sw.reset();
        	log.debug("Done building {}-dimensional contexts on all attributes (duration: {}ms)", level,latticeNodesBuildTimeCurLevel);
        	
        	
        	
        	
        	log.debug("Memory Usage: {}", checkMemoryUsage());
    		
    		if(curLatticeNodes.size() == 0){
    			log.debug("No more dense contexts, thus no need to level up anymore");
    			break;
    		}
    			
    		
        	log.debug("Find {}-dimensional contextual outliers",level);
        	sw.start();
        	int numDenseContextsCurLevel = 0;
        	//run contextual outlier detection
        	for(LatticeNode node: curLatticeNodes){
        		for(Context context: node.getDenseContexts()){
        			contextualOutlierDetection(data,context);
        			numDenseContextsCurLevel++;
        		}
        	}
        	sw.stop();
        	long contextualOutlierDetectionTimeCurLevel = sw.elapsed(TimeUnit.MILLISECONDS);
        	sw.reset();
        	log.debug("Done Find {}-dimensional contextual outliers (duration: {}ms)", level, contextualOutlierDetectionTimeCurLevel);
        	log.debug("Done Find {}-dimensional contextual outliers, there are {} dense contexts(average duration per context: {}ms)", level, numDenseContextsCurLevel,(numDenseContextsCurLevel == 0)?0:contextualOutlierDetectionTimeCurLevel/numDenseContextsCurLevel);
        	log.debug("Done Find {}-dimensional contextual outliers, Context Pruning: {}", level,ContextPruning.print());
            log.debug("Done Find {}-dimensional contextual outliers, densityPruning2: {}, "
            		+ "numOutlierDetectionRunsWithoutTrainingWithoutScoring: {},  "
            		+ "numOutlierDetectionRunsWithoutTrainingWithScoring: {},  "
            		+ "numOutlierDetectionRunsWithTrainingWithScoring: {}", 
            		level,densityPruning2,
            		numOutlierDetectionRunsWithoutTrainingWithoutScoring,
            		numOutlierDetectionRunsWithoutTrainingWithScoring,
            		numOutlierDetectionRunsWithTrainingWithScoring);
            log.debug("----------------------------------------------------------");

        	
            //free up memory 
        	if(level >= 2){
        		for(LatticeNode node: preLatticeNodes){
        			for(Context context: node.getDenseContexts()){
        				context2BitSet.remove(context);
        			}
        		}
        	}
        	
        	
			preLatticeNodes = curLatticeNodes;
			
			
		}
    	
    	return context2OutlierClassifier;
    }
    
    private List<Datum> findInputOutliers(List<Datum> data){
		List<Datum> inputOutliers = new ArrayList<Datum>();

		if(isEncoderSetup() == false)
			return inputOutliers;
		
		String contextualAPIOutlierPredicates = conf.getString(MacroBaseConf.CONTEXTUAL_API_OUTLIER_PREDICATES,
				MacroBaseDefaults.CONTEXTUAL_API_OUTLIER_PREDICATES);

		String[] splits = contextualAPIOutlierPredicates.split(" = ");
		String columnName = splits[0];
		String columnValue = splits[1];

		int contextualDiscreteAttributeIndex = contextualDiscreteAttributes.indexOf(columnName);
		for (Datum datum : data) {

			if (contextualDiscreteAttributeIndex != -1) {
				int encodedValue = datum.getContextualDiscreteAttributes().get(contextualDiscreteAttributeIndex);
				if (encoder.getAttribute(encodedValue).getValue().equals(columnValue)) {
					inputOutliers.add(datum);
				}
			}

		}
		if(contextualOut != null)
			contextualOut.close();
		return inputOutliers;
    }
    
    /**
     * Interface 2: Given some outliers, search contexts for which they are outliers
     * @param data
     * @param zScore
     * @param encoder
     * @param inputOutliers
     */
    public Map<Context,OutlierClassifier> searchContextGivenOutliers(List<Datum> data){
    	
    	List<Datum> inputOutliers = findInputOutliers(data);
    	
    	return searchContextGivenOutliers(data,inputOutliers);
    }
    
    /**
     * Interface 2: Given some outliers, search contexts for which they are outliers
     * @param data
     * @param zScore
     * @param encoder
     * @param inputOutliers
     */
    public Map<Context,OutlierClassifier> searchContextGivenOutliers(List<Datum> data, List<Datum> inputOutliers){
    	
    	
    	//result contexts that have the input outliers
    	List<Context> result = new ArrayList<Context>();
    	
    	if(inputOutliers == null || inputOutliers.size() == 0){
    		log.info("There is no input outliers");
    		return context2OutlierClassifier;
    	}
    	
    	Stopwatch sw = Stopwatch.createUnstarted();
    	
    	log.debug("Find global context outliers on data num tuples: {} , MBs {} ",data.size());
    	sw.start();
    	
    	HashSet<Datum> sample = randomSampling(data);
        globalContext = new Context(sample);
    	ContextPruning.data = data;
    	ContextPruning.sample = sample;
    	ContextPruning.alpha = 0.05;
    	ContextPruning.contextPruningOptions = contextPruningOptions;

    	
        List<Datum> globalOutliers = contextualOutlierDetection(data,globalContext);
    	if(globalOutliers != null && globalOutliers.contains(inputOutliers)){
    		result.add(globalContext);
    	}
        
        
    	sw.stop();
    	long globalOutlierDetecionTime = sw.elapsed(TimeUnit.MILLISECONDS);
    	sw.reset();
    	log.debug("Done global context outlier remaining data size {} : (duration: {}ms)", data.size(),globalOutlierDetecionTime);
    	
    	
    	
    	List<LatticeNode> preLatticeNodes = new ArrayList<LatticeNode>();
    	List<LatticeNode> curLatticeNodes = new ArrayList<LatticeNode>();
    	for(int level = 1; level <= totalContextualDimensions; level++){
			
    		if(level > maxPredicates)
    			break;
    		
    		log.debug("Build {}-dimensional contexts on all attributes",level);
    		sw.start();
    		if(level == 1){
    			curLatticeNodes = buildOneDimensionalLatticeNodesGivenOutliers(data,inputOutliers);
        	}else{
        		curLatticeNodes = levelUpLattice(preLatticeNodes, data);	
        	}
    		sw.stop();
    		long latticeNodesBuildTimeCurLevel = sw.elapsed(TimeUnit.MILLISECONDS);
    		sw.reset();
        	log.debug("Done building {}-dimensional contexts on all attributes (duration: {}ms)", level,latticeNodesBuildTimeCurLevel);
        	
        	
        	
        	
        	log.debug("Memory Usage: {}", checkMemoryUsage());
    		
    		if(curLatticeNodes.size() == 0){
    			log.debug("No more dense contexts, thus no need to level up anymore");
    			break;
    		}
    			
    		
        	log.debug("Find {}-dimensional contextual outliers",level);
        	sw.start();
        	int numDenseContextsCurLevel = 0;
        	//run contextual outlier detection
        	for(LatticeNode node: curLatticeNodes){
        		for(Context context: node.getDenseContexts()){
        			List<Datum> outliers = contextualOutlierDetection(data,context);
        			if(outliers != null && outliers.containsAll(inputOutliers)){
        	    		result.add(context);
        	    	}
        			numDenseContextsCurLevel++;
        		}
        	}
        	sw.stop();
        	long contextualOutlierDetectionTimeCurLevel = sw.elapsed(TimeUnit.MILLISECONDS);
        	sw.reset();
        	log.debug("Done Find {}-dimensional contextual outliers (duration: {}ms)", level, contextualOutlierDetectionTimeCurLevel);
        	log.debug("Done Find {}-dimensional contextual outliers, there are {} dense contexts(average duration per context: {}ms)", level, numDenseContextsCurLevel,(numDenseContextsCurLevel == 0)?0:contextualOutlierDetectionTimeCurLevel/numDenseContextsCurLevel);
        	log.debug("Done Find {}-dimensional contextual outliers, Context Pruning: {}", level,ContextPruning.print());
        	log.debug("Done Find {}-dimensional contextual outliers, densityPruning2: {}, "
            		+ "numOutlierDetectionRunsWithoutTrainingWithoutScoring: {},  "
            		+ "numOutlierDetectionRunsWithoutTrainingWithScoring: {},  "
            		+ "numOutlierDetectionRunsWithTrainingWithScoring: {}", 
            		level,densityPruning2,
            		numOutlierDetectionRunsWithoutTrainingWithoutScoring,
            		numOutlierDetectionRunsWithoutTrainingWithScoring,
            		numOutlierDetectionRunsWithTrainingWithScoring);
            log.debug("----------------------------------------------------------");       
            
        	
            //free up memory 
        	if(level >= 2){
        		for(LatticeNode node: preLatticeNodes){
        			for(Context context: node.getDenseContexts()){
        				context2BitSet.remove(context);
        			}
        		}
        	}
        	
        	
			preLatticeNodes = curLatticeNodes;
			
			
		}
    	
    	Map<Context,OutlierClassifier> context2OutlierClassifierGivenOutlier = new HashMap<Context,OutlierClassifier>();
    	for(Context context: result){
    		context2OutlierClassifierGivenOutlier.put(context, context2OutlierClassifier.get(context));
    	}
    	if(contextualOut != null)
    		contextualOut.close();
    	return context2OutlierClassifierGivenOutlier;
    }
    
    private HashSet<Datum> randomSampling(List<Datum> data){
    	
    	List<Datum> sampleData = new ArrayList<Datum>();
    	
    	int minSampleSize = 100;
    	int numSample = (int) (minSampleSize / denseContextTau);
    	
    	Random rnd = new Random();
		for(int i = 0; i < data.size(); i++){
			Datum d = data.get(i);
			if(sampleData.size() < numSample){
				sampleData.add(d);
			}else{
				int j = rnd.nextInt(i); //j in [0,i)
				if(j < sampleData.size()){
					sampleData.set(j, d);
				}
			}
			
		}
		
		return new HashSet<Datum>(sampleData);
    }
    
    private String checkMemoryUsage(){
    	Runtime runtime = Runtime.getRuntime();

    	NumberFormat format = NumberFormat.getInstance();

    	StringBuilder sb = new StringBuilder();
    	long maxMemory = runtime.maxMemory();
    	long allocatedMemory = runtime.totalMemory();
    	long freeMemory = runtime.freeMemory();

    	sb.append("free memory: " + format.format(freeMemory / 1024) + "<br/>");
    	sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "<br/>");
    	sb.append("max memory: " + format.format(maxMemory / 1024) + "<br/>");
    	sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "<br/>");
    	return sb.toString();
    }
    
    
  /**
   * Walking up the lattice, construct the lattice node, when include those lattice nodes that contain at least one dense context
   * @param latticeNodes
   * @param data
   * @return
   */
	private List<LatticeNode> levelUpLattice(List<LatticeNode> latticeNodes, List<Datum> data){
		
		//sort the subspaces by their dimensions
		Stopwatch sw = Stopwatch.createUnstarted();
		
		log.debug("\tSorting lattice nodes in level {} by their dimensions " , latticeNodes.get(0).dimensions.size());
		sw.start();
		
		
		List<LatticeNode> latticeNodeByDimensions = new ArrayList<LatticeNode>(latticeNodes);
	    Collections.sort(latticeNodeByDimensions, new LatticeNode.DimensionComparator());

	    sw.stop();
	    long sortingTime = sw.elapsed(TimeUnit.MILLISECONDS);
	    sw.reset();
		log.debug("\tDone Sorting lattice nodes in level {} by their dimensions (duration: {}ms)" , latticeNodes.get(0).dimensions.size(), sortingTime);

	    
	    //find out dense candidate subspaces 
	    List<LatticeNode> result = new ArrayList<LatticeNode>();
		
	    
		log.debug("\tJoining lattice nodes in level {} by their dimensions " , latticeNodes.get(0).dimensions.size());
		sw.start();
		
		int numLatticeNodeJoins = 0;
		int numDenseContexts = 0;
		for(int i = 0; i < latticeNodeByDimensions.size(); i++ ){
			for(int j = i +1; j < latticeNodeByDimensions.size(); j++){
	    		
	    		LatticeNode s1 = latticeNodeByDimensions.get(i);
	    		LatticeNode s2 = latticeNodeByDimensions.get(j);
	    		LatticeNode joined = s1.join(s2, data, denseContextTau);
	    		
	    		if(joined != null){
	    			numLatticeNodeJoins++;
	    			//only interested in nodes that have dense contexts
	    			if(joined.getDenseContexts().size() != 0){
	    				result.add(joined);
	    				numDenseContexts += joined.getDenseContexts().size();
	    			}
	    				
	    		}
	    	}
	    }
	    
	    sw.stop();
	    long joiningTime = sw.elapsed(TimeUnit.MILLISECONDS);
	    sw.reset();
	    
		log.debug("\tDone Joining lattice nodes in level {} by their dimensions (duration: {}ms)" , latticeNodes.get(0).dimensions.size(), joiningTime);
		log.debug("\tDone Joining lattice nodes in level {} by their dimensions,"
				+ " there are {} joins and {} dense contexts (average duration per lattice node pair join: {}ms)" , 
				latticeNodes.get(0).dimensions.size(), numLatticeNodeJoins,numDenseContexts,  (numLatticeNodeJoins==0)?0:joiningTime/numLatticeNodeJoins);

		
		
		return result;
	}
	
	
    private int densityPruning2 = 0;
    private int numOutlierDetectionRunsWithoutTrainingWithoutScoring = 0;
    private int numOutlierDetectionRunsWithoutTrainingWithScoring = 0;
    private int numOutlierDetectionRunsWithTrainingWithScoring = 0;
    /**
     * Run outlier detection algorithm on contextual data
     * The algorithm has to static threhold classifier
     * @param data
     * @param context
     * @return
     */
    public List<Datum> contextualOutlierDetection(List<Datum> data, Context context){
    	
    		
    	BitSet bs = context.getContextualBitSet(data,context2BitSet);
    	context2BitSet.put(context, bs);
    	List<Datum> contextualData = null;
    	
    	
    	Context p1 = (context.getParents().size() > 0)?context.getParents().get(0):null;
    	Context p2 = (context.getParents().size() > 1)?context.getParents().get(1):null;
    	boolean requiresTraining = true;
    	if(p1 != null && ContextPruning.sameDistribution(context, p1)){
    		//training
    		if(contextPruningOptions.isDistributionPruningForTraining()){
    			context.setDetector(p1.getDetector());
    			requiresTraining = false;
    		}
			else{
				try {
					context.setDetector(constructDetector());
				} catch (ConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
    		//scoring
    		if(contextPruningOptions.isDistributionPruningForScoring()){
    			numOutlierDetectionRunsWithoutTrainingWithoutScoring++;
    		}else{
    			contextualData = new ArrayList<Datum>();
            	numOutlierDetectionRunsWithoutTrainingWithScoring++;
    		}
    	}else if(p2 != null && ContextPruning.sameDistribution(context, p2)){
    		
    		if(contextPruningOptions.isDistributionPruningForTraining()){
    			context.setDetector(p2.getDetector());
    			requiresTraining = false;
    		}
			else{
				try {
					context.setDetector(constructDetector());
				} catch (ConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
    		
    		if(contextPruningOptions.isDistributionPruningForScoring()){
    			numOutlierDetectionRunsWithoutTrainingWithoutScoring++;
    		}else{
    			contextualData = new ArrayList<Datum>();
            	numOutlierDetectionRunsWithoutTrainingWithScoring++;
    		}
    	}else{
    		//training
    		try {
				context.setDetector(constructDetector());
			} catch (ConfigurationException e) {
				e.printStackTrace();
			}
			contextualData = new ArrayList<Datum>();
        	numOutlierDetectionRunsWithTrainingWithScoring++;
    	}
    	if(contextualData == null){
    		// pruned by distribution
    		return null;
    	}else{
    		List<Integer> indexes = bitSet2Indexes(bs);
        	for(Integer index: indexes){
        		contextualData.add(data.get(index));
        	}
        	context.setSize(contextualData.size());
        	double realDensity = (double)contextualData.size() / data.size();
        	if(realDensity < denseContextTau){
        		densityPruning2++;
        		return null;
        	}
    	}
    	
    	//I do the training and scoring here, instead of using FeatureTransform and OutlierClassifier
    	//because Iterator design only allows us to iterate the result once
    	List<Datum> outliers = new ArrayList<Datum>();
    	if(requiresTraining){
    		context.getDetector().train(contextualData);
    	}
    	double  thresholdScore = conf.getDouble(MacroBaseConf.OUTLIER_STATIC_THRESHOLD, MacroBaseDefaults.OUTLIER_STATIC_THRESHOLD);
    	for(Datum datum: contextualData){
    		double score = context.getDetector().score(datum);
    		if(score > thresholdScore){
    			outliers.add(datum);
    		}
    	}
    	if(outliers.size() > 0){
    		if(contextualOut != null){
    			contextualOut.println("Context: " + context.print(conf.getEncoder()));
    			contextualOut.println("\t Number of inliners " + (contextualData.size() - outliers.size()));
    			contextualOut.println("\t Number of outliers " + outliers.size());
    			contextualOut.flush();
    		}
    		
    		FeatureTransform featureTransform = new BatchScoreFeatureTransform(conf, contextualData.iterator(), context.getDetector(), requiresTraining);
        	OutlierClassifier outlierClassifier =  new StaticThresholdClassifier(conf,featureTransform);
        	context2OutlierClassifier.put(context, outlierClassifier);
    	}
    	return outliers;
    	/*
    	FeatureTransform featureTransform = new BatchScoreFeatureTransform(conf, contextualData.iterator(), context.getDetector(), requiresTraining);
    	OutlierClassifier outlierClassifier =  new StaticThresholdClassifier(conf,featureTransform);
        
    	List<Datum> outliers = new ArrayList<Datum>();
        if(outlierClassifier != null){
        	for (OutlierClassificationResult result : IterUtils.iterable(outlierClassifier)) {
                if (result.isOutlier()) {
                	//note that the datum has changed after feature transformation
                    outliers.add(result.getDatum().getParentDatum());
                } else {
                    //inliers.add(result.getDatum());
                }
            }
        	
        }
        if(outliers.size() > 0)	{
        	context2OutlierClassifier.put(context, outlierClassifier);
        }
        return outliers;
        */
    }
    
    /*
    public void explainContextualOutliers(Context context, BatchTrainScore.BatchResult or){
    	 if(isEncoderSetup() == false)
    		 return;
    	 FPGrowthEmerging fpg = new FPGrowthEmerging();
    	 double minOIRatio = conf.getDouble(MacroBaseConf.MIN_OI_RATIO, MacroBaseDefaults.MIN_OI_RATIO);
         double minSupport = conf.getDouble(MacroBaseConf.MIN_SUPPORT, MacroBaseDefaults.MIN_SUPPORT);
         List<Datum> inliners = new ArrayList<Datum>();
         List<Datum> outliers = new ArrayList<Datum>();
         for(DatumWithScore dw: or.getInliers()){
        	 inliners.add(dw.getDatum());
         }
         for(DatumWithScore dw: or.getOutliers()){
        	 outliers.add(dw.getDatum());
         }
         List<ItemsetResult> isr = fpg.getEmergingItemsetsWithMinSupport(inliners,
                                                                         outliers,
                                                                         minSupport,
                                                                         minOIRatio,
                                                                         encoder);
         
         log.info("Context: " + context.print(encoder));
     	 log.info("Number of Inliers: " +  or.getInliers().size());
     	 log.info("Number of Outliers: " + or.getOutliers().size());
     	 
     	 log.info("Possible Explanations with minSupport " + minSupport + " and minOIRatio " +  minOIRatio + " : ");
     	 for(ItemsetResult is: isr){
     		StringBuilder sb = new StringBuilder();
     		StringJoiner joiner = new StringJoiner("\n");
     		is.getItems().stream()
                .forEach(i -> joiner.add(String.format("\t%s: %s",
                                                       i.getColumn(),
                                                       i.getValue())));
     		sb.append("\t" + " support: " + is.getSupport() + " oiRation: " + is.getRatioToInliers() + " explanation: " + joiner);
     		log.info(sb.toString());
     	 }
     	 
     	 
     	 
     	 //order the isr by their support
		//         isr.sort(new Comparator<ItemsetResult>(){
		//			@Override
		//			public int compare(ItemsetResult o1, ItemsetResult o2) {
		//				if(o1.getSupport() > o2.getSupport())
		//					return 1;
		//				else if(o1.getSupport() < o2.getSupport())
		//					return -1;
		//				else 
		//					return 0;
		//			}});
         
         
         //order the isr by their OIRatio
		//         isr.sort(new Comparator<ItemsetResult>(){
		// 			@Override
		// 			public int compare(ItemsetResult o1, ItemsetResult o2) {
		// 				if(o1.getRatioToInliers() > o2.getRatioToInliers())
		// 					return 1;
		// 				else if(o1.getRatioToInliers() < o2.getRatioToInliers())
		// 					return -1;
		// 				else
		// 					return 0;
		// 			}});
          
         
    }*/
    
    
    /**
     * Every context stores its own detector
     * @return
     * @throws ConfigurationException
     */
    private BatchTrainScore constructDetector() throws ConfigurationException {
       return conf.constructTransform(conf.getTransformType());
    }
    /**
	 * Find one dimensional lattice nodes with dense contexts
	 * @param data
	 * @return
	 */
	private List<LatticeNode> buildOneDimensionalLatticeNodes(List<Datum> data){
		
		
		//create subspaces
		List<LatticeNode> latticeNodes = new ArrayList<LatticeNode>();
				
		for(int dimension = 0; dimension < totalContextualDimensions; dimension++){
			LatticeNode ss = new LatticeNode(dimension);
			List<Context> denseContexts = initOneDimensionalDenseContextsAndContext2Data(data,dimension, denseContextTau);
			for(Context denseContext: denseContexts){
				ss.addDenseContext(denseContext);
				if(isEncoderSetup())
					log.debug(denseContext.toString() + " ---- " + denseContext.print(encoder));
				else
					log.debug(denseContext.toString());
			}
			latticeNodes.add(ss);
		}
		
		return latticeNodes;
	}
	

	private List<LatticeNode> buildOneDimensionalLatticeNodesGivenOutliers(List<Datum> data, List<Datum> inputOutliers){
		//create subspaces
		List<LatticeNode> latticeNodes = new ArrayList<LatticeNode>();
				
		for(int dimension = 0; dimension < totalContextualDimensions; dimension++){
			LatticeNode ss = new LatticeNode(dimension);
			List<Context> denseContexts = initOneDimensionalDenseContextsAndContext2DataGivenOutliers(data, dimension, inputOutliers);
			for(Context denseContext: denseContexts){
				ss.addDenseContext(denseContext);
				if(isEncoderSetup())
					log.debug(denseContext.toString() + " ---- " + denseContext.print(encoder));
				else
					log.debug(denseContext.toString());
				
			}
			latticeNodes.add(ss);
		}
		
		return latticeNodes;
	}
	
	
	private boolean isEncoderSetup(){
		if(encoder == null)
			return false;
		
		if(encoder.getNextKey() == 0)
			return false;
		
		return true;
	}
	/**
	 * Does this interval worth considering
	 * A = null is not worth considering
	 * @param interval
	 * @param encoder
	 * @return
	 */
	private boolean isInterestingInterval(Interval interval){
		if(isEncoderSetup() == false)
			return true;
		
		if(interval instanceof IntervalDiscrete){
			IntervalDiscrete id = (IntervalDiscrete) interval;
			String columnValue = encoder.getAttribute(id.getValue()).getValue();
			if(columnValue == null || columnValue.equals("null")){
				return false;
			}
		}
		
		return true;
		
	}
	
	/**
	 * Initialize one dimensional dense contexts
	 * The number of passes of data is O(totalContextualDimensions)
	 * Store the datums of every one dimensional context in memory
	 * @param data
	 * @param dimension
	 * @return
	 */
	private List<Context> initOneDimensionalDenseContextsAndContext2Data(List<Datum> data,int dimension, double curDensityThreshold){
		int discreteDimensions = contextualDiscreteAttributes.size();
		
		
		List<Context> result = new ArrayList<Context>();
		
		if(dimension < discreteDimensions){
			Map<Integer,List<Integer>> distinctValue2Data = new HashMap<Integer,List<Integer>>();
			for(int i = 0; i < data.size(); i++){
				Datum datum = data.get(i);
				Integer value = datum.getContextualDiscreteAttributes().get(dimension);
				if(distinctValue2Data.containsKey(value)){
					distinctValue2Data.get(value).add(i);
				}else{
					List<Integer> temp = new ArrayList<Integer>();
					temp.add(i);
					distinctValue2Data.put(value, temp);
				}
				
			}
			for(Integer value: distinctValue2Data.keySet()){
				//boolean denseContext = !ContextPruning.densityPruning(data.size(), distinctValue2Count.get(value), denseContextTau);
				boolean denseContext = ( (double) distinctValue2Data.get(value).size() / data.size() >= curDensityThreshold)?true:false;
				if(denseContext){
					Interval interval = new IntervalDiscrete(dimension,contextualDiscreteAttributes.get(dimension),value);
					if(isInterestingInterval(interval)){
						Context context = new Context(dimension, interval, globalContext);
						result.add(context);
						
						BitSet bs = indexes2BitSet(distinctValue2Data.get(value),data.size());
						context2BitSet.put(context, bs);
					}
					
				}
			}
		}else{
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			//find out the min, max
			for(Datum datum: data){
				double value = datum.getContextualDoubleAttributes().getEntry(dimension - discreteDimensions );
				if(value > max){
					max = value;
				}
				if(value < min){
					min = value;
				}
			}
			HashSet<Interval> allIntervals = new HashSet<Interval>();
			// divide the interval into numIntervals
			double step = (max - min) / numIntervals;
			double start = min;
			for(int i = 0; i < numIntervals; i++){
				if(i != numIntervals - 1){
					Interval interval = new IntervalDouble(dimension,contextualDoubleAttributes.get(dimension - discreteDimensions), start, start + step);
					start += step;
					allIntervals.add(interval);
				}else{
					//make the max a little bit larger
					Interval interval = new IntervalDouble(dimension, contextualDoubleAttributes.get(dimension - discreteDimensions),start, max + 0.000001);
					allIntervals.add(interval);
				}
			}
			//count the interval
			HashMap<Interval,List<Integer>> interval2Data = new HashMap<Interval,List<Integer>>();
			for(int i = 0; i < data.size(); i++){
				Datum datum = data.get(i);
				double value = datum.getContextualDoubleAttributes().getEntry(dimension - discreteDimensions );
				for(Interval interval: allIntervals){
					if(interval.contains(value)){
						if(interval2Data.containsKey(interval)){
							interval2Data.get(interval).add(i);
						}else{
							List<Integer> temp = new ArrayList<Integer>();
							temp.add(i);
							interval2Data.put(interval,temp);
						}
						break;
					}
				}
			}
			for(Interval interval: interval2Data.keySet()){
				//boolean denseContext =!ContextPruning.densityPruning(data.size(), interval2Count.get(interval), denseContextTau);
				boolean denseContext = ( (double) interval2Data.get(interval).size() / data.size() >= curDensityThreshold)?true:false;
				if(denseContext){
					if(isInterestingInterval(interval)){
						Context context = new Context(dimension, interval,globalContext);
						result.add(context);
						
						BitSet bs = indexes2BitSet(interval2Data.get(interval),data.size());
						context2BitSet.put(context, bs);
					}
					
				}
				
				
			}
		}
		
		return result;
	}

	
	private List<Context> initOneDimensionalDenseContextsAndContext2DataGivenOutliers(List<Datum> data, int dimension, List<Datum> inputOutliers){
		List<Context> contextsContainingOutliers = initOneDimensionalDenseContextsAndContext2Data(inputOutliers,dimension, 1.0);
		
		List<Context> result = new ArrayList<Context>();
		
		//re-initialize context2Bitset
		for(Context context: contextsContainingOutliers){
			List<Integer> temp = new ArrayList<Integer>();
			for(int i = 0; i < data.size(); i++){
				Datum datum = data.get(i);
				if(context.containDatum(datum)){
					temp.add(i);
				}
			}
			boolean denseContext = ( (double) temp.size() / data.size() >= denseContextTau)?true:false;
			if(denseContext){
				BitSet bs = indexes2BitSet(temp,data.size());
				context2BitSet.put(context, bs);
				result.add(context);
			}
			
		}
		return result;
	}
	
	
	//trade memory for efficiency
	//private Map<Context,HashSet<Datum>> context2Data = new HashMap<Context,HashSet<Datum>>();
	private Map<Context,BitSet> context2BitSet = new HashMap<Context,BitSet>();
	
	private BitSet indexes2BitSet(List<Integer> indexes, int total){
		BitSet bs = new BitSet(total);
		for(int i = 0; i < indexes.size(); i++){
			int index = indexes.get(i);
			bs.set(index);
		}
		return bs;
	}
	private List<Integer> bitSet2Indexes(BitSet bs){
		List<Integer> indexes = new ArrayList<Integer>();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
		     // operate on index i here
			indexes.add(i);
		     if (i == Integer.MAX_VALUE) {
		         break; // or (i+1) would overflow
		     }
		}
		return indexes;
	}
    
	
	
}
