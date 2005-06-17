package clus.data.type;

import java.io.*;

import clus.io.*;
import clus.util.*;
import clus.data.rows.*;

public class IntegerAttrType extends ClusAttrType {

	public final static int THIS_TYPE = 4;
	public final static String THIS_TYPE_NAME = "Integer";	

	public IntegerAttrType(String name) {
		super(name);
	}
		
	public ClusAttrType cloneType() {
		IntegerAttrType at = new IntegerAttrType(m_Name);	
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
		return VALUE_TYPE_INT;
	}	
			
	public String getString(DataTuple tuple) {
		return String.valueOf(tuple.m_Ints[m_ArrayIndex]);
	}
	
	public int compareValue(DataTuple t1, DataTuple t2) {
		int s1 = t1.m_Ints[m_ArrayIndex];
		int s2 = t2.m_Ints[m_ArrayIndex];		
		return s1 == s2 ? 0 : 1;
	}
		
	public ClusSerializable createRowSerializable(RowData data) throws ClusException {
		return new MySerializable(data);
	}	
	
	// FIXME make serializable on level of superclass
	// With:
	//	* initialise() 
	//	* setData() 	
	//
	// -> makes it possible to make derived attributes.
	
	public class MySerializable extends RowSerializable {

		protected int m_Index;

		public MySerializable(RowData data) {
			super(data);
		}

		public void read(ClusReader data, DataTuple tuple) throws IOException {
			tuple.setIntVal(m_Index++, getArrayIndex());
		}
	}
}

