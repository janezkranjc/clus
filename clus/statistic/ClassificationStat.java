package clus.statistic;

import jeans.math.*;
import jeans.util.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.*;

import clus.main.*;
import clus.util.*;
import clus.data.cols.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.data.attweights.*;

public class ClassificationStat extends ClusStatistic {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public int m_NbTarget;
	public NominalAttrType[] m_Attrs;
	public double[][] m_ClassCounts;
	public int[] m_MajorityClasses;
	
  /**
   * Constructor for this class.
   * @param nomAtts array of nominal attributes
   */
	public ClassificationStat(NominalAttrType[] nomAtts) {
  		m_NbTarget = nomAtts.length;
  		m_ClassCounts = new double[m_NbTarget][];
    	for (int i = 0; i < m_NbTarget; i++) {
    		m_ClassCounts[i] = new double[nomAtts[i].getNbValues()];
    	}
    	m_Attrs = nomAtts;
  	} 

  	public int getNbNominalAttributes() {
  		return m_NbTarget;
  	}

  	public ClusStatistic cloneStat() {
  		return new ClassificationStat(m_Attrs);
  	}
	
	public void initSingleTargetFrom(double[] distro) {
		m_ClassCounts[0] = distro;
		m_SumWeight = 0.0;
		for (int i = 0; i < distro.length; i++) {
			m_SumWeight += distro[i];
		}
	}

  /** Added because RegressionStat has this method.
   * 
   * @param idx
   * @return
   */
  public NominalAttrType getAttribute(int idx) {
    return m_Attrs[idx];
  }

