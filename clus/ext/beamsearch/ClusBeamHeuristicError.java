/*
 * Created on Apr 6, 2005
 */
package clus.ext.beamsearch;

import jeans.math.MathUtil;
import clus.main.ClusNode;
import clus.main.Settings;
import clus.statistic.ClusStatistic;

public class ClusBeamHeuristicError extends ClusBeamHeuristic {
	
	public ClusBeamHeuristicError(ClusStatistic stat) {
		super(stat);
	}

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		double n_tot = c_tstat.m_SumWeight;
		double n_pos = c_pstat.m_SumWeight;
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		if (missing.m_SumWeight <= MathUtil.C1E_9) {
			double pos_error = c_pstat.getError();
			double neg_error = c_tstat.getErrorDiff(c_pstat);
			return m_TreeOffset - (pos_error + neg_error)/m_NbTrain - 2*Settings.SIZE_PENALTY;
		} else {
			double pos_freq = n_pos / n_tot;
			m_Pos.copy(c_pstat);
			m_Neg.copy(c_tstat);
			m_Neg.subtractFromThis(c_pstat);
			m_Pos.addScaled(pos_freq, missing);			
			m_Neg.addScaled(1.0-pos_freq, missing);
			double pos_error = m_Pos.getError();
			double neg_error = m_Neg.getError();
			return m_TreeOffset - (pos_error + neg_error)/m_NbTrain - 2*Settings.SIZE_PENALTY;			
		}		
	}
	
	public double estimateBeamMeasure(ClusNode tree) {
		if (tree.atBottomLevel()) {
			ClusStatistic total = tree.getClusteringStat();
			return -total.getError()/m_NbTrain - Settings.SIZE_PENALTY;
		} else {
			double result = 0.0;
			for (int i = 0; i < tree.getNbChildren(); i++) {
				ClusNode child = (ClusNode)tree.getChild(i);
				result += estimateBeamMeasure(child);
			}
			return result - Settings.SIZE_PENALTY;
		}
	}	
	
	public double computeLeafAdd(ClusNode leaf) {
		return -leaf.getClusteringStat().getError()/m_NbTrain;		
	}	
	
	public String getName() {
		return "Beam Heuristic (Reduced Error)"+getAttrHeuristicString();
	}
}
