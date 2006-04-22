package clus.ext.sspd;

import clus.main.*;
import clus.statistic.*;
import clus.heuristic.*;
import clus.data.rows.*;

// This is used in combination with SSPDStatistic
// Pairwise distances are taken from matrix

public class SSPDHeuristic extends ClusHeuristic {

	protected RowData m_Data;
	
	// FIXME - matrix not used !!!
	protected SSPDMatrix m_Matrix;

	public SSPDHeuristic(SSPDMatrix mtrx) {
		m_Matrix = mtrx;
	}

	public void setData(RowData data) {
		m_Data = data;
	}

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		SSPDStatistic tstat = (SSPDStatistic)c_tstat;
		SSPDStatistic pstat = (SSPDStatistic)c_pstat;
		double n_tot = tstat.m_SumWeight; 
		double n_pos = pstat.m_SumWeight; 
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Calculate value
		double ss_tot = tstat.getSS(m_Data);
		double ss_pos = pstat.getSS(m_Data);
		double ss_neg = tstat.getDiffSS(pstat, m_Data);
		return FTest.calcSSHeuristic(n_tot, ss_tot, ss_pos, ss_neg);		
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic[] c_pstat, int nbsplit) {
		return Double.NEGATIVE_INFINITY;
	}
	
	public String getName() {
		return "Sum of Squared Pairwise Distances (SSPD)";
	}
}
