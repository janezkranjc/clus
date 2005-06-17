package clus.ext.hierarchical;

import java.util.*;
import jeans.util.*;

import clus.data.rows.*;
import clus.util.ClusException;

public class ClassHierIntPreproc extends ClassHierarchyPreproc {
	
	protected transient double[] m_IntermedAbundances;
	protected transient MyArray m_Scratch;
	
	public ClassHierIntPreproc(ClassesAttrType type, boolean single) {
		super(type);
		if (single) createScratch();
	}
	
	public void preproc(int pass, DataTuple tuple) throws ClusException {
		super.preproc(pass, tuple);
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
	
	public void done(int pass) {
		super.done(pass);
		if (pass == 0) createScratch();
	}
	
	private void createScratch() {
		m_IntermedAbundances = new double[getHier().getTotal()];
		m_Scratch = new MyArray();	
	}
	
	private void addIntermediateElems(ClassesTuple ct) throws ClusException {
		Arrays.fill(m_IntermedAbundances, 0.0);
		ct.addIntermediateElems(getHier(), m_IntermedAbundances, m_Scratch);
		m_Scratch.removeAllElements();
	}
}
