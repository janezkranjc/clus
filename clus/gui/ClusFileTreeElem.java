/*
 * Created on Apr 20, 2005
 */
package clus.gui;

public class ClusFileTreeElem {
	
	protected int m_Type = -1;
	protected String m_SName, m_LName;
	protected Object m_Obj1;
	
	public ClusFileTreeElem(String sname, String lname) {
		m_SName = sname;
		m_LName = lname;
	}
	
	public String getFullName() {
		return m_LName;
	}
	
	public String toString() {
		return m_SName;
	}
	
	public int getType() {
		return m_Type;
	}
	
	public void setType(int type) {
		m_Type = type;
	}
	
	public Object getObject1() {
		return m_Obj1;
	}
	
	public void setObject1(Object obj) {
		m_Obj1 = obj;
	} 	
}
