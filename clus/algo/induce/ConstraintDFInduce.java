/*
 * Created on May 1, 2005
 */
package clus.algo.induce;

import java.io.*;

import clus.data.rows.RowData;
import clus.data.type.NominalAttrType;
import clus.data.type.NumericAttrType;
import clus.error.multiscore.MultiScore;
import clus.main.*;
import clus.util.*;
import clus.statistic.*;
import clus.ext.constraint.*;
import clus.model.test.*;
import clus.data.type.*;

public class ConstraintDFInduce extends DepthFirstInduce {
	
	protected boolean m_FillIn;
	protected String m_ConstrFile;
	
	public ConstraintDFInduce(ClusSchema schema, Settings sett, boolean fillin) throws ClusException, IOException {
		super(schema, sett);
		m_FillIn = fillin;
		m_ConstrFile = sett.getConstraintFile();
	}
	
	public ConstraintDFInduce(ClusInduce other) {
		super(other);
	}
	
	public void fillInStatsAndTests(ClusNode node, RowData data) {
		NodeTest test = node.getTest();
		if (test == null) {
			// No test, so this is a leaf node
			return;
		}
		if (!test.hasConstants()) {
			// no constants in test, find optimal split constant
			if (initSelectorAndStopCrit(node, data)) {
				node.makeLeaf();
				return;		
			}
			ClusAttrType at = test.getType();
			if (at instanceof NominalAttrType) findNominal((NominalAttrType)at, data);
			else findNumeric((NumericAttrType)at, data);
			if (m_Selector.hasBestTest()) {
				m_Selector.testToNode(node);
				if (Settings.VERBOSE > 0) System.out.println("Fill in Test: "+node.getTestString()+" -> "+m_Selector.m_BestHeur);	
			} else {
				node.makeLeaf();
				return;
			}
		}
		NodeTest best_test = node.getTest();		
		for (int j = 0; j < node.getNbChildren(); j++) {
			ClusNode child = (ClusNode)node.getChild(j);
			RowData subset = data.applyWeighted(best_test, j);				
			child.initTotalStat(m_StatManager, subset);								
			fillInStatsAndTests(child, subset);
		}
	}
	
	public void induceRecursive(ClusNode node, RowData data) {
		if (node.atBottomLevel()) {
			induce(node, data);
		} else {
			NodeTest test = node.getTest();
			for (int j = 0; j < node.getNbChildren(); j++) {
				ClusNode child = (ClusNode)node.getChild(j);
				RowData subset = data.applyWeighted(test, j);				
				induceRecursive(child, subset);
			}			
		}
	}
	
	public ClusNode createRootNode(RowData data, ClusStatistic stat) {
		ClusConstraintFile file = ClusConstraintFile.getInstance();
		ClusNode root = file.getClone(m_ConstrFile);
		root.setTotalStat(stat);
		fillInStatsAndTests(root, data);
		return root;		
	}
	
	public ClusNode induce(ClusRun cr, MultiScore score) {
		RowData data = (RowData)cr.getTrainingSet();
		ClusStatistic stat = createTotalStat(data);
		initSelectorAndSplit(stat);
		ClusNode root = createRootNode(data, stat);
		if (!m_FillIn) {
			// Call induce on each leaf
			induceRecursive(root, data);
		}	
		root.postProc(score);
		cleanSplit();
		return root;
	}		
}
