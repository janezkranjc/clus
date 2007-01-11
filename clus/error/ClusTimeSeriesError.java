package clus.error;

import clus.data.rows.DataTuple;
import clus.data.type.NumericAttrType;
import clus.data.type.TimeSeriesAttrType;
import clus.statistic.ClusStatistic;

public abstract class ClusTimeSeriesError extends ClusError {

	protected TimeSeriesAttrType[] m_Attrs;
	
	public ClusTimeSeriesError(ClusErrorParent par, TimeSeriesAttrType[] ts) {
		super(par, ts.length);
		m_Attrs = ts;
	}
	

}
