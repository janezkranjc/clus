package clus.data.cols.attribute;

import clus.data.type.*;

public abstract class NominalAttrBase extends ClusAttribute {

	protected NominalAttrType m_Type;

	public NominalAttrBase(NominalAttrType type) {
		m_Type = type;
	}

	public NominalAttrType getNominalType() {
		return m_Type;
	}
	
	public ClusAttrType getType() {
		return m_Type;
	}	
}
