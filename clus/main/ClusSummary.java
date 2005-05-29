package clus.main;

import clus.error.*;

public class ClusSummary extends CRParent {

	protected int m_Runs;
	protected ClusErrorParent m_TrainErr;
	protected ClusErrorParent m_TestErr;
	protected ClusErrorParent m_ValidErr;
	protected ClusStatManager m_StatMgr;

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
		
	public ClusSummary getSummaryClone() {
		ClusSummary summ = new ClusSummary();
		summ.setModels(cloneModels());
		return summ;
	}
			
	public void addSummary(ClusRun cr) {	
		m_IndTime += cr.getInductionTime();
		m_PrepTime += cr.getPrepareTime();				
		m_Runs++;	
		int nb_models = getNbModels();
		for (int i = 0; i < nb_models; i++) {
			ClusModelInfo my = getModelInfo(i);
			my.add(cr.getModelInfo(i));
		}
	}
	
	public int addModel(String name) {
		ClusModelInfo inf = new ClusModelInfo(name, getTrainError(), getTestError(), getValidationError());
		m_Models.addElement(inf);
		return m_Models.size()-1;
	}	
}
