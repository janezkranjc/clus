/*
 * Created on May 2, 2005
 */
package clus.algo.rules;

import clus.heuristic.*;
import clus.main.Settings;
import clus.statistic.ClusStatistic;

public class ClusRuleHeuristicError extends ClusHeuristic {

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		double n_pos = c_pstat.m_SumWeight;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		double pos_error = c_pstat.getError(null);
		return -pos_error;
	}
		
	public String getName() {
		return "Rule Heuristic (Reduced Error)";
	}
}
