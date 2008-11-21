package clus.algo.rules;

import java.io.IOException;

import clus.Clus;
import clus.algo.tdidt.ClusDecisionTree;
import clus.data.type.ClusSchema;
import clus.ext.ensembles.ClusBoostingForest;
import clus.ext.ensembles.ClusEnsembleInduce;
import clus.ext.ensembles.ClusForest;
import clus.main.ClusRun;
import clus.main.ClusStatManager;
import clus.main.Settings;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.util.ClusException;

public class ClusRuleFromTreeInduce extends ClusRuleInduce {

	protected Clus m_Clus;
	
	public ClusRuleFromTreeInduce(ClusSchema schema, Settings sett, Clus clus) throws ClusException, IOException {
		super(schema, sett);
		m_Clus = clus;
	}

	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		ClusEnsembleInduce ensemble = new ClusEnsembleInduce(this, m_Clus);
		ensemble.induceAll(cr);
		ClusForest orig = (ClusForest)cr.getModel(ClusModel.ORIGINAL);
		// TODO: Get the rules from each of these models (trees) and put
		// them together. Then call induce rules with these rules as a parameter.
		orig.getModels();
		
		ClusRuleSet rset = new ClusRuleSet(getStatManager());
		
		return rset;
	}
	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		ClusModel model = induceSingleUnpruned(cr);		
		ClusModelInfo default_model = cr.addModelInfo(ClusModel.DEFAULT);
		ClusModel def = ClusDecisionTree.induceDefault(cr);
		default_model.setModel(def);
		default_model.setName("Default");		
		ClusModelInfo model_info = cr.addModelInfo(ClusModel.ORIGINAL);
		model_info.setName("Original");
		model_info.setModel(model);
	}

}
