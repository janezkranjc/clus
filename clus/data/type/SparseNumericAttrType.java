package clus.data.type;

import java.io.IOException;

import clus.data.io.ClusReader;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.rows.SparseDataTuple;
import clus.io.ClusSerializable;
import clus.main.Settings;
import clus.util.ClusException;
import java.util.ArrayList;

public class SparseNumericAttrType extends NumericAttrType {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected Integer m_IntIndex;
	protected ArrayList<Integer> m_ExampleIndices;

	public SparseNumericAttrType(String name) {
		super(name);
		setSparse(true);
		m_ExampleIndices = new ArrayList<Integer>();
	}

	public SparseNumericAttrType(NumericAttrType type) {
		super(type.getName());
		setIndex(type.getIndex());
		setSparse(true);
		m_ExampleIndices = new ArrayList<Integer>();
	}

	public SparseNumericAttrType cloneType() {
		SparseNumericAttrType at = new SparseNumericAttrType(m_Name);
		cloneType(at);
		at.setIndex(getIndex());
		return at;
	}

	public void setIndex(int idx) {
		m_Index = idx;
		m_IntIndex = new Integer(idx);
	}

	public int getValueType() {
		return VALUE_TYPE_NONE;
	}
	
	public ArrayList getExampleIndices() {
		return m_ExampleIndices;
	}
	
	public void addExampleIndex(int index) {
		m_ExampleIndices.add(new Integer(index));
	}
	
	public ArrayList pruneIndexList(RowData data) {
		ArrayList<Integer> newIndices = new ArrayList<Integer>();
		for (int i=0; i<data.getNbRows(); i++) {
			if (m_ExampleIndices.contains(new Integer(data.getTuple(i).getIndex()))) {
				newIndices.add(new Integer(data.getTuple(i).getIndex()));
			}
		}
		return newIndices;
	}
	

	public double getNumeric(DataTuple tuple) {
		return ((SparseDataTuple)tuple).getDoubleValueSparse(getIndex());
	}

	public boolean isMissing(DataTuple tuple) {
		return ((SparseDataTuple)tuple).getDoubleValueSparse(m_IntIndex) == MISSING;
	}

	public void setNumeric(DataTuple tuple, double value) {
		((SparseDataTuple)tuple).setDoubleValueSparse(value, m_IntIndex);
	}

	public ClusSerializable createRowSerializable() throws ClusException {
		return new MySerializable();
	}

	public class MySerializable extends ClusSerializable {

		public boolean read(ClusReader data, DataTuple tuple) throws IOException {
			if (!data.readNoSpace()) return false;
			double value = data.getFloat();
			setNumeric(tuple, value);
//			System.out.println(" adding " + tuple.getIndex());
//			addExampleIndex(new Integer(tuple.getIndex()));			
			return true;
		}
	}
}
