package clus.data.rows;

import java.io.*;
import java.util.*;

import clus.main.*;
import clus.util.*;
import clus.data.type.*;

public abstract class TupleIterator {

	protected DataPreprocs m_Procs;
	protected boolean m_ShouldAttach;

	public TupleIterator() {
		m_Procs = new DataPreprocs();
	}

	public TupleIterator(DataPreprocs procs) {
		m_Procs = procs != null ? procs : new DataPreprocs();
	}
	
	public abstract DataTuple readTuple() throws IOException, ClusException;
	
	public abstract ClusSchema getSchema();
		
	public void init() throws IOException, ClusException {
	}	

	public void close() throws IOException {
	}

	public final void preprocTuple(DataTuple tuple) throws ClusException {
		if (tuple != null) m_Procs.preprocSingle(tuple);
	}
	
	public final void setPreprocs(DataPreprocs procs) {
		m_Procs = procs;
	}
	
	public final boolean shouldAttach() {
		return m_ShouldAttach;
	}
	
	public final void setShouldAttach(boolean attach) {
		m_ShouldAttach = attach;
	}
	
	public ClusData getData() {
		return null;
	}

	public ClusData createInMemoryData() throws IOException, ClusException {
		init();
		ArrayList list = new ArrayList();
		DataTuple tuple = readTuple();		
		while (tuple != null) {
			list.add(tuple);
			tuple = readTuple();
		}
		return new RowData(list, getSchema());
	}
}
