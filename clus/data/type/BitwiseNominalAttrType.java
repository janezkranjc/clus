package clus.data.type;
import clus.data.rows.DataTuple;
import java.lang.Math;

public class BitwiseNominalAttrType extends NominalAttrType {
	
	public final static long serialVersionUID = 1L;

	protected int m_BitPosition;
	public final static double LOG2 = Math.log(2.0);

	public BitwiseNominalAttrType(String name, String type) {
		super(name, type);
	}

	public BitwiseNominalAttrType(String name, String[] values) {
		super(name, values);
	}

	public BitwiseNominalAttrType(String name) {
		super(name);
	}

	public BitwiseNominalAttrType(String name, int nbvalues) {
		super(name, nbvalues);
	}


	public int getValueType() {
		return VALUE_TYPE_BITWISEINT;
	}

	public int getNominal(DataTuple tuple) {
		int value = (tuple.getIntVal(getArrayIndex()) >> getBitPosition()) & ((int)Math.pow(2,getNbBits())-1);
		//System.out.println("BIT getNominal: " + value+ " arrayindex: " + getArrayIndex() + " bitpos: " + getBitPosition());
		return value;
	}

	public void setNominal(DataTuple tuple, int value) {
		//System.out.println("BIT setNominal: " + value+ " arrayindex: " + getArrayIndex() + " bitpos: " + getBitPosition());
		int intvalue = tuple.getIntVal(getArrayIndex()) | (value << getBitPosition());
		tuple.setIntVal(intvalue,getArrayIndex());
	}

	public int getNbBits() {
		return Math.round((float)((Math.log((double)(getNbValues()+1)) / LOG2)+0.5));
	}

	public void setBitPosition(int bp) {
		m_BitPosition = bp;
	}

	public int getBitPosition() {
		return m_BitPosition;
	}

	public ClusAttrType cloneType() {
		BitwiseNominalAttrType at = new BitwiseNominalAttrType(m_Name, m_Values);
		cloneType(at);
		return at;
	}

	public void copyArrayIndex(ClusAttrType type) {
		m_Index = type.m_Index;
		m_ArrayIndex = type.m_ArrayIndex;
		m_Status = type.m_Status;
		m_BitPosition = ((BitwiseNominalAttrType)type).m_BitPosition;
	}
}
