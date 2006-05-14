package clus.ext.hierarchical;

import java.util.ArrayList;
import java.util.Arrays;

import clus.data.rows.*;
import clus.util.*;

public class ClassHierarchyPreproc implements TuplePreproc {
	
	protected ClassesAttrType m_Type;
	protected boolean m_IsSinglePass;
	protected boolean m_AddIntermediates;
	protected transient boolean[] m_Intermediates;
	protected transient ArrayList m_Scratch;
	
	public ClassHierarchyPreproc(ClassesAttrType type, boolean addinter) {
		m_Type = type;
		// if hierarchy is already defined and locked, then we can add the indices
		// in a single pass, otherwise we must build the hierarchy in a first pass
		m_IsSinglePass = type.getHier().isLocked();
		m_AddIntermediates = addinter;
		if (addinter && m_IsSinglePass) createScratch();
	}
	
	public boolean isAddIntermediateClasses() {
		return m_AddIntermediates;
	}
	
	public boolean isSinglePass() {
		return m_IsSinglePass;
	}

	public final ClassHierarchy getHier() {
		return m_Type.getHier();
	}
	
	public int getNbPasses() {
		return isSinglePass() ? 1 : 2;
	}
	
	public void preproc(int pass, DataTuple tuple) throws ClusException {
		ClassesTuple ct = (ClassesTuple)tuple.getObjVal(m_Type.getArrayIndex());
		if (!isSinglePass() && pass == 0) {
			ct.addToHierarchy(getHier());
		}	else {
			ct.addHierarchyIndices(getHier());
			if (isAddIntermediateClasses()) addIntermediateElems(ct);			
		}
	}
	
	public void preprocSingle(DataTuple tuple) throws ClusException {
		ClassesTuple ct = (ClassesTuple)tuple.getObjVal(m_Type.getArrayIndex());
		ct.addHierarchyIndices(getHier());
		if (isAddIntermediateClasses()) addIntermediateElems(ct);
	}
	
	public void done(int pass) throws ClusException {
		if (pass > 0) return;
		// in single-pass mode, the hierarhcy is already initialized and locked
		if (!isSinglePass()) {
			getHier().initialize();
			if (isAddIntermediateClasses()) createScratch();
		}
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
