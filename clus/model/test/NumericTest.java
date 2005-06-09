package clus.model.test;

import java.text.*;

import clus.main.*;
import clus.data.type.*;
import clus.util.*;
import clus.data.rows.*;

public class NumericTest extends NodeTest {

	protected double m_Bound;
	protected NumericAttrType m_Type;
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public NumericTest(ClusAttrType attr, double bound, double posfreq) {
		m_Type = (NumericAttrType)attr;
		m_Bound = bound;
		setArity(2);
		setPosFreq(posfreq);
	}
	
	public NumericTest(ClusAttrType attr) {
		this(attr, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
	}	

	public boolean isInverseNumeric() {
		return false;
	}
	
	public final int getAttrIndex() {
		return m_Type.getSpecialIndex();
	}

	public final NumericAttrType getNumType() {
		return m_Type;
	}		
	
	public final double getBound() {
		return m_Bound;
	}			
	
	public final void setBound(double bound) {
		m_Bound = bound;
	}				

	public ClusAttrType getType() {
		return m_Type;
	}	
	
	public void setType(ClusAttrType type) {
		m_Type = (NumericAttrType)type;
	}
	
	public String getString() {
		String value = m_Bound != Double.NEGATIVE_INFINITY ? NumberFormat.getInstance().format(m_Bound) : "?"; 
		return m_Type.getName() + " > " + value;
	}
	
	public boolean hasConstants() {
		return m_Bound != Double.NEGATIVE_INFINITY;
	}

	public boolean equals(NodeTest test) {
		if (m_Type != test.getType()) return false;
		NumericTest ntest = (NumericTest)test;
		if (isInverseNumeric() != ntest.isInverseNumeric()) return false;
		return m_Bound == ntest.m_Bound;
	}
	
	public int hashCode() {
		long v = Double.doubleToLongBits(m_Bound);
		return m_Type.getIndex() + (int)(v^(v>>>32));
	}
	
	public int softEquals(NodeTest test) {
		if (m_Type != test.getType()) return N_EQ;
		NumericTest ntest = (NumericTest)test;
		if (m_Bound == ntest.m_Bound) return H_EQ;
		if (Math.abs(getPosFreq() - ntest.getPosFreq()) < 0.1) return S_EQ;
		return N_EQ;
	}	

	public int numericPredict(double value) {
		if (value == Double.POSITIVE_INFINITY) 
			return ClusRandom.nextDouble(ClusRandom.RANDOM_TEST_DIR) < getPosFreq() ? 
			       ClusNode.YES : ClusNode.NO;
		return value > m_Bound ? ClusNode.YES : ClusNode.NO;
	}
	
	public int numericPredictWeighted(double value) {
		if (value == Double.POSITIVE_INFINITY) {
			return hasUnknownBranch() ? ClusNode.UNK : UNKNOWN;
		} else {
			return value > m_Bound ? ClusNode.YES : ClusNode.NO;
		}
	}	
	
	public int predictWeighted(DataTuple tuple) {
		double val = tuple.m_Doubles[m_Type.getSpecialIndex()];
		return numericPredictWeighted(val);
	}
	
	public NodeTest getBranchTest(int i) {
		if (i == ClusNode.YES) {
			return this;
		} else {
			return new InverseNumericTest(m_Type, getBound(), 1.0-getPosFreq());
		}
	}

	public NodeTest simplifyConjunction(NodeTest other) {
		if (getType() != other.getType()) {
			return null;
		} else {
			NumericTest onum = (NumericTest)other;
			if (isInverseNumeric() != onum.isInverseNumeric()) {
				return null;
			}
			double pos_freq = Math.min(getPosFreq(), onum.getPosFreq());
			if (isInverseNumeric()) {
				double new_bound = Math.max(getBound(), onum.getBound());
				return new InverseNumericTest(m_Type, new_bound, pos_freq);				
			} else {
				double new_bound = Math.min(getBound(), onum.getBound());				
				return new NumericTest(m_Type, new_bound, pos_freq);
			}			
		}
	}
	
/*	
	public int predict(ClusAttribute attr, int idx) {
		NumericAttribute nattr = (NumericAttribute)attr;
		return numericPredict(nattr.m_Data[idx]);
	}
*/	
}
