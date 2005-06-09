package clus.data.type;

import java.io.*;
import java.util.*;

import clus.io.*;
import clus.util.*;
import clus.data.rows.*;
import clus.data.cols.*;
import clus.data.cols.attribute.*;

import clus.algo.kNN.NominalStatistic;

public class NominalAttrType extends ClusAttrType {

	public final static long serialVersionUID = 1L;	
	
	public final static String[] BINARY_NAMES = {"1", "0"};
	public final static int THIS_TYPE = 0;
	public final static String THIS_TYPE_NAME = "Nominal";

	public int m_NbValues;
	public String[] m_Values;
	protected transient Hashtable m_Hash;

	public NominalAttrType(String name, String type) {
		super(name);
		int len = type.length();
		StringTokenizer tokens = new StringTokenizer(type.substring(1,len-1), ",");
		m_NbValues = tokens.countTokens();
		m_Values = new String[m_NbValues];
		for (int i = 0; i < m_NbValues; i++) {
			m_Values[i] = tokens.nextToken().trim();
		}
		createHash();
	}

    public String[] getValues(){
        return m_Values;
    }

	public NominalAttrType(String name, String[] values) {
		super(name);
		m_NbValues = values.length;
		m_Values = values;
	}

	public NominalAttrType(String name) {
		super(name);
		m_NbValues = 2;
		m_Values = BINARY_NAMES;
	}

	public NominalAttrType(String name, int nbvalues) {
		super(name);
		m_NbValues = nbvalues;
		m_Values = new String[nbvalues];
	}

	public final void setValue(int idx, String name) {
		m_Values[idx] = name;
	}

	public ClusAttrType cloneType() {
		NominalAttrType at = new NominalAttrType(m_Name, m_Values);
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

	public int getNbValues() {
		return m_NbValues;
	}

	public String getValue(int idx) {
		return m_Values[idx];
	}

	public Integer getValueIndex(String value) {
		return (Integer)m_Hash.get(value);
	}

	public int getMaxNbStats() {
		// Add one for missing value index
		return m_NbValues + 1;
	}
/*
	public boolean addToData(ColData data) {
		if (!super.addToData(data)) {
			data.addAttribute(new NominalAttribute(this));
		}
		return true;
	}
*/
	
	public void createHash() {
		m_Hash = new Hashtable();
		for (int i = 0; i < m_NbValues; i++) {
			m_Hash.put(m_Values[i], new Integer(i));
		}
	}

	public String getString(DataTuple tuple) {
		int idx = tuple.m_Ints[m_SpecialIdx];
		return idx >= m_NbValues ? "?" : m_Values[idx];
	}
	
	public boolean isMissing(DataTuple tuple) {
		return tuple.m_Ints[m_SpecialIdx] >= m_NbValues;
	}	

	public int getNominal(DataTuple tuple) {
		return tuple.getIntVal(m_SpecialIdx);
	}

	public int compareValue(DataTuple t1, DataTuple t2) {
		int i1 = t1.m_Ints[m_SpecialIdx];
		int i2 = t2.m_Ints[m_SpecialIdx];
		return i1 == i2 ? 0 : 1;
	}

	public ClusAttribute createTargetAttr(ColTarget target) {
		return new NominalTarget(target, this, getSpecialIndex());
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
			if (value.equals("?")) {
				incNbMissing();
				tuple.setIntVal(getNbValues(), getSpecialIndex());
				return;
			}
			Integer i = (Integer)getValueIndex(value);
			if (i != null) {
				tuple.setIntVal(i.intValue(), getSpecialIndex());
			} else {
				throw new IOException("Illegal value '"+value+"' for attribute "+getName()+" at row "+(data.getRow()+1));
			}
		}
	}

	// new for knn

	public void setStatistic(NominalStatistic stat){
		$stat = stat;
	}
	public NominalStatistic getStatistic(){
		return $stat;
	}

	private NominalStatistic $stat;
}
