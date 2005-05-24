package clus.heuristic;

import clus.main.*;
import clus.statistic.*;
import clus.data.rows.*;

public class SSDHeuristic extends ClusHeuristic {

	protected RowData m_Data;
	protected String m_BasicDist;
	protected ClusStatistic m_NegStat;
	protected TargetWeightProducer m_TargetWeights;

	public SSDHeuristic(String basicdist, ClusStatistic negstat, TargetWeightProducer targetweights) {
		m_BasicDist = basicdist;
		m_NegStat = negstat;
		m_TargetWeights = targetweights;			
	}
	
	public void setData(RowData data) {
		m_Data = data;
	}	
		
	public double calcHeuristic(ClusStatistic tstat, ClusStatistic pstat, ClusStatistic missing) {
		double n_tot = tstat.m_SumWeight; 
		double n_pos = pstat.m_SumWeight;
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Calculate value						
		double ss_tot = tstat.getSS(m_TargetWeights, m_Data);
		double ss_pos = pstat.getSS(m_TargetWeights, m_Data);		
		m_NegStat.copy(tstat);
		m_NegStat.subtractFromThis(pstat);
		double ss_neg = m_NegStat.getSS(m_TargetWeights, m_Data);
		double value = FTest.calcSSHeuristic(n_tot, ss_tot, ss_pos, ss_neg);
		if (Settings.VERBOSE >= 10) {
			System.out.println("TOT: "+tstat.getDebugString());
			System.out.println("POS: "+pstat.getDebugString());
			System.out.println("NEG: "+m_NegStat.getDebugString());
			System.out.println("-> ("+ss_tot+", "+ss_pos+", "+ss_neg+") "+value);
		}
		if (value < 1e-6) return Double.NEGATIVE_INFINITY;
		return value;
	}
	
	public void setRootStatistic(ClusStatistic stat) {
		m_TargetWeights.setTotalStat(stat);
	}
	
	public String getName() {
		return "SS Reduction ("+m_BasicDist+", "+m_TargetWeights.getName()+") (FTest = "+FTest.getSettingSig()+")";
	}
}
