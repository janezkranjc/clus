package clus.heuristic;

import clus.statistic.*;
import clus.data.rows.*;

public abstract class ClusHeuristic {

	public final static double DELTA = 1e-6;

	public void setData(RowData data) {
	}
	
	public void setRootStatistic(ClusStatistic stat) {
	}

	public abstract double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing);
	
	public abstract String getName();
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic[] c_pstat, int nbsplit) {
		return Double.NEGATIVE_INFINITY;		
	}

	public static double nonZero(double val) {
		if (val < 1e-6) return Double.NEGATIVE_INFINITY;
		return val;
	}
}
