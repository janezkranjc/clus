package clus.data.type;

import java.io.IOException;

import clus.data.io.ClusReader;
import clus.data.rows.DataTuple;
import clus.data.rows.SparseDataTuple;
import clus.data.type.NumericAttrType.MySerializable;
import clus.io.ClusSerializable;
import clus.util.ClusException;

public class SparseNumericAttrType extends NumericAttrType {

	public SparseNumericAttrType(String name) {
		super(name);
		setSparse(true);
	}

	public SparseNumericAttrType(NumericAttrType type) {
		super(type.getName());
		setIndex(type.getIndex());
		setSparse(true);
	}

	public SparseNumericAttrType cloneType() {
		SparseNumericAttrType at = new SparseNumericAttrType(m_Name);
		cloneType(at);
		return at;
	}

	public int getValueType() {
		return VALUE_TYPE_NONE;
	}

	public double getNumeric(DataTuple tuple) {
		return ((SparseDataTuple)tuple).getDoubleValueSparse(getIndex());
	}

	public boolean isMissing(DataTuple tuple) {
		return ((SparseDataTuple)tuple).getDoubleValueSparse(getIndex()) == MISSING;
	}

	public void setNumeric(DataTuple tuple, double value) {
		((SparseDataTuple)tuple).setDoubleValueSparse(value, getIndex());
	}

	public ClusSerializable createRowSerializable() throws ClusException {
		return new MySerializable();
	}

	public class MySerializable extends ClusSerializable {

		public boolean read(ClusReader data, DataTuple tuple) throws IOException {
			if (!data.readNoSpace()) return false;
			double value = data.getFloat();
			setNumeric(tuple, value);
			return true;
		}
	}
}
