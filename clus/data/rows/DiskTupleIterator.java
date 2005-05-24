package clus.data.rows;

import java.io.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;

public class DiskTupleIterator extends FileTupleIterator {

	protected String m_File;
	protected ClusSchemaInitializer m_Init;
	
	public DiskTupleIterator(String file, ClusSchemaInitializer init) {
		this(file, init, null);
	}	
	
	public DiskTupleIterator(String file, ClusSchemaInitializer init, DataPreprocs procs) {
		super(procs);
		m_File = file;
		m_Init = init;
	}	
		
	public void init() throws IOException, ClusException {
		System.out.println("Loading '"+m_File+"'");			
		m_Reader = new ClusReader(m_File);
		ARFFFile arff = new ARFFFile(m_Reader);
		ClusSchema schema = arff.read();
		if (m_Init != null) m_Init.initSchema(schema);
		schema.addIndices(ClusSchema.ROWS);		
		m_Data = new RowData(schema);		
		super.init();
	}		
}
