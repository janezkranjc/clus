package clus.main;

import java.io.*;

import clus.selection.*;
import clus.error.*;
import clus.data.rows.*;
import clus.util.*;

public class ClusRun extends CRParent {

	protected int m_Index;
	protected boolean m_FileTestSet;
	protected ClusData m_TrainData, m_Prune, m_Orig;
	protected ClusSelection m_TestSel, m_PruneSel;
	protected TupleIterator m_Test, m_Train;
	protected ClusSummary m_Summary;
	
	public ClusRun(ClusData train, ClusSummary summary) {
		m_Index = 1;
		m_TrainData = train;		
		m_Summary = summary;
		m_Models = summary.cloneModels();
	}	
	
	public ClusStatManager getStatManager() {
		return m_Summary.getStatManager();
	}
	
/***************************************************************************
 * Getting to the summary
 ***************************************************************************/	
	
	public ClusSummary getSummary() {
		return m_Summary;
	}
	
	public ClusErrorParent getTrainError() {
		return m_Summary.getTrainError();
	}
	
	public ClusErrorParent getTestError() {
		return m_Summary.getTestError();
	}
	
	public ClusErrorParent getValidationError() {
		return m_Summary.getValidationError();
	}			
	
/***************************************************************************
 * Index of clus run
 ***************************************************************************/	
			
	public final int getIndex() {
		return m_Index;
	}
	
	public final void setIndex(int idx) {
		m_Index = idx;
	}		
	
	public final String getIndexString() {
		String ridx = String.valueOf(getIndex());
		if (getIndex() < 10) ridx = "0"+ridx;
		return ridx;
	}

/***************************************************************************
 * Original set
 ***************************************************************************/	
	
	public final ClusData getOriginalSet() {
		return m_Orig;
	}		
	
	public final void setOrigSet(ClusData data) {
		m_Orig = data;
	}		
	
/***************************************************************************
 * Training set
 ***************************************************************************/
				
	public final ClusData getTrainingSet() {
		return m_TrainData;
	}	
	
	public final void setTrainingSet(ClusData data) {
		m_TrainData = data;
	}	
	
	public final void setTrainSet(TupleIterator iter) {
		m_Train = iter;
	}
	
	public final TupleIterator getTrainIter() {
		return m_Train;
	}				
	
	// To keep training examples in same order :-)
	public final void createTrainIter() {
		RowData clone = (RowData)m_TrainData.cloneData();
		setTrainSet(clone.getIterator());
	}					
		
	public final ClusSelection getTestSelection() {
		return m_TestSel;
	}	
	
/***************************************************************************
 * Test set
 ***************************************************************************/	
	
	public final void setTestSet(TupleIterator iter) {
		m_Test = iter;
	}
		
	public final TupleIterator getTestIter() {
		return m_Test;
	}
	
	// If the test set is specified as a separate file, this method first reads the entire
	// file into memory, while the above method provides an interator that reads tuples one by one
	public final RowData getTestSet() throws IOException, ClusException {
		RowData data = (RowData)m_Test.getData();
		if (data == null) {
			data = (RowData)m_Test.createInMemoryData();
			m_Test = data.getIterator();
		}
		return data;		
	}
		
/***************************************************************************
 * Pruning set
 ***************************************************************************/		
	
	public final ClusData getPruneSet() {
		return m_Prune;
	}
	
	public final TupleIterator getPruneIter() {
		return ((RowData)m_Prune).getIterator();
	}		
	
	public final void setPruneSet(ClusData data, ClusSelection sel) {
		m_Prune = data;
		m_PruneSel = sel;
	}	
	
	public final ClusSelection getPruneSelection() {
		return m_PruneSel;
	}		

/***************************************************************************
 * Preparation
 ***************************************************************************/		
				
	public void changeTestError(ClusErrorParent par) {
		m_Summary.setTestError(par);
		int nb_models = getNbModels();
		for (int i = 0; i < nb_models; i++) {
			ClusModelInfo my = getModelInfo(i);
			my.setTestError(par.getErrorClone());
		}		
	}	
}
