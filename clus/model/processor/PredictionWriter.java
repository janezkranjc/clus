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
	protected Settings m_Sett;
	protected StringBuffer m_ModelParts;
	protected ClusSchema m_OutSchema;
	protected ClusStatistic m_Target;
	
	public PredictionWriter(String fname, Settings sett, ClusStatistic target) {
		m_Fname = fname;
		m_Sett = sett;
		m_Target = target;
		m_ModelParts = new StringBuffer();		
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

	public boolean needsModelUpdate() {
		return true;
	}	
	
	public void modelUpdate(DataTuple tuple, ClusModel model) throws IOException {
		if (m_ModelParts.length() != 0) m_ModelParts.append("+");
		m_ModelParts.append(String.valueOf(model.getID()));
	}	
	
	public void exampleUpdate(DataTuple tuple, ClusStatistic distr) {
		for (int j = 0; j < m_Attrs.size(); j++) {
			if (j != 0) m_Writer.print(",");
			ClusAttrType at = (ClusAttrType)m_Attrs.elementAt(j);					
			m_Writer.print(at.getString(tuple));					
		}
		m_Writer.print(",");
		m_Writer.print(distr.getPredictString());
		m_Writer.print(",'"+m_ModelParts+"'");
		m_Writer.println();
		m_ModelParts.setLength(0);
	}
	
	private void doInitialize(ClusSchema schema) throws IOException {
		m_Attrs = new MyArray();
		int nb = schema.getNbAttributes();
		m_OutSchema = new ClusSchema(schema.getRelationName()+"-predictions");
		for (int i = 0; i < nb; i++) {
			ClusAttrType at = schema.getAttrType(i);
			if (at.getStatus() == ClusAttrType.STATUS_KEY) {
				m_Attrs.addElement(at);
				m_OutSchema.addAttrType(at.cloneType());
			}
		}
		for (int i = 0; i < nb; i++) {
			ClusAttrType at = schema.getAttrType(i);
			if (at.getStatus() == ClusAttrType.STATUS_TARGET) {
				m_Attrs.addElement(at);
				m_OutSchema.addAttrType(at.cloneType());
			}
		}
		m_Writer = m_Sett.getFileAbsoluteWriter(m_Fname);		
	}
}
