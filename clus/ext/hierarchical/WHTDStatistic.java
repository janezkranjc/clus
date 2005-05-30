/*
 * Created on May 17, 2005
 */
package clus.ext.hierarchical;

import java.io.*;

import org.apache.commons.math.distribution.*;
import org.apache.commons.math.*;

import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;
import clus.util.*;

import jeans.util.array.*;

public class WHTDStatistic extends RegressionStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected static DistributionFactory m_Fac = DistributionFactory.newInstance();
	
	protected ClassHierarchy m_Hier;
	protected ClassesTuple m_MeanTuple;
	protected double[] m_DiscrMean;	
	protected WHTDStatistic m_Global, m_Validation;
	protected double m_SigLevel;
		
	public WHTDStatistic(ClassHierarchy hier) {
		this(hier, false);
	}

	public WHTDStatistic(ClassHierarchy hier, boolean onlymean) {
		super(hier.getTotal(), onlymean);
		m_Hier = hier;		
	}
	
	public void setValidationStat(WHTDStatistic valid) {
		m_Validation = valid;
	}
	
	public void setGlobalStat(WHTDStatistic global) {
		m_Global = global;
	}	
	
	public void setSigLevel(double sig) {
		m_SigLevel = sig;
	}
	
	public ClusStatistic cloneStat() {
		return new WHTDStatistic(m_Hier, false);
	}
	
	public ClusStatistic cloneSimple() {
		return new WHTDStatistic(m_Hier, true);		
	}
	
	public void addPrediction(ClusStatistic other, double weight) {
		WHTDStatistic or = (WHTDStatistic)other;
		super.addPrediction(other, weight);
		if (or.m_Validation != null) {
			if (m_Validation == null) { 
				m_Validation = (WHTDStatistic)or.m_Validation.cloneSimple();
				m_Global = or.m_Global;
				m_SigLevel = or.m_SigLevel;
			}
			m_Validation.addPrediction(or.m_Validation, weight);
		}
	}
	
	public void updateWeighted(DataTuple tuple, double weight) {
		int sidx = m_Hier.getType().getSpecialIndex();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
		m_SumWeight += weight;
		// Add one to the elements in the tuple, zero to the others
		for (int j = 0; j < tp.size(); j++) {
			ClassesValue val = tp.elementAt(j);
			int idx = val.getIndex();
			m_SumValues[idx] += weight;
			m_SumSqValues[idx] += weight;
		}
		// No missing values in the hierarchy
		for (int i = 0; i < m_SumWeights.length; i++) {
			m_SumWeights[i] += weight;
		}
	}
	
	public boolean isValid() {
		// There is a valid prediction the weight of a non-root class is non-zero
		for (int i = 1; i < m_NbTarget; i++) {
			if (getMean(i) >= 1) return true;
		}
		return false;
	}
	
	public final ClassHierarchy getHier() {
		return m_Hier;
	}
	
	public void calcMean() {
		super.calcMean();
		m_MeanTuple = m_Hier.getBestTupleMaj(m_Means);
		m_DiscrMean = m_MeanTuple.getVectorWithParents(m_Hier);
		if (m_Validation != null) {
			for (int i = 0; i < m_DiscrMean.length; i++) {
				if (m_DiscrMean[i] > 0.5) {
					/* Predicted class i, check sig? */
					int nb_correct = (int)(m_Validation.getTotalWeight()*m_Validation.m_Means[i]);
					int sampleSize = (int)m_Validation.getTotalWeight();
					int populationSize = (int)m_Global.getTotalWeight();
					int numberOfSuccesses = (int)(m_Global.getTotalWeight()*m_Global.m_Means[i]);
					HypergeometricDistribution dist = m_Fac.createHypergeometricDistribution(populationSize, numberOfSuccesses, sampleSize);
					try {
						double stat = dist.cumulativeProbability(nb_correct, sampleSize);
						if (stat >= m_SigLevel) {
							m_DiscrMean[i] = 0.0;
						}
					} catch (MathException me) {
						System.err.println("Math error: "+me.getMessage());
					}
				}
			}
			m_MeanTuple = m_Hier.getBestTupleMaj(m_DiscrMean);
		}
	}
	
	protected ClassesTuple getMeanTuple() {
		return m_MeanTuple;
	}
	
	public double[] getDiscretePred() {
		return m_DiscrMean;
	}
	
	public void printTree() {
		m_Hier.print(ClusFormat.OUT_WRITER, m_SumValues);
		ClusFormat.OUT_WRITER.flush();
	}
	
	public String getString() {
//		return m_MeanTuple.toStringHuman()+" "+super.getString();
		return m_MeanTuple.toStringHuman()+" "+getTotalWeight();
	}
	
	public void showRootInfo() {
		try {
			double[] weights = m_Hier.getWeights();
			PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream("hierarchy.txt")));
			wrt.println("Hier #nodes: "+m_Hier.getTotal());
			wrt.println("Hier classes by level: "+MIntArray.toString(m_Hier.getClassesByLevel()));
			wrt.println("Root weighs: "+weights[m_Hier.getRoot().getIndex()]);
			m_Hier.print(wrt, m_SumValues, null);
			wrt.close();
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		}
	}	
}
