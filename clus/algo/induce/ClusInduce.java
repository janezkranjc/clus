package clus.algo.induce;

import clus.main.*;
import clus.data.rows.*;
import clus.util.*;
import clus.error.multiscore.*;
import clus.model.modelio.*;

import java.io.*;

public abstract class ClusInduce {

	protected ClusSchema m_Schema;
	protected ClusStatManager m_StatManager;	
	protected int m_MaxStats;	
	
	public ClusInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		m_Schema = schema;
		m_StatManager = new ClusStatManager(schema, sett);
	}
	
	public ClusInduce(ClusInduce other) {
		m_Schema = other.m_Schema;
		m_StatManager = other.m_StatManager;		
		m_MaxStats = other.m_MaxStats;
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
	
	public void initialize() throws ClusException {
		m_StatManager.initSH();
		m_MaxStats = m_Schema.getMaxNbStats();		
	}
	
	public void getPreprocs(DataPreprocs pps) {
		m_StatManager.getPreprocs(pps);
	}
	
	public boolean isModelWriter() {
		return false;
	}
	
	public void writeModel(ClusModelCollectionIO strm) throws IOException {
	}
	
	public abstract ClusData createData();

	public abstract ClusNode induce(ClusRun cr, MultiScore score) throws ClusException;

}
