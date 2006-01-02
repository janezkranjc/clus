
package clus.ext.hierarchical;

import clus.error.*;
import clus.data.rows.*;
import clus.statistic.*;
import clus.main.*;

import jeans.util.array.*;

import java.io.*;

public class WAHNDSqError extends ClusError {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected HierNodeWeights m_Weights;
	protected boolean m_Square;
	
	protected double m_TreeErr;
	protected double m_SumWeight;    
	protected ClassesAttrType m_Attr;
	
	protected transient double[] m_Mean;
	
	public WAHNDSqError(ClusErrorParent par, ClassesAttrType attr, boolean square, HierNodeWeights weights) {
		super(par, 0);
		m_Weights = weights;
		m_Square = square;
		m_Attr = attr;
	}
	
	public void add(ClusError other) {
		WAHNDSqError err = (WAHNDSqError)other;
		m_TreeErr += err.m_TreeErr;
		m_SumWeight += err.m_SumWeight;
	}
	
	public void showModelError(PrintWriter out, int detail) {
		if (m_Square) out.println(Math.sqrt(m_TreeErr/m_SumWeight));
		else out.println(m_TreeErr/m_SumWeight);
	}	
	
	public void addExample(ClusData data, int idx, ClusStatistic pred) {
		System.out.println("WAHNDSqError: addExample/3 not implemented");
	}
	
	public void addInvalid(DataTuple tuple) {
	}
	
	public double calcDistance(ClassesTuple actual, HierStatistic distr) {
		double[] counts = distr.getCounts();
		if (m_Mean == null) m_Mean = new double[counts.length];
		System.arraycopy(counts, 0, m_Mean, 0, counts.length);
		MDoubleArray.dotscalar(m_Mean, 1.0/distr.getTotalWeight());
		for (int j = 0; j < actual.size(); j++) {
			ClassesValue val = actual.elementAt(j);
			m_Mean[val.getIndex()] -= val.getAbundance();
		}
		double sum = 0.0;
		for(int k = 0; k < counts.length; k++) {
			sum += m_Weights.getWeight(k)*Math.abs(m_Mean[k]);
		}
		if (m_Square) return sum*sum;
		else return sum;
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double weight = tuple.getWeight();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(m_Attr.getArrayIndex());
		m_TreeErr += weight*calcDistance(tp, (HierStatistic)pred);
		m_SumWeight += weight;
	}
	
	public double getModelError() {
		return m_TreeErr/m_SumWeight;
	}
	
	public void reset() {
		m_TreeErr = 0.0;
		m_SumWeight = 0.0;	
	}		
	
	public String getName() {
		String sq = m_Square ? "Root mean squared" : "Mean";
		return sq+" WAHND with "+ m_Weights.getName();
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new WAHNDSqError(par, m_Attr, m_Square, m_Weights);
	}
}
