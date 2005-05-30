
package clus.ext.hierarchical;

import jeans.tree.*;

public class HierNodeWeights {
	
	double[] m_Weights;
	String m_Name;
	
	public final double getWeight(int nodeidx) {
		return m_Weights[nodeidx];
	}
	
	public final double[] getWeights() {
		return m_Weights;
	}
	
	public final String getName() {
		return m_Name;
	}
	
	public void initExponentialDepthWeights(ClassHierarchy hier, double w0) {
		m_Weights = new double[hier.getTotal()];
		CompleteTreeIterator it_i = hier.getRootIter();
		while (it_i.hasMoreNodes()) {
			ClassTerm ni = (ClassTerm)it_i.getNextNode();
			m_Weights[ni.getIndex()] = calcExponentialDepthWeight(ni.getLevel(), w0);
		}
		m_Name = "Exponential depth weights "+w0;
	}
	
	private final static double calcExponentialDepthWeight(int depth, double w0) {
		return Math.pow(w0, (double)depth);
	}	
}
