/*
 * Created on May 26, 2005
 */
package clus.ext.hierarchical;

import clus.pruning.*;
import clus.util.ClusException;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.main.*;

public class HierRemoveInsigClasses extends PruneTree {

	PruneTree m_Pruner;
	ClusData m_PruneSet;
	ClassHierarchy m_Hier;
	boolean m_NoRoot;
	boolean m_UseBonferroni;
	double m_SigLevel;	
	int m_Bonferroni;
	
	public HierRemoveInsigClasses(ClusData pruneset, PruneTree other, boolean bonf, ClassHierarchy hier) {
		m_Pruner = other;
		m_PruneSet = pruneset;
		m_Hier = hier;
		m_UseBonferroni = bonf;
	}
	
	public void setNoRootPreds(boolean noroot) {
		m_NoRoot = noroot;
	}
		
	public void setSignificance(double siglevel) {
		m_SigLevel = siglevel;
	}
	
	public void prune(ClusNode node) throws ClusException {
		m_Pruner.prune(node);
		if (m_SigLevel != 0.0 && m_PruneSet.getNbRows() != 0) {
			// Make sure global statistic is also computed on prune set!
			m_Bonferroni = computeNRecursive(node);
			WHTDStatistic global = (WHTDStatistic)node.getTargetStat().cloneStat();
			m_PruneSet.calcTotalStat(global);
			global.calcMean();
			executeRecursive(node, global, (RowData)m_PruneSet);
			// executeRecursive(node, (WHTDStatistic)node.getTotalStat(), (RowData)m_PruneSet);
		}
	}
	
	public int computeNRecursive(ClusNode node) {
		int result = 0;
		if (node.atBottomLevel()) {
			WHTDStatistic stat = (WHTDStatistic)node.getTargetStat();
			result += stat.getNbPredictedClasses();
		}		
		for (int i = 0; i < node.getNbChildren(); i++) {
			result += computeNRecursive((ClusNode)node.getChild(i));
		}
		return result;
	}
	
	public boolean executeRecursive(ClusNode node, WHTDStatistic global, RowData data) {
		int nbok = 0;
		int arity = node.getNbChildren();
		for (int i = 0; i < arity; i++) {
			RowData subset = data.applyWeighted(node.getTest(), i);
			boolean isok = executeRecursive((ClusNode)node.getChild(i), global, subset);
			if (isok) nbok++;
		}
		if (node.atBottomLevel()) {
			WHTDStatistic orig = (WHTDStatistic)node.getTargetStat();
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
			if (m_UseBonferroni) {
				pred.setSigLevel(m_SigLevel/m_Bonferroni);
			} else {
				pred.setSigLevel(m_SigLevel);				
			}
			pred.calcMean();
			node.setTargetStat(pred);
			return !pred.m_MeanTuple.isRoot();
		}
		return true;
	}	
}
