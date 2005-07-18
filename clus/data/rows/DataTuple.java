package clus.data.rows;

import java.io.Serializable;

import clus.main.*;

public class DataTuple implements Serializable {

	// Attributes can have several base types
//	public int[] m_Ints;
	
	public int[] m_Ints;
	public double[] m_Doubles;
	public Object[] m_Objects;
		
	// Each example can have a weight
	public double m_Weight;
	
	// Hack for efficient xval, should be replaced later
	public int[] m_Folds;
	public int m_Index;

	protected DataTuple() {
	}

	public DataTuple(ClusSchema schema) {
		// Initialize arrays for three base types
		int nb_int = schema.getNbInts();
		if (nb_int > 0) m_Ints = new int[nb_int];		
		int nb_double = schema.getNbDoubles();
		if (nb_double > 0) m_Doubles = new double[nb_double];		
		int nb_obj = schema.getNbObjects();
		if (nb_obj > 0) m_Objects = new Object[nb_obj];
		// Initialize weight
		m_Weight = 1.0;
	}
	
	public final DataTuple cloneTuple() {
		DataTuple res = new DataTuple();
		res.m_Ints = m_Ints;
		res.m_Doubles = m_Doubles;
		res.m_Objects = m_Objects;
		res.m_Weight = m_Weight;
		res.m_Index = m_Index;
		res.m_Folds = m_Folds;	
		return res;
	}
	
	public final DataTuple changeWeight(double weight) {
		DataTuple res = new DataTuple();
		res.m_Ints = m_Ints;
		res.m_Doubles = m_Doubles;
		res.m_Objects = m_Objects;
		res.m_Index = m_Index;
		res.m_Folds = m_Folds;		
		res.m_Weight = weight;
		return res;
	}
	
	public final DataTuple multiplyWeight(double weight) {
		DataTuple res = new DataTuple();
		res.m_Ints = m_Ints;
		res.m_Doubles = m_Doubles;
		res.m_Objects = m_Objects;
		res.m_Index = m_Index;
		res.m_Folds = m_Folds;		
		res.m_Weight = m_Weight * weight;
		return res;
	}		
	
	public final int getClassification() {
		return m_Ints[0];
	}
	
	public final boolean hasNumMissing(int idx) {
		return m_Doubles[idx] == Double.POSITIVE_INFINITY;
	}
	
	public final double getDoubleVal(int idx) {
		return m_Doubles[idx];
	}	
	
	public final int getIntVal(int idx) {
		return m_Ints[idx];
	}		
	
	public final Object getObjVal(int idx) {
		return m_Objects[idx];
	}			
	
	public final void setIntVal(int value, int idx) {
		m_Ints[idx] = value;
	}
	
	public final void setDoubleVal(double value, int idx) {
		m_Doubles[idx] = value;	
	}
	
	public final void setObjectVal(Object value, int idx) {
		m_Objects[idx] = value;	
	}	
	
	public final void setIndex(int idx) {
		m_Index = idx;
	}
	
	public final int getIndex() {
		return m_Index;
	}
	
	public final double getWeight() {
		return m_Weight;
	}	
		
}
