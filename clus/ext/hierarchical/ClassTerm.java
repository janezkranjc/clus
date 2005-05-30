package clus.ext.hierarchical;

import java.io.*;
import java.util.*;

import jeans.math.*;
import jeans.util.*;
import jeans.util.compound.*;

import jeans.tree.*;
import clus.util.*;
import clus.main.*;

public class ClassTerm extends IndexedItem implements Node {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected String m_ID;
	protected ClassTerm m_Parent;
	protected Hashtable m_Hash = new Hashtable();
	protected MyArray m_SubTerms = new MyArray();

	public ClassTerm() {
		m_Parent = null;
		m_ID = "root";
	}
	
	public ClassTerm(ClassTerm parent, String id) {
		m_Parent = parent;
		m_ID = id;
	}
	
	public void addClass(ClassesValue val, int level) {
		String cl_idx = val.getClassID(level);
		if (!cl_idx.equals("0")) {
			ClassTerm found = (ClassTerm)m_Hash.get(cl_idx);
			if (found == null) {
				found = new ClassTerm(this, cl_idx);
				m_Hash.put(cl_idx, found);
				m_SubTerms.addElement(found);
			}
			level++;
			if (level < val.getNbLevels()) found.addClass(val, level);
		}
	}	
	
	public void getMeanBranch(boolean[] enabled, SingleStat stat) {
		int nb_branch = 0;
		for (int i = 0; i < getNbChildren(); i++) {
			ClassTerm child = (ClassTerm)getChild(i);
			if (enabled == null || enabled[child.getIndex()]) {
				nb_branch += 1;
				child.getMeanBranch(enabled, stat);
			}
		}
		if (nb_branch != 0) {
			stat.addFloat(nb_branch);
		}
	}
	
	public void toBoolVector(boolean[] enable) {
		ClassTerm cr = this;
		while (cr != null) {
			enable[cr.getIndex()] = true;
			cr = cr.getCTParent();
		}
	}
	
	public Node getParent() {
		return m_Parent;
	}

	public Node getChild(int idx) {
		return (Node)m_SubTerms.elementAt(idx);
	}

	public int getNbChildren() {
		return m_SubTerms.size();
	}

	public boolean atTopLevel() {
		return m_Parent == null;
	}

	public boolean atBottomLevel() {
		return m_SubTerms.size() == 0;	
	}
	
	public void setParent(Node parent) {
		m_Parent = (ClassTerm)parent;
	}
		
	public final String getID() {
		return m_ID;
	}	
	
	public final void setID(String id) {
		m_ID = id;
	}		
	
	public final ClassTerm getByName(String name) {
		return (ClassTerm)m_Hash.get(name);
	}

	public final ClassTerm getCTParent() {
		return m_Parent;		
	}
		
	// FIXME remove - is now in HierIO	
	public final void print(int tabs, PrintWriter wrt, double[] counts, double[] weights) {
		for (int i = 0; i < m_SubTerms.size(); i++) {
			ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);		
			wrt.print(StringUtils.makeString(' ', tabs)+subterm.getID());
			int no = subterm.getIndex();
			if (counts != null) {
				double count = counts[no];
				wrt.print(": "+ClusFormat.FOUR_AFTER_DOT.format(count));
			}
			if (weights != null) {
				double weight = weights[no];
				wrt.print(": "+ClusFormat.THREE_AFTER_DOT.format(weight));
			}			
			wrt.println();
			subterm.print(tabs+6, wrt, counts, weights);
		}
	}
	
	public int getNbLeaves() {
		int nbc = m_SubTerms.size();
		if (nbc == 0) {
			return 1;
		} else {
			int total = 0;
			for (int i = 0; i < m_SubTerms.size(); i++) {
				ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);		
				total += subterm.getNbLeaves();
			}
			return total;
		}
	}	
		
	public void addChild(Node node) {
		String id = ((ClassTerm)node).getID();
		m_Hash.put(id, node);	
		m_SubTerms.addElement(node);
	}
	
	public void removeChild(int idx) {
		ClassTerm child = (ClassTerm)getChild(idx);
		m_Hash.remove(child.getID());
		m_SubTerms.removeElementAt(idx);
	}
	
	public void numberChildren() {
		m_Hash.clear();
		for (int i = 0; i < m_SubTerms.size(); i++) {
			ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);	
			String key = String.valueOf(i+1);
			subterm.setID(key);
			m_Hash.put(key, subterm);
		}
	}

	public void removeChild(Node node) {
	}
	
	public final double accuWeight(double[] accu, double[] weights) {
		double sum = 0.0;
		for (int i = 0; i < m_SubTerms.size(); i++) {
			ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);		
			sum += subterm.accuWeight(accu, weights);
		}
		int idx = getIndex();
		sum += weights[idx];
		accu[idx] = sum;
		return sum;	
	}
	
	public final double maxWeight(double[] weights) {
		double max = 0.0;
		for (int i = 0; i < m_SubTerms.size(); i++) {
			ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);		
			max = Math.max(max, subterm.maxWeight(weights));
		}
		int idx = getIndex();		
		max = Math.max(max, weights[idx]);
		weights[idx] = max;
		return max;	
	}
	
	public final double maxShowWeight(double[] weights) {
		double max = 0.0;
		for (int i = 0; i < m_SubTerms.size(); i++) {
			ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);		
			max = Math.max(max, subterm.maxShowWeight(weights));
		}
		int idx = getIndex();		
		max = Math.max(max, weights[idx]);
		weights[idx] = max;
		if (max > 0.0) {
			System.out.println("Show: "+this+" "+max);
		}
		return max;	
	}	
	
	public final void makeRelative(double[] accu) {
		double sum = 0.0;
		for (int i = 0; i < m_SubTerms.size(); i++) {
			ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);		
			subterm.makeRelative(accu);
			sum += accu[subterm.getIndex()];
		}
		if (sum != 0.0) {		
			for (int i = 0; i < m_SubTerms.size(); i++) {
				ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);		
				accu[subterm.getIndex()] /= sum;
			}
		}
	}	
	
	public int getLevel() {
		int depth = 0;
		ClassTerm parent = getCTParent();
		while (parent != null) {
			parent = parent.getCTParent();
			depth++;
		}
		return depth;
	}	
	
	public int getMaxDepth() {
		int depth = 0;
		for (int i = 0; i < m_SubTerms.size(); i++) {
			ClassTerm subterm = (ClassTerm)m_SubTerms.elementAt(i);		
			depth = Math.max(depth, subterm.getMaxDepth());
		}
		return depth+1;
	}	
	
	public String toString() {
		String res = "" + getID();
		ClassTerm cr = getCTParent();
		while (cr != null) {
			if (cr.getCTParent() != null) res = cr.getID() + ClassesValue.HIERARCY_SEPARATOR + res;
			cr = cr.getCTParent();
		}
		return res;
	}
}
