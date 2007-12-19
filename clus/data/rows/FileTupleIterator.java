package clus.data.rows;

import java.io.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.type.*;

public class FileTupleIterator extends TupleIterator {

	protected ClusReader m_Reader;
	protected RowData m_Data;
	protected ClusView m_View;
	
	public FileTupleIterator(DataPreprocs preproc) {
		super(preproc);
	}			
	
	public FileTupleIterator(ClusSchema schema, ClusReader reader) throws ClusException {
		m_Data = new RowData(schema);
		m_Reader = reader;
	}	

	public FileTupleIterator(ClusSchema schema, ClusReader reader, DataPreprocs procs) throws ClusException {
		super(procs);
		m_Data = new RowData(schema);
		m_Reader = reader;
	}
			
	public final ClusSchema getSchema() {
		return m_Data.getSchema();
	}		
	
	public void init() throws IOException, ClusException {
		ClusSchema schema = getSchema();
		m_View = m_Data.createNormalView(schema);
		schema.setReader(true);	
	}		
	
	public final DataTuple readTuple() throws IOException, ClusException {
		DataTuple tuple = m_View.readDataTuple(m_Reader, m_Data);
		preprocTuple(tuple);
		return tuple;
	}

	public final void close() throws IOException {
		ClusSchema schema = m_Data.getSchema();
		schema.setReader(false);
		m_Reader.close();
	}
}
