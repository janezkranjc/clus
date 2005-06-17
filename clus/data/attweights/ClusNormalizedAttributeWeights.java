/*
 * Created on Jun 17, 2005
 *
 */
package clus.data.attweights;

import clus.data.type.*;

public class ClusNormalizedAttributeWeights extends ClusAttributeWeights {

	protected double[] m_NormalizationWeights;
	
	public ClusNormalizedAttributeWeights(ClusAttributeWeights norm) {
		super(norm.getNbAttributes());
		m_NormalizationWeights = new double[norm.getNbAttributes()];
		copyNormalizedFrom(norm);
	}	
	
	public double getWeight(ClusAttrType atttype) {
		int idx = atttype.getIndex();
		return m_Weights[idx] * m_NormalizationWeights[idx];
	}
	
	public double getComposeWeight(ClusAttrType atttype) {
		return m_Weights[atttype.getIndex()];
	}

	public double getNormalizationWeight(ClusAttrType atttype) {
		return m_NormalizationWeights[atttype.getIndex()];
	}
	
	public double[] getNormalizationWeights() {
		return m_NormalizationWeights;
	}
	
	public void copyNormalizedFrom(ClusAttributeWeights other) {
		System.arraycopy(other.getWeights(), 0, this.getNormalizationWeights(), 0, getNbAttributes());
	}	
}
