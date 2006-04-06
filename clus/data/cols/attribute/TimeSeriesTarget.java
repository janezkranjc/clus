package clus.data.cols.attribute;

import java.io.IOException;

import clus.data.cols.ColTarget;
import clus.data.type.TimeSeriesAttrType;
import clus.io.ClusReader;
import clus.ext.timeseries.*;
 
public class TimeSeriesTarget extends TimeSeriesAttrBase {

	protected ColTarget m_Target;
	protected int m_Index;	
	
	public TimeSeriesTarget(ColTarget target, TimeSeriesAttrType type, int index) {
		super(type);
		m_Target = target;
		m_Index = index;
	}
	
	public void read(ClusReader data, int row) throws IOException {
//		m_Target.setTimeSeries(m_Index, row, new TimeSeries(data.readTimeSeries()));
	}

}
