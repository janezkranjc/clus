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
	
	public void prune(int result, ClusNode node) throws ClusException {
		pruneRecursive(node, m_Tresholds[result]);
	}
	
	public void pruneRecursive(ClusNode node, double treshold) throws ClusException {
		if (node.atBottomLevel()) {
			WHTDStatistic stat = (WHTDStatistic)node.getTargetStat();
			WHTDStatistic new_stat = (WHTDStatistic)stat.cloneStat();
			new_stat.copyAll(stat);
			new_stat.calcMean(treshold);
			node.setTargetStat(new_stat);			
		} else {
			for (int i = 0; i < node.getNbChildren(); i++) {
				pruneRecursive((ClusNode)node.getChild(i), treshold);
			}
		}
	}	
}
