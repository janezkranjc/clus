package clus.ext.hierarchical;

import java.util.*;
import jeans.util.array.*;
import jeans.util.compound.*;

import clus.util.*;
import clus.main.*;

public class ClassesValue extends IndexedItem {

	public static String HIERARCY_SEPARATOR = "/";
	public static String ABUNDANCE_SEPARATOR = ":";
	public static int PATH_ORDER = 1;
	
	public final static int NO_ABUNDANCE = 0;
	public final static int PATH_ABUNDANCE = 1;
	public final static int NODE_ABUNDANCE = 2;	

	protected String[] m_Path;
	protected boolean m_Intermediate = true;	
	protected double m_Abundance = 1.0;
	protected ClassTerm m_ClassTerm;

	public ClassesValue(String constr, StringTable table) throws ClusException {
		StringTokenizer tokens = new StringTokenizer(constr, HIERARCY_SEPARATOR + ABUNDANCE_SEPARATOR);
		int plen = tokens.countTokens();		
		int mode = NO_ABUNDANCE;
		if (Settings.HIER_USE_ABUNDANCES.getValue()) {
			if (Settings.HIER_NODE_ABUNDANCES.getValue()) mode = NODE_ABUNDANCE;
			else mode = PATH_ABUNDANCE;
		}			
		switch (mode) {
			case NODE_ABUNDANCE: plen /= 2; break;	
			case PATH_ABUNDANCE: plen--; break;
		}
		m_Path = new String[plen];
		if (plen == 0)
			throw new ClusException("Path length should be >= 1");
		int nb = 0;
		int idx = PATH_ORDER == 1 ? 0 : plen-1;
		while (tokens.hasMoreTokens()) {
			m_Path[idx] = table.get(tokens.nextToken());
			if (mode != NO_ABUNDANCE) {
				if (nb == plen-1) {
					m_Abundance = Double.parseDouble(tokens.nextToken());
				} else if (mode == NODE_ABUNDANCE) {
					tokens.nextToken();
				}
			}
			nb++; 
			idx += PATH_ORDER;
		}
	}
	
	public ClassesValue(ClassTerm term) {
		setIndex(term.getIndex());
		copyPath(term);	
		m_ClassTerm = term;
	}
	
	public ClassesValue(ClassTerm term, double abundance) {
		setIndex(term.getIndex());
		copyPath(term);	
		m_Abundance = abundance;
		m_ClassTerm = term;
	}	

	public ClassesValue(int len) {
		m_Path = new String[len];
	}
	
	public boolean isRoot() {
		return m_Path.length == 0;
	}
	
	public ClassTerm getTerm() {
		return m_ClassTerm;
	}
	
	public void setClassTerm(ClassTerm term) {
		m_ClassTerm = term;
	}
	
	public ClassesValue toFlat(StringTable table) {
		ClassesValue val = new ClassesValue(1);
		val.setPath(table.get(toPathString()), 0);
		return val;
	}
	
	public double getAbundance() {
		return m_Abundance;
	}
	
	public void setAbundance(double abundance) {
		m_Abundance = abundance;
	}	
		
	public void setIntermediate(boolean intm) {
		m_Intermediate = true;
	}

	public static void setHSeparator(String hsep) {
		HIERARCY_SEPARATOR = hsep;
	}

	public boolean equalsValue(ClassesValue other) {
		if (m_Path.length != other.m_Path.length) return false;
		for (int i = 0; i < m_Path.length; i++) {
			if (!m_Path[i].equals(other.m_Path[i])) return false;
		}
		return true;
	}

	public String getClassID(int level) {
		return m_Path[level];
	}

	public int getNbLevels() {
		return m_Path.length;
	}
	
	public void setPath(String strg, int i) {
		m_Path[i] = strg;
	}
	
	public String toPathString() {
		if (m_Path.length == 0) {
			return "R";
		} else {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < m_Path.length; i++) {
				if (i != 0) buf.append(HIERARCY_SEPARATOR);
				buf.append(m_Path[i]);
			}
			return buf.toString();
		}
	}

	public String toString() {
		String path = toPathString();
		if (Settings.HIER_USE_ABUNDANCES.getValue()) {
			String abn = ClusFormat.TWO_AFTER_DOT.format(getAbundance());
			return path + ABUNDANCE_SEPARATOR + abn;
		} else {
			return path;
		}
	}
	
	private void copyPath(ClassTerm term) {
		int depth = term.getLevel();	
		m_Path = new String[depth];
		for (int i = depth-1; i >= 0; i--) {
			m_Path[i] = term.getID();
			term = term.getCTParent();
		}
	}
}
