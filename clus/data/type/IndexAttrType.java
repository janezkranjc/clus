package clus.data.type;

import java.io.*;

import clus.io.*;
import clus.util.*;
import clus.data.rows.*;

public class IndexAttrType extends ClusAttrType {

	public final static long serialVersionUID = 1L;	
	
	public final static int THIS_TYPE = 2;
	public final static String THIS_TYPE_NAME = "Index";

	protected int m_CrValue;

	protected int[] m_Index;
	protected int m_Max = Integer.MIN_VALUE;
	protected int m_Min = Integer.MAX_VALUE;

	public IndexAttrType(String name) {
		super(name);
	}
	
	public IndexAttrType(String name, int min, int max) {
		super(name);
		m_Max = max;
		m_Min = min;
	}
	
	public ClusAttrType cloneType() {
		IndexAttrType at = new IndexAttrType(m_Name, m_Min, m_Max);
		cloneType(at);
		return at;		
	}
	
	public int getCrValue() {
		return m_CrValue;
	}
	
	public int getTypeIndex() {
		return THIS_TYPE;
	}	
	
	public String getTypeName() {
		return THIS_TYPE_NAME;
	}		
	
	public int getValueType() {
		return VALUE_TYPE_DOUBLE;
	}	
	
	public void setNbRows(int nb) {
		m_Index = new int[nb];
	}

	public void setValue(int row, int value) {
		m_Index[row] = value;
		if (value > m_Max) m_Max = value;
		if (value < m_Min) m_Min = value;		
	}
	
	public int getMaxValue() {
		return m_Max;
	}
	
	public int getValue(int row) {
		return m_Index[row];
	}
	
	public int getNbRows() {
		return m_Index.length;
	}
	
/*	
	public boolean addToData(ColData data) {
		return true;
	}		
*/	
	public ClusSerializable createRowSerializable(RowData data, boolean istarget) throws ClusException {
		return new MyReader();
	}
	
	public class MyReader extends ClusSerializable {
	
		public int readValue(ClusReader data) throws IOException {
			String value = data.readString();
			try {
				int ival = Integer.parseInt(value);
				if (ival > m_Max) m_Max = ival;
				if (ival < m_Min) m_Min = ival;
				return ival;
			} catch (NumberFormatException e) {
				throw new IOException("Illegal value '"+value+"' for attribute "+getName()+" at row "+(data.getRow()+1));
			}				
		}
				
		public void read(ClusReader data, int row) throws IOException {
			m_Index[row] = readValue(data);
		}
		
		public void read(ClusReader data, DataTuple tuple) throws IOException {
			m_CrValue = readValue(data);
		}
	}	
}
