/*
 * Created on May 12, 2005
 */
package clus.pruning;

import clus.main.*;
import clus.statistic.*;
import clus.data.rows.*;

// import clus.weka.*;

public class M5Pruner extends PruneTree {
	
	double m_PruningMult = 2;
	double m_GlobalDeviation;
	TargetWeightProducer m_TargetWeights;
	RowData m_TrainingData;
	
	public M5Pruner(TargetWeightProducer prod) {
		m_TargetWeights = prod;
	}
	
	public void prune(ClusNode node) {
		// ClusNode orig = null;
		// orig = (ClusNode)node.cloneTree();
		RegressionStat stat = (RegressionStat)node.getTotalStat();
		m_TargetWeights.setTotalStat(stat);
		m_GlobalDeviation = Math.sqrt(stat.getSS(m_TargetWeights)/stat.getTotalWeight());
		pruneRecursive(node);
		// System.out.println("Performing test of M5 pruning");
		// TestM5PruningRuleNode.performTest(orig, node, m_GlobalDeviation, m_TargetWeights, m_TrainingData);
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
		double rmsModel = Math.sqrt(total.getSS(m_TargetWeights)/total.getTotalWeight());
		double adjustedErrorModel = rmsModel * pruningFactor(total.getTotalWeight(), 1);		
	    double rmsSubTree = Math.sqrt(node.estimateSS(m_TargetWeights)/total.getTotalWeight());
	    double adjustedErrorNode = rmsSubTree * pruningFactor(total.getTotalWeight(), node.getModelSize());
	    // System.out.println("C mode: "+rmsModel+" tree: "+rmsSubTree);
	    // System.out.println("C modeadj: "+adjustedErrorModel +" treeadj: "+adjustedErrorNode);
	    if ((adjustedErrorModel <= adjustedErrorNode) 
             || (adjustedErrorModel < (m_GlobalDeviation * 0.00001))) {
	    	node.makeLeaf();
	    }
	}

	public void setTrainingData(RowData data) {
		m_TrainingData = data;
	}
}
