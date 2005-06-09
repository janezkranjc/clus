package clus.statistic;

import java.text.*;

import clus.main.Settings;
import clus.util.*;
import clus.data.cols.*;
import clus.data.rows.*;

public class RegressionStat extends ClusStatistic {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;	
	
	public int m_NbTarget;
	public double[] m_SumValues;
	public double[] m_SumWeights;
	public double[] m_SumSqValues;
	public double[] m_Means;

	public RegressionStat(int nbTarget) {
		this(nbTarget, false);
	}
	
	public RegressionStat(int nbTarget, boolean onlymean) {
		m_NbTarget = nbTarget;
		if (onlymean) {
			m_Means = new double[m_NbTarget];
		} else {
			m_SumValues = new double[m_NbTarget];
			m_SumWeights = new double[m_NbTarget];		
			m_SumSqValues = new double[m_NbTarget];			
		}
	}
	
	public ClusStatistic cloneStat() {
		return new RegressionStat(m_NbTarget, false);
	}
	
	public ClusStatistic cloneSimple() {
		return new RegressionStat(m_NbTarget, true);		
	}
	
	public void reset() {
		m_SumWeight = 0.0;
		for (int i = 0; i < m_NbTarget; i++) {
			m_SumWeights[i] = 0.0;
			m_SumValues[i] = 0.0;
			m_SumSqValues[i] = 0.0;
		}
	}
	
	public void copy(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;	
		m_SumWeight = or.m_SumWeight;
		System.arraycopy(or.m_SumWeights, 0, m_SumWeights, 0, m_NbTarget);
		System.arraycopy(or.m_SumValues, 0, m_SumValues, 0, m_NbTarget);
		System.arraycopy(or.m_SumSqValues, 0, m_SumSqValues, 0, m_NbTarget);
	}	

	public void addPrediction(ClusStatistic other, double weight) {
		RegressionStat or = (RegressionStat)other;
		for (int i = 0; i < m_NbTarget; i++) {		
			m_Means[i] += weight*or.m_Means[i];
		}
	}
	
