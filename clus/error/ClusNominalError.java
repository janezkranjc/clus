package clus.error;

import clus.data.type.*;
import clus.statistic.*;

public abstract class ClusNominalError extends ClusError {

	protected int[] m_Default;
	protected NominalAttrType[] m_Attrs;

	public ClusNominalError(ClusErrorParent par, NominalAttrType[] nom) {
		super(par, nom.length);
		m_Attrs = nom;
	}
	
	public NominalAttrType getAttr(int i) {
		return m_Attrs[i];
	}
	
	public ClusNominalError(ClusErrorParent par, int nb_nominal) {
		super(par, nb_nominal);
	}	
	
	public void setDefault(int[] value) {
		m_Default = value;
	}
	
	public void setDefault(ClusStatistic pred) {
		m_Default = ((ClassificationStat)pred).m_MajorityClasses;
	}	
}
