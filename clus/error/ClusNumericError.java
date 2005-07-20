package clus.error;

import clus.data.type.*;

public abstract class ClusNumericError extends ClusError {

	protected NumericAttrType[] m_Attrs;

	public ClusNumericError(ClusErrorParent par, NumericAttrType[] num) {
		super(par, num.length);
		m_Attrs = num;
	}
	
	public NumericAttrType getAttr(int i) {
		return m_Attrs[i];
	}
	
	public ClusNumericError(ClusErrorParent par, int nbnum) {
		super(par, nbnum);
	}
	
	public abstract void addExample(double[] real, double[] predicted);
}
