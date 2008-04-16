package clus.error;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.type.NominalAttrType;

public class MSNominalError extends ClusNominalError {

	protected ClusAttributeWeights m_Weights;
	
	public MSNominalError(ClusErrorList par, NominalAttrType[] nom, ClusAttributeWeights weights) {
		super(par, nom);
		m_Weights = weights;
	}

	public ClusError getErrorClone(ClusErrorList par) {
		return new MSNominalError(par, m_Attrs, m_Weights);
	}

	public String getName() {
		return "Mean Squared Error for Nominal Attributes";
	}
}
