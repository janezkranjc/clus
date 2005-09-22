package clus.ext.hierarchical;

import java.util.*;
import java.io.Serializable;

import jeans.util.*;
import jeans.util.array.*;
import jeans.util.compound.*;
import jeans.math.matrix.*;

import clus.main.Settings;
import clus.util.*;

public class ClassesTuple implements MySparseVector, Serializable {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;	
	
	protected IndexedItem[] m_Tuple;
	protected int m_Count;
	
	public ClassesTuple() {
	}
	
	public ClassesTuple(String constr, StringTable table) throws ClusException {
		int idx = 0;
		StringTokenizer tokens = new StringTokenizer(constr, "@");
		int tlen = tokens.countTokens();
		m_Tuple = new IndexedItem[tlen];
		while (tokens.hasMoreTokens()) {
			ClassesValue val = new ClassesValue(tokens.nextToken(), table);
			m_Tuple[idx++] = val;
		}
		if (tlen == 0) new ClusException("Number of classes should be >= 1");
	}
	
	public ClassesTuple(int size) {
		m_Tuple = new IndexedItem[size];
	}
	
	public boolean isRoot() {
		if (m_Tuple.length == 0) return true;
		if (m_Tuple.length == 1) {
			return ((ClassesValue)m_Tuple[0]).isRoot();
		}
		return false;
	}
	
	public boolean hasClass(int index) {
		for (int i = 0; i < m_Tuple.length; i++) {
			ClassesValue val = elementAt(i);
			if (index == val.getIndex()) return true;
		}
		return false;
	}
	
	public void updateDistribution(double[] distr, double weight) {
		for (int i = 0; i < m_Tuple.length; i++) {
			ClassesValue val = elementAt(i);
			distr[val.getIndex()] += weight;
		}		
	}
	
	public void toBoolVector(boolean[] enable) {
		Arrays.fill(enable, false);
		for (int i = 0; i < m_Tuple.length; i++) {
			ClassesValue val = elementAt(i);
			enable[val.getIndex()] = true;
		}
	}
	
	public ClassesTuple toFlat(StringTable table) {
		ClassesTuple tuple = new ClassesTuple(m_Tuple.length);
		for (int i = 0; i < m_Tuple.length; i++) {
			tuple.setItemAt(elementAt(i).toFlat(table), i);
		}
		return tuple;
	}
	
	public void setLength(int size) {
		IndexedItem[] old = m_Tuple;
		m_Tuple = new IndexedItem[size];
		System.arraycopy(old, 0, m_Tuple, 0, size);		
	}
	
	public boolean equalsTuple(ClassesTuple other) {
		if (m_Tuple.length != other.m_Tuple.length) return false;
		for (int i = 0; i < m_Tuple.length; i++) {
			if (!m_Tuple[i].equalsValue(other.m_Tuple[i])) return false;
		}
		return true;
	}
	
	public final void setItemAt(IndexedItem item, int pos) {
		m_Tuple[pos] = item;
	}
	
	public final void addItem(IndexedItem item) {
		m_Tuple[m_Count++] = item;
	}	
	
	public ClassesValue first() {
		return (ClassesValue)m_Tuple[0];
	}
	
	public int getNbNonZero() {
		return m_Tuple.length;
	}
	
	public int getLength() {
		return m_Tuple.length;
	}	
	
	public int getPosition(int idx) {
		return m_Tuple[idx].getIndex();
	}
	
	public double getValue(int idx) {
		return ((ClassesValue)m_Tuple[idx]).getAbundance();	
	}
	
	public final int size() {
		return m_Tuple.length;
	}
	
	public final ClassesValue elementAt(int idx) {
		return (ClassesValue)m_Tuple[idx];
	}
	
	public final double[] getVector(ClassHierarchy hier) {
		double[] vec = new double[hier.getTotal()];
		for (int i = 0; i < size(); i++) {
			int index = elementAt(i).getIndex();
			vec[index] = 1.0;
		}
		return vec;
	}
	
	public final double[] getVectorWithParents(ClassHierarchy hier) {
		double[] vec = new double[hier.getTotal()];
		for (int i = 0; i < size(); i++) {
			ClassesValue val = elementAt(i);
			ClassTerm term = val.getTerm();
			while (term != null && vec[term.getIndex()] == 0.0) {
				vec[term.getIndex()] = 1.0;
				term = term.getCTParent();
			}
		}
		return vec;
	}
	
	public final void addToHierarchy(ClassHierarchy hier) {
		for (int i = 0; i < size(); i++) {
			hier.addClass(elementAt(i));
		}
	}
	
	public final void addHierarchyIndices(ClassHierarchy hier) throws ClusException {
		for (int i = 0; i < size(); i++) {
			ClassesValue val = elementAt(i);
			ClassTerm term = hier.getClassTerm(val);
			val.setClassTerm(term);
			val.setIndex(term.getIndex());
		}
	}
	