	public void add(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight += or.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			m_SumWeights[i] += or.m_SumWeights[i];
			m_SumValues[i] += or.m_SumValues[i];
			m_SumSqValues[i] += or.m_SumSqValues[i];
		}
	}	
	
	public void subtractFromThis(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight -= or.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			m_SumWeights[i] -= or.m_SumWeights[i];
			m_SumValues[i] -= or.m_SumValues[i];
			m_SumSqValues[i] -= or.m_SumSqValues[i];
		}
	}		
	
	public void subtractFromOther(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight = or.m_SumWeight - m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			m_SumWeights[i] = or.m_SumWeights[i] - m_SumWeights[i];
			m_SumValues[i] = or.m_SumValues[i] - m_SumValues[i];
			m_SumSqValues[i] = or.m_SumSqValues[i] - m_SumSqValues[i];
		}
	}			

	public void update(ColTarget target, int idx) {
		m_SumWeight += 1.0;
		double[] values = target.m_Numeric[idx];
		for (int i = 0; i < m_NbTarget; i++) {
			double val = values[i];
			if (val != Double.POSITIVE_INFINITY) {
				m_SumWeights[i] += 1.0;
				m_SumValues[i] += val;
				m_SumSqValues[i] += val*val;
			}
		}
	}	
	
	public void updateWeighted(DataTuple tuple, int idx) {
		updateWeighted(tuple, tuple.getWeight());
	}
	
	public void updateWeighted(DataTuple tuple, double weight) {
		m_SumWeight += weight;
		for (int i = 0; i < m_NbTarget; i++) {
			double val = tuple.m_Doubles[i];
			if (val != Double.POSITIVE_INFINITY) {
				m_SumWeights[i] += weight;
				m_SumValues[i] += weight*val;
				m_SumSqValues[i] += weight*val*val;
			}
		}	
	}	

	public void calcMean() {
		if (m_Means == null) {
			m_Means = new double[m_NbTarget];
			for (int i = 0; i < m_NbTarget; i++) {
				m_Means[i] = m_SumWeights[i] != 0.0 ? m_SumValues[i] / m_SumWeights[i] : 0.0;
			}
		}
	}
	
	public double getMean(int i) {
		return m_SumWeights[i] != 0.0 ? m_SumValues[i] / m_SumWeights[i] : 0.0;		
	}
	
	public double getVariance(int i) {
		double n_tot = m_SumWeight; 
		double k_tot = m_SumWeights[i];
		double sv_tot = m_SumValues[i];
		double ss_tot = m_SumSqValues[i];
		double var = (k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0;
		return var / n_tot;
	}
	
	public double getStandardDeviation(int i) {
		double n_tot = m_SumWeight; 
		double k_tot = m_SumWeights[i];
		double sv_tot = m_SumValues[i];
		double ss_tot = m_SumSqValues[i];
		double var = (k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0;
		return Math.sqrt(var / (n_tot - 1));
	}
	
	public double[] getNumericPred() {
		return m_Means;
	}
	
	public int getNbTarget() {
		return m_NbTarget;
	}
	
	public double getError(TargetWeightProducer scale) {
		return getSS(scale);
	}
	
	public double getErrorDiff(TargetWeightProducer scale, ClusStatistic other) {
		return getSSDiff(scale, other);
	}	

	public double getSS(TargetWeightProducer scale) {
		double result = 0.0;
		for (int i = 0; i < m_NbTarget; i++) {
			double n_tot = m_SumWeight; 
			double k_tot = m_SumWeights[i];
			double sv_tot = m_SumValues[i];
			double ss_tot = m_SumSqValues[i];
			result += ((k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0)*scale.m_NumWeights[i];
		}
		return result / m_NbTarget;
	}
	
	public double getSSDiff(TargetWeightProducer scale, ClusStatistic other) {
		double result = 0.0;
		RegressionStat or = (RegressionStat)other;
		for (int i = 0; i < m_NbTarget; i++) {
			double n_tot = m_SumWeight - or.m_SumWeight; 
			double k_tot = m_SumWeights[i] - or.m_SumWeights[i];
			double sv_tot = m_SumValues[i] - or.m_SumValues[i];
			double ss_tot = m_SumSqValues[i] - or.m_SumSqValues[i];
			result += ((k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0)*scale.m_NumWeights[i];
		}
		return result / m_NbTarget;
	}	
		
	public String getString() {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		StringBuffer buf = new StringBuffer();		
		buf.append("[");
		for (int i = 0; i < m_NbTarget; i++) {
			if (i != 0) buf.append(",");
			buf.append(fr.format(m_Means[i]));
		}
		buf.append("]");		
		return buf.toString();
	}
	
	public String getDebugString() {
		NumberFormat fr = ClusFormat.THREE_AFTER_DOT;
		StringBuffer buf = new StringBuffer();		
		buf.append("[");
		for (int i = 0; i < m_NbTarget; i++) {
			if (i != 0) buf.append(",");
			buf.append(fr.format(getMean(i)));
		}
		buf.append("]");
		buf.append("[");
		for (int i = 0; i < m_NbTarget; i++) {
			if (i != 0) buf.append(",");
			buf.append(fr.format(getVariance(i)));
		}
		buf.append("]");		
		return buf.toString();
	}
	
	public void printDebug() {
		for (int i = 0; i < getNbTarget(); i++) {
			double n_tot = m_SumWeight; 
			double k_tot = m_SumWeights[i];
			double sv_tot = m_SumValues[i];
			double ss_tot = m_SumSqValues[i];
			System.out.println("n: "+n_tot+" k: "+k_tot);
			System.out.println("sv: "+sv_tot);
			System.out.println("ss: "+ss_tot);			
			double mean = sv_tot / n_tot;
			double var = ss_tot - n_tot*mean*mean;
			System.out.println("mean: "+mean);			
			System.out.println("var: "+var);					
		}
		System.out.println("err: "+getError());		
	}
}
