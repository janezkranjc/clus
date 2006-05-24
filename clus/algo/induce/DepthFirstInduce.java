package clus.algo.induce;

import clus.main.*;
import clus.util.*;
import clus.algo.rules.ClusRuleHeuristicCompactness;
import clus.data.rows.*;
import clus.data.type.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.nominal.split.*;
import clus.error.multiscore.*;

import java.io.*;
import java.util.*;

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
    int nb_rows = data.getNbRows();
		if (!getSettings().isCompHeurRuleDist()) {
			// For each attribute value   
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				int value = at.getNominal(tuple);     
				m_Selector.m_TestStat[value].updateWeighted(tuple, i);      
			}
		} else {
			// TODO: Perhaps ListArray[nbvalues] instead of int[nbvalues][nb_rows] would be better? 
			int[][] data_idx_per_val = new int[nbvalues][nb_rows];
			for (int j = 0; j < nbvalues; j++) {
				for (int i = 0; i < nb_rows; i++) {
					data_idx_per_val[j][i] = -1;
				}
			}
			// For each attribute value
			int[] counts = new int[nbvalues];
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				int value = at.getNominal(tuple);     
				m_Selector.m_TestStat[value].updateWeighted(tuple, i);
				if (value < nbvalues) {  // Skip missing values, will this be a problem somewhere?
					data_idx_per_val[value][i] = tuple.getIndex();
					counts[value]++;
				}
			}
			// Skip -1s
			int[][] data_ipv = new int[nbvalues][];
			for (int j = 0; j < nbvalues; j++) {
				data_ipv[j] = new int[counts[j]];
				int k = 0;
				for (int i = 0; i < nb_rows; i++) {
					if (data_idx_per_val[j][i] != -1) {
						data_ipv[j][k] = data_idx_per_val[j][i];
						k++;
					}
				}
			}
			((ClusRuleHeuristicCompactness)m_Selector.m_Heuristic).setDataIndexesPerVal(data_ipv);
		}
    // Find best split
    m_Split.findSplit(m_Selector, at);
  }
  
  /**
   * Randomly generates nominal split
   * @param at
   * @param data
   * @param rn
   */
  public void findNominalRandom(NominalAttrType at, RowData data, Random rn) {
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
    // Find the split
    m_Split.findRandomSplit(m_Selector, at, rn);
  }
  
  public void findNumeric(NumericAttrType at, RowData data) { 
    DataTuple tuple;
    int idx = at.getArrayIndex();
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
  	int[] data_idx = new int[nb_rows]; // TODO: Skip missing ones?!
  	if (getSettings().isCompHeurRuleDist()) {
  		for (int i = first; i < nb_rows; i++) {
  			data_idx[i] = data.getTuple(i).getIndex();
  		}
  	}
  	for (int i = first; i < nb_rows; i++) {
  		tuple = data.getTuple(i);
  		double value = tuple.getDoubleVal(idx);
  		if (value != prev) {
  			if (value != Double.NaN) {
  				if (getSettings().isCompHeurRuleDist()) {
  					int[] subset_idx = new int[i-first];
  					System.arraycopy(data_idx, first, subset_idx, 0, i-first);
  					((ClusRuleHeuristicCompactness)m_Selector.m_Heuristic).setDataIndexes(subset_idx);
  				}
  				System.err.println("Value (>): " + value);
  				m_Selector.updateNumeric(value, at);
  			}
  			prev = value;
  		}       
  		m_Selector.m_PosStat.updateWeighted(tuple, i);
  	}
  	// For rules check inverse splits also
  	if (m_StatManager.isRuleInduce()) {
  		m_Selector.reset();
  		DataTuple next_tuple = data.getTuple(nb_rows-1);
  		double next = next_tuple.getDoubleVal(idx);
  		for (int i = nb_rows-1; i > first; i--) {
  			tuple = next_tuple;
  			next_tuple = data.getTuple(i-1);
  			double value = next;
  			next = next_tuple.getDoubleVal(idx);
  			m_Selector.m_PosStat.updateWeighted(tuple, i);
  			if ((value != next) && (value != Double.NaN)) {
  				if (getSettings().isCompHeurRuleDist()) {
  					int[] subset_idx = new int[nb_rows-i];
  					System.arraycopy(data_idx, i, subset_idx, 0, nb_rows-i);
  					((ClusRuleHeuristicCompactness)m_Selector.m_Heuristic).setDataIndexes(subset_idx);
  				}
  				System.err.println("Value (<=): " + value);
  				m_Selector.updateInverseNumeric(value, at);
  			}
  		}
  	}
  }

  /**
   * Randomly generates numeric split value
   * @param at
   * @param data
   * @param rn
   */
  public void findNumericRandom(NumericAttrType at, RowData data, RowData orig_data, Random rn) { 
    DataTuple tuple;
    int idx = at.getArrayIndex();
    // Sort values from large to small
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
    // Do the same for original data, except updating the statistics:
    // Sort values from large to small
    if (at.isSparse()) {
      orig_data.sortSparse(idx);
    } else {
      orig_data.sort(idx);
    }
    // Missing values
    int orig_first = 0;        
    int orig_nb_rows = orig_data.getNbRows();
    if (at.hasMissing()) {
      // Because of sorting, all missing values are in the front :-)
      while (orig_first < orig_nb_rows && 
             (tuple = orig_data.getTuple(orig_first)).hasNumMissing(idx)) {
        orig_first++;
      }
    }   
    // Generate the random split value based on the original data
    double min_value = orig_data.getTuple(orig_nb_rows-1).getDoubleVal(idx);
    double max_value = orig_data.getTuple(orig_first).getDoubleVal(idx);
    double split_value = (max_value - min_value) * rn.nextDouble() + min_value;
    for (int i = first; i < nb_rows; i++) {
      tuple = data.getTuple(i);
      if (tuple.getDoubleVal(idx) <= split_value) break;
      m_Selector.m_PosStat.updateWeighted(tuple, i);        
    }
    m_Selector.updateNumeric(split_value, at);
  }
  
	public void initSelectorAndSplit(ClusStatistic totstat) throws ClusException {
		m_Selector.create(m_StatManager, m_MaxStats);
		m_Selector.setRootStatistic(totstat);
		if (Settings.BINARY_SPLIT) m_Split = new SubsetSplit();
		else m_Split = new NArySplit();
		m_Split.initialize(m_StatManager);	
	}
	
	public boolean initSelectorAndStopCrit(ClusNode node, RowData data) {
		int max = Settings.TREE_MAX_DEPTH.getValue();
		if (max != -1 && node.getLevel() >= max) {
			return true;		
		}
		return initSelectorAndStopCrit(node.getClusteringStat(), data);
	}
	
	public boolean initSelectorAndStopCrit(ClusStatistic total, RowData data) {
		m_Selector.initTestSelector(total, data);
		// FIXME - split only if there are enabled nominal attrs		
		m_Split.setSDataSize(data.getNbRows());
		return m_Selector.stopCrit();
	}
	
	public void induce(ClusNode node, RowData data) {
		// long t0;
		// Initialize selector and perform various stopping criteria
		if (initSelectorAndStopCrit(node, data)) {
			node.makeLeaf();
			return;		
		}
		// Find best test
		ClusSchema schema = data.getSchema();
		ClusAttrType[] attrs = schema.getDescriptiveAttributes();
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
				child.initClusteringStat(m_StatManager, subset);								
				child.initTargetStat(m_StatManager, subset);
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
	
	public ClusStatistic createTotalClusteringStat(RowData data) {
		ClusStatistic stat = m_StatManager.createClusteringStat();
		stat.setSDataSize(data.getNbRows());
		data.calcTotalStat(stat);
		stat.optimizePreCalc(data);
		return stat;
	}
	
	public ClusStatistic createTotalTargetStat(RowData data) {
		ClusStatistic stat = m_StatManager.createTargetStat();
		stat.setSDataSize(data.getNbRows());
		data.calcTotalStat(stat);
		stat.optimizePreCalc(data);
		return stat;
	}	
	
	public ClusNode induce(ClusRun cr, MultiScore score) throws ClusException {
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
		root.postProc(score);
		cleanSplit();
		return root;
	}
}
