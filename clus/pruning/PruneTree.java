package clus.pruning;

import clus.data.rows.RowData;
import clus.main.*;
import clus.util.ClusException;

public class PruneTree {
	
	protected ClusNode m_CrTree, m_OrigTree;

	public void prune(ClusNode node) throws ClusException {
	}
	
	public void setTrainingData(RowData data) {
	}
	
// Methods for pruners that may return more than one result
	public int getNbResults() {
		return 1;
	}
	
	public void prune(int result, ClusNode node) throws ClusException {
		prune(node);
	}
	
// Methods for sequence based pruners, such as CartPruning and SizeConstraintPruning
	public void sequenceInitialize(ClusNode node) {
	}

	public void sequenceReset() {
	}
	
	public ClusNode sequenceNext() {
		return null;
	}
	
	public void sequenceToElemK(ClusNode node, int k) {
	}
	
	public ClusNode getCurrentTree() {
		return m_CrTree;
	}
	
	public void setCurrentTree(ClusNode node) {
		m_CrTree = node;
	}
	
	public ClusNode getOriginalTree() {
		return m_OrigTree;
	}
	
	public void setOriginalTree(ClusNode node) {
		m_OrigTree = node;
	}
}
