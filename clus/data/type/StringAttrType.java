package clus.data.type;

import java.io.*;

import clus.io.*;
import clus.main.Settings;
import clus.util.*;
import clus.data.rows.*;

public class StringAttrType extends ClusAttrType {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public final static int THIS_TYPE = 3;
	public final static String THIS_TYPE_NAME = "String";	

	public StringAttrType(String name) {
		super(name);
	}
		
	public ClusAttrType cloneType() {
		StringAttrType at = new StringAttrType(m_Name);	
		cloneType(at);
		return at;		
	}
	
	public int getTypeIndex() {
		return THIS_TYPE;
	}	
	
	public String getTypeName() {
		return THIS_TYPE_NAME;
	}		
	
	public int getValueType() {
		return VALUE_TYPE_OBJECT;
	}	
			
	public String getString(DataTuple tuple) {
		return (String)tuple.m_Objects[m_ArrayIndex];
	}
	
	public int compareValue(DataTuple t1, DataTuple t2) {
		String s1 = (String)t1.m_Objects[m_ArrayIndex];
		String s2 = (String)t2.m_Objects[m_ArrayIndex];		
		return s1.equals(s2) ? 0 : 1;
	}
		
	public ClusSerializable createRowSerializable(RowData data) throws ClusException {
		return new MySerializable(data);
	}	
	
	public class MySerializable extends RowSerializable {

		public MySerializable(RowData data) {
			super(data);
		}

		public void read(ClusReader data, DataTuple tuple) throws IOException {
			String value = data.readString();
			tuple.setObjectVal(value, getArrayIndex());
		}
	}
}
