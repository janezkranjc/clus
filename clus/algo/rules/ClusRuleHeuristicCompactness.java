/*
 * Created on June 23, 2005
 */
package clus.algo.rules;

import clus.heuristic.*;
import clus.main.Settings;
import clus.statistic.*;
import clus.data.attweights.*;

public class ClusRuleHeuristicCompactness extends ClusHeuristic {

	// private ClusAttributeWeights m_ClusteringWeights;
	
	public ClusRuleHeuristicCompactness(ClusAttributeWeights prod) {
		// m_ClusteringWeights = prod;
	}

  // We only need the second parameter for rules!
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		double n_pos = c_pstat.m_SumWeight;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		double pos_comp = ((CombStat)c_pstat).compactnessHeur(); //  getError(m_ClusteringWeights);
    // System.out.println("  |> Compactness value: " + pos_comp);
		return -pos_comp;
	}
		
	public String getName() {
		return "Rule Heuristic (Increased Compactness)";
	}
}
