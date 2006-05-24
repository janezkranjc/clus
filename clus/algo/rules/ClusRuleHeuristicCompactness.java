/*
 * Created on June 23, 2005
 */
package clus.algo.rules;

import java.util.*;

import clus.heuristic.*;
import clus.main.Settings;
import clus.statistic.*;
import clus.data.attweights.*;

public class ClusRuleHeuristicCompactness extends ClusHeuristic {
	
	// protected RowData m_Data;
	private int[] m_DataIndexes;
	protected int[][] m_DataIndexesPerVal;
	protected ArrayList m_CoveredBitVectArray;
	private int m_NbTuples;

	// private ClusAttributeWeights m_ClusteringWeights;
	
	public ClusRuleHeuristicCompactness(ClusAttributeWeights prod) {
		// m_ClusteringWeights = prod;
	}

  // We only need the second parameter for rules!
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		double n_pos = c_pstat.m_SumWeight;
		// Acceptable?
		if (n_pos-Settings.MINIMAL_WEIGHT < 1e-6) {
			return Double.NEGATIVE_INFINITY;
		}
		double pos_comp = ((CombStat)c_pstat).compactnessHeur() + 1.0; // See below, why +1.0
		// System.out.println("Before: " + pos_comp);
		if (((CombStat)c_pstat).getSettings().isCompHeurRuleDist() &&
				(m_CoveredBitVectArray.size() > 0)) {
			double avg_dist = 0.0;
			int nb_rules = m_CoveredBitVectArray.size();
			boolean[] bit_vect = new boolean[m_NbTuples];
			for (int i = 0; i < m_DataIndexes.length; i++) {
				bit_vect[m_DataIndexes[i]] = true;
			}
			boolean[] bit_vect_c = new boolean[m_NbTuples];
			for (int j = 0; j < nb_rules; j++) {
				bit_vect_c = ((boolean[])(m_CoveredBitVectArray.get(j)));
				double single_dist = 0;
				for (int i = 0; i < m_NbTuples; i++) {
					// if (bit_vect[i] != ((boolean[])(m_CoveredBitVectArray.get(j)))[i]) {
					if (bit_vect[i] != bit_vect_c[i]) {
						single_dist++;
					}
				}
				single_dist /= m_NbTuples;
				avg_dist += single_dist;
			}
			avg_dist /= nb_rules;
			// System.out.print("\nAvg_dist: " + avg_dist + " Norm part: " + pos_comp + " ");
			// h = -(compactness + (1 - average_distance_to_other_rules)
			pos_comp -= ((CombStat)c_pstat).getSettings().getCompHeurRuleDistPar() * avg_dist;
			// System.out.print("Total: " + pos_comp + "\n\n");
		}
		return -pos_comp;
	}

	public void setDataIndexes(int[] indexes) {
		m_DataIndexes = indexes;
	}
	
	public void setDataIndexes(boolean[] isin) {
		if ((m_DataIndexesPerVal != null) && (isin.length == m_DataIndexesPerVal.length)) {
			int size = 0;
			for (int i = 0; i < isin.length; i++) {
				if (isin[i]) {
					size += m_DataIndexesPerVal[i].length;
				}
			}
			int[] new_data_idx = new int[size];
			int pt = 0;
			for (int i = 0; i < m_DataIndexesPerVal.length; i++) {
				if (isin[i]) {
					System.arraycopy(m_DataIndexesPerVal[i],0,new_data_idx,pt,m_DataIndexesPerVal[i].length);
					pt += m_DataIndexesPerVal[i].length;
				}
			}
			setDataIndexes(new_data_idx);
		} else {
			System.err.println("ClusRuleHeuristicCompactness: setDataIndexes(boolean[])");
			System.exit(1); // Exception???
		}
	}
	
	public void setDataIndexesPerVal(int[][] indexes) {
		m_DataIndexesPerVal = indexes;
	}

	public int[][] getDataIndexesPerVal() {
		return m_DataIndexesPerVal;
	}

	public void initCoveredBitVectArray(int size) {
		m_CoveredBitVectArray = new ArrayList();
		m_NbTuples = size;
	}
	
	/*
	public int getCoveredBitVectSize() {
		if (m_CoveredBitVect != null) {
			return m_CoveredBitVect.length;
		} else {
			return -1;
		}
	}*/

	public void setCoveredBitVectArray(ArrayList bit_vect_array) {
		m_CoveredBitVectArray = bit_vect_array;
	}
	
	public String getName() {
		return "Rule Heuristic (Increased Compactness)";
	}

}
