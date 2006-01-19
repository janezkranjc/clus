/*
 * Created on May 29, 2005
 */
package clus.weka;

import java.util.ArrayList;

import weka.core.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.data.attweights.*;
import clus.main.*;

public class ClusToWekaData {

	protected ClusSchema m_Schema;
	protected ArrayList m_NomAttrs = new ArrayList();
	protected ArrayList m_NumAttrs = new ArrayList();
	protected FastVector m_WekaTypes = new FastVector();
	protected ClusAttributeWeights m_Weights;
	protected Instances m_Instances;
	protected int m_Target;
	
	public ClusToWekaData(ClusSchema schema) {
		m_Schema = schema;
		for (int i = 0; i < m_Schema.getNbAttributes(); i++) {
			ClusAttrType type = m_Schema.getAttrType(i);	
			if (type.getStatus() != ClusAttrType.STATUS_DISABLED) {
				if (type instanceof NumericAttrType) {
					m_NumAttrs.add(type);
				} else {
					m_NomAttrs.add(type);
				}
			}
		}
		for (int j = 0; j < m_NumAttrs.size(); j++) {
			NumericAttrType type = (NumericAttrType)m_NumAttrs.get(j);
			if (type.getStatus() == ClusAttrType.STATUS_TARGET) {
				m_Target = m_WekaTypes.size();
			}			
			m_WekaTypes.addElement(new Attribute(type.getName()));
		}		
		for (int j = 0; j < m_NomAttrs.size(); j++) {
			NominalAttrType type = (NominalAttrType)m_NomAttrs.get(j);
			if (type.getStatus() == ClusAttrType.STATUS_TARGET) {
				m_Target = m_WekaTypes.size();
			}			
			FastVector values = new FastVector();
			for (int k = 0; k < type.getNbValues(); k++) {
				values.addElement(type.getValue(k));
			}			
			m_WekaTypes.addElement(new Attribute(type.getName(), values));
		}
		m_Instances = new Instances(m_Schema.getRelationName(), m_WekaTypes, 0);
		m_Instances.setClassIndex(getClassIndex());
	}
	
	public int getIndex(String name) {
		for (int i = 0; i < m_WekaTypes.size(); i++) {
			Attribute attr = (Attribute)m_WekaTypes.elementAt(i);
			if (attr.name().equals(name)) return i;
		}
		return -1;
	}
	
	public void setTargetWeights(ClusAttributeWeights weights) {
		m_Weights = weights;
	}
	
	public int getClassIndex() {
		return m_Target;
	}
	
	public Instances getDummyData() {
		return m_Instances;
	}

	public Instance convertInstance(DataTuple tuple) {
		double[] values = new double[m_WekaTypes.size()];
		for (int j = 0; j < values.length; j++) {
			values[j] = Instance.missingValue();
		}
		int pos = 0;
		for (int j = 0; j < m_NumAttrs.size(); j++) {
			NumericAttrType type = (NumericAttrType)m_NumAttrs.get(j);
			if (type.getStatus() == ClusAttrType.STATUS_TARGET) {
				double weight = m_Weights == null ? 1.0 : m_Weights.getWeight(type);
				values[pos++] = type.getNumeric(tuple)*Math.sqrt(weight);
			} else {
				values[pos++] = type.getNumeric(tuple);
			}
		}
		for (int j = 0; j < m_NomAttrs.size(); j++) {
			NominalAttrType type = (NominalAttrType)m_NomAttrs.get(j);
			values[pos++] = (double)type.getNominal(tuple);
		}		
		return new Instance(tuple.getWeight(), values);
	}
	
	public Instances convertData(RowData data) {
		Instances weka_data = new Instances(m_Schema.getRelationName(), m_WekaTypes, data.getNbRows());
		for (int i = 0; i < data.getNbRows(); i++) {
			weka_data.add(convertInstance(data.getTuple(i)));
		}
		weka_data.setClassIndex(m_WekaTypes.size()-1);
		return weka_data;
	}
}
