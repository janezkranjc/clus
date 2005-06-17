package clus.ext.hierarchical;

import jeans.util.array.*;

import clus.main.*;
import clus.statistic.*;
import clus.data.cols.*;
import clus.data.rows.*;
import clus.util.*;

public abstract class HierStatistic extends ClusStatistic {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected ClassHierarchy m_Hier;
	protected double[] m_Counts;
	protected double[] m_Variances;	
	public ClassesTuple m_MeanTuple;
		
	public HierStatistic(ClassHierarchy hier) {
		m_Hier = hier;
		m_Counts = new double[hier.getTotal()];
	}
	
	public final ClassHierarchy getHier() {
		return m_Hier;
	}
	
	public ClassesTuple getMeanTuple() {
		return m_MeanTuple;
		
	}
	
	public void update(ColTarget target, int idx) {
		System.out.println("update(): Not yet implemented");
	}
	
	public final double[] getCounts() {
		return m_Counts;
	}
	
	public final double[] getVariances() {
		
		return m_Variances; 
		
	}
	public void addPrediction(ClusStatistic other, double weight) {
		HierStatistic or = (HierStatistic)other;
		m_SumWeight += weight*or.m_SumWeight;
		MDoubleArray.add(m_Counts, or.m_Counts, weight);
	}
	
	public void calcMean() {
		m_MeanTuple = m_Hier.getBestTuple(m_Counts, m_SumWeight);
	}
	
	public void initVariance() {
		
		m_Variances = new double[m_Counts.length];
	}
	
	public void divideVariance(){
		
		MDoubleArray.dotscalar(m_Variances, 1.0/m_SumWeight);
	}
	
	public void addToVariance(DataTuple tuple) {
		
		double[] meanDif = new double[m_Counts.length];
		
		ClassesAttrType type = m_Hier.getType();		
		int sidx = type.getArrayIndex();
		System.arraycopy(m_Counts, 0, meanDif, 0, meanDif.length);
		MDoubleArray.dotscalar(meanDif, 1.0/m_SumWeight);				 
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
		for (int j = 0; j < tp.size(); j++) {
			ClassesValue val = tp.elementAt(j);
			int idx = val.getIndex();
			meanDif[idx] -= val.getAbundance();
		}
		for(int k=0; k < meanDif.length; k ++) {
			m_Variances[k] += meanDif[k]*meanDif[k];
		}
		
		
	}
	
	public void printTree() {
		m_Hier.print(ClusFormat.OUT_WRITER, m_Counts);
		ClusFormat.OUT_WRITER.flush();
	}
	
	public String getString() {
		return m_MeanTuple.toString();
	}
	
	public double[] getNumericPred() {
		System.out.println("getNumericPred(): Not yet implemented");
		return null;
	}
	
	public int[] getNominalPred() {
		System.out.println("getNumericPred(): Not yet implemented");
		return null;
	}
}
