package clus.main;

import jeans.util.*;
import java.io.*;

import clus.io.*;
import clus.data.rows.*;

public class ClusView {

	protected MyArray m_Attr = new MyArray();

	public int getNbAttributes() {
		return m_Attr.size();
	}
	
	public ClusSerializable getAttribute(int idx) {
		return (ClusSerializable)m_Attr.elementAt(idx);
	}

	public void addAttribute(ClusSerializable attr) {
		m_Attr.addElement(attr);
	}
	
	public void readData(ClusReader reader, ClusSchema schema) throws IOException {
		schema.setReader(true);
		int nb = m_Attr.size();
		int rows = schema.getNbRows();		
		for (int i = 0; i < rows; i++) {
			boolean sparse = reader.isNextChar('{'); 
			if (sparse) {
				while (!reader.isNextChar('}')) {
					int idx = reader.readIntIndex();
					if (idx < 1 || idx > m_Attr.size()) {
						throw new IOException("Error attribute index '"+idx+"' out of range [1,"+m_Attr.size()+"] at row "+(reader.getRow()+1));
					}
					ClusSerializable attr = (ClusSerializable)m_Attr.elementAt(idx-1);
					attr.read(reader, i);
				}
			} else {
				for (int j = 0; j < nb; j++) {
					ClusSerializable attr = (ClusSerializable)m_Attr.elementAt(j);
					attr.read(reader, i);
				}
			}
			reader.readEol();
		}		
		for (int j = 0; j < nb; j++) {
			ClusSerializable attr = (ClusSerializable)m_Attr.elementAt(j);
			attr.term(schema);
		}
		schema.setReader(false);
	}	
	
	public DataTuple readDataTuple(ClusReader reader, RowData data) throws IOException {
		if (!reader.hasMoreTokens()) return null;
		DataTuple tuple = data.createTuple();	
		int nb = m_Attr.size();
		if (nb > 0) {
			try {
				ClusSerializable attr_0 = (ClusSerializable)m_Attr.elementAt(0);
				attr_0.read(reader, tuple);
			} catch (IOException e) {
				if (reader.ensureAtEnd()) return null;
				else throw e;
			}
			for (int j = 1; j < nb; j++) {
				ClusSerializable attr = (ClusSerializable)m_Attr.elementAt(j);
				attr.read(reader, tuple);
			}
		}
		reader.readEol();
		return tuple;
	}	
}
