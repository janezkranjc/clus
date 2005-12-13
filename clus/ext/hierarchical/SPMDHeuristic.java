package clus.ext.hierarchical;

import jeans.math.*;

import clus.main.*;
import clus.statistic.*;
import clus.heuristic.*;
import clus.data.rows.*;

public class SPMDHeuristic extends ClusHeuristic {

	protected ClassHierarchy m_Hier;
	protected RowData m_Data;

	public SPMDHeuristic(ClassHierarchy hier) {
		m_Hier = hier;
	}
	
	public void setData(RowData data) {
		m_Data = data;
	}

/*	
	private double calcSPMD_sq_dist(SPMDStatistic mean, ClassesTuple tuple) {
		double[] vec = tuple.getVector(m_Hier);
		MDoubleArray.add(vec, mean.getCounts(), -1.0/mean.m_SumWeight);
		MSymMatrix KM = m_Hier.getKMatrix();
		double dist = KM.xtAx(vec);
		System.out.println("Tuple: "+tuple+" "+dist);		
		return dist;
	}		
*/
	
	public static double transformHeur(double value, double n_tot, double n_pos, double n_neg) {
		if (value < 1e-6) return Double.NEGATIVE_INFINITY;
		if (Settings.GAIN_RATIO) {
			double si = ClassificationStat.computeSplitInfo(n_tot, n_pos, n_neg);
			if (si < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
			return value / si;
		} else {
			return value;
		}
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		SPMDStatistic tstat = (SPMDStatistic)c_tstat;
		SPMDStatistic pstat = (SPMDStatistic)c_pstat;
		double n_tot = tstat.m_SumWeight; 
		double n_pos = pstat.m_SumWeight; 
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Calculate value..
		double ss_tot = tstat.getSS();
		double ss_pos = pstat.getSS();
		double ss_neg = tstat.getDiffSS(pstat);
		double val = ss_tot - ss_pos - ss_neg;		
		// Calculate FTest
		int n_2 = (int)Math.floor(n_tot - 2.0 + 0.5);
		if (n_2 <= 0) {
			if (Settings.FTEST_LEVEL == 0) return transformHeur(val, n_tot, n_pos, n_neg);
			else return Double.NEGATIVE_INFINITY;
		} else {
			double sum_ss = ss_pos + ss_neg;
			if (FTest.ftest(Settings.FTEST_LEVEL, ss_tot, sum_ss, n_2)) {
				return transformHeur(val, n_tot, n_pos, n_neg);
			} else {
				return Double.NEGATIVE_INFINITY;
			}
		}
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic[] c_pstat, int nbsplit) {
		return Double.NEGATIVE_INFINITY;
	}
	
	public String getName() {
		return "Shortest Path Matching Distance";
	}

}
