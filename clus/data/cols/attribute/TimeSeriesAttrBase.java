package clus.data.cols.attribute;

import clus.data.type.ClusAttrType;
import clus.data.type.TimeSeriesAttrType;

public abstract class TimeSeriesAttrBase extends ClusAttribute{

	protected TimeSeriesAttrType m_Type;

	public TimeSeriesAttrBase(TimeSeriesAttrType type) {
		m_Type = type;
	}
	
	public ClusAttrType getType() {
		return m_Type;
	}

}
