
package clus.main;

import java.io.*;

import clus.data.type.*;
import clus.data.rows.*;

public class TargetSchema implements Serializable {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected int[] m_NbTypes;
	protected ClusAttrType[][] m_Types;
	
	public TargetSchema(int[] nb_types) {
		m_NbTypes = nb_types;
		m_Types = new ClusAttrType[ClusAttrType.NB_TYPES][];
		for (int i = 0; i < ClusAttrType.NB_TYPES; i++) {
			m_Types[i] = new ClusAttrType[nb_types[i]];
		}
	}
	
	public TargetSchema(int nb_nominal, int nb_numeric) {	
		m_NbTypes = new int [ClusAttrType.NB_TYPES];
		m_Types = new ClusAttrType[ClusAttrType.NB_TYPES][];
		m_NbTypes[NominalAttrType.THIS_TYPE] = nb_nominal;
		m_Types[NominalAttrType.THIS_TYPE] = new ClusAttrType[nb_nominal];
		m_NbTypes[NumericAttrType.THIS_TYPE] = nb_numeric;
		m_Types[NumericAttrType.THIS_TYPE] = new ClusAttrType[nb_numeric];
	}
	
	public boolean isMissingTarget(DataTuple tuple) {
		ClusAttrType[] nomtypes = getNomTypes();
		for (int i = 0; i < nomtypes.length; i++) {
			if (nomtypes[i].isMissing(tuple)) return true;			
		}
		ClusAttrType[] numtypes = getNumTypes();
		for (int i = 0; i < numtypes.length; i++) {
			if (numtypes[i].isMissing(tuple)) return true;			
		}	
		return false;
	}
	
	public int getNbNomAndNum() {
		return getNbNom() + getNbNum();
	}

	public void setType(int id, int idx, ClusAttrType type) {
		m_Types[id][idx] = type;
	}
	
	public int getNbType(int type) {
		return m_NbTypes[type];	
	}
	
	public ClusAttrType getType(int type, int idx) {
		return m_Types[type][idx];
	}
	
	public int getNbNom() {
		return m_NbTypes[NominalAttrType.THIS_TYPE];
	}		
	
	public int getNbNum() {
		return m_NbTypes[NumericAttrType.THIS_TYPE];
	}
	
	public ClusAttrType[] getNomTypes() {
		return m_Types[NominalAttrType.THIS_TYPE];
	}
	
	public ClusAttrType[] getNumTypes() {
		return m_Types[NumericAttrType.THIS_TYPE];
	}
	
	public NominalAttrType getNomType(int i) {
		return (NominalAttrType)m_Types[NominalAttrType.THIS_TYPE][i];
	}
	
	public NumericAttrType getNumType(int i) {
		return (NumericAttrType)m_Types[NumericAttrType.THIS_TYPE][i];
	}
	
	public int getNbNomValues(int i) {
		return getNomType(i).m_NbValues;
	}
	
	public String getIntVal(int i, int j) {
	        return getNomType(i).m_Values[j];
	}
	
	public String getNomName(int i) {
		return getNomType(i).getName();
	}
	
	public String getNumName(int i) {
		return getNumType(i).getName();
	}
}
