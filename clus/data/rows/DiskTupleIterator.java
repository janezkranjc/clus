package clus.data.rows;

import java.io.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;

public class DiskTupleIterator extends FileTupleIterator {

	protected String m_File;
	protected ClusSchemaInitializer m_Init;
	protected Settings m_Sett;
	
	public DiskTupleIterator(String file, ClusSchemaInitializer init, Settings sett) {
		this(file, init, null, sett);
	}	
	
	public DiskTupleIterator(String file, ClusSchemaInitializer init, DataPreprocs procs, Settings sett) {
		super(procs);
		m_File = file;
		m_Init = init;
		m_Sett = sett;
	}	
		
	public void init() throws IOException, ClusException {
		System.out.println("Loading '"+m_File+"'");			
		m_Reader = new ClusReader(m_File, m_Sett);
		ARFFFile arff = new ARFFFile(m_Reader);
		ClusSchema schema = arff.read(m_Sett);
		if (m_Init != null) m_Init.initSchema(schema);
		schema.addIndices(ClusSchema.ROWS);		
		m_Data = new RowData(schema);		
		super.init();
	}		
}
