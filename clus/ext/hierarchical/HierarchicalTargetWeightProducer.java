/*
 * Created on May 18, 2005
 */
package clus.ext.hierarchical;

import clus.statistic.*;
import clus.main.*;

public class HierarchicalTargetWeightProducer extends TargetWeightProducer {

	protected ClassHierarchy m_Hier;
	
	public HierarchicalTargetWeightProducer(ClusStatManager mgr, ClassHierarchy hier) {
		super(mgr);
		m_Hier = hier;
	}
	
	public void setTotalStat(ClusStatistic stat) {
		setNumWeights(m_Hier.getWeights());
	}
	
	public String getName() {
		return "Hierarchical weights";
	}	
}
