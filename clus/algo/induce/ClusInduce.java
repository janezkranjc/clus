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

package clus.algo.induce;

import clus.main.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.util.*;
import clus.model.modelio.*;

import java.io.*;

/*
 * Subclasses should implement:
 * 
 *	public ClusModel induceSingleUnpruned(ClusRun cr);
 *
 * In addition, subclasses may also want to implement (to return more than one model):
 *
 * 	public void induceAll(ClusRun cr);
 *
 **/

public abstract class ClusInduce {

	protected ClusSchema m_Schema;
	protected ClusStatManager m_StatManager;	
	
	public ClusInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		m_Schema = schema;
		m_StatManager = new ClusStatManager(schema, sett);
	}
	
	public ClusInduce(ClusInduce other) {
		m_Schema = other.m_Schema;
		m_StatManager = other.m_StatManager;		
	}
	
	public ClusSchema getSchema() {
		return m_Schema;
	}
	
	public ClusStatManager getStatManager() {
		return m_StatManager;
	}
	
	public Settings getSettings() {
		return m_StatManager.getSettings();
	}	
	
	public void initialize() throws ClusException, IOException {
		m_StatManager.initSH();
	}
	
	public void getPreprocs(DataPreprocs pps) {
		m_StatManager.getPreprocs(pps);
	}
	
	public boolean isModelWriter() {
		return false;
	}
	
	public void writeModel(ClusModelCollectionIO strm) throws IOException {
	}
	
	public ClusData createData() {
		return new RowData(m_Schema);
	}

	public void induceAll(ClusRun cr) throws ClusException, IOException {
		ClusModel model = induceSingleUnpruned(cr);
		ClusModelInfo model_info = cr.addModelInfo(ClusModels.ORIGINAL);
		model_info.setModel(model);		
	}
	
	public abstract ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException;
	
	public void initializeHeuristic() {
	}
}