	public final void addIntermediateElems(ClassHierarchy hier, double[] intabundances, MyArray scratch) throws ClusException {
		for (int i = 0; i < size(); i++) {
			ClassesValue val = elementAt(i);
			intabundances[val.getIndex()] = val.getAbundance();
		}
		for (int i = 0; i < size(); i++) {
			ClassesValue val = elementAt(i);
			double abundance = val.getAbundance();
			ClassTerm term = hier.getClassTerm(val).getCTParent();
			while (term != null) {
				int idx = term.getIndex();
				if (intabundances[idx] == 0.0) {
					ClassesValue intm = new ClassesValue(term);
					intm.setIntermediate(true);
					scratch.addElement(intm);
				}
				intabundances[idx] = Math.max(intabundances[idx], abundance);
				term = term.getCTParent();		
			}
		}
		for (int i = 0; i < scratch.size(); i++) {
			ClassesValue intm = (ClassesValue)scratch.elementAt(i);
			intm.setAbundance(intabundances[intm.getIndex()]);
		}		
		IndexedItem[] old = m_Tuple;		
		m_Tuple = new IndexedItem[old.length + scratch.size()];
		System.arraycopy(old, 0, m_Tuple, 0, old.length);
		System.arraycopy(scratch.getObjects(), 0, m_Tuple, old.length, scratch.size());
	}
	
	public final void cloneFrom(ClassesTuple tuple) {
		int size = tuple.m_Tuple.length;
		m_Tuple = new IndexedItem[size];
		System.arraycopy(tuple.m_Tuple, 0, m_Tuple, 0, size);
	}
	
	public final void cloneFirstN(int nb, ClassesTuple tuple) {
		System.arraycopy(tuple.m_Tuple, 0, m_Tuple, 0, nb);
	}
	
	public final boolean containsFirstN(int nb, IndexedItem item) {
		for (int i = 0; i < nb; i++)
			if (m_Tuple[i] == item) return true;
		return false;
	}
	
	public String toString() {
		if (m_Tuple.length > 0) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < m_Tuple.length; i++) {
				if (i != 0) buf.append("@");
				buf.append(m_Tuple[i].toString());
			}
			return buf.toString();
		} else {
			return "none";
		}
	}
	
	public String toStringHuman() {
		if (m_Tuple.length > 0) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < m_Tuple.length; i++) {
				if (i != 0) buf.append(",");
				buf.append(m_Tuple[i].toString());
			}
			return buf.toString();
		} else {
			return "none";
		}
	}
	
	public String toStringHumanNoIntermediate() {
		if (m_Tuple.length > 0) {
			boolean hasvalue = false;
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < m_Tuple.length; i++) {				
				if (!((ClassesValue)m_Tuple[i]).isIntermediate()) {
					if (hasvalue) buf.append(",");
					buf.append(m_Tuple[i].toString());
					hasvalue = true;
				}
			}
			return buf.toString();
		} else {
			return "none";
		}
	}
	
	public String toStringHumanNoIntermediate(ClassHierarchy hier) {
		if (m_Tuple.length > 0) {
			double[] vec = getVectorWithParents(hier);
			ClassesTuple tuple = hier.getBestTupleMajNoParents(vec, 0.5);
			return tuple.toStringHuman();
		} else {
			return "none";
		}
	}	
	
	public void setAllIntermediate(boolean inter) {
		for (int i = 0; i < m_Tuple.length; i++) {
			((ClassesValue)m_Tuple[i]).setIntermediate(inter);
		}		
	}
	
	public boolean isValidPrediction() {
		return m_Tuple.length > 0;
	}
	
	public final static void quickSort(ClassesTuple tuple, int low, int high) {
		if (low < high) {
			int mid = partition(tuple,low,high);
			quickSort(tuple,low,mid-1);
			quickSort(tuple,mid+1,high);
		}
	}
	
	private final static int partition(ClassesTuple tuple, int start, int end) {
		
		;
		//set the pivot
		int pivot = tuple.elementAt(start).getIndex();
		ClassesValue pivotValue = tuple.elementAt(start);
		
		do {
			//look for an index smaller than pivot from the end
			while (start < end && tuple.elementAt(end).getIndex() >= pivot) 
				end--;
			if (start < end) {//found a smaller index
				//System.out.println("found");
				tuple.setItemAt(tuple.elementAt(end),start);
				
				//Now find an index larger than pivot from the start
				
				while (start < end && tuple.elementAt(start).getIndex() <= pivot)
					start++;
				
				if (start<end) {//found a larger index
					tuple.setItemAt(tuple.elementAt(start),end);
					
				}
			}
		} while (start < end);
		
		//done, move pivot back 
		tuple.setItemAt(pivotValue,start);
		return start;
	}	
}
