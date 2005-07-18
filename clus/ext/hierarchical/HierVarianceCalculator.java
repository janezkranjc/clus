package clus.ext.hierarchical;

import jeans.tree.*;

import clus.data.rows.*;
import clus.main.*;

import java.io.*;

public class HierVarianceCalculator extends ClusModelProcessor{ 
	
	
	public HierVarianceCalculator() {
		
	}
	
	public boolean needsModelUpdate() {
		return true;
	}
	
	public boolean needsInternalNodes() {
		return true;
	}
	
	public void terminate(ClusModel model) throws IOException {
		ClusNode root = (ClusNode)model;
		CompleteTreeIterator iter = new CompleteTreeIterator(root);	
		while (iter.hasMoreNodes()) {
			ClusNode node = (ClusNode)iter.getNextNode();
			((HierStatistic)node.getClusteringStat()).divideVariance();
		}   
	}
	
	public void initialize(ClusModel model, ClusSchema schema) {
		
		ClusNode root = (ClusNode)model;
		CompleteTreeIterator iter = new CompleteTreeIterator(root);	
		while (iter.hasMoreNodes()) {
			ClusNode node = (ClusNode)iter.getNextNode();
			((HierStatistic)node.getClusteringStat()).initVariance();
		}		
	}	
	
	public void modelUpdate(DataTuple tuple, ClusModel model) {
		ClusNode node = (ClusNode)model;
		((HierStatistic)node.getClusteringStat()).addToVariance(tuple);
		
	}
	
}

