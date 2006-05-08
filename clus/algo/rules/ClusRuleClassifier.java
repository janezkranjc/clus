/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import jeans.util.cmdline.CMDLineArgs;
import clus.*;
import clus.algo.tdidt.ClusDecisionTree;
import clus.main.*;
import clus.util.ClusException;
import clus.algo.induce.*;

public class ClusRuleClassifier extends ClusClassifier {
	
	public ClusRuleClassifier(Clus clus) {
		super(clus);
	}
	
	public void initializeInduce(ClusInduce induce, CMDLineArgs cargs) throws ClusException {
		induce.getStatManager().setRuleInduce(true);
		induce.getStatManager().initRuleSettings();
	}	
	
	public void printInfo() {
		if (!getSettings().isRandomRules()) {
			System.out.println("RuleSystem based on CN2");
			System.out.println("Heuristic: "+getStatManager().getHeuristicName());
		} else {
			System.out.println("RuleSystem generating random rules");
		}
	}
	
	public ClusModel induceSingle(ClusRun cr) throws ClusException, IOException {
		// ClusRulesForAttrs rfa = new ClusRulesForAttrs();
		// return rfa.constructRules(cr);
		DepthFirstInduce tree_induce = (DepthFirstInduce)getInduce();
		ClusRuleInduce rule_induce = new ClusRuleInduce(tree_induce);
		if (!getSettings().isRandomRules()) {
			return rule_induce.induce(cr);
		} else {
			return rule_induce.induceRandomly(cr);
		}
	}
	
	public void induce(ClusRun cr) throws ClusException, IOException {
		ClusModel model = induceSingle(cr);
		// FIXME: implement cloneModel();
		// cr.getModelInfo(ClusModels.ORIGINAL).setModel(model);
		// ClusModel pruned = model.cloneModel();
		ClusModel pruned = model;
		cr.getModelInfo(ClusModels.PRUNED).setModel(pruned);
		ClusModel defmodel = ClusDecisionTree.induceDefault(cr);
		cr.getModelInfo(ClusModels.DEFAULT).setModel(defmodel);		
	}
	
	public void initializeSummary(ClusSummary summ) {	
		ClusModels.DEFAULT = summ.addModel("Default");
		ClusModels.ORIGINAL = summ.addModel("Original");
		ClusModels.PRUNED = summ.addModel("Pruned");
	}
}
