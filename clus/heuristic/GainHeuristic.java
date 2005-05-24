package clus.heuristic;

import clus.main.*;
import clus.statistic.*;

import jeans.math.*;

public class GainHeuristic extends ClusHeuristic {

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		ClassificationStat tstat = (ClassificationStat)c_tstat;
		ClassificationStat pstat = (ClassificationStat)c_pstat;
		// Equal for all target attributes
		int nb = tstat.m_NbTarget;
		double n_tot = tstat.m_SumWeight;
		double n_pos = pstat.m_SumWeight;
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Initialize entropy's
		double pos_ent = 0.0;
		double neg_ent = 0.0;
		double tot_ent = 0.0;		
		// Entropy?		
		for (int i = 0; i < nb; i++) {
			pos_ent += pstat.entropy(i, n_pos);
			tot_ent += tstat.entropy(i, n_tot);
			neg_ent += tstat.entropyDifference(i, pstat, n_neg);
		}
		// Gain?
		double value = tot_ent - (n_pos*pos_ent + n_neg*neg_ent)/n_tot;
		if (value < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
		if (Settings.GAIN_RATIO) {
			double si = ClassificationStat.computeSplitInfo(n_tot, n_pos, n_neg);
			if (si < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
			return value / si;
		}
		return value;
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic[] c_pstat, int nbsplit) {
		ClassificationStat tstat = (ClassificationStat)c_tstat;
		// Acceptable?
/*		
		for (int i = 0; i < nbsplit; i++)
			if (((ClassificationStat)c_pstat[i]).m_SumWeight < Settings.MINIMAL_WEIGHT)
				return Double.NEGATIVE_INFINITY;
*/
		// Total Entropy
		double value = 0.0;
		int nb = tstat.m_NbTarget;
		double n_tot = tstat.m_SumWeight;
		for (int i = 0; i < nb; i++) 
			value += tstat.entropy(i, n_tot);
		// Subset entropy
		for (int i = 0; i < nbsplit; i++) {
			ClassificationStat s_stat = (ClassificationStat)c_pstat[i];
			double n_set = s_stat.m_SumWeight;			
			// Calculate entropy
			double s_ent = 0.0;					
			for (int j = 0; j < nb; j++)
				s_ent += tstat.entropy(j, n_set);
			// Update gain
			value -= n_set*s_ent/n_tot;				
		}
		if (value < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
		if (Settings.GAIN_RATIO) {
			// Compute split information
			double si = 0;
			for (int i = 0; i < nbsplit; i++) {
				double n_set = ((ClassificationStat)c_pstat[i]).m_SumWeight;
				if (n_set >= MathUtil.C1E_6) {
					double div = n_set/n_tot;
					si -= div*Math.log(div);
				}
			}
			si /= MathUtil.M_LN2;
			// Return calculated gainratio
			if (si < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
			return value / si;
		}
		return value;
	}
	
	public String getName() {
		return Settings.GAIN_RATIO ? "Gainratio" : "Gain";
	}
}

