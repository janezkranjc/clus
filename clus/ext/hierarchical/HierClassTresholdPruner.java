/*
 * Created on Sep 22, 2005
 *
 */
package clus.ext.hierarchical;

import clus.main.ClusNode;
import clus.pruning.PruneTree;
import clus.util.ClusException;

public class HierClassTresholdPruner extends PruneTree {

	protected double[] m_Tresholds;
	
	public HierClassTresholdPruner(double[] tresholds) {
		m_Tresholds = tresholds;
	}
	
	public void prune(ClusNode node) throws ClusException {
		prune(0, node);
	}
	
	public int getNbResults() {
		return m_Tresholds.length;
	}
	
	public double getThreshold(int i) {
		return m_Tresholds[i];
	}
	
	public void prune(int result, ClusNode node) throws ClusException {
		pruneRecursive(node, m_Tresholds[result]);
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
