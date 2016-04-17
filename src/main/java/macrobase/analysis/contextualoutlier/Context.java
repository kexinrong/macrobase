package macrobase.analysis.contextualoutlier;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import macrobase.analysis.stats.BatchTrainScore;
import macrobase.datamodel.Datum;
import macrobase.ingest.DatumEncoder;


public class Context {
    private static final Logger log = LoggerFactory.getLogger(Context.class);

    
    /**
	 * A list of ordered contextual dimensions this node represents
	 * and intervals for each dimension
	 */
	private List<Integer> dimensions = new ArrayList<Integer>();
	private List<Interval> intervals = new ArrayList<Interval>();;
	
	private int size = -1;
	private List<Context> parents = new ArrayList<Context>();
 
	private HashSet<Context> oneDimensionalAncestors = new HashSet<Context>();
	
	//the sample is maintained in the lattice
	private HashSet<Datum> sample = new HashSet<Datum>();
	
	
	//the outlier detector used for detection in this context
	private BatchTrainScore detector;
	
	
	/**
	 * Global context
	 */
	public Context(HashSet<Datum> sample){
		this.sample = sample;
	}
	
	/**
	 * Initialize a one dimensional context
	 * @param dimension
	 * @param interval
	 * @param parent
	 */
	public Context(int dimension, Interval interval, Context parent){
		dimensions.add(dimension);
		intervals.add(interval);
		parents.add(parent);
		
		for(Datum d: parent.sample){
			if(containDatum(d)){
				sample.add(d);
			}
		}
		
		oneDimensionalAncestors.add(this);
		
		
	}
	
	public Context(List<Integer> dimensions, List<Interval> intervals, Context parent1, Context parent2) {
		this.dimensions = dimensions;
		this.intervals = intervals;
		
		parents.add(parent1);
		parents.add(parent2);
	
		sample.addAll(parent1.sample);
		sample.retainAll(parent2.sample);
		
		oneDimensionalAncestors.addAll(parent1.oneDimensionalAncestors);
		oneDimensionalAncestors.addAll(parent2.oneDimensionalAncestors);

		
	}
	
	public BitSet getContextualBitSet(List<Datum> data, Map<Context,BitSet> context2BitSet){
		
		//global context
		if(parents.size() == 0){
			BitSet bs = new BitSet(data.size());
			bs.set(0, data.size());
			return bs;
		}
		BitSet bs = null;
		//one dimensional context
		if(parents.size() == 1 && context2BitSet.containsKey(this)){
			bs = context2BitSet.get(this);
		}
		//context whose parents are known
		if(parents.size() == 2){
			Context p1 = parents.get(0);
			Context p2 = parents.get(1);
			if(context2BitSet.containsKey(p1) && context2BitSet.containsKey(p2)){
				BitSet b1 = context2BitSet.get(p1);
				BitSet b2 = context2BitSet.get(p2);
				bs = (BitSet) b1.clone();
				bs.and(b2);
			}
		}
		return bs;
		/*
		if(bs != null)
			return bs;
		
		//contexts whose ancestors are known
		boolean useIntersection = true;
		for(Context c: oneDimensionalAncestors){
			if(!context2BitSet.containsKey(c)){
				useIntersection = false;
				break;
			}
		}
		if(useIntersection){
			for(Context c: oneDimensionalAncestors){
				if(bs == null){
					bs = (BitSet) context2BitSet.get(c).clone();
				}else{
					bs.and(context2BitSet.get(c));
				}
			}
		}
		if(bs != null)
			return bs;
		
		//contexts don't know anything
		bs = new BitSet(data.size());
		for(int i = 0; i < data.size(); i++){
			Datum datum = data.get(i);
			if(containDatum(datum)){
				bs.set(i);
			}
		}
		return bs;
		*/
			
		
			
	}
	
	
	
	
	/**
	 * Determine if the unit contains the tuple
	 * @param datum
	 * @return
	 */
	public boolean containDatum(Datum datum){
		
		int discreteDimensions = datum.getContextualDiscreteAttributes().size();
		int doubleDimensions = datum.getContextualDoubleAttributes().getDimension();
		int totalDimensions = discreteDimensions + doubleDimensions ;
	
		
		for(int i = 0; i < dimensions.size(); i++){
			int k = dimensions.get(i);
			if( k >=0 && k < discreteDimensions){
				int value = datum.getContextualDiscreteAttributes().get(k);
				if(!intervals.get(i).contains(value))
					return false;
			}else if( k >= discreteDimensions && k < totalDimensions){
				double value = datum.getContextualDoubleAttributes().getEntry(k - discreteDimensions);
				if(!intervals.get(i).contains(value))
					return false;
			}
		}
		
		return true;
	}
	/**
	 * Join this Context with other Context, can only be joined if the first (k-1) dimensions
	 * have the same interval, and the last dimension has different interval
	 * 
	 * can be joined only if the size of the context is at least minSize
	 * 
	 * 
	 * @param other
	 * @return
	 */
	public Context join(Context other, List<Datum> data, double tau){
		
		List<Integer> newDimensions = new ArrayList<Integer>();
		List<Interval> newIntervals = new ArrayList<Interval>();
		
		
		List<Integer> dimensions1 = dimensions;
		List<Integer> dimensions2 = other.dimensions;
		if(dimensions1.size() != dimensions2.size())
			return null;
		
		for(int i = 0; i < dimensions1.size(); i++){
			int dimension1 = dimensions1.get(i);
			int dimension2 = dimensions2.get(i);
			Interval interval1 = intervals.get(i);
			Interval interval2 = other.intervals.get(i);
		
			if(i !=dimensions1.size() - 1 ){
				if(dimension1 != dimension2)
					return null;
					if(interval1 != interval2)
					return null;
					newDimensions.add(dimension1);
					newIntervals.add(interval1);
				
			}else{
				newDimensions.add(dimension1);
				newIntervals.add(interval1);
				
				newDimensions.add(dimension2);
				newIntervals.add(interval2);
				
			}
			
		}
	
		Context newUnit = new Context(newDimensions, newIntervals, this, other);
		
		
		if(ContextPruning.densityPruning(newUnit, tau)){
			return null;
		}
		
		if(ContextPruning.dependencyPruning(newUnit)){
			return null;
		}
		
		return newUnit;
	}
	

	public String print(DatumEncoder encoder){
		if(dimensions.size() == 0){
			return "Global Context: ";
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < dimensions.size(); i++){
			sb.append( intervals.get(i).print(encoder) + " ");
		}
		return sb.toString();
	}
	
	@Override
	public String toString(){
		if(dimensions.size() == 0){
			return "Global Context: ";
		}
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < dimensions.size(); i++){
			sb.append(dimensions.get(i) + ":" + intervals.get(i).toString() + " ");
		}
		return sb.toString();
	}


	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
	
	public HashSet<Datum> getSample(){
		return sample;
	}
	
	public List<Context> getParents(){
		return parents;
	}

	public BatchTrainScore getDetector() {
		return detector;
	}

	public void setDetector(BatchTrainScore detector) {
		this.detector = detector;
	}
}