	public void reset() {
		m_SumWeight = 0.0;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] clcts = m_ClassCounts[i];
			for (int j = 0; j < clcts.length; j++) clcts[j] = 0.0;		
		}
	}
	
	public void copy(ClusStatistic other) {
		ClassificationStat or = (ClassificationStat)other;	
		m_SumWeight = or.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			System.arraycopy(your, 0, my, 0, my.length);
		}
	}	
	
	public boolean samePrediction(ClusStatistic other) {
		ClassificationStat or = (ClassificationStat)other;
		for (int i = 0; i < m_NbTarget; i++) 
			if (m_MajorityClasses[i] != or.m_MajorityClasses[i]) 
				return false;
		return true;
	}	
	
	public void addPrediction(ClusStatistic other, double weight) {
		ClassificationStat or = (ClassificationStat)other;
		m_SumWeight += weight*or.m_SumWeight;		
		for (int i = 0; i < m_NbTarget; i++) {		
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] += weight*your[j];
		}
	}	
	
	public void add(ClusStatistic other) {
		ClassificationStat or = (ClassificationStat)other;
		m_SumWeight += or.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] += your[j];
		}
	}
	
	public void addScaled(double scale, ClusStatistic other) {
		ClassificationStat or = (ClassificationStat)other;
		m_SumWeight += scale*or.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] += scale*your[j];
		}
	}		
	
	public void subtractFromThis(ClusStatistic other) {
		ClassificationStat or = (ClassificationStat)other;
		m_SumWeight -= or.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] -= your[j];
		}
	}
	
	public void subtractFromOther(ClusStatistic other) {
		ClassificationStat or = (ClassificationStat)other;
		m_SumWeight = or.m_SumWeight - m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] = your[j] - my[j];
		}
	}	
	
	public void update(ColTarget target, int idx) {
		m_SumWeight += 1.0;		
		int[] values = target.m_Nominal[idx];
		for (int i = 0; i < m_NbTarget; i++) {
			m_ClassCounts[i][values[i]] += 1.0;
		}
	}		
	
	public void update(int j, int[] values) {
		m_SumWeight += 1.0;		
		for (int i = 0; i < m_NbTarget; i++) {
			m_ClassCounts[i][values[i]] += 1.0;
		}
	}	
	
	public void updateWeighted(DataTuple tuple, int idx) {
		updateWeighted(tuple, tuple.getWeight());
	}
	
	public void updateWeighted(DataTuple tuple, double weight) {
		m_SumWeight += weight;		
		// int[] values = tuple.m_Ints;		
		int[] values = tuple.m_Ints;
		for (int i = 0; i < m_NbTarget; i++) {
			int val = values[m_Attrs[i].getArrayIndex()];
			if (val != m_Attrs[i].getNbValues()) {				
				m_ClassCounts[i][val] += weight;
			}
		}	
	}
	
	public int getMajorityClass(int attr) {
		int m_class = -1;
		double m_max = Double.NEGATIVE_INFINITY;
		double[] clcts = m_ClassCounts[attr];
		for (int i = 0; i < clcts.length; i++) {
			if (clcts[i] > m_max) {
				m_class = i;
				m_max = clcts[i];
			}
		}
		return m_class;
	}
	
	public int getMajorityClassDiff(int attr, ClassificationStat other) {
		int m_class = -1;
		double m_max = Double.NEGATIVE_INFINITY;
		double[] clcts1 = m_ClassCounts[attr];
		double[] clcts2 = other.m_ClassCounts[attr];
		for (int i = 0; i < clcts1.length; i++) {
			double diff = clcts1[i] - clcts2[i];
			if (diff > m_max) {
				m_class = i;
				m_max = diff;
			}
		}
		return m_class;
	}
	
	public double entropy(int attr, double total) {
		if (total < 1e-6) {
			return 0.0;
		} else {
			double acc = 0.0;
			double[] clcts = m_ClassCounts[attr];
			for (int i = 0; i < clcts.length; i++) {
				if (clcts[i] != 0.0) {
					double prob = clcts[i]/total;
					acc += prob*Math.log(prob);
				}
			}
			return -acc/MathUtil.M_LN2;
		}
	}
	
	public double entropyDifference(int attr, ClassificationStat other, double total) {
		double acc = 0.0;
		double[] clcts = m_ClassCounts[attr];
		double[] otcts = other.m_ClassCounts[attr];
		for (int i = 0; i < clcts.length; i++) {
			double diff = clcts[i] - otcts[i];
			if (diff != 0.0) acc += diff/total*Math.log(diff/total);
		}
		return -acc/MathUtil.M_LN2;
	}	
	
	public static double computeSplitInfo(double sum_tot, double sum_pos, double sum_neg) {
		if (sum_pos == 0.0)
			return -1.0*sum_neg/sum_tot*Math.log(sum_neg/sum_tot)/MathUtil.M_LN2;
		if (sum_neg == 0.0)
			return -1.0*sum_pos/sum_tot*Math.log(sum_pos/sum_tot)/MathUtil.M_LN2;
		return -(sum_pos/sum_tot*Math.log(sum_pos/sum_tot) + 
				sum_neg/sum_tot*Math.log(sum_neg/sum_tot))/MathUtil.M_LN2;
	}	
	
	public boolean isCalcMean() {
		return m_MajorityClasses != null;
	}
	
	public void calcMean() {
		m_MajorityClasses = new int[m_NbTarget];
		for (int i = 0; i < m_NbTarget; i++) {
      m_MajorityClasses[i] = getMajorityClass(i); 
    }
	}
	
	public double[] getNumericPred() {
		return null;
	}
	
	public int[] getNominalPred() {
		return m_MajorityClasses;
	}	
	
	public String getString2() {
		StringBuffer buf = new StringBuffer();		
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		buf.append(fr.format(m_SumWeight));
		buf.append(" ");
		buf.append(super.toString());
		return buf.toString();
	}	
	
	public String getString() {
		StringBuffer buf = new StringBuffer();		
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		if (m_MajorityClasses != null) {
			buf.append("[");
			for (int i = 0; i < m_NbTarget; i++) {
				if (i != 0) buf.append(",");
				buf.append(m_Attrs[i].getValue(m_MajorityClasses[i]));
			}
			buf.append("]");
		} else {
			buf.append("?");
		}
		for (int j = 0; j < m_NbTarget; j++) {		
			buf.append("[");
			for (int i = 0; i < m_ClassCounts[j].length; i++) {
				if (i != 0) buf.append(",");
				buf.append(m_Attrs[j].getValue(i));
				buf.append(":");
				buf.append(fr.format(m_ClassCounts[j][i]));
			}
			buf.append("]");
		}
		buf.append(":");		
		buf.append(fr.format(m_SumWeight));
		return buf.toString();
	}
	
	public int getNbClasses(int idx) {
		return m_ClassCounts[idx].length;
	}
	
	public double getCount(int idx, int cls) {
		return m_ClassCounts[idx][cls];
	}	
	
	public String getPredictedClassName(int idx) {
		return m_Attrs[idx].getValue(m_MajorityClasses[idx]);
	} 
	
	public String getClassName(int idx, int cls) {
		return m_Attrs[idx].getValue(cls);
	} 
	
	public void setCount(int idx, int cls, double count) {
		m_ClassCounts[idx][cls] = count;
	}
	
	public String getSimpleString() {
		return getClassString() + " : " + super.getSimpleString();
	}	
	
	public String getClassString() {
		StringBuffer buf = new StringBuffer();		
		for (int i = 0; i < m_NbTarget; i++) {
			if (i != 0) buf.append(",");
			buf.append(m_Attrs[i].getValue(m_MajorityClasses[i]));
		}
		return buf.toString();
	}
	
	public double getError(ClusAttributeWeights scale) {
		double result = 0.0;
		for (int i = 0; i < m_NbTarget; i++) {
			int maj = getMajorityClass(i);
			result += m_SumWeight - m_ClassCounts[i][maj]; 
		}
		return result / m_NbTarget;
	}
	
	public double getErrorDiff(ClusAttributeWeights scale, ClusStatistic other) {
		double result = 0.0;
		ClassificationStat or = (ClassificationStat)other;
		double diff_total = m_SumWeight - other.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			int maj = getMajorityClassDiff(i, or);
			double diff_maj = m_ClassCounts[i][maj] - or.m_ClassCounts[i][maj];
			result += diff_total - diff_maj; 
		}
		return result / m_NbTarget;
	}
	
	public double getSS(ClusAttributeWeights scale) {
		double result = 0.0;
		double sum = m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			result += entropy(i, sum) * sum; 
		}
		return result / m_NbTarget;
	}
	
	public double getSSDiff(ClusAttributeWeights scale, ClusStatistic other) {
		double result = 0.0;
		double sum = m_SumWeight - other.m_SumWeight;
		ClassificationStat cother = (ClassificationStat)other;
		for (int i = 0; i < m_NbTarget; i++) {
			result += entropyDifference(i, cother, sum) * sum; 
		}
		return result / m_NbTarget;		
	}
	
	public double[] getClassCounts(int i) {
		return m_ClassCounts[i];
	}
	
	public String toString() {
		return getString();
	}
	
	public void printDistribution(PrintWriter wrt) throws IOException {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		for (int i = 0; i < m_Attrs.length; i++) {
			wrt.print(StringUtils.printStr(m_Attrs[i].getName(), 35));
			wrt.print(" [");
			double sum = 0.0;
			for (int j = 0; j < m_ClassCounts[i].length; j++) {
				if (j != 0) wrt.print(",");
				wrt.print(m_Attrs[i].getValue(j)+":");
				wrt.print(fr.format(m_ClassCounts[i][j]));
				sum += m_ClassCounts[i][j];
			}
			wrt.println("]: "+fr.format(sum));	
		}
	}	
}
