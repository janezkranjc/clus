/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.algo.tdidt;

import java.io.IOException;

import clus.main.*;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.pruning.*;
import clus.util.*;
import clus.algo.*;
import clus.algo.rules.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.ext.ilevelc.*;
import clus.*;

import jeans.util.cmdline.*;

public class ClusDecisionTree extends ClusInductionAlgorithmType {

	public final static int LEVEL_WISE = 0;
	public final static int DEPTH_FIRST = 1;

	public ClusDecisionTree(Clus clus) {
		super(clus);
	}

	public void printInfo() {
		System.out.println("TDIDT");
		System.out.println("Heuristic: "+getStatManager().getHeuristicName());
	}

	public ClusInductionAlgorithm createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {
		if (sett.hasConstraintFile()) {
			boolean fillin = cargs.hasOption("fillin");
			return new ConstraintDFInduce(schema, sett, fillin);
		} else if (sett.isSectionILevelCEnabled()){
			return new ILevelCInduce(schema, sett);
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
	
	/**
	 * Convert the tree to rules
	 * @param cr
	 * @param tree ClusModel tree type (default, pruned, original). 
	 * @throws ClusException
	 * @throws IOException
	 */
	public void convertToRules(ClusRun cr, int tree) throws ClusException, IOException {
		ClusNode tree_root = (ClusNode)cr.getModel(tree);
		ClusRulesFromTree rft = new ClusRulesFromTree(true);
		ClusRuleSet rule_set = null;
		boolean compDis = getSettings().computeDispersion(); // Do we want to compute dispersion
		

		rule_set = rft.constructRules(cr, tree_root, getStatManager(), compDis,
				getSettings().getRulePredictionMethod());
//		rule_set = rft.constructRules(tree_root, getStatManager());
	
		ClusModelInfo rules_info = cr.addModelInfo();
		rules_info.setModel(rule_set);
		rules_info.setName("Rules - "+cr.getModelName(tree));
	}

	public void pruneAll(ClusRun cr)	throws ClusException, IOException {
		ClusNode orig = (ClusNode)cr.getModel(ClusModel.ORIGINAL);
		orig.numberTree();
		PruneTree pruner = getStatManager().getTreePruner(cr.getPruneSet());
		pruner.setTrainingData((RowData) cr.getTrainingSet());
		int nb = pruner.getNbResults();
		for (int i = 0; i < nb; i++) {
			ClusNode pruned = (ClusNode) orig.cloneTree();
			pruner.prune(i, pruned);
			pruned.numberTree();
			ClusModelInfo pruned_info = cr.addModelInfo(ClusModel.PRUNED + i);
			pruned_info.setModel(pruned);
			pruned_info.setName(pruner.getPrunedName(i));
		}
	}

	public final ClusModel pruneSingle(ClusModel orig, ClusRun cr) throws ClusException {
		ClusNode pruned = (ClusNode)((ClusNode)orig).cloneTree();
		PruneTree pruner = getStatManager().getTreePruner(cr.getPruneSet());
		pruner.setTrainingData((RowData) cr.getTrainingSet());
		pruner.prune(pruned);
		return pruned;
	}

	/**
	 * Post processing decision tree. E.g. converting to rules. 
	 * 
	 */
	public void postProcess(ClusRun cr) throws ClusException, IOException {
		ClusNode orig = (ClusNode)cr.getModel(ClusModel.ORIGINAL);
		ClusModelInfo orig_info = cr.getModelInfo(ClusModel.ORIGINAL);
		orig_info.setName("Original");
		ClusNode defmod = pruneToRoot(orig);
		ClusModelInfo def_info = cr.addModelInfo(ClusModel.DEFAULT);
		def_info.setModel(defmod);
		def_info.setName("Default");
		if (getSettings().rulesFromTree() == Settings.CONVERT_RULES_PRUNED) {
			convertToRules(cr, ClusModel.PRUNED);
		} else if (getSettings().rulesFromTree() == Settings.CONVERT_RULES_ALL) {
			int cr_nb = cr.getNbModels();
			for (int i = 0; i < cr_nb; i++) {
				if (i != ClusModel.DEFAULT) {
					convertToRules(cr, i);
				}
			}
		}
	}
}
