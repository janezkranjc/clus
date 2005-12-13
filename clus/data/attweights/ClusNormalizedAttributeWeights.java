/*
 * Created on Jun 17, 2005
 *
 */
package clus.data.attweights;

import clus.data.type.*;
import clus.main.Settings;
import clus.util.ClusFormat;

public class ClusNormalizedAttributeWeights extends ClusAttributeWeights {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected double[] m_NormalizationWeights;
	
	public ClusNormalizedAttributeWeights(ClusAttributeWeights norm) {
		super(norm.getNbAttributes());
		m_NormalizationWeights = norm.getWeights();
	}	
	
	public double getWeight(ClusAttrType atttype) {
		int idx = atttype.getIndex();
		return m_Weights[idx] * m_NormalizationWeights[idx];
	}
	
	public double getWeight(int idx) {
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
	
	public String getName(ClusAttrType[] type) {
		if (type.length > 50) {
			return "Weights ("+type.length+")";
		} else {
			StringBuffer buf = new StringBuffer();
			buf.append("Weights C=[");
			for (int i = 0; i < type.length; i++) {
				if (i != 0) buf.append(",");
				buf.append(ClusFormat.THREE_AFTER_DOT.format(getComposeWeight(type[i])));
			}
			buf.append("], N=[");
			for (int i = 0; i < type.length; i++) {
				if (i != 0) buf.append(",");
				buf.append(ClusFormat.THREE_AFTER_DOT.format(getNormalizationWeight(type[i])));
			}
			buf.append("]");						
			return buf.toString();
		}		
	}
}
