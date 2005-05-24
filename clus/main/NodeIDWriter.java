package clus.main;

import jeans.util.*;

import java.io.*;

import clus.data.rows.*;
import clus.data.type.*;

public class NodeIDWriter extends ClusModelProcessor {

	protected boolean m_Missing;
	protected String m_Fname;
	protected PrintWriter m_Writer;
	protected ClusSchema m_Schema;
	protected MyArray m_Attrs;
	protected boolean m_First;
	
	public NodeIDWriter(String fname, boolean missing) {
		m_Fname = fname;
		m_Missing = missing;
	}

	public void initialize(ClusModel model, ClusSchema schema) throws IOException {
		m_Attrs = new MyArray();
		int nb = schema.getNbAttributes();
		for (int i = 0; i < nb; i++) {
			ClusAttrType at = schema.getAttrType(i);
			if (at.getStatus() == ClusAttrType.STATUS_KEY) m_Attrs.addElement(at);
		}
		if (m_Attrs.size() == 0) {
			for (int i = 0; i < nb; i++) {
				ClusAttrType at = schema.getAttrType(i);
				if (at.getStatus() == ClusAttrType.STATUS_TARGET) m_Attrs.addElement(at);
			}
		}
		m_First = true;
		m_Writer = Settings.getFileAbsoluteWriter(m_Fname);
	}
	
	public void terminate(ClusModel model) throws IOException {
		m_Writer.close();
	}

	public void modelUpdate(DataTuple tuple, ClusModel model) {	
		ClusNode node = (ClusNode)model;
		if (m_First) {
			m_Writer.print("pred(");
			for (int j = 0; j < m_Attrs.size(); j++) {
				ClusAttrType at = (ClusAttrType)m_Attrs.elementAt(j);					
				m_Writer.print(at.getString(tuple));					
			}
			m_First = false;
		}
		m_Writer.print(",");
		if (m_Missing) {
			m_Writer.print("("+tuple.getWeight()+","+node.getID()+")");
		} else {
			m_Writer.print(node.getID());
		}
	}
		
	public void modelDone()	{
		m_Writer.println(").");	
		m_First = true;
	}
}


