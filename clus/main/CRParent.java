package clus.main;

import java.io.PrintWriter;
import java.util.*;

import clus.error.ClusErrorParent;

public abstract class CRParent {

	public final static int TRAINSET = 0;
	public final static int TESTSET = 1;
	public final static int VALIDATIONSET = 2;
	
	protected ArrayList m_Models = new ArrayList();
	protected long m_IndTime, m_PrepTime, m_PruneTime;

/***************************************************************************
 * Iterating over models
 ***************************************************************************/		

	public int getNbModels() {
		return m_Models.size();	
	}
		
	public ClusModelInfo getModelInfo(int i) {
		return (ClusModelInfo)m_Models.get(i);
	}
	
	public void setModelInfo(int i, ClusModelInfo info) {
		m_Models.set(i, info);
	}	
	
	public ClusModel getModel(int i) {
		return getModelInfo(i).getModel();
	}
	
	public String getModelName(int i) {
		return getModelInfo(i).getName();
	}	

	public void setModels(ArrayList models) {
		m_Models = models;
	}
	
	public void showModelInfos() {
		for (int i = 0; i < getNbModels(); i++) {
			ClusModelInfo info = (ClusModelInfo)getModelInfo(i);
			System.out.println("Model "+i+" name: '"+info.getName()+"'");
		}
	}
	
	/***************************************************************************
	 * Adding models to it
	 ***************************************************************************/	
		
		public ClusModelInfo addModelInfo() {
			String name = "M" + (m_Models.size() + 1);
			ClusModelInfo inf = new ClusModelInfo(name);
			inf.setAllErrorsClone(getTrainError(), getTestError(), getValidationError());
			inf.setStatManager(getStatManager());
			m_Models.add(inf);
			return inf;
		}
		
		public ClusModelInfo addModelInfo(int i) {
			while (i >= m_Models.size()) addModelInfo();
			return (ClusModelInfo)m_Models.get(i);
		}

		public abstract ClusStatManager getStatManager();

		public abstract ClusErrorParent getTrainError();
		
		public abstract ClusErrorParent getTestError();
		
		public abstract ClusErrorParent getValidationError();


/***************************************************************************
 * Functions for all models
 ***************************************************************************/		
	
	public ArrayList cloneModels() {
		int nb_models = getNbModels();
		ArrayList clones = new ArrayList();		
		for (int i = 0; i < nb_models; i++) {
			ClusModelInfo my = getModelInfo(i);
			clones.add(my.cloneModelInfo());
		}
		return clones;
	}
	
	public void checkModelInfo() {
		int nb_models = getNbModels();
		for (int i = 0; i < nb_models; i++) {
			ClusModelInfo my = getModelInfo(i);
			my.check();
		}		
	}

	public boolean hasModel(int i) {
		ClusModelInfo my = getModelInfo(i);
		return my.getNbModels() > 0;
	}
		
/***************************************************************************
 * Induction time
 ***************************************************************************/		
		
	public final void setInductionTime(long time) {
		m_IndTime = time;
	}
	
	public final long getInductionTime() {
		return m_IndTime;
	}
	
	public final void setPruneTime(long time) {
		m_PruneTime = time;
	}
	
	public final long getPruneTime() {
		return m_PruneTime;
	}	
		
	public final void setPrepareTime(long time) {
		m_PrepTime = time;
	}
	
	public final long getPrepareTime() {
		return m_PrepTime;
	}
}
