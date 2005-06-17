/*
 * Created on May 3, 2005
 */
package clus.model.test;

import java.text.NumberFormat;

import clus.data.rows.*;
import clus.data.type.*;
import clus.main.*;
import clus.util.*;

public class InverseNumericTest extends NumericTest {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public InverseNumericTest(ClusAttrType attr, double bound, double posfreq) {
		super(attr, bound, posfreq);
	}
	
	public String getString() {
		String value = m_Bound != Double.NEGATIVE_INFINITY ? NumberFormat.getInstance().format(m_Bound) : "?"; 
		return m_Type.getName() + " <= " + value;
	}

	public boolean isInverseNumeric() {
		return true;
	}
	
	public int hashCode() {
		long v = Double.doubleToLongBits(m_Bound);
		return m_Type.getIndex() + (int)(v^(v>>>32)) + 1;
	}
	
	public int numericPredict(double value) {
		if (value == Double.POSITIVE_INFINITY) 
			return ClusRandom.nextDouble(ClusRandom.RANDOM_TEST_DIR) < getPosFreq() ? 
			       ClusNode.YES : ClusNode.NO;
		return value <= m_Bound ? ClusNode.YES : ClusNode.NO;
	}
	
	public int numericPredictWeighted(double value) {
		if (value == Double.POSITIVE_INFINITY) {
			return hasUnknownBranch() ? ClusNode.UNK : UNKNOWN;
		} else {
			return value <= m_Bound ? ClusNode.YES : ClusNode.NO;
		}
	}	
	
	public int predictWeighted(DataTuple tuple) {
		double val = tuple.m_Doubles[m_Type.getArrayIndex()];
		return numericPredictWeighted(val);
	}
	
	public NodeTest getBranchTest(int i) {
		if (i == ClusNode.YES) {
			return this;
		} else {
			return new NumericTest(m_Type, getBound(), 1.0-getPosFreq());
		}
	}
}
