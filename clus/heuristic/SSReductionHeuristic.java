package clus.heuristic;

import clus.main.*;
import clus.statistic.*;
import clus.data.attweights.*;
import clus.data.type.*;

public class SSReductionHeuristic extends ClusHeuristic {
	
	private ClusAttributeWeights m_TargetWeights;
	private NumericAttrType[] m_Attrs;
	
	public SSReductionHeuristic(ClusAttributeWeights prod, NumericAttrType[] attrs) {
		m_TargetWeights = prod;
		m_Attrs = attrs;
	}

// handling missing values is done as follows:
// * mean = sum of known values / number of known values 
// * SS = (N-1) / (K-1) * SS_known
//      with K = # known values, N = # vectors in set
//      (use N-1/K-1 instead of N/K to remove bias towards smaller sets)
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		RegressionStat tstat = (RegressionStat)c_tstat;	
		RegressionStat pstat = (RegressionStat)c_pstat;
		// Initialize sum of ss's
		double s_ss_tot = 0.0;
		double s_ss_pos = 0.0;
		double s_ss_neg = 0.0;
		// Equal for all target attributes
		int nb = tstat.m_NbAttrs;
		double n_tot = tstat.m_SumWeight; 
		double n_pos = pstat.m_SumWeight; 
		double n_neg = n_tot - n_pos;
		
//		System.out.println("SS Reduction tot: "+n_tot+" pos: "+n_pos+" neg: "+n_neg);
		
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		double[] fac = m_TargetWeights.getWeights();
		// Sum for each numeric target attribute
		for (int i = 0; i < nb; i++) {
			// Total values
			double k_tot = tstat.m_SumWeights[i];
			double sv_tot = tstat.m_SumValues[i];
			double ss_tot = tstat.m_SumSqValues[i];
			// Positive values
			double k_pos = pstat.m_SumWeights[i];
			double sv_pos = pstat.m_SumValues[i];
			double ss_pos = pstat.m_SumSqValues[i];
			// Negative values
			double k_neg = k_tot - k_pos;
			double sv_neg = sv_tot - sv_pos;			
			double ss_neg = ss_tot - ss_pos;
			// Add to sums
			int aidx = tstat.getAttribute(i).getIndex();
			s_ss_pos += ((k_pos > 1.0) ? ss_pos * (n_pos - 1) / (k_pos - 1) - n_pos * sv_pos/k_pos*sv_pos/k_pos : 0.0) * fac[aidx];
			s_ss_neg += ((k_neg > 1.0) ? ss_neg * (n_neg - 1) / (k_neg - 1) - n_neg * sv_neg/k_neg*sv_neg/k_neg : 0.0) * fac[aidx];
			s_ss_tot += ((k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0) * fac[aidx];
		}		
		return FTest.calcSSHeuristic(n_tot, s_ss_tot, s_ss_pos, s_ss_neg);
	}
	
	public String getName() {
		return "SS-Reduction (ftest: "+Settings.FTEST_VALUE+", "+m_TargetWeights.getName(m_Attrs)+")";
	}
}
