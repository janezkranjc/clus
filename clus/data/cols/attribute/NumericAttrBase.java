package clus.data.cols.attribute;

import clus.data.type.*;

public abstract class NumericAttrBase extends ClusAttribute {

	protected NumericAttrType m_Type;		

	public NumericAttrBase(NumericAttrType type) {
		m_Type = type;
	}

	public ClusAttrType getType() {
		return m_Type;
	}
}
