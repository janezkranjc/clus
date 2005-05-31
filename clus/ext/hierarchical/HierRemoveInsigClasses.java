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
	boolean m_NoRoot;
	boolean m_NoRootAfterInSig;
	double m_SigLevel;	
	int m_Bonferroni;
	
	public HierRemoveInsigClasses(ClusData pruneset, PruneTree other, ClassHierarchy hier) {
		m_Pruner = other;
		m_PruneSet = pruneset;
		m_Hier = hier;
	}
	
	public void setNoRootPreds(boolean noroot) {
		m_NoRoot = noroot;
	}
	
	public void setNoRootAfterInSigPreds(boolean noroot) {
		m_NoRootAfterInSig = noroot;
	}	
	
	public void setSignificance(double siglevel) {
		m_SigLevel = siglevel;
	}
	
	public void prune(ClusNode node) {
		m_Pruner.prune(node);
/*		if (m_NoRoot) {
			executeNoRootPrune(node);
		}
*/		
		if (m_SigLevel != 0.0 && m_PruneSet.getNbRows() != 0) {
			m_Bonferroni = computeNRecursive(node);
			executeRecursive(node, (WHTDStatistic)node.getTotalStat(), (RowData)m_PruneSet);
		}
	}
	
	public int computeNRecursive(ClusNode node) {
		int result = 0;
		if (node.atBottomLevel()) {
			WHTDStatistic stat = (WHTDStatistic)node.getTotalStat();
			result += stat.getNbPredictedClasses();
		}		
		for (int i = 0; i < node.getNbChildren(); i++) {
			result += computeNRecursive((ClusNode)node.getChild(i));
		}
		return result;
	}
	
	public boolean executeNoRootPrune(ClusNode node) {
		int nbok = 0;
		int arity = node.getNbChildren();
		for (int i = 0; i < arity; i++) {
			boolean isok = executeNoRootPrune((ClusNode)node.getChild(i));
			if (isok) nbok++;
		}
		if (nbok == 0) {
			node.makeLeaf();
		}
		if (node.atBottomLevel()) {
			return node.getTotalStat().isValid();
		}
		return true;
	}
	
	public boolean executeRecursive(ClusNode node, WHTDStatistic global, RowData data) {
		int nbok = 0;
		int arity = node.getNbChildren();
		for (int i = 0; i < arity; i++) {
			RowData subset = data.applyWeighted(node.getTest(), i);
			boolean isok = executeRecursive((ClusNode)node.getChild(i), global, subset);
			if (isok) nbok++;
		}
/*		
		if (m_NoRootAfterInSig && nbok == 0) {
			node.makeLeaf();
		}
*/		
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
			pred.setSigLevel(m_SigLevel/m_Bonferroni);
			pred.calcMean();
			node.setTotalStat(pred);
			return !pred.m_MeanTuple.isRoot();
		}
		return true;
	}	
}
