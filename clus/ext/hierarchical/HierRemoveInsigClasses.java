/*
 * Created on May 26, 2005
 */
package clus.ext.hierarchical;

import clus.pruning.*;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.main.*;

public class HierRemoveInsigClasses extends PruneTree {

	PruneTree m_Pruner;
	ClusData m_PruneSet;
	ClassHierarchy m_Hier;
	double m_SigLevel;	
	
	public HierRemoveInsigClasses(double siglevel, ClusData pruneset, PruneTree other, ClassHierarchy hier) {
		m_Pruner = other;
		m_SigLevel = siglevel;
		m_PruneSet = pruneset;
		m_Hier = hier;
	}
	
	public void prune(ClusNode node) {
		m_Pruner.prune(node);
		executeRecursive(node, (WHTDStatistic)node.getTotalStat(), (RowData)m_PruneSet);
	}
	
	public void executeRecursive(ClusNode node, WHTDStatistic global, RowData data) {
		if (node.atBottomLevel()) {
			WHTDStatistic orig = (WHTDStatistic)node.getTotalStat();
			WHTDStatistic valid = (WHTDStatistic)orig.cloneStat();			
			for (int i = 0; i < data.getNbRows(); i++) {
				DataTuple tuple = data.getTuple(i);
				valid.updateWeighted(tuple, i);
			}
			valid.calcMean();
			WHTDStatistic pred = (WHTDStatistic)orig.cloneStat();
			pred.copy(orig);
			pred.setValidationStat(valid);
			pred.setGlobalStat(global);
			pred.setSigLevel(m_SigLevel);
			pred.calcMean();
			node.setTotalStat(pred);
		}
		int arity = node.getNbChildren();
		for (int i = 0; i < arity; i++) {
			RowData subset = data.applyWeighted(node.getTest(), i);
			executeRecursive((ClusNode)node.getChild(i), global, subset);
		}
	}	
}



