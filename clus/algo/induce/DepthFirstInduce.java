package clus.algo.induce;

import clus.main.*;
import clus.util.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.nominal.split.*;
import clus.error.multiscore.*;

import java.io.*;

public class DepthFirstInduce extends ClusInduce {
	
	protected NominalSplit m_Split;
	protected TestSelector m_Selector = new TestSelector();
	
	public DepthFirstInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		super(schema, sett);
	}
	
	public DepthFirstInduce(ClusInduce other) {
		super(other);
	}
	
	public DepthFirstInduce(ClusInduce other, NominalSplit split) {
		super(other);
		m_Split = split;
	}
	
	public ClusData createData() {
		return new RowData(m_Schema);
	}
	
	public void findNominal(NominalAttrType at, RowData data) {
		// Reset positive statistic
		int nbvalues = at.getNbValues();
		m_Selector.reset(nbvalues + 1);
		// For each attribute value		
		int nb_rows = data.getNbRows();
		for (int i = 0; i < nb_rows; i++) {
			DataTuple tuple = data.getTuple(i);
			int value = at.getNominal(tuple);			
			m_Selector.m_TestStat[value].updateWeighted(tuple, i);			
		}
		// Find best split
		m_Split.findSplit(m_Selector, at);
	}
	
	public void findNumeric(NumericAttrType at, RowData data) {	
		DataTuple tuple;
		int idx = at.getSpecialIndex();
		if (at.isSparse()) {
			data.sortSparse(idx);
		} else {
			data.sort(idx);
		}
		m_Selector.reset(2);		
		// Missing values
		int first = 0;				
		int nb_rows = data.getNbRows();
		// Copy total statistic into corrected total
		m_Selector.copyTotal();
		if (at.hasMissing()) {
			// Because of sorting, all missing values are in the front :-)
			while (first < nb_rows && (tuple = data.getTuple(first)).hasNumMissing(idx)) {
				m_Selector.m_MissingStat.updateWeighted(tuple, first);
				first++;
			}
			m_Selector.subtractMissing();
		}		
		double prev = Double.NaN;
		if (Settings.ONE_NOMINAL) {
			int prev_cl = -1;		
			for (int i = first; i < nb_rows; i++) {
				tuple = data.getTuple(i);
				double value = tuple.getDoubleVal(idx);
				int crcl = tuple.getClassification();				
				if (prev_cl == -1 && value != prev && value != Double.NaN) {
					m_Selector.updateNumeric(value, at);
					prev_cl = crcl;
				}
				prev = value; 				
				if (prev_cl != crcl) prev_cl = -1;
				m_Selector.m_PosStat.updateWeighted(tuple, i);				
			}		
		} else {
			for (int i = first; i < nb_rows; i++) {			
				tuple = data.getTuple(i);
				double value = tuple.getDoubleVal(idx);						
				if (value != prev) {
					if (value != Double.NaN) {
						m_Selector.updateNumeric(value, at);
					}
					prev = value;
				}				
				m_Selector.m_PosStat.updateWeighted(tuple, i);
			}
		}
	}
	
	public void initSelectorAndSplit(ClusStatistic totstat) {
		m_Selector.create(m_StatManager, m_MaxStats);
		m_Selector.setRootStatistic(totstat);
		if (Settings.BINARY_SPLIT) m_Split = new SubsetSplit();
		else m_Split = new NArySplit();
		m_Split.initialize(m_StatManager);	
	}
	
	public boolean initSelectorAndStopCrit(ClusNode node, RowData data) {
		int max = Settings.TREE_MAX_DEPTH.getValue();
		if (max != -1 && node.getDepth() >= max) {
			return true;		
		}
		return initSelectorAndStopCrit(node.getTotalStat(), data);
	}
	
	public boolean initSelectorAndStopCrit(ClusStatistic total, RowData data) {
		m_Selector.initTestSelector(total, data);
		// FIXME - split only if there are enabled nominal attrs		
		m_Split.setSDataSize(data.getNbRows());
		return m_Selector.stopCrit();
	}
	
	public void induce(ClusNode node, RowData data) {
		long t0;
		// Initialize selector and perform various stopping criteria
		if (initSelectorAndStopCrit(node, data)) {
			node.makeLeaf();
			return;		
		}
		// Find best test
		ClusSchema schema = data.getSchema();
		ClusAttrType[] attrs = schema.getNormalAttrs();
		int nb_normal = attrs.length;
		for (int i = 0; i < nb_normal; i++) {
			ClusAttrType at = attrs[i];
			if (at instanceof NominalAttrType) findNominal((NominalAttrType)at, data);
			else findNumeric((NumericAttrType)at, data);
		}			
		// Partition data + recursive calls
		if (m_Selector.hasBestTest()) {
			m_Selector.testToNode(node);
			// Output best test
			if (Settings.VERBOSE > 0) System.out.println("Test: "+node.getTestString()+" -> "+m_Selector.m_BestHeur);	
			// Create children
			int arity = node.updateArity();
			NodeTest test = node.getTest();
			for (int j = 0; j < arity; j++) {
				ClusNode child = new ClusNode();
				node.setChild(child, j);				
				RowData subset = data.applyWeighted(test, j);				
				child.initTotalStat(m_StatManager, subset);								
				induce(child, subset);
			}
		} else {
			node.makeLeaf();
		}
	}
	
	public void cleanSplit() {
		m_Split = null;
	}
	
	public TestSelector getSelector() {
		return m_Selector;
	}
	
	public ClusStatistic createTotalStat(RowData data) {
		ClusStatistic stat = m_StatManager.createStatistic();
		stat.setSDataSize(data.getNbRows());
		data.calcTotalStat(stat);
		stat.optimizePreCalc(data);
		return stat;
	}
	
	public ClusNode induce(ClusRun cr, MultiScore score) {
		RowData data = (RowData)cr.getTrainingSet();
		ClusNode root = null;
		// Begin of induction process
		int nbr = 0;
		while (true) {
			nbr++;			
			// Init root node
			root = new ClusNode();
			root.initTotalStat(m_StatManager, data);
			root.getTotalStat().showRootInfo();
			initSelectorAndSplit(root.getTotalStat());
			// Induce the tree
			induce(root, data);
			// Refinement finished
			if (Settings.EXACT_TIME == false) break;
		}
		root.postProc(score);
		cleanSplit();
		return root;
	}
}
