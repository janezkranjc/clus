package clus.main;

import jeans.util.*;

public class CRParent {

	protected MyArray m_Models = new MyArray();
	protected long m_IndTime, m_PrepTime, m_PruneTime;

/***************************************************************************
 * Iterating over models
 ***************************************************************************/		

	public int getNbModels() {
		return m_Models.size();	
	}
	
	public ClusModelInfo getModelInfo(int i) {
		return (ClusModelInfo)m_Models.elementAt(i);
	}
	
	public void setModelInfo(int i, ClusModelInfo info) {
		m_Models.setElementAt(info, i);
	}	
	
	public ClusModel getModel(int i) {
		return getModelInfo(i).getModel();
	}

	public void setModels(MyArray models) {
		m_Models = models;
	}

/***************************************************************************
 * Functions for all models
 ***************************************************************************/		
	
	public MyArray cloneModels() {
		int nb_models = getNbModels();
		MyArray clones = new MyArray(nb_models);		
		for (int i = 0; i < nb_models; i++) {
			ClusModelInfo my = getModelInfo(i);
			clones.setElementAt(my.cloneModelInfo(), i);
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
