package clus.ext.ilevelc;

import clus.data.rows.*;

public class ILevelConstraint {

	public final static int ILevelCMustLink = 0;
	public final static int ILevelCCannotLink = 1;	
	
	protected int m_Type;
	protected DataTuple m_T1, m_T2;
	
	public ILevelConstraint(DataTuple t1, DataTuple t2, int type) {
		m_T1 = t1; m_T2 = t2; m_Type = type;
	}
	
	public DataTuple getT1() {
		return m_T1;
	}
	
	public DataTuple getT2() {
		return m_T2;
	}
	
	public int getType() {
		return m_Type;
	}
}
