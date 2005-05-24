package clus.data.rows;

import clus.main.*;
import clus.util.ClusException;

public class MemoryTupleIterator extends TupleIterator {

	protected RowData m_Data;
	protected int m_Index;
	
	public MemoryTupleIterator(RowData data) {
		m_Data = data;
	}	
	
	public MemoryTupleIterator(RowData data, DataPreprocs procs) {
		super(procs);
		m_Data = data;
	}
	
	public void reset() {
		m_Index = 0;
	}
	
	public int getNbExamples() {
		return m_Data.getNbRows();
	}
		
	public final ClusSchema getSchema() {
		return m_Data.getSchema();
	}
		
	public final DataTuple readTuple() throws ClusException {
		if (m_Index >= m_Data.getNbRows()) return null;
		DataTuple tuple = m_Data.getTuple(m_Index++);
		preprocTuple(tuple);
		return tuple;
	}	
}
