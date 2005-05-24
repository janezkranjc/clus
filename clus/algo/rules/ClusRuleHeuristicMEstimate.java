/*
 * Created on May 3, 2005
 */
package clus.algo.rules;

import clus.heuristic.*;
import clus.main.*;
import clus.statistic.*;

public class ClusRuleHeuristicMEstimate extends ClusHeuristic {
	
	double m_MValue;
	double m_Prior;
	
	public ClusRuleHeuristicMEstimate(double m_value) {
		m_MValue = m_value;
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		double n_pos = c_pstat.m_SumWeight;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		double correct = n_pos - c_pstat.getError();
		double m_estimate = (correct + m_MValue*m_Prior) / (n_pos + m_MValue);		
		return m_estimate;
	}
	
	public void setRootStatistic(ClusStatistic stat) {		
		m_Prior = (stat.getTotalWeight()-stat.getError()) / stat.getTotalWeight();
		System.out.println("Setting prior: "+m_Prior);
	}
	
	public String getName() {
		return "Rule Heuristic (M-Estimate, M = " + m_MValue + ")";
	}
}
