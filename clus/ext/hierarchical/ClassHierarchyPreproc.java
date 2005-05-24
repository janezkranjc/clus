package clus.ext.hierarchical;

import clus.data.rows.*;
import clus.util.*;

public class ClassHierarchyPreproc implements TuplePreproc {
	
	protected ClassesAttrType m_Type;
	
	public ClassHierarchyPreproc(ClassesAttrType type) {
		m_Type = type;
	}
	
	public final ClassHierarchy getHier() {
		return m_Type.getHier();
	}
	
	public int getNbPasses() {
		return 2;
	}
	
	public void preproc(int pass, DataTuple tuple) throws ClusException {
		ClassesTuple ct = (ClassesTuple)tuple.getObjVal(m_Type.getSpecialIndex());
		if (pass == 0) ct.addToHierarchy(getHier());
		else ct.addHierarchyIndices(getHier());
	}
	
	public void preprocSingle(DataTuple tuple) throws ClusException {
		ClassesTuple ct = (ClassesTuple)tuple.getObjVal(m_Type.getSpecialIndex());
		ct.addHierarchyIndices(getHier());
	}
	
	public void showHier() {
		System.out.println();
		getHier().print(ClusFormat.OUT_WRITER);
		ClusFormat.OUT_WRITER.flush();
	}
	
	public void done(int pass) {
		if (pass > 0) return;
		getHier().initialize();
		showHier();
		//		m_Hier.showEigenValues();
	}
}
