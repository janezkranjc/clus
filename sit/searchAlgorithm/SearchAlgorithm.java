package sit.searchAlgorithm;

import sit.TargetSet;
import sit.mtLearner.MTLearner;
import clus.data.type.ClusAttrType;

public interface SearchAlgorithm {
	
	
	public TargetSet search(ClusAttrType mainTarget,TargetSet candidates);	
	
	public void setMTLearner(MTLearner learner);
	
	public String getName();
	
}
