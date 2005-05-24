package clus.model.processor;

import jeans.util.*;

import java.io.*;

import clus.main.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.statistic.*;

public class PredictionWriter extends ClusModelProcessor {

	protected String m_Fname;
	protected PrintWriter m_Writer;
	protected MyArray m_Attrs;
	protected boolean m_Global;
	
	public PredictionWriter(String fname) {
		m_Fname = fname;
	}
	
	public void globalInitialize(ClusSchema schema) throws IOException {
		m_Global = true;
		doInitialize(schema);
	}
	
	public PrintWriter getWrt() {
		return m_Writer;
	}
	
	public void close() throws IOException {
		m_Writer.close();
	}

	public void initialize(ClusModel model, ClusSchema schema) throws IOException {
		if (!m_Global) doInitialize(schema);
	}
	
	public void terminate(ClusModel model) throws IOException {
		if (!m_Global) close();
	}

	public void exampleUpdate(DataTuple tuple, ClusStatistic distr) {
		for (int j = 0; j < m_Attrs.size(); j++) {
			if (j != 0) m_Writer.print(",");
			ClusAttrType at = (ClusAttrType)m_Attrs.elementAt(j);					
			m_Writer.print(at.getString(tuple));					
		}
		m_Writer.print(",");
		m_Writer.println(distr.getString());
	}
	
	private void doInitialize(ClusSchema schema) throws IOException {
		m_Attrs = new MyArray();
		int nb = schema.getNbAttributes();
		for (int i = 0; i < nb; i++) {
			ClusAttrType at = schema.getAttrType(i);
			if (at.getStatus() == ClusAttrType.STATUS_KEY) m_Attrs.addElement(at);
		}
		for (int i = 0; i < nb; i++) {
			ClusAttrType at = schema.getAttrType(i);
			if (at.getStatus() == ClusAttrType.STATUS_TARGET) m_Attrs.addElement(at);
		}
		m_Writer = Settings.getFileAbsoluteWriter(m_Fname);
	}
}
