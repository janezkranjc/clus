package clus.algo.induce;

import clus.main.*;
import clus.data.rows.*;
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
