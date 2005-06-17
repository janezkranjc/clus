/*
 * Created on May 18, 2005
 */
package clus.ext.hierarchical;

import clus.main.*;
import clus.data.attweights.*;

public class HierarchicalTargetWeightProducer extends ClusAttributeWeights {

	protected ClassHierarchy m_Hier;
	
	public HierarchicalTargetWeightProducer(ClusStatManager mgr, ClassHierarchy hier) {
		super(hier.getTotal());
		m_Hier = hier;
	}
		
	public String getName() {
		return "Hierarchical weights";
	}	
}
