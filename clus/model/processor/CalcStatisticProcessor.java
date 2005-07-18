
package clus.model.processor;

import java.io.*;

import jeans.tree.*;

import clus.main.*;
import clus.data.rows.*;
import clus.statistic.*;

public class CalcStatisticProcessor extends ClusModelProcessor {

	ClusStatistic m_Clone;

	public CalcStatisticProcessor(ClusStatistic clone) {
		m_Clone = clone;	
	}
	
        public boolean needsModelUpdate() {
		return true;
        }
	
	public boolean needsInternalNodes() {
    		return true;
	}	
        
	public void initialize(ClusModel model, ClusSchema schema) {
		CompleteTreeIterator iter = new CompleteTreeIterator((ClusNode)model);	
		while (iter.hasMoreNodes()) {
			ClusNode node = (ClusNode)iter.getNextNode();
			ClusStatistic stat = m_Clone.cloneStat();
			node.setClusteringStat(stat);
			stat.setSDataSize(1);
		}		
	}
	
	public void terminate(ClusModel model) throws IOException {
		CompleteTreeIterator iter = new CompleteTreeIterator((ClusNode)model);	
		while (iter.hasMoreNodes()) {
			ClusNode node = (ClusNode)iter.getNextNode();
			node.getClusteringStat().calcMean();
		}
	}

	public void modelUpdate(DataTuple tuple, ClusModel model) {
		ClusNode node = (ClusNode)model;
		node.getClusteringStat().updateWeighted(tuple, 0);
	}
}
