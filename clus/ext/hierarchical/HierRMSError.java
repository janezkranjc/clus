/*
 * Created on May 19, 2005
 */
package clus.ext.hierarchical;

import java.util.*;

import clus.data.rows.*;
import clus.error.*;
import clus.statistic.*;

public class HierRMSError extends MSError {

	protected ClassHierarchy m_Hier;
	protected double[] m_Scratch;
	protected boolean m_Root, m_ContPred;
	
	public HierRMSError(ClusErrorParent par, TargetWeightProducer weights, boolean root, boolean proto, ClassHierarchy hier) {
		this(par, weights, false, root, proto, hier);
	}
	
	public HierRMSError(ClusErrorParent par, TargetWeightProducer weights, boolean printall, boolean root, boolean proto, ClassHierarchy hier) {
		super(par, weights, printall, hier.getTotal());
		m_Hier = hier;
		m_Root = root;
		m_ContPred = proto;
		m_Scratch = new double[m_Dim];
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
		Arrays.fill(m_Scratch, 0.0);
		for (int i = 0; i < tp.getLength(); i++) {
			ClassesValue val = tp.elementAt(i);
			m_Scratch[val.getIndex()] = 1.0;
		}
		if (m_ContPred) {
			addExample(m_Scratch, pred.getNumericPred());
		} else {
			addExample(m_Scratch, ((WHTDStatistic)pred).getDiscretePred());			
		}
	}
	
	public double getModelError() {
		if (m_Root)	return Math.sqrt(super.getModelError());
		else return super.getModelError();
	}
	
	public double getErrorComp(int i) {
		if (m_Root) return Math.sqrt(super.getErrorComp(i));
		else return super.getErrorComp(i);
	}
	
	public String getName() {
		String root = m_Root ? "RMSE" : "MSE";
		String proto = m_ContPred ? "with continuous predictions" : "with discrete predictions";
		if (m_Weights == null) return "Hierarchical "+root+" "+proto;
		else return "Hierarchical weighted "+root+" ("+m_Weights.getName()+") "+proto;
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new HierRMSError(par, m_Weights, m_PrintAllComps, m_Root, m_ContPred, m_Hier);
	}
}
