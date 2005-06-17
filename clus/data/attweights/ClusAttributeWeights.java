/*
 * Created on Jun 17, 2005
 *
 */
package clus.data.attweights;

import java.util.*;
import java.io.*;

import clus.data.type.*;
import clus.main.Settings;

public class ClusAttributeWeights implements Serializable {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public double[] m_Weights;
	
	public ClusAttributeWeights(int nbAttr) {
		m_Weights = new double[nbAttr];
	}
	
	public double getWeight(ClusAttrType atttype) {
		return m_Weights[atttype.getIndex()];
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
		return "Weights";
	}
}
