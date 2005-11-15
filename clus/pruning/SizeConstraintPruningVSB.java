package clus.pruning;

import java.util.*;
import java.io.*;

import jeans.io.MyFile;

import clus.main.*;
import clus.data.attweights.*;
import clus.data.rows.*;
import clus.error.*;

public class SizeConstraintPruningVSB extends PruneTree {

	protected RowData m_VSB;
	protected ClusErrorParent m_Error;
	protected ClusAttributeWeights m_Weights;
	protected int m_MaxSize;
	protected boolean m_1SERule;
	protected boolean m_HasMissing;
	protected String m_Output;
	
	public SizeConstraintPruningVSB(RowData data, ClusErrorParent error, ClusAttributeWeights weights, int maxsize) {
			m_VSB = data;
			m_Error = error;
			m_Weights = weights;
			m_MaxSize = maxsize;
			m_HasMissing = true;
	}
		
	public static void resize(ArrayList list, int size) {
		list.ensureCapacity(size);
		for (int i = 0; i < size; i++) {
			list.add(null);
		}
	}
	
	public void setOutputFile(String output) {
		m_Output = output;
	}
	
	public void set1SERule(boolean enable) {
		m_1SERule = enable;
	}
	
	public void setHasMissing(boolean missing) {
		m_HasMissing = missing;
	}
		
	public void prune(ClusNode node) {
		int max_size = node.getNbNodes();
		if (m_MaxSize != -1 && max_size > m_MaxSize) m_MaxSize = max_size;
		SizeConstraintPruning pruner = new SizeConstraintPruning(max_size, m_Weights);
		pruner.pruneInitialize(node, max_size);
		// Optimization if data set has missing values
		if (!m_HasMissing) {
			try {
				ClusError err = m_Error.getFirstError();
				SizeConstraintErrorComputer.initializeTestErrorsData(node, m_VSB, err);
			} catch (IOException e) { /* Can never occur */ }
		}
		// Create list of error measures
		ArrayList errors = new ArrayList();
		resize(errors, max_size+1);
		// Compute the error for all sizes
		for (int size = max_size; size >= 1; size--) {
			ClusNode pruned = node.cloneTreeWithVisitors();
			pruner.pruneExecute(pruned, size);
			ClusError err = SizeConstraintErrorComputer.computeErrorOptimized(pruned, m_VSB, m_Error, m_HasMissing);
			errors.set(size, err);			
		}
		// Find the size corresponding to the smallest error
		int best_size = -1;
		double min_error = Double.POSITIVE_INFINITY;
		for (int size = max_size; size >= 1; size--) {
			ClusError error = (ClusError)errors.get(size);
			double value = error.getModelError();
			if (value <= min_error) {
				min_error = value;
				best_size = size;
			}
		}
		// Find smallest tree within 1-SE.
		ClusError best_error = (ClusError)errors.get(best_size);
		double se = best_error.getModelErrorStandardError();
		int se1_size = -1;
		for (int size = max_size; size >= 1; size--) {
			ClusError error = (ClusError)errors.get(size);
			double value = error.getModelError();
			if (value <= min_error + se) {
				se1_size = size;
			}
		}
		if (se1_size == -1) se1_size = best_size;
		ClusError leaf_error = (ClusError)errors.get(1);
		System.out.println("Best size: "+best_size+" 1-SE size: "+se1_size+" Best MSE: "+min_error+" SE: "+se+" Leaf MSE: "+leaf_error.getModelError());
		// Write output to file
		if (m_Output != null) {
			MyFile log = new MyFile(m_Output);
			for (int size = 1; size <= max_size; size++) {
				ClusError error = (ClusError)errors.get(size);
				double value = error.getModelError();
				log.log(String.valueOf(size)+"\t"+String.valueOf(value));
			}
			log.close();
		}
		// Finally prune given tree to size specified
		if (m_1SERule) {
			pruner.pruneExecute(node, se1_size);
		} else {
			pruner.pruneExecute(node, best_size);			
		}
	}
}




