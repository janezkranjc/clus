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

import clus.main.*;
import clus.util.*;
import clus.algo.*;
import clus.algo.split.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.model.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.ext.ensembles.*;

import java.io.*;
import java.util.*;

public class DepthFirstInduce extends ClusInductionAlgorithm {

	protected FindBestTest m_FindBestTest;
	protected ClusNode m_Root;

	public DepthFirstInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		super(schema, sett);
		m_FindBestTest = new FindBestTest(getStatManager());
	}

	public DepthFirstInduce(ClusInductionAlgorithm other) {
		super(other);
		m_FindBestTest = new FindBestTest(getStatManager());
	}

	public DepthFirstInduce(ClusInductionAlgorithm other, NominalSplit split) {
		super(other);
		m_FindBestTest = new FindBestTest(getStatManager(), split);
	}

	public void initialize() throws ClusException, IOException {
		super.initialize();
	}

	public FindBestTest getFindBestTest() {
		return m_FindBestTest;
	}

	public CurrentBestTestAndHeuristic getBestTest() {
		return m_FindBestTest.getBestTest();
	}

	public boolean initSelectorAndStopCrit(ClusNode node, RowData data) {
		int max = getSettings().getTreeMaxDepth();
		if (max != -1 && node.getLevel() >= max) {
			return true;
		}
		return m_FindBestTest.initSelectorAndStopCrit(node.getClusteringStat(), data);
	}

	public ClusAttrType[] getDescriptiveAttributes() {
		ClusSchema schema = getSchema();
		Settings sett = getSettings();
		if (!sett.isEnsembleMode()) {
			return schema.getDescriptiveAttributes();
		} else {
			switch (sett.getEnsembleMethod()) {
			case Settings.ENSEMBLE_BAGGING:
				return schema.getDescriptiveAttributes();
			case Settings.ENSEMBLE_RFOREST:
				ClusAttrType[] attrsAll = schema.getDescriptiveAttributes();
				ClusEnsembleInduce.selectRandomSubspaces(attrsAll, schema.getSettings().getNbRandomAttrSelected());
				return ClusEnsembleInduce.getRandomSubspaces();
			case Settings.ENSEMBLE_RSUBSPACES:
				return ClusEnsembleInduce.getRandomSubspaces();
			case Settings.ENSEMBLE_BAGSUBSPACES:
				return ClusEnsembleInduce.getRandomSubspaces();
			default:
				return schema.getDescriptiveAttributes();
			}
		}
	}

	public void filterAlternativeSplits(ClusNode node, RowData data, RowData[] subsets) {
		boolean removed = false;
		CurrentBestTestAndHeuristic best = m_FindBestTest.getBestTest();
		int arity = node.getTest().updateArity();
		ArrayList v = best.getAlternativeBest(); // alternatives: all tests that result in same heuristic value
		for (int k = 0; k < v.size(); k++) {
			NodeTest nt = (NodeTest) v.get(k);
			int altarity = nt.updateArity();
			// remove alternatives that have different arity than besttest
			if (altarity != arity) {
				v.remove(k);
				k--;
				//System.out.println("Alternative split: " + nt.getString());
				removed = true;
			} else {
				boolean okay = true;
				for (int l = 0; l < altarity; l++) {
					if (okay) {
						RowData altrd = data.applyWeighted(nt, l);
						// remove alternatives that result in subsets with different nb of tuples than besttest
						if (subsets[l].getNbRows()!=altrd.getNbRows()) {
							v.remove(k);
							k--;
							//System.out.println("Alternative split: " + nt.getString());
							removed = true;
							okay = false;
						} else {
							for (int m=0; m<subsets[l].getNbRows(); m++) {
								if (okay) {
									// remove alternatives that result in subsets with different tuples than besttest
									if (!subsets[l].getTuple(m).equals(altrd.getTuple(m))) {
										v.remove(k);
										k--;
										//System.out.println("Alternative split: " + nt.getString());
										removed = true;
										okay = false;
									}
								}
							}
						}
					}
				}
			}
		}
		node.setAlternatives(v);
//		if (removed) System.out.println("Alternative splits were possible");
	}

	public void makeLeaf(ClusNode node) {
		node.makeLeaf();
		if (getSettings().hasTreeOptimize(Settings.TREE_OPTIMIZE_NO_CLUSTERING_STATS)) {
			node.setClusteringStat(null);
		}
	}
	
	public void induce(ClusNode node, RowData data) {
		// Initialize selector and perform various stopping criteria
		if (initSelectorAndStopCrit(node, data)) {
			makeLeaf(node);
			return;
		}
		// Find best test
		ClusAttrType[] attrs = getDescriptiveAttributes();
		for (int i = 0; i < attrs.length; i++) {
			ClusAttrType at = attrs[i];
			if (at instanceof NominalAttrType) m_FindBestTest.findNominal((NominalAttrType)at, data);
			else m_FindBestTest.findNumeric((NumericAttrType)at, data);
		}
		// Partition data + recursive calls
		CurrentBestTestAndHeuristic best = m_FindBestTest.getBestTest();
		if (best.hasBestTest()) {
			node.testToNode(best);
			// Output best test
			if (Settings.VERBOSE > 0) System.out.println("Test: "+node.getTestString()+" -> "+best.getHeuristicValue());
			// Create children
			int arity = node.updateArity();
			NodeTest test = node.getTest();
			RowData[] subsets = new RowData[arity];
			for (int j = 0; j < arity; j++) {
				subsets[j] = data.applyWeighted(test, j);
			}
			if (getSettings().showAlternativeSplits()) {
				filterAlternativeSplits(node, data, subsets);
			}
			if (node != m_Root && getSettings().hasTreeOptimize(Settings.TREE_OPTIMIZE_NO_INODE_STATS)) {
				// Don't remove statistics of root node; code below depends on them
				node.setClusteringStat(null);
				node.setTargetStat(null);
			}
			for (int j = 0; j < arity; j++) {
				ClusNode child = new ClusNode();
				node.setChild(child, j);
				child.initClusteringStat(m_StatManager, m_Root.getClusteringStat(), subsets[j]);
				child.initTargetStat(m_StatManager, m_Root.getTargetStat(), subsets[j]);
				induce(child, subsets[j]);
			}
		} else {
			makeLeaf(node);
		}
	}

	public void initSelectorAndSplit(ClusStatistic stat) throws ClusException {
		m_FindBestTest.initSelectorAndSplit(stat);
	}

	public void setInitialData(ClusStatistic stat, RowData data) throws ClusException {
		m_FindBestTest.setInitialData(stat,data);
	}

	public void cleanSplit() {
		m_FindBestTest.cleanSplit();
	}

	public ClusNode induceSingleUnpruned(RowData data) throws ClusException, IOException {
		m_Root = null;
		// Begin of induction process
		int nbr = 0;
		while (true) {
			nbr++;
			// Init root node
			m_Root = new ClusNode();
			m_Root.initClusteringStat(m_StatManager, data);
			m_Root.initTargetStat(m_StatManager, data);
			m_Root.getClusteringStat().showRootInfo();
			initSelectorAndSplit(m_Root.getClusteringStat());
			setInitialData(m_Root.getClusteringStat(),data);
			// Induce the tree
			induce(m_Root, data);
			// Refinement finished
			if (Settings.EXACT_TIME == false) break;
		}
		m_Root.postProc(null);
		cleanSplit();
		return m_Root;
	}

	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		return induceSingleUnpruned((RowData)cr.getTrainingSet());
	}
}
