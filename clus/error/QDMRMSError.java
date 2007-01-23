package clus.error;

import clus.data.rows.DataTuple;
import clus.data.type.TimeSeriesAttrType;
import clus.ext.timeseries.TimeSeries;
import clus.ext.timeseries.TimeSeriesStat;
import clus.statistic.ClusStatistic;

public class QDMRMSError extends ClusTimeSeriesError {

	protected double m_SumSqErr;

	public QDMRMSError(ClusErrorParent par, TimeSeriesAttrType[] ts) {
		super(par, ts);
	}
	
	public void reset() {
		m_SumSqErr = 0.0;
	}

	public void add(ClusError other) {
		QDMRMSError oe = (QDMRMSError)other;
		m_SumSqErr += oe.m_SumSqErr;
	}

	public void addExample(DataTuple tuple, ClusStatistic pred) {
		TimeSeries predicted = pred.getTimeSeriesPred();
		double err = ((TimeSeriesStat)pred).calcDistance(getAttr(0).getTimeSeries(tuple),  predicted);			
		m_SumSqErr += sqr(err);
	}

	public final static double sqr(double value) {
		return value*value;
	}
	
	public double getModelErrorAdditive() {
		// return squared error not divided by the number of examples
		// optimized, e.g., by size constraint pruning
		return m_SumSqErr;
	}
	
	public double getModelError() {
		return getModelErrorComponent(0);
	}
	
	public boolean shouldBeLow() {
		return true;
	}	
	
	public TimeSeriesAttrType getAttr(int i) {
		return m_Attrs[i];
	}

	public void addInvalid(DataTuple tuple) {
	}

	public ClusError getErrorClone(ClusErrorParent par) {
		return new QDMRMSError(par, m_Attrs);
	}

	public String getName() {
		return "QDMRMSError";
	}

	public double getModelErrorComponent(int i) {
		int nb = getNbExamples();
		double err = nb != 0 ? m_SumSqErr/nb : 0.0;
		return Math.sqrt(err);
	}
}
