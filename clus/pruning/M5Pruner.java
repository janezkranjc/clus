/*
 * Created on May 12, 2005
 */
package clus.pruning;

import clus.main.*;
import clus.statistic.*;

public class M5Pruner implements PruneTree {
	
	double m_PruningMult = 2;
	double m_GlobalDeviation;
	TargetWeightProducer m_TargetWeights;
	
	public M5Pruner(TargetWeightProducer prod) {
		m_TargetWeights = prod;
	}
	
	public void prune(ClusNode node) {
		RegressionStat stat = (RegressionStat)node.getTotalStat();
		m_TargetWeights.setTotalStat(stat);
		m_GlobalDeviation = Math.sqrt(stat.getError(m_TargetWeights)/stat.getTotalWeight());
		pruneRecursive(node);
	}
	
	private double pruningFactor(double num_instances, int num_params) {
		if (num_instances <= num_params) {
			return 10.0;    // Caution says Yong in his code
		} 
		return ((double) (num_instances + m_PruningMult * num_params) 
				/ (double) (num_instances - num_params));
	} 
	
	public void pruneRecursive(ClusNode node) {
		if (node.atBottomLevel()) {
			return;
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClusNode child = (ClusNode)node.getChild(i);
			pruneRecursive(child);
		}
		ClusStatistic total = node.getTotalStat();
		double rmsModel = Math.sqrt(total.getError(m_TargetWeights)/total.getTotalWeight());
		double adjustedErrorModel = rmsModel * pruningFactor(total.getTotalWeight(), 1);		
	    double rmsSubTree = Math.sqrt(node.estimateError(m_TargetWeights));
	    double adjustedErrorNode = rmsSubTree * pruningFactor(total.getTotalWeight(), node.getModelSize());
	    if ((adjustedErrorModel <= adjustedErrorNode) 
             || (adjustedErrorModel < (m_GlobalDeviation * 0.00001))) {
	    	node.makeLeaf();
	    }
	}

}
