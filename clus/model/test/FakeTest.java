
package clus.model.test;

import jeans.util.*;

import clus.data.rows.*;
import clus.data.type.*;
import clus.main.Settings;

public class FakeTest extends NodeTest {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected MyArray m_Lines = new MyArray();
	protected String m_Line;

	public FakeTest() {
	}
	
	public void setLine(String line) {
		m_Line = line;
	}
	
	public void addLine(String line) {
		m_Lines.addElement(line);
	}

	public int predictWeighted(DataTuple tuple) {
		return -1;
	}
		
	public boolean equals(NodeTest test) {
		return false;
	}

	public ClusAttrType getType() {
		return null;
	}
	
	public void setType(ClusAttrType type) {
	}

	public String getString() {
		return m_Line;
	}	

	public int getNbLines() {
		return m_Lines.size();
	}
	
	public String getLine(int i) {
		return (String)m_Lines.elementAt(i);
	}
}
