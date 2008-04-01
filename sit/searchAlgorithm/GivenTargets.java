package sit.searchAlgorithm;

import java.util.Iterator;

import sit.TargetSet;
import sit.mtLearner.MTLearner;
import clus.data.type.ClusAttrType;

/**
 * This class always returns a fixed set of targets, no real search
 */
public class GivenTargets implements SearchAlgorithm{
	
	public TargetSet search(ClusAttrType mainTarget, TargetSet candidates) {
		
		
		
		TargetSet results = new TargetSet();
		Iterator c = candidates.iterator();
		
		while(c.hasNext()){
			ClusAttrType t = (ClusAttrType) c.next();
			if(t.getIndex()==82
					|| t.getIndex()==48){
				results.add(t);
				 
			}
		}
		System.out.println("Fixed set:"+ results);
		return results;
	}

	public void setMTLearner(MTLearner learner) {}

	public String getName() {
		return "GivenTargets";
	}

}
