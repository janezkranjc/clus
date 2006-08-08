package clus.main;

import clus.error.*;
import clus.util.ClusException;

public class ClusSummary extends CRParent {

	protected int m_Runs;
	protected int m_TotalRuns = 1;
	protected ClusErrorParent m_TrainErr;
	protected ClusErrorParent m_TestErr;
	protected ClusErrorParent m_ValidErr;
	protected ClusStatManager m_StatMgr;

	public void resetAll() {
		m_Models.clear();
		m_IndTime = 0; m_PrepTime = 0; m_PruneTime = 0;
	}
	
	public void setStatManager(ClusStatManager mgr) {
		m_StatMgr = mgr;
	}
	
	public ClusStatManager getStatManager() {
		return m_StatMgr;
	}

	public ClusErrorParent getTrainError() {
		return m_TrainErr;
	}
	
	public ClusErrorParent getTestError() {
		return m_TestErr;
	}
	
	public ClusErrorParent getValidationError() {
		return m_ValidErr;
	}		
	
	public boolean hasTestError() {
		return m_TestErr != null;
	}		
	
	public void setTrainError(ClusErrorParent err) {
		m_TrainErr = err;
	}
	
	public void setTestError(ClusErrorParent err) {
		m_TestErr = err;
	}
	
	public void setValidationError(ClusErrorParent err) {
		m_ValidErr = err;
	}		
				
	public int getNbRuns() {
		return m_Runs;
	}
	
	public int getTotalRuns() {
		return m_TotalRuns;
	}
	
	public void setTotalRuns(int tot) {
		m_TotalRuns = tot;
	}
		
	public ClusSummary getSummaryClone() {
		ClusSummary summ = new ClusSummary();
		summ.setModels(cloneModels());
		return summ;
	}
			
	public void addSummary(ClusRun cr) throws ClusException {	
		m_Runs++;	
		m_IndTime += cr.getInductionTime();
		m_PruneTime += cr.getPruneTime();
		m_PrepTime += cr.getPrepareTime();				
		int nb_models = cr.getNbModels();
		for (int i = 0; i < nb_models; i++) {
			ClusModelInfo my = addModelInfo(i);
			my.add(cr.getModelInfo(i));
		}
	}
}
