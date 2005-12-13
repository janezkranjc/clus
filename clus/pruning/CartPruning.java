package clus.pruning;

import clus.data.attweights.*;
import clus.main.*;

public class CartPruning extends PruneTree {

	protected ClusAttributeWeights m_Weights;
	
	public CartPruning(ClusAttributeWeights weights) {
	}
	
	public void sequenceInitialize(ClusNode node) {
		TreeErrorComputer.recursiveInitialize(node, new CartVisitor());
		internalRecursiveInitialize(node);
		setOriginalTree(node);		
	}

	public void sequenceReset() {
		setCurrentTree(null);
	}
	
	public ClusNode sequenceNext() {
		ClusNode result = getCurrentTree();
		if (result == null) {
			result = getOriginalTree().cloneTreeWithVisitors();			
		} else {
			internalSequenceNext(result);			
		}
		setCurrentTree(result);
		return result;
	}
	
	public void sequenceToElemK(ClusNode node, int k) {
		for (int i = 0; i < k; i++) {
			internalSequenceNext(node);
		}
	}
	
	public void internalSequenceNext(ClusNode node) {
		
		
	}
	
	public void internalRecursiveInitialize(ClusNode node) {
		int nb_c = node.getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			internalRecursiveInitialize((ClusNode)node.getChild(i));
		}
		CartVisitor cart = (CartVisitor)node.getVisitor();
		if (nb_c == 0) {
			cart.delta_u1 = 0; // number of leaves
			cart.delta_u2 = 0; // training set error
			cart.lambda_min = Double.POSITIVE_INFINITY;
		} else {
			//cart.lambda = - cart.delta_u2
		}
	}
}

















