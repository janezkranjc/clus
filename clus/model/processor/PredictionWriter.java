package clus.model.processor;

import jeans.util.*;

import java.io.*;
import java.util.*;

import clus.main.*;
import clus.io.*;
import clus.util.*;
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
	protected boolean m_Initialized;
	protected String m_ToPrint;
	protected ArrayList m_ModelNames = new ArrayList();
	protected HashSet m_ModelNamesMap = new HashSet();
	
	public PredictionWriter(String fname, Settings sett, ClusStatistic target) {
		m_Fname = fname;
		m_Sett = sett;
		m_Target = target;
		m_ModelParts = new StringBuffer();		
	}
	
	public boolean shouldProcessModel(ClusModelInfo info) {
		if (info.getName().equals("Default")) return false;
		else return true;
	}
	
	public void addModelInfo(ClusModelInfo info) {
		if (!m_ModelNamesMap.contains(info.getName())) {
			m_ModelNamesMap.add(info.getName());
			m_ModelNames.add(info.getName());
		}
	}
	
	public void addTargetAttributesForEachModel() {
		for (int i = 0; i < m_ModelNames.size(); i++) {
			String mn = (String)m_ModelNames.get(i);
			m_Target.addPredictWriterSchema(mn, m_OutSchema);
			m_OutSchema.addAttrType(new StringAttrType(mn+"-p-models"));
		}
	}
	
	public void println(String line) {
		if (m_Initialized) m_Writer.println(line);
		else m_ToPrint = line;
	}
		
	public void initializeAll(ClusSchema schema) throws IOException, ClusException {
		if (m_Initialized) return;
		if (!m_Global) doInitialize(schema);
		addTargetAttributesForEachModel();
		System.out.println("PredictionWriter is writing the ARFF header");
		ARFFFile.writeArffHeader(m_Writer, m_OutSchema);
		m_Writer.println("@DATA");
		if (m_ToPrint != null) {
			m_Writer.println(m_ToPrint);
			m_ToPrint = null;
		}
		m_Initialized = true;
	}

	public void terminateAll() throws IOException {
		if (!m_Global) close();
	}
	
	public void globalInitialize(ClusSchema schema) throws IOException, ClusException {
		m_Global = true;
		doInitialize(schema);
	}
	
	public PrintWriter getWrt() {
		return m_Writer;
	}
	
	public void close() throws IOException {
		m_Writer.close();
	}

	public boolean needsModelUpdate() {
		return true;
	}	
	
	public void modelUpdate(DataTuple tuple, ClusModel model) throws IOException {
		if (m_ModelParts.length() != 0) m_ModelParts.append("+");
		m_ModelParts.append(String.valueOf(model.getID()));
	}	
	
	public void exampleUpdate(DataTuple tuple) {
		for (int j = 0; j < m_Attrs.size(); j++) {
			if (j != 0) m_Writer.print(",");
			ClusAttrType at = (ClusAttrType)m_Attrs.elementAt(j);					
			m_Writer.print(at.getString(tuple));					
		}
	}
	
	public void exampleDone() {
		m_Writer.println();
		m_ModelParts.setLength(0);
	}	
	
	public void exampleUpdate(DataTuple tuple, ClusStatistic distr) {
		m_Writer.print(",");
		m_Writer.print(distr.getPredictWriterString(tuple));
		m_Writer.print(",\""+m_ModelParts+"\"");
		m_ModelParts.setLength(0);
	}
	
	private void doInitialize(ClusSchema schema) throws IOException, ClusException {
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
