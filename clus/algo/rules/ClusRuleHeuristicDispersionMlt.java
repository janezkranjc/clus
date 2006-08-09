/*
 * Created on August 4, 2006
 */
package clus.algo.rules;

import clus.main.ClusStatManager;
import clus.main.Settings;
import clus.statistic.*;
import clus.data.attweights.*;

public class ClusRuleHeuristicDispersionMlt extends ClusRuleHeuristicDispersion {
	
	public ClusRuleHeuristicDispersionMlt(ClusAttributeWeights prod) {
	}

	public ClusRuleHeuristicDispersionMlt(ClusStatManager stat_mgr, ClusAttributeWeights prod) {
		m_StatManager = stat_mgr;
	}

	/*
	 *  Larger values are better!
	 */
  // We only need the second parameter for rules!
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		double n_pos = c_pstat.m_SumWeight;
		// Acceptable?
		if (n_pos-Settings.MINIMAL_WEIGHT < 1e-6) {
			return Double.NEGATIVE_INFINITY;
		}
		double pos_comp = ((CombStat)c_pstat).dispersionMltHeur();
		// Rule distance part
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
					if (bit_vect[i] != bit_vect_c[i]) {
						single_dist++;
					}
				}
				single_dist /= m_NbTuples;
				avg_dist += single_dist;
			}
			avg_dist /= nb_rules;
			double dist_par = ((CombStat)c_pstat).getSettings().getCompHeurRuleDistPar();
			double dist_part = avg_dist > 0 ? 1 / avg_dist * dist_par : 100; // 100 ???
	    pos_comp *= 1.0 + dist_part;
		}
		return -pos_comp;
	}

	public String getName() {
		return "Rule Heuristic (Reduced Dispersion, Multiplicative ver.)";
	}

}
