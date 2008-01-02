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

public class DepthFirstInduce extends ClusInductionAlgorithm {
	
	protected FindBestTest m_FindBestTest;
	
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
				return ClusForest.selectAttributesForRandomForest(attrsAll, schema.getSettings().getNbRandomAttrSelected());				
			case Settings.ENSEMBLE_RSUBSPACES:
				return ClusForest.getRandomSubspaces();
			case Settings.ENSEMBLE_BAGSUBSPACES:
				return ClusForest.getRandomSubspaces();
			default:
				return schema.getDescriptiveAttributes();
			}
		}
	}
	
	public void induce(ClusNode node, RowData data) {
		// Initialize selector and perform various stopping criteria
		if (initSelectorAndStopCrit(node, data)) {
			node.makeLeaf();
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
			for (int j = 0; j < arity; j++) {
				ClusNode child = new ClusNode();
				node.setChild(child, j);				
				RowData subset = data.applyWeighted(test, j);				
				child.initClusteringStat(m_StatManager, subset);								
				child.initTargetStat(m_StatManager, subset);
				induce(child, subset);
			}
		} else {
			node.makeLeaf();
		}
	}
			
	public void initSelectorAndSplit(ClusStatistic stat) throws ClusException {
		m_FindBestTest.initSelectorAndSplit(stat);
	}
	
	public void cleanSplit() {
		m_FindBestTest.cleanSplit();
	}
		
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		RowData data = (RowData)cr.getTrainingSet();
		ClusNode root = null;
		// Begin of induction process
		int nbr = 0;
		while (true) {
			nbr++;			
			// Init root node
			root = new ClusNode();
			root.initClusteringStat(m_StatManager, data);
			root.initTargetStat(m_StatManager, data);			
			root.getClusteringStat().showRootInfo();
			initSelectorAndSplit(root.getClusteringStat());
			// Induce the tree
			induce(root, data);
			// Refinement finished
			if (Settings.EXACT_TIME == false) break;
		}
		root.postProc(null);
		cleanSplit();
		return root;
	}
}
