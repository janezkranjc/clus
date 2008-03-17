package sit.mtLearner;

import java.util.*;

import sit.TargetSet;
import clus.Clus;
import clus.algo.ClusInductionAlgorithmType;
import clus.algo.tdidt.ClusDecisionTree;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;
import clus.data.type.ClusSchema;
import clus.main.ClusRun;
import clus.main.Settings;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.model.ClusModelPredictor;

public class ClusLearner extends MTLearnerImpl {

	protected Clus m_Clus;
	
	public void init(RowData data, Settings sett) {
		super.init(data, sett);
		m_Clus = new Clus();
		ClusInductionAlgorithmType clss = new ClusDecisionTree(m_Clus);
		try {
			m_Clus.initialize(data, sett, clss);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
		
	protected RowData[] LearnModel(TargetSet targets, RowData train, RowData test) {
		try {
			ClusSchema schema = m_Clus.getSchema();			
			schema.clearAttributeStatus();
			Iterator targetIterator = targets.iterator();
			while (targetIterator.hasNext()) {
				ClusAttrType attr = (ClusAttrType)targetIterator.next();
				ClusAttrType clusAttr = schema.getAttrType(attr.getIndex());
				clusAttr.setStatus(ClusAttrType.STATUS_TARGET);
				clusAttr.setClustering(true);
			}
			schema.initDescriptiveAttributes();
			schema.addIndices(ClusSchema.ROWS);
			ClusRun cr = m_Clus.train(train);
			ClusModel pruned = cr.getModel(ClusModel.PRUNED);
			RowData predictions = ClusModelPredictor.predict(pruned, test);
			RowData[] final_result = {test, predictions};
			return final_result;
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}
}






