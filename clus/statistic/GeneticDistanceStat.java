package clus.statistic;

import java.text.NumberFormat;
import java.util.ArrayList;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.cols.ColTarget;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.NominalAttrType;
import clus.main.Settings;
import clus.util.ClusFormat;
import jeans.list.BitList;

public class GeneticDistanceStat extends BitVectorStat {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public int m_NbTarget;
	public NominalAttrType[] m_Attrs;
	public double[][] m_ClassCounts;
	public int[] m_MajorityClasses;

  /**
   * Constructor for this class.
   * @param nomAtts array of nominal attributes
   */
	public GeneticDistanceStat(NominalAttrType[] nomAtts) {
		m_NbTarget = nomAtts.length;
		m_Attrs = nomAtts;
		m_ClassCounts = new double[m_NbTarget][];
		for (int i = 0; i < m_NbTarget; i++) {
			m_ClassCounts[i] = new double[nomAtts[i].getNbValues()];
		}
	}
	
	public BitList getBits() {
		return m_Bits;
	}
	
	
	public void updateWeighted(DataTuple tuple, int idx) {
		m_SumWeight += tuple.getWeight();
		m_Bits.setBit(idx);
		m_Modified = true;

		for (int i = 0; i < m_NbTarget; i++) {
			int val = m_Attrs[i].getNominal(tuple);
//			System.out.println("val: "+ val);
			if (val != m_Attrs[i].getNbValues()) {
				m_ClassCounts[i][val] += tuple.getWeight();
			}
		}	
	}
	
	
	// returns the index of the index-th tuple in the statistic (i.e. with bit 1)
	public int getTupleIndex(int index) {
		int size = getBits().size();
		int nbones=0;
		int i=0;
		while (nbones<=index && i<size) {
			if (getBits().getBit(i)) {
				nbones++;
			}
			i++;
		}
		if (nbones == index+1) {
			return i-1;
		}
		else {
			System.err.println("error in getTuple (GeneticDistanceStat), requesting tuple" + index);
			return -1;
		}
	}
	

