/*
 * Created on Sep 22, 2005
 *
 */
package clus.ext.hierarchical;

import clus.main.ClusNode;
import clus.pruning.PruneTree;
import clus.util.ClusException;

public class HierClassTresholdPruner extends PruneTree {

	protected double[] m_Thresholds;
	
	public HierClassTresholdPruner(double[] tresholds) {
		m_Thresholds = tresholds;
	}
	
	public void prune(ClusNode node) throws ClusException {
		prune(0, node);
	}
	
	public int getNbResults() {
		return m_Thresholds.length;
	}
	
	public String getPrunedName(int i) {
		return "T("+m_Thresholds[i]+")";
	}
	
	public double getThreshold(int i) {
		return m_Thresholds[i];
	}
	
	public void prune(int result, ClusNode node) throws ClusException {
		pruneRecursive(node, m_Thresholds[result]);
	}
	
	public void pruneRecursive(ClusNode node, double threshold) throws ClusException {
		WHTDStatistic stat = (WHTDStatistic)node.getTargetStat();
		WHTDStatistic new_stat = (WHTDStatistic)stat.cloneStat();
		new_stat.copyAll(stat);
		new_stat.setThreshold(threshold);
		new_stat.calcMean();
		node.setTargetStat(new_stat);
		for (int i = 0; i < node.getNbChildren(); i++) {
				pruneRecursive((ClusNode)node.getChild(i), threshold);
		}
	}	
}
