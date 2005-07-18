package clus.error;

import clus.data.type.*;
import clus.statistic.*;

public abstract class ClusNumericError extends ClusError {

	protected double[] m_Default;
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
	
	public void setDefault(double[] value) {
		m_Default = value;
	}
	
	public void setDefault(ClusStatistic pred) {
		m_Default = ((RegressionStat)pred).m_SumValues;
	}	
	
	public abstract void addExample(double[] real, double[] predicted);
}
