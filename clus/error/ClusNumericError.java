package clus.error;

import clus.main.*;
import clus.data.rows.*;
import clus.statistic.*;

public abstract class ClusNumericError extends ClusError {

	protected double[] m_Default;

	public ClusNumericError(ClusErrorParent par) {
		super(par, par.getNbNumeric());
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
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		addExample(tuple.m_Doubles, pred.getNumericPred());
	}
	
	public void addExample(ClusData data, int idx, ClusStatistic pred) {
		addExample(data.getNumeric(idx), pred.getNumericPred());
	}	

	public abstract void addExample(double[] real, double[] predicted);
}
