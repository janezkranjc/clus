/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import jeans.util.cmdline.CMDLineArgs;
import clus.*;
import clus.algo.tdidt.ClusDecisionTree;
import clus.main.*;
import clus.algo.induce.*;

public class ClusRuleClassifier extends ClusClassifier {
	
	public ClusRuleClassifier(Clus clus) {
		super(clus);
	}

	public void initializeInduce(ClusInduce induce, CMDLineArgs cargs) {
		induce.getStatManager().setRuleInduce(true);
	}	
	
	public void printInfo() {
		System.out.println("RuleSystem based on CN2");
		System.out.println("Heuristic: "+getStatManager().getHeuristicName());
	}
	
	public ClusModel induceSingle(ClusRun cr) {
		DepthFirstInduce tree_induce = (DepthFirstInduce)getInduce();
		ClusRuleInduce rule_induce = new ClusRuleInduce(tree_induce);
		return rule_induce.induce(cr);		
	}

	public void induce(ClusRun cr){
		ClusModel model = induceSingle(cr);
		cr.getModelInfo(ClusModels.ORIGINAL).setModel(model);
		cr.getModelInfo(ClusModels.PRUNED).setModel(model);
		ClusModel defmodel = ClusDecisionTree.induceDefault(cr);
		cr.getModelInfo(ClusModels.DEFAULT).setModel(defmodel);		
	}
	
	public void initializeSummary(ClusSummary summ) {	
		ClusModels.DEFAULT = summ.addModel("Default");
		ClusModels.ORIGINAL = summ.addModel("Original");
		ClusModels.PRUNED = summ.addModel("Pruned");
	}
}
