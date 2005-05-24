package clus.ext.hierarchical;

public class ClassesTupleElement {

	public static String HIERARCY_SEPARATOR = ".";
	public static String LOAD_HSEP = HIERARCY_SEPARATOR;

	protected String[] m_Path;
	protected int m_Index;
/*
	public ClassesValue(String constr, StringTable table) throws ClusException {
		int idx = 0;
		StringTokenizer tokens = new StringTokenizer(constr, LOAD_HSEP);
		int plen = tokens.countTokens();
		m_Path = new String[plen];
		while (tokens.hasMoreTokens()) {
			m_Path[idx++] = table.get(tokens.nextToken());
		}
		if (plen == 0) throw new ClusException("Path length should be >= 1");
	}*/
	
	public static void setLoadFlat(boolean yes) {
		if (yes) LOAD_HSEP = "$";
		else LOAD_HSEP = HIERARCY_SEPARATOR;
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
	
	public final void setIndex(int idx) {
		m_Index = idx;
	}
	
	public final int getIndex() {
		return m_Index;
	}	
	
	public String getClassID(int level) {
		return m_Path[level];
	} 
	
	public int getNbLevels() {
		return m_Path.length;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < m_Path.length; i++) {
			if (i != 0) buf.append("/");
			buf.append(m_Path[i]);
		}
		return buf.toString();
	}
}
