package clus.data.type;

import java.io.IOException;
import java.io.PrintWriter;

import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.rows.RowSerializable;
import clus.io.ClusReader;
import clus.io.ClusSerializable;
import clus.main.Settings;
import clus.util.ClusException;
import clus.ext.timeseries.*;

public class TimeSeriesAttrType extends ClusAttrType{

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public final static String THIS_TYPE_NAME = "TimeSeries";
	public final static int THIS_TYPE = 3;
	
	public TimeSeriesAttrType(String name) {
		super(name);
	}

	public ClusAttrType cloneType() {
		TimeSeriesAttrType tsat = new TimeSeriesAttrType(m_Name); 
		return tsat;
	}

	public int getTypeIndex() {
		return THIS_TYPE;
	}

	public int getValueType() {
		return VALUE_TYPE_OBJECT;
	}

	public String getTypeName() {
		return THIS_TYPE_NAME;
	}
	
	public TimeSeries getTimeSeries(DataTuple tuple) {
		return (TimeSeries)tuple.getObjVal(m_ArrayIndex);
	}
	
	public void setTimeSeries(DataTuple tuple, TimeSeries value) {
		tuple.setObjectVal(value, m_ArrayIndex);
	}	
	
	public String getString(DataTuple tuple) {
		TimeSeries ts_data = (TimeSeries)tuple.getObjVal(0);
		return ts_data.toString();
	}	
	
	public ClusSerializable createRowSerializable(RowData data) throws ClusException {
		return new MySerializable(data);
	}
	
	public class MySerializable extends RowSerializable {


		public MySerializable(RowData data) {
			super(data);
		}

		public String getString(DataTuple tuple){
			TimeSeries ts_data=(TimeSeries)tuple.getObjVal(0);
			double[] data=ts_data.getValues();
			String str="[";
			for (int k=0;k<data.length;k++){
				str.concat(String.valueOf(data[k]));
				if (k < (data.length-1))str.concat(", ");
			}
			str.concat("]");
			return str;
		}
		
		public void read(ClusReader data, DataTuple tuple) throws IOException {
			TimeSeries value=new TimeSeries(data.readTimeSeries());
			tuple.setObjectVal(value, 0);
		}
	}

	public void writeARFFType(PrintWriter wrt) throws ClusException {
		wrt.print("TimeSeries");
	}
	
	
}
