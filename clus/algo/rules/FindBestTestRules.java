/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.algo.rules;

import clus.algo.split.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.main.*;

public class FindBestTestRules extends FindBestTest {
	
	public FindBestTestRules(ClusStatManager mgr) {
		super(mgr);
	}	
		
	public FindBestTestRules(ClusStatManager mgr, NominalSplit split) {
		super(mgr, split);
	}
	
	public void findNominal(NominalAttrType at, RowData data) {
		// Reset positive statistic
		int nbvalues = at.getNbValues();
		m_BestTest.reset(nbvalues + 1);
		int nb_rows = data.getNbRows();
		if (!getSettings().isCompHeurRuleDist()) {
			// For each attribute value   
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				int value = at.getNominal(tuple);     
				m_BestTest.m_TestStat[value].updateWeighted(tuple, i);      
			}
		} else {
			// TODO: Perhaps ListArray[nbvalues] instead of int[nbvalues][nb_rows] would be better? 
			int[][] data_idx_per_val = new int[nbvalues][nb_rows];
			for (int j = 0; j < nbvalues; j++) {
				for (int i = 0; i < nb_rows; i++) {
					data_idx_per_val[j][i] = -1;
				}
			}
			// For each attribute value
			int[] counts = new int[nbvalues];
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				int value = at.getNominal(tuple);     
				m_BestTest.m_TestStat[value].updateWeighted(tuple, i);
				if (value < nbvalues) {  // Skip missing values, will this be a problem somewhere?
					data_idx_per_val[value][i] = tuple.getIndex();
					counts[value]++;
				}
			}
			// Skip -1s
			int[][] data_ipv = new int[nbvalues][];
			for (int j = 0; j < nbvalues; j++) {
				data_ipv[j] = new int[counts[j]];
				int k = 0;
				for (int i = 0; i < nb_rows; i++) {
					if (data_idx_per_val[j][i] != -1) {
						data_ipv[j][k] = data_idx_per_val[j][i];
						k++;
					}
				}
			}
			((ClusRuleHeuristicDispersion)m_BestTest.m_Heuristic).setDataIndexesPerVal(data_ipv);
		}
		// Find best split
		m_Split.findSplit(m_BestTest, at);
	}
  
	public void findNumeric(NumericAttrType at, RowData data) { 
		DataTuple tuple;
		int idx = at.getArrayIndex();
		if (at.isSparse()) {
			data.sortSparse(at);
		} else {
			data.sort(at);
		}
		m_BestTest.reset(2);    
		// Missing values
		int first = 0;        
		int nb_rows = data.getNbRows();
		// Copy total statistic into corrected total
		m_BestTest.copyTotal();
		if (at.hasMissing()) {
			// Because of sorting, all missing values are in the front :-)
			while (first < nb_rows && (tuple = data.getTuple(first)).hasNumMissing(idx)) {
				m_BestTest.m_MissingStat.updateWeighted(tuple, first);
				first++;
			}
			m_BestTest.subtractMissing();
		}   
		double prev = Double.NaN;
		int[] data_idx = new int[nb_rows]; // TODO: Skip missing ones?!
		if (getSettings().isCompHeurRuleDist()) {
			for (int i = first; i < nb_rows; i++) {
				data_idx[i] = data.getTuple(i).getIndex();
			}
		}
		for (int i = first; i < nb_rows; i++) {
			tuple = data.getTuple(i);
			double value = tuple.getDoubleVal(idx);
			if (value != prev) {
				if (value != Double.NaN) {
					if (getSettings().isCompHeurRuleDist()) {
						int[] subset_idx = new int[i-first];
						System.arraycopy(data_idx, first, subset_idx, 0, i-first);
						((ClusRuleHeuristicDispersion)m_BestTest.m_Heuristic).setDataIndexes(subset_idx);
					}
					// System.err.println("Value (>): " + value);
					m_BestTest.updateNumeric(value, at);
				}
				prev = value;
			}       
			m_BestTest.m_PosStat.updateWeighted(tuple, i);
		}
		// For rules check inverse splits also
		if (m_StatManager.isRuleInduce()) {
			m_BestTest.reset();
			DataTuple next_tuple = data.getTuple(nb_rows-1);
			double next = next_tuple.getDoubleVal(idx);
			for (int i = nb_rows-1; i > first; i--) {
				tuple = next_tuple;
				next_tuple = data.getTuple(i-1);
				double value = next;
				next = next_tuple.getDoubleVal(idx);
				m_BestTest.m_PosStat.updateWeighted(tuple, i);
				if ((value != next) && (value != Double.NaN)) {
					if (getSettings().isCompHeurRuleDist()) {
						int[] subset_idx = new int[nb_rows-i];
						System.arraycopy(data_idx, i, subset_idx, 0, nb_rows-i);
						((ClusRuleHeuristicDispersion)m_BestTest.m_Heuristic).setDataIndexes(subset_idx);
					}
					// System.err.println("Value (<=): " + value);
					m_BestTest.updateInverseNumeric(value, at);
				}
			}
		}
	}
}
