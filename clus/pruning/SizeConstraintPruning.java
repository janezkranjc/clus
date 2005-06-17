package clus.pruning;

import clus.main.*;
import clus.data.attweights.*;

public class SizeConstraintPruning extends PruneTree {

	public int[] m_MaxSize;
	public ClusAttributeWeights m_TargetWeights;

	public SizeConstraintPruning(int maxsize, ClusAttributeWeights prod) {
		m_MaxSize = new int[1];
		m_MaxSize[0] = maxsize;
		m_TargetWeights = prod;
	}
	
	public SizeConstraintPruning(int[] maxsize, ClusAttributeWeights prod) {
		m_MaxSize = maxsize;
		m_TargetWeights = prod;
	}	
	
	public ClusAttributeWeights getTargetWeights() {
		return m_TargetWeights;
	}

	public void pruneInitialize(ClusNode node, int size) {
		recursiveInitialize(node, size);		
	}
	
	public void pruneExecute(ClusNode node, int size) {
		computeCosts(node, size);
		pruneToSizeK(node, size);
	}
	
	public void prune(ClusNode node) {
		pruneInitialize(node, m_MaxSize[0]);
		pruneExecute(node, m_MaxSize[0]);
	}
	
	public int getNbResults() {
		return m_MaxSize.length;
	}
	
	public void prune(int result, ClusNode node) {
		pruneInitialize(node, m_MaxSize[result]);
		pruneExecute(node, m_MaxSize[result]);
	}	
	
	private static void recursiveInitialize(ClusNode node, int size) {
		/* Create array for each node */
		SizeConstraintVisitor visitor = new SizeConstraintVisitor(size); 
		node.setVisitor(visitor);
		/* Recursively visit children */
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClusNode child = (ClusNode)node.getChild(i);
			recursiveInitialize(child, size);
		}
	}
	
	public double computeCosts(ClusNode node, int l) {
		SizeConstraintVisitor visitor = (SizeConstraintVisitor)node.getVisitor();
		if (visitor.computed[l]) return visitor.cost[l];
		if (l < 3 || node.atBottomLevel()) {
			visitor.cost[l] = node.getTotalStat().getError(m_TargetWeights);
		} else {
			visitor.cost[l] = node.getTotalStat().getError(m_TargetWeights);
			ClusNode ch1 = (ClusNode)node.getChild(0);
			ClusNode ch2 = (ClusNode)node.getChild(1);
			for (int k1 = 1; k1 <= l-2; k1++) {
				int k2 = l - k1 - 1;
				double cost = computeCosts(ch1, k1) + 
				              computeCosts(ch2, k2);
				// System.out.println("cost "+cost+" "+arr[l].cost);
				if (cost < visitor.cost[l]) {
					visitor.cost[l] = cost;
					visitor.left[l] = k1;
				} 
			}		
		}
		// System.out.println("Node: "+node+" cost "+l+" = "+arr[l]);
		visitor.computed[l] = true;
		return visitor.cost[l];
	}

	public static void pruneToSizeK(ClusNode node, int l) {
		if (node.atBottomLevel()) return;
		SizeConstraintVisitor visitor = (SizeConstraintVisitor)node.getVisitor();
		if (l < 3 || visitor.left[l] == 0) {
			node.makeLeaf();
		} else {
			int k1 = visitor.left[l];
			int k2 = l - k1 - 1;
			pruneToSizeK((ClusNode)node.getChild(0), k1);
			pruneToSizeK((ClusNode)node.getChild(1), k2);
		}
	}
}
