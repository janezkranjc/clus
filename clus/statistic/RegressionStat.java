package clus.statistic;

import java.text.*;

import clus.main.Settings;
import clus.util.*;
import clus.data.cols.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.data.attweights.*;

public class RegressionStat extends ClusStatistic {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;	
	
	public int m_NbAttrs;
	public double[] m_SumValues;
	public double[] m_SumWeights;
	public double[] m_SumSqValues;
	public double[] m_Means;
	public NumericAttrType[] m_Attrs;

	public RegressionStat(NumericAttrType[] attrs) {
		this(attrs, false);
	}
	
	public RegressionStat(NumericAttrType[] attrs, boolean onlymean) {
		m_Attrs = attrs;
		m_NbAttrs = attrs.length;
		if (onlymean) {
			m_Means = new double[m_NbAttrs];
		} else {
			m_SumValues = new double[m_NbAttrs];
			m_SumWeights = new double[m_NbAttrs];		
			m_SumSqValues = new double[m_NbAttrs];			
		}
	}
	
	// FIXME: delete this constructor after issue with hierarchical classification is solved
	public RegressionStat(int nb, boolean onlymean) {		
	}
	
	public ClusStatistic cloneStat() {
		return new RegressionStat(m_Attrs, false);
	}
	
	public ClusStatistic cloneSimple() {
		return new RegressionStat(m_Attrs, true);		
	}
	
	public NumericAttrType getAttribute(int idx) {
		return m_Attrs[idx];
	}
	
	public void reset() {
		m_SumWeight = 0.0;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] = 0.0;
			m_SumValues[i] = 0.0;
			m_SumSqValues[i] = 0.0;
		}
	}
	
	public void copy(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;	
		m_SumWeight = or.m_SumWeight;
		System.arraycopy(or.m_SumWeights, 0, m_SumWeights, 0, m_NbAttrs);
		System.arraycopy(or.m_SumValues, 0, m_SumValues, 0, m_NbAttrs);
		System.arraycopy(or.m_SumSqValues, 0, m_SumSqValues, 0, m_NbAttrs);
	}	

	public void addPrediction(ClusStatistic other, double weight) {
		RegressionStat or = (RegressionStat)other;
		for (int i = 0; i < m_NbAttrs; i++) {		
			m_Means[i] += weight*or.m_Means[i];
		}
	}
	
	public void add(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight += or.m_SumWeight;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] += or.m_SumWeights[i];
			m_SumValues[i] += or.m_SumValues[i];
			m_SumSqValues[i] += or.m_SumSqValues[i];
		}
	}	
	
	public void subtractFromThis(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight -= or.m_SumWeight;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] -= or.m_SumWeights[i];
			m_SumValues[i] -= or.m_SumValues[i];
			m_SumSqValues[i] -= or.m_SumSqValues[i];
		}
	}		
	
	public void subtractFromOther(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight = or.m_SumWeight - m_SumWeight;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] = or.m_SumWeights[i] - m_SumWeights[i];
			m_SumValues[i] = or.m_SumValues[i] - m_SumValues[i];
			m_SumSqValues[i] = or.m_SumSqValues[i] - m_SumSqValues[i];
		}
	}			

	public void update(ColTarget target, int idx) {
		m_SumWeight += 1.0;
		double[] values = target.m_Numeric[idx];
		for (int i = 0; i < m_NbAttrs; i++) {
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
		for (int i = 0; i < m_NbAttrs; i++) {
			double val = tuple.m_Doubles[m_Attrs[i].getArrayIndex()];
			if (val != Double.POSITIVE_INFINITY) {
				m_SumWeights[i] += weight;
				m_SumValues[i] += weight*val;
				m_SumSqValues[i] += weight*val*val;
			}
		}	
	}	

	public void calcMean() {
		if (m_Means == null) {
			m_Means = new double[m_NbAttrs];
			for (int i = 0; i < m_NbAttrs; i++) {
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
	
  public int getNbNumericAttributes() {
    return m_NbAttrs;
  }
	
	public double getError(ClusAttributeWeights scale) {
		return getSS(scale);
	}
	
	public double getErrorDiff(ClusAttributeWeights scale, ClusStatistic other) {
		return getSSDiff(scale, other);
	}	

	public double getSS(ClusAttributeWeights scale) {
		double result = 0.0;
		for (int i = 0; i < m_NbAttrs; i++) {
			double n_tot = m_SumWeight; 
			double k_tot = m_SumWeights[i];
			double sv_tot = m_SumValues[i];
			double ss_tot = m_SumSqValues[i];
			result += ((k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0)*scale.getWeight(m_Attrs[i]);
		}
		return result / m_NbAttrs;
	}
	
	public double getSSDiff(ClusAttributeWeights scale, ClusStatistic other) {
		double result = 0.0;
		RegressionStat or = (RegressionStat)other;
		for (int i = 0; i < m_NbAttrs; i++) {
			double n_tot = m_SumWeight - or.m_SumWeight; 
			double k_tot = m_SumWeights[i] - or.m_SumWeights[i];
			double sv_tot = m_SumValues[i] - or.m_SumValues[i];
			double ss_tot = m_SumSqValues[i] - or.m_SumSqValues[i];
			result += ((k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0)*scale.getWeight(m_Attrs[i]);
		}
		return result / m_NbAttrs;
	}	

	public void initNormalizationWeights(ClusAttributeWeights weights, boolean[] shouldNormalize) {
		for (int i = 0; i < m_NbAttrs; i++) {
			int idx = m_Attrs[i].getIndex();
			if (shouldNormalize[idx]) weights.setWeight(m_Attrs[i], 1/getVariance(i));
		}
	}	
	
	public String getString() {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		StringBuffer buf = new StringBuffer();		
		buf.append("[");
		for (int i = 0; i < m_NbAttrs; i++) {
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
		for (int i = 0; i < m_NbAttrs; i++) {
			if (i != 0) buf.append(",");
			buf.append(fr.format(getMean(i)));
		}
		buf.append("]");
		buf.append("[");
		for (int i = 0; i < m_NbAttrs; i++) {
			if (i != 0) buf.append(",");
			buf.append(fr.format(getVariance(i)));
		}
		buf.append("]");		
		return buf.toString();
	}
	
	public void printDebug() {
		for (int i = 0; i < getNbAttributes(); i++) {
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
