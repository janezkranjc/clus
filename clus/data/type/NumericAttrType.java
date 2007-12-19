package clus.data.type;

import java.io.*;

import clus.io.*;
import clus.util.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.data.cols.*;
import clus.data.cols.attribute.*;

import clus.algo.kNN.NumericStatistic;

public class NumericAttrType extends ClusAttrType {

	public final static long serialVersionUID = 1L;	
	
	public final static int THIS_TYPE = 1;
	public final static String THIS_TYPE_NAME = "Numeric";

	public final static double MISSING = Double.POSITIVE_INFINITY;
	
	protected boolean m_Sparse;
	
	public NumericAttrType(String name) {
		super(name);
	}

	public ClusAttrType cloneType() {
		NumericAttrType at = new NumericAttrType(m_Name);
		cloneType(at);
		at.m_Sparse = m_Sparse;
		return at;
	}
	
	public boolean isSparse() {
		return m_Sparse;
	}
	
	public void setSparse(boolean sparse) {
		m_Sparse = sparse;
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

/*	public boolean addToData(ColData data) {
		if (!super.addToData(data)) {
			data.addAttribute(new NumericAttribute(this));
		}
		return true;
	}
*/
	public int getMaxNbStats() {
		// Positive statistic and missing value statistic
		return 2;
	}

	public String getString(DataTuple tuple) {
		double val = tuple.m_Doubles[m_ArrayIndex];
// FIXME - SOON - STATUS_KEY attribute :-)
		if (getStatus() == STATUS_KEY) {
			return String.valueOf((int)val);
		} else {
			return val == MISSING ? "?" : ClusFormat.SIX_AFTER_DOT.format(val);
		}
	}
	
	public boolean isMissing(DataTuple tuple) {
		return tuple.m_Doubles[m_ArrayIndex] == MISSING;
	}

	public double getNumeric(DataTuple tuple) {
		return tuple.getDoubleVal(m_ArrayIndex);
	}
	
	public void setNumeric(DataTuple tuple, double value) {
		tuple.setDoubleVal(value, m_ArrayIndex);
	}	

	public int compareValue(DataTuple t1, DataTuple t2) {
		double v1 = t1.m_Doubles[m_ArrayIndex];
		double v2 = t2.m_Doubles[m_ArrayIndex];
		if (v1 == v2) return 0;
		return v1 > v2 ? 1 : -1;
	}

	public ClusAttribute createTargetAttr(ColTarget target) {
		return new NumericTarget(target, this, getArrayIndex());
	}

	public ClusSerializable createRowSerializable(RowData data) throws ClusException {
		return new MySerializable(data);
	}
	
	public void writeARFFType(PrintWriter wrt) throws ClusException {
		wrt.print("numeric");
	}

	public class MySerializable extends RowSerializable {

		public int m_NbZero;
		
		public MySerializable(RowData data) {
			super(data);
		}

		public void read(ClusReader data, DataTuple tuple) throws IOException {
			double val = data.readFloat();
			tuple.setDoubleVal(val, getArrayIndex());
			if (val == MISSING) {
				incNbMissing();
				m_NbZero++;
			}
			if (val == 0.0) {
				m_NbZero++;
			}
		}
		
		public void term(ClusSchema schema) {
			if (m_NbZero > schema.getNbRows()*75/100) {
				setSparse(true);
			}
		}
	}

	//new for knn

	public void setStatistic(NumericStatistic stat){
		$stat = stat;
	}
	public NumericStatistic getStatistic(){
		return $stat;
	}

	private NumericStatistic $stat;
}