	public void reset() {
		m_SumWeight = 0.0;
		m_Bits.reset();
		m_Modified = true;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] clcts = m_ClassCounts[i];
			for (int j = 0; j < clcts.length; j++) clcts[j] = 0.0;
		}
	}

	
	public GeneticDistanceStat cloneStat() {
		GeneticDistanceStat stat = new GeneticDistanceStat(m_Attrs);
		stat.cloneFrom(this);
		return stat;
	}

	public void cloneFrom(GeneticDistanceStat other) {
		int nb = other.m_Bits.size();
		m_NbTarget = other.m_NbTarget;
		m_Attrs = other.m_Attrs;
		if (nb > 0) {
			//System.out.println("Cloned nonzero size GeneticDistance stat " + nb);
			setSDataSize(nb);
		}
	}


	public void copy(ClusStatistic other) {
		GeneticDistanceStat or = (GeneticDistanceStat)other;
		m_SumWeight = or.m_SumWeight;
		m_Bits.copy(or.m_Bits);
		m_Modified = or.m_Modified;
		m_NbTarget = or.m_NbTarget;
		m_Attrs = or.m_Attrs;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			System.arraycopy(your, 0, my, 0, my.length);
		}
	}

	public void addPrediction(ClusStatistic other, double weight) {
		GeneticDistanceStat or = (GeneticDistanceStat)other;
		m_SumWeight += weight*or.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] += weight*your[j];
		}
	}

	public void add(ClusStatistic other) {
		GeneticDistanceStat or = (GeneticDistanceStat)other;
		m_SumWeight += or.m_SumWeight;
		m_Bits.add(or.m_Bits);
		m_Modified = true;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] += your[j];
		}
	}

	public void addScaled(double scale, ClusStatistic other) {
		GeneticDistanceStat or = (GeneticDistanceStat)other;
		m_SumWeight += scale*or.m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] += scale*your[j];
		}
	}
	
	
	public void subtractFromThis(ClusStatistic other) {
		GeneticDistanceStat or = (GeneticDistanceStat)other;
		m_SumWeight -= or.m_SumWeight;
		m_Bits.subtractFromThis(or.m_Bits);
		m_Modified = true;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] -= your[j];
		}
	}

	public void subtractFromOther(ClusStatistic other) {
		GeneticDistanceStat or = (GeneticDistanceStat)other;
		m_SumWeight = or.m_SumWeight - m_SumWeight;
		m_Bits.subtractFromOther(or.m_Bits);
		m_Modified = true;
		for (int i = 0; i < m_NbTarget; i++) {
			double[] my = m_ClassCounts[i];
			double[] your = or.m_ClassCounts[i];
			for (int j = 0; j < my.length; j++) my[j] = your[j] - my[j];
		}
	}
	
	
	public int[] getNominalPred() {
		System.out.println("getNominalPred: not implemented for GeneticDistanceStat");
		return  null;
	}
		
	
	public String getString(StatisticPrintInfo info) {
		StringBuffer buf = new StringBuffer();
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		if (m_MajorityClasses != null) {//print the name of the majority class
			buf.append("[");
			for (int i = 0; i < m_NbTarget; i++) {
				if (i != 0) buf.append(",");
				buf.append(m_Attrs[i].getValue(m_MajorityClasses[i]));
			}
			buf.append("]");
		}
		else {
			buf.append("?");
		}
		if (info.SHOW_DISTRIBUTION) {
			for (int j = 0; j < m_NbTarget; j++) {
				buf.append(" [");
				for (int i = 0; i < m_ClassCounts[j].length; i++) {
					if (i != 0) buf.append(",");
					buf.append(m_Attrs[j].getValue(i));
					buf.append(":");
					buf.append(fr.format(m_ClassCounts[j][i]));
				}
				buf.append("]");
			}//end for
			if (info.SHOW_EXAMPLE_COUNT) {
				buf.append(":");
				buf.append(fr.format(m_SumWeight));
			}
		}//end if show distribution
		else {
			// print stat on the majority classes
			if (m_MajorityClasses != null) {
				buf.append(" [");
				for (int i = 0; i < m_NbTarget; i++) {
					if (i != 0) buf.append(",");
					buf.append(m_ClassCounts[i][m_MajorityClasses[i]]);
				}
				// added colon here to make trees print correctly
				buf.append("]: ");
				buf.append(fr.format(m_SumWeight));
			}
		}
		return buf.toString();
	}
	
	
	
	public void calcMean() {
		m_MajorityClasses = new int[m_NbTarget];
		for (int i = 0; i < m_NbTarget; i++) {
			m_MajorityClasses[i] = getMajorityClass(i);
		}
	}
	
	public double getCount(int idx, int cls) {
		return m_ClassCounts[idx][cls];
	}
	
	public ClusStatistic getClassificationStat() {
		return this;
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

	public String getPredictedClassName(int idx) {
		return m_Attrs[idx].getValue(getMajorityClass(idx));
	} 
	
	public double[][] getProbabilityPrediction(){
		double[][] result = new double[m_NbTarget][];
		for (int i = 0; i < m_NbTarget; i++) {//for each target
			double total = 0.0;
			for (int k = 0; k < m_ClassCounts[i].length; k++)
				total += m_ClassCounts[i][k]; //get the number of instances
			result[i] = new double[m_ClassCounts[i].length];
			for (int j = 0; j < result[i].length; j++)
				result[i][j] = m_ClassCounts[i][j]/total;//store the frequencies
		}
		return result;
	}
	
	public double getSVarS() {
		System.out.println("SVARS");
		double result = 0.0;
		double sum = m_SumWeight;
		for (int i = 0; i < m_NbTarget; i++) {
			result += gini(i) * sum;
		}
		return result / m_NbTarget;
	}
	
	public double gini(int attr) {
		if (m_SumWeight == 0) {
			return 0.0;
		} else {
			double sum = 0.0;
			double[] clcts = m_ClassCounts[attr];
			for (int i = 0; i < clcts.length; i++) {
				double prob = clcts[i]/m_SumWeight;
				sum += prob*prob;
			}
			return 1.0 - sum;
		}
	}

}
