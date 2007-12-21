/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.main;

import java.io.*;
import java.util.*;

import clus.error.*;
import clus.util.*;
import clus.data.type.*;

public abstract class CRParent implements Serializable {

	public final static int TRAINSET = 0;
	public final static int TESTSET = 1;
	public final static int VALIDATIONSET = 2;
	
	protected ClusModelInfo m_AllModelsMI = new ClusModelInfo("AllModels");
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
	
	public ClusModelInfo getAllModelsMI() {
		return m_AllModelsMI;
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

	public abstract ClusErrorList getTrainError();
		
	public abstract ClusErrorList getTestError();
		
	public abstract ClusErrorList getValidationError();

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
	
	public void deleteModels() {
		int nb_models = getNbModels();
		for (int i = 0; i < nb_models; i++) {
			ClusModelInfo my = getModelInfo(i);
			my.deleteModel();
		}
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
	
	public void copyAllModelsMIs() {
		ClusModelInfo allmi = getAllModelsMI();
		int nb_models = getNbModels();
		for (int i = 0; i < nb_models; i++) {
			ClusModelInfo my = getModelInfo(i);
			allmi.copyModelProcessors(my);
		}
	}
	
	public void initModelProcessors(int type, ClusSchema schema)  throws IOException, ClusException {
		ClusModelInfo allmi = getAllModelsMI();
		allmi.initAllModelProcessors(type, schema);
		for (int i = 0; i < getNbModels(); i++) {
			ClusModelInfo mi = getModelInfo(i);
			mi.initModelProcessors(type, schema);
		}
	}	

	public void termModelProcessors(int type)  throws IOException, ClusException {
		ClusModelInfo allmi = getAllModelsMI();
		allmi.termAllModelProcessors(type);		
		for (int i = 0; i < getNbModels(); i++) {
			ClusModelInfo mi = getModelInfo(i);
			mi.termModelProcessors(type);
		}
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
