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

package clus.algo.split;

import clus.main.*;
import clus.algo.rules.ClusRuleHeuristicDispersion;
import clus.data.type.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.heuristic.*;

import java.util.*;

public class SubsetSplit extends NominalSplit {

	ClusStatistic m_PStat, m_CStat, m_MStat;
	ClusStatManager m_StatManager;

	public void initialize(ClusStatManager manager) {
		m_PStat = manager.createClusteringStat();
		m_CStat = m_PStat.cloneStat();
		m_MStat = m_PStat.cloneStat();
		m_StatManager = manager;
	}
	
	public void setSDataSize(int size) {
		m_PStat.setSDataSize(size);
		m_CStat.setSDataSize(size);
		m_MStat.setSDataSize(size);		
	}
	
	public ClusStatManager getStatManager() {
		return m_StatManager;
	}
	
	public void showTest(NominalAttrType type, boolean[] isin, int add, double mheur, ClusStatistic tot, ClusStatistic pos) {
		int count = 0;
		System.out.print(type.getName()+ " in {");
		for (int i = 0; i < type.getNbValues(); i++) {
			if (isin[i] || i == add) {
				if (count != 0) System.out.print(",");
				System.out.print(type.getValue(i));
				count++;
			}
		}
		tot.calcMean(); pos.calcMean();
		// System.out.println("}: "+mheur+" "+tot+" "+pos);
		System.out.println("}: "+mheur);
	}

	public void findSplit(CurrentBestTestAndHeuristic node, NominalAttrType type) {
		double unk_freq = 0.0;		
		int nbvalues = type.getNbValues();
		boolean isin[] = new boolean[nbvalues];
		// If has missing values?
		if (type.hasMissing()) {
			ClusStatistic unknown = node.m_TestStat[nbvalues];
			m_MStat.copy(node.m_TotStat);
			m_MStat.subtractFromThis(unknown);
			unk_freq = unknown.m_SumWeight / node.m_TotStat.m_SumWeight;
		} else {
			m_MStat.copy(node.m_TotStat);
		}
		int card = 0;
		double pos_freq = 0.0;
		double bheur = Double.NEGATIVE_INFINITY;
		/* Not working for rules! */
		if (nbvalues == 2 && !getStatManager().isRuleInduce()) {
			// Handle binary splits efficiently
			card = 1;
			isin[0] = true;			
			ClusStatistic CStat = node.m_TestStat[0];
			bheur = node.calcHeuristic(m_MStat, CStat);
			// showTest(type, isin, -1, bheur, m_MStat, m_CStat);			
			pos_freq = CStat.m_SumWeight / m_MStat.m_SumWeight;
		} else {
			// Try to add values to subsets
			// Each iteration the cardinality increases by at most one
			m_PStat.reset();			
			int bvalue = 0;	
			if ((m_PStat instanceof CombStat) &&
					((CombStat)m_PStat).getSettings().isCompHeurRuleDist()) {
				((ClusRuleHeuristicDispersion)node.m_Heuristic).setDataIndexes(new int[0]);
			}
			while ((bvalue != -1) && ((card+1) < nbvalues)) {
				bvalue = -1;
				for (int j = 0; j < nbvalues; j++) {
					if (!isin[j]) {
						// Try to add this one to the positive stat
						m_CStat.copy(m_PStat);
						m_CStat.add(node.m_TestStat[j]);
						if ((m_PStat instanceof CombStat) &&
								((CombStat)m_PStat).getSettings().isCompHeurRuleDist()) {
							boolean isin_current[] = new boolean[nbvalues];
							for (int k = 0; k < nbvalues; k++) {
								isin_current[k] = isin[k];
							}
							isin_current[j] = true;
							((ClusRuleHeuristicDispersion)node.m_Heuristic).setDataIndexes(isin_current);
						}
						// Calc heuristic
						double mheur = node.calcHeuristic(m_MStat, m_CStat);
						// showTest(type, isin, j, mheur, m_MStat, m_CStat);
						if (mheur > bheur) {
							bheur = mheur;
							bvalue = j;
							// Calculate pos freq (of current best one)
							pos_freq = m_CStat.m_SumWeight / m_MStat.m_SumWeight;
						}
					}
				}
				if (bvalue != -1) {
					card++;
					isin[bvalue] = true;
					m_PStat.add(node.m_TestStat[bvalue]);
				}
			}
		}
		// Found better test :-)
			//System.out.print("In SubsetSlip, new test is "+type.getName());
			//System.out.println(" with heurisitc :"+bheur);
		if (bheur > node.m_BestHeur + ClusHeuristic.DELTA) {
			node.m_UnknownFreq = unk_freq;
			node.m_BestHeur = bheur;
			node.m_TestType = CurrentBestTestAndHeuristic.TYPE_TEST;
			node.m_BestTest = new SubsetTest(type, card, isin, pos_freq);
			node.resetAlternativeBest();
//			System.out.println("attr: " + type + "  best test: " + node.m_BestTest.getString());
		}
		else if (getStatManager().getSettings().showAlternativeSplits() && (bheur > node.m_BestHeur - ClusHeuristic.DELTA) && (bheur < node.m_BestHeur + ClusHeuristic.DELTA)) {
			// if same heuristic: add to altnernatives (list will later be pruned to remove those tests that do
			// not give rise to exactly the same subsets)
			node.addAlternativeBest(new SubsetTest(type, card, isin, pos_freq));		
		}
	}

  public void findRandomSplit(CurrentBestTestAndHeuristic node, NominalAttrType type, Random rn) {
    double unk_freq = 0.0;    
    int nbvalues = type.getNbValues();
    boolean isin[] = new boolean[nbvalues];
    // If has missing values?
    if (type.hasMissing()) {
      ClusStatistic unknown = node.m_TestStat[nbvalues];
      m_MStat.copy(node.m_TotStat);
      m_MStat.subtractFromThis(unknown);
      unk_freq = unknown.m_SumWeight / node.m_TotStat.m_SumWeight;
    } else {
      m_MStat.copy(node.m_TotStat);
    }
    int card = 0;
    double pos_freq = 0.0;
    // Generate non-empty and non-full subset
    while (true) {
      for (int i = 0; i < isin.length; i++) {
        isin[i] = rn.nextBoolean();
      }
      int sum = 0;
      for (int i = 0; i < isin.length; i++) {
        if (isin[i]) {
          sum++;
        }
      }
      if (!((sum == 0) || (sum == nbvalues))) {
        card = sum;
        break;
      }
    }
    // Calculate statistics ...
    m_PStat.reset();
    for (int j = 0; j < nbvalues; j++) {
      if (isin[j]) {
         	m_PStat.add(node.m_TestStat[j]);
      }
    }
    pos_freq = m_PStat.m_SumWeight / m_MStat.m_SumWeight;
    node.m_UnknownFreq = unk_freq;
    node.m_BestHeur = node.calcHeuristic(m_MStat, m_PStat);
    node.m_TestType = CurrentBestTestAndHeuristic.TYPE_TEST;
    node.m_BestTest = new SubsetTest(type, card, isin, pos_freq);
  }
}
