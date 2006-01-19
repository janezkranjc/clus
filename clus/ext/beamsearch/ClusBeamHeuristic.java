/*
 * Created on Apr 22, 2005
 */
package clus.ext.beamsearch;

import clus.heuristic.ClusHeuristic;
import clus.main.ClusNode;
import clus.statistic.ClusStatistic;

public abstract class ClusBeamHeuristic extends ClusHeuristic {

	protected double m_NbTrain;
	protected double m_TreeOffset;
	protected ClusStatistic m_Pos, m_Neg;
	protected ClusHeuristic m_AttrHeuristic;
	
	public ClusBeamHeuristic(ClusStatistic stat) {
		m_Pos = stat;
		m_Neg = stat.cloneStat();
	}

	public abstract double estimateBeamMeasure(ClusNode tree);
	
	public abstract double computeLeafAdd(ClusNode leaf);	
	
	public void setTreeOffset(double value) {
		m_TreeOffset = value;
	}
	
	public void setRootStatistic(ClusStatistic stat) {
		m_NbTrain = stat.m_SumWeight;
	}
	
	public void setAttrHeuristic(ClusHeuristic heur) {
		m_AttrHeuristic = heur;
	}
	
	public String getAttrHeuristicString() {
		if (m_AttrHeuristic == null) return "";
		else return ", attribute heuristic = "+m_AttrHeuristic.getName();
	}
}
