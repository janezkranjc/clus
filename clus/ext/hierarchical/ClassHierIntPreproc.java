package clus.ext.hierarchical;

import java.util.*;

import clus.data.rows.*;
import clus.util.ClusException;

public class ClassHierIntPreproc extends ClassHierarchyPreproc {
	
	protected transient boolean[] m_Intermediates;
	protected transient ArrayList m_Scratch;
	
	public ClassHierIntPreproc(ClassesAttrType type, boolean single) {
		super(type);
		if (single) createScratch();
	}
	
	public void preproc(int pass, DataTuple tuple) throws ClusException {
		super.preproc(pass, tuple);
		// Pass 0 adds the tuple to the class hierarchy (cf. superclass)
		// Pass 1 adds intermediate elements (nodes) to it
		if (pass == 1) {
			ClassesTuple ct = (ClassesTuple)tuple.getObjVal(m_Type.getArrayIndex());
			addIntermediateElems(ct);       	        
		}
	}
	
	public void preprocSingle(DataTuple tuple) throws ClusException {
		super.preprocSingle(tuple);
		ClassesTuple ct = (ClassesTuple)tuple.getObjVal(m_Type.getArrayIndex());
		addIntermediateElems(ct);
	}		
	
	public void done(int pass) throws ClusException {
		super.done(pass);
		if (pass == 0) createScratch();
	}
	
	private void createScratch() {
		m_Intermediates = new boolean[getHier().getTotal()];
		m_Scratch = new ArrayList();	
	}
	
	private void addIntermediateElems(ClassesTuple ct) throws ClusException {
		Arrays.fill(m_Intermediates, false);
		ct.addIntermediateElems(getHier(), m_Intermediates, m_Scratch);
		m_Scratch.clear();
	}
}
