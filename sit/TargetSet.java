package sit;
import java.util.*;

import jeans.util.IntervalCollection;

import clus.data.type.ClusAttrType;
import clus.data.type.ClusSchema;

public class TargetSet extends java.util.TreeSet{
	
	
	
	public TargetSet(ClusAttrType MainTarget){
		super();
		this.add(MainTarget);
	}
	
	public TargetSet(TargetSet Other){
		super();
		this.addAll(Other);
	}
	
	public TargetSet(){
		super();
	}
	
	/**
	 * Creates a targetset from a ClusSchema and an IntervalCollection
	 * 
	 * @param schema The schema containing all targets
	 * @param targets IntervalCollection of the targets that should be added to this TargetSet
	 */
	public TargetSet(ClusSchema schema,IntervalCollection targets){
		super();
		targets.reset();
		while(targets.hasMoreInts()){
			this.add(schema.getAttrType(targets.nextInt()-1));//"the interval counts from 1"
		}
	}
}