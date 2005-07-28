/*
 * Created on Jul 22, 2005
 */
package clus.pruning;

import java.io.IOException;

import clus.data.rows.*;
import clus.error.ClusError;
import clus.main.*;
import clus.statistic.ClusStatistic;
import clus.util.ClusException;

public class SizeConstraintErrorComputer  extends ClusModelProcessor {

	public void modelUpdate(DataTuple tuple, ClusModel model) throws IOException {
		ClusNode tree = (ClusNode)model;
		SizeConstraintVisitor visitor = (SizeConstraintVisitor)tree.getVisitor();
		visitor.testerr.addExample(tuple, tree.getClusteringStat());					
	}
	
	public boolean needsModelUpdate() {
		return true;
	}		
	
	public boolean needsInternalNodes() {
		return true;
	}
	
	public static void computeErrorStandard(ClusNode tree, RowData test, ClusError error) throws ClusException {
		for (int i = 0; i < test.getNbRows(); i++) {
			DataTuple tuple = test.getTuple(i);
			ClusStatistic pred = tree.predictWeighted(tuple);			
			error.addExample(tuple, pred);
		}
	}

	public static void initializeTestErrors(ClusNode node, ClusError error) {
		SizeConstraintVisitor visitor = (SizeConstraintVisitor)node.getVisitor();
		visitor.testerr = error.getErrorClone(error.getParent());
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClusNode child = (ClusNode)node.getChild(i);
			initializeTestErrors(child, error);
		}
	}

	public static void computeErrorSimple(ClusNode node, ClusError sum) {		
		if (node.atBottomLevel()) {
			SizeConstraintVisitor visitor = (SizeConstraintVisitor)node.getVisitor();
			sum.add(visitor.testerr);
		} else {
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				computeErrorSimple(child, sum);
			}		
		}
	}
}
