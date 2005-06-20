/*
 * Created on Jun 17, 2005
 *
 */
package clus.data.attweights;

import java.util.*;
import java.io.*;

import clus.data.type.*;
import clus.main.Settings;
import clus.util.*;

public class ClusAttributeWeights implements Serializable {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public double[] m_Weights;
	
	public ClusAttributeWeights(int nbAttr) {
		m_Weights = new double[nbAttr];
	}
	
	public double getWeight(ClusAttrType atttype) {
		return m_Weights[atttype.getIndex()];
	}

	public double getWeight(int i) {
		return m_Weights[i];
	}	
	
	public void setWeight(ClusAttrType atttype, double weight) {
		m_Weights[atttype.getIndex()] = weight;
	}
	
	public void setWeight(int attidx, double weight) {
		m_Weights[attidx] = weight;
	}	
	
	public void setAllWeights(double value) {
		Arrays.fill(m_Weights, value);
	}	
	
	public int getNbAttributes() {
		return m_Weights.length;
	}
	
	public double[] getWeights() {
		return m_Weights;
	}
	
	public void copyFrom(ClusAttributeWeights other) {
		System.arraycopy(other.getWeights(), 0, this.getWeights(), 0, getNbAttributes());
	}
	
	public String getName() {
		if (getNbAttributes() > 10) {
			return "Weights ("+getNbAttributes()+")";
		} else {
			StringBuffer buf = new StringBuffer();
			buf.append("Weights [");
			for (int i = 0; i < getNbAttributes(); i++) {
				if (i != 0) buf.append(",");
				buf.append(ClusFormat.THREE_AFTER_DOT.format(getWeight(i)));
			}
			buf.append("]");
			return buf.toString();
		}		
	}
	
	public String getName(ClusAttrType[] type) {
		if (type.length > 10) {
			return "Weights ("+type.length+")";
		} else {
			StringBuffer buf = new StringBuffer();
			buf.append("Weights [");
			for (int i = 0; i < type.length; i++) {
				if (i != 0) buf.append(",");
				buf.append(ClusFormat.THREE_AFTER_DOT.format(getWeight(type[i])));
			}
			buf.append("]");
			return buf.toString();
		}		
	}	
}
