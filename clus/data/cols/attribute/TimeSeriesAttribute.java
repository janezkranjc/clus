package clus.data.cols.attribute;

import java.io.IOException;

import clus.data.type.TimeSeriesAttrType;
import clus.io.ClusReader;

public class TimeSeriesAttribute extends TimeSeriesAttrBase{

	public double[][] m_Data;
	
	public TimeSeriesAttribute(TimeSeriesAttrType type) {
		super(type);
	}


	public void read(ClusReader data, int row) throws IOException {
		System.err.println("TimeSerriesAttribute:read(ClusReader,int) - not implemented");
	}

	
	
}
