/*
 * Created on August 4, 2006
 * ClusRuleHeuristicCompactnes replaced by this abstract class and its
 * sub-classes: *DispersionAdt, *DispersionMlt, *WRDispersionAdt, *WRDispersionMlt
 * Original ClusRuleHeuristicCompactnes created on June 23, 2005
 */
package clus.algo.rules;

import java.util.*;

import clus.heuristic.*;
import clus.main.ClusStatManager;
import clus.main.Settings;

public abstract class ClusRuleHeuristicDispersion extends ClusHeuristic {
	
	public ClusStatManager m_StatManager = null;
	public int[] m_DataIndexes;
	public int[][] m_DataIndexesPerVal;
	public ArrayList m_CoveredBitVectArray;
	public int m_NbTuples;

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
	
	public void setCoveredBitVectArray(ArrayList bit_vect_array) {
		m_CoveredBitVectArray = bit_vect_array;
	}
	
  public Settings getSettings() {
    return m_StatManager.getSettings();
  }

}
