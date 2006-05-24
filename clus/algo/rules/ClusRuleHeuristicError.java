/*
 * Created on May 2, 2005
 */
package clus.algo.rules;

import clus.heuristic.*;
import clus.main.Settings;
import clus.statistic.ClusStatistic;
import clus.main.ClusStatManager;
import clus.data.attweights.*;

public class ClusRuleHeuristicError extends ClusHeuristic {

	private ClusAttributeWeights m_TargetWeights;
	private ClusStatManager m_StatManager = null;
	
	public ClusRuleHeuristicError(ClusAttributeWeights prod) {
		m_TargetWeights = prod;
	}
	
	public ClusRuleHeuristicError(ClusStatManager stat_mgr, ClusAttributeWeights prod) {
		m_StatManager = stat_mgr;
		m_TargetWeights = prod;
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		double n_pos = c_pstat.m_SumWeight;
		// Acceptable?
		// if (n_pos < Settings.MINIMAL_WEIGHT) {
		if (n_pos-Settings.MINIMAL_WEIGHT < 1e-6) {
			return Double.NEGATIVE_INFINITY;
		}
		double pos_error = c_pstat.getError(m_TargetWeights);
		// Prefer rules that cover more examples
		double global_sum_w = m_StatManager.getGlobalStat().getTotalWeight();
		double heur_par = getSettings().getHeurCoveragePar();
		pos_error *= (1 + heur_par*global_sum_w/c_pstat.m_SumWeight);

		return -pos_error;
	}
		
	public String getName() {
		return "Rule Heuristic (Reduced Error)";
	}
	
  public Settings getSettings() {
    return m_StatManager.getSettings();
  }

}
/*
double global_sum_w = m_StatManager.getGlobalStat().getTotalWeight();
double heur_par = getSettings().getCompHeurCoveragePar();
comp = comp * (1 + heur_par*global_sum_w/m_SumWeight);
*/