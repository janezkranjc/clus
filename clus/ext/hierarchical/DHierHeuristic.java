package clus.ext.hierarchical;

import clus.main.*;
import clus.statistic.*;
import clus.heuristic.*;
import clus.data.rows.*;

// No idea what this is?

public class DHierHeuristic extends ClusHeuristic {

	protected ClassHierarchy m_Hier;
	protected RowData m_Data;

	public DHierHeuristic(ClassHierarchy hier) {
		m_Hier = hier;
	}
	
	public void setData(RowData data) {
		m_Data = data;
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		DHierStatistic tstat = (DHierStatistic)c_tstat;
		DHierStatistic pstat = (DHierStatistic)c_pstat;
		double n_tot = tstat.m_SumWeight; 
		double n_pos = pstat.m_SumWeight; 
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Calculate value
//		System.out.println("Total:");
		double ss_tot = tstat.getSS(m_Data);
//		System.out.println("Pos:");				
		double ss_pos = pstat.getSS(m_Data);		
		double ss_neg = tstat.getDiffSS(pstat, m_Data);
		double ss_sum = ss_pos + ss_neg;
//		System.out.println("SS: "+ss_tot+" "+ss_pos+" "+ss_neg);
		return FTest.calcSSHeuristicRatio(ss_tot, ss_sum, n_tot, n_pos, n_neg);
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic[] c_pstat, int nbsplit) {
		return Double.NEGATIVE_INFINITY;
	}
	
	public String getName() {
		return "Discrete Squared Shortest Path Distance";
	}
}
