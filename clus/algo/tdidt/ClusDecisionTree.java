
package clus.algo.tdidt;

import java.io.IOException;

import clus.main.*;
import clus.pruning.*;
import clus.util.*;
import clus.algo.induce.*;
import clus.algo.rules.*;
import clus.data.rows.*;
import clus.*;

import jeans.util.cmdline.*;

public class ClusDecisionTree extends ClusClassifier {

	public ClusDecisionTree(Clus clus) {
		super(clus);
	}

	public void printInfo() {
		System.out.println("TDIDT");
		System.out.println("Heuristic: "+getStatManager().getHeuristicName());
	}
	
	public ClusInduce createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {
		if (sett.hasConstraintFile()) {
			boolean fillin = cargs.hasOption("fillin");
			return new ConstraintDFInduce(schema, sett, fillin);
		} else {
			return new DepthFirstInduce(schema, sett);
		}
	}	
	
	public final static ClusNode pruneToRoot(ClusNode orig) {
		ClusNode pruned = (ClusNode) orig.cloneNode();
		pruned.makeLeaf();
		return pruned;
	}
	
	public static ClusModel induceDefault(ClusRun cr) {
		ClusNode node = new ClusNode();
		RowData data = (RowData)cr.getTrainingSet();
		node.initTargetStat(cr.getStatManager(), data);
		node.computePrediction();
		node.makeLeaf();		
		return node;
	}
	
	public void convertToRules(ClusRun cr, int tree) throws ClusException, IOException {
		ClusNode tree_root = (ClusNode)cr.getModel(tree);
		ClusRulesFromTree rft = new ClusRulesFromTree(true);
		ClusRuleSet rule_set = null;
		if (getSettings().computeCompactness()) {
			rule_set = rft.constructRules(cr, tree_root, getStatManager());
		} else {
			rule_set = rft.constructRules(tree_root, getStatManager());
		}
		ClusModelInfo rules_info = cr.addModelInfo();
		rules_info.setModel(rule_set);
		rules_info.setName("Rules: "+cr.getModelName(tree));
	}

	public void pruneAll(ClusRun cr)	throws ClusException, IOException {
		ClusNode orig = (ClusNode)cr.getModel(ClusModels.ORIGINAL);
		orig.numberTree();
		PruneTree pruner = getStatManager().getTreePruner(cr.getPruneSet());
		pruner.setTrainingData((RowData) cr.getTrainingSet());
		int nb = pruner.getNbResults();
		for (int i = 0; i < nb; i++) {
			ClusNode pruned = (ClusNode) orig.cloneTree();
			pruner.prune(i, pruned);
			pruned.numberTree();
			ClusModelInfo pruned_info = cr.addModelInfo(ClusModels.PRUNED + i);
			pruned_info.setModel(pruned);
			if (nb == 1) {
				pruned_info.setName("Pruned");
			} else {
				pruned_info.setName("P"+(i+1));
			}
		}
	}
	
	public final ClusModel pruneSingle(ClusModel orig, ClusRun cr) throws ClusException {
		ClusNode pruned = (ClusNode)((ClusNode)orig).cloneTree();
		PruneTree pruner = getStatManager().getTreePruner(cr.getPruneSet());
		pruner.setTrainingData((RowData) cr.getTrainingSet());
		pruner.prune(pruned);
		return pruned;
	}	
	
	public void postProcess(ClusRun cr) throws ClusException, IOException {
		ClusNode orig = (ClusNode)cr.getModel(ClusModels.ORIGINAL);		
		ClusModelInfo orig_info = cr.getModelInfo(ClusModels.ORIGINAL);
		orig_info.setName("Original");
		ClusNode defmod = pruneToRoot(orig);
		ClusModelInfo def_info = cr.addModelInfo(ClusModels.DEFAULT);
		def_info.setModel(defmod);
		def_info.setName("Default");
		if (getSettings().rulesFromTree() == Settings.CONVERT_RULES_PRUNED) {
			convertToRules(cr, ClusModels.PRUNED);
		} else if (getSettings().rulesFromTree() == Settings.CONVERT_RULES_ALL) {
			int cr_nb = cr.getNbModels();
			for (int i = 0; i < cr_nb; i++) {
				if (i != ClusModels.DEFAULT) {
					convertToRules(cr, i);					
				}
			}
		}
	}
}
