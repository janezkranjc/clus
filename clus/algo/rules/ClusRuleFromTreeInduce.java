package clus.algo.rules;

import java.io.IOException;
import java.util.ArrayList;

import clus.Clus;
import clus.algo.tdidt.ClusDecisionTree;
import clus.algo.tdidt.ClusNode;
import clus.algo.rules.ClusRulesFromTree;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;
import clus.data.type.ClusSchema;
//import clus.ext.ensembles.ClusBoostingForest;
import clus.ext.ensembles.ClusEnsembleInduce;
import clus.ext.ensembles.ClusForest;
import clus.main.ClusRun;
//import clus.main.ClusStatManager;
import clus.main.Settings;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.statistic.ClusStatistic;
import clus.util.ClusException;
//import clus.algo.tdidt.ClusNode;

/**
 * Create rules by decision tree ensemble algorithms (forests).
 * Use this by 'CoveringMethod = RulesFromTree' .
 * 
 * This has to be own induce class because we need Clus instance for creating tree ensemble. 
 * @author Timo Aho
 *
 * 
 */
public class ClusRuleFromTreeInduce extends ClusRuleInduce {

	protected Clus m_Clus;
		
	public ClusRuleFromTreeInduce(ClusSchema schema, Settings sett, Clus clus)
			throws ClusException, IOException {
		super(schema, sett);
		m_Clus = clus;
	}


	/**
	 * Induces rules from ensemble tree, similar to ClusRuleInduce.induce
	 */
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		
		// Train the decision tree ensemble with hopefully all the available settings.
		ClusEnsembleInduce ensemble = new ClusEnsembleInduce(this, m_Clus);
		
		ensemble.induceAll(cr);
		
		/** The real trained ensemble model without pruning. Use unpruned tree because weight optimizing
		 * should get rid of bad rules anyway. */  
		ClusForest forestModel = (ClusForest)cr.getModel(ClusModel.ORIGINAL);

		/** 
		 * The class for transforming single trees to rules
		 */
		ClusRulesFromTree treeTransform = new ClusRulesFromTree(true); // Parameter always true
		ClusRuleSet ruleSet = new ClusRuleSet(getStatManager()); // Manager from super class
		
		//ClusRuleSet ruleSet = new ClusRuleSet(m_Clus.getStatManager());
				
		// Get the trees and transform to rules
		int numberOfUniqueRules = 0;

		for (int iTree = 0; iTree < forestModel.getNbModels(); iTree++)
		{
			// Take the root node of the tree
			ClusNode treeRootNode = (ClusNode)forestModel.getModel(iTree);

			// Transform the tree into rules and add them to current rule set
			numberOfUniqueRules += 
				ruleSet.addRuleSet(treeTransform.constructRules(treeRootNode,
						getStatManager()));
		}
						
		System.out.println("Transformed tree ensemble into rules. " + ruleSet.getModelSize()
	            +  " rules created. (" + numberOfUniqueRules + " of them had unique descriptions.)");	
		
		
		RowData trainingData = (RowData)cr.getTrainingSet();
		
		// ************************** The following copied from ClusRuleInduce.separateAndConquor
		// Do not have any idea what it is about
		
		// The default rule
		// TODO: Investigate different possibilities for the default rule
		ClusStatistic left_over;
		if (trainingData.getNbRows() > 0) {
			left_over = createTotalTargetStat(trainingData);
			left_over.calcMean();
		} else {
			System.out.println("All training examples covered - default rule on entire training set!");
			ruleSet.m_Comment = new String(" (on entire training set)");
			left_over = getStatManager().getTrainSetStat(ClusAttrType.ATTR_USE_TARGET).cloneStat();
			left_over.copy(getStatManager().getTrainSetStat(ClusAttrType.ATTR_USE_TARGET));
			left_over.calcMean();
			//left_over.setSumWeight(0);
			System.err.println(left_over.toString());
		}
		System.out.println("Left Over: "+left_over);
		ruleSet.setTargetStat(left_over);
		
		
		
		
		// ************************** The following are copied from ClusRuleInduce.induce
		// Do not have much idea what it is about. However, optimization is needed
		
		// The rule set was altered. Compute the means (predictions?) for rules again.
		ruleSet.postProc();
		

		// Optimizing rule set
		if (getSettings().isRulePredictionOptimized()) {
			ruleSet = optimizeRuleSet(ruleSet, (RowData)cr.getTrainingSet());
		}
		ruleSet.setTrainErrorScore(); // Not always needed?
		ruleSet.addDataToRules(trainingData);

		// Computing dispersion
		if (getSettings().computeDispersion()) {
			ruleSet.computeDispersion(ClusModel.TRAIN);
			ruleSet.removeDataFromRules();
			if (cr.getTestIter() != null) {
				RowData testdata = (RowData)cr.getTestSet(); // or trainingData?
				ruleSet.addDataToRules(testdata);
				ruleSet.computeDispersion(ClusModel.TEST);
				ruleSet.removeDataFromRules();
			}
		}

		// Number rules (for output purpose in WritePredictions)
		ruleSet.numberRules();
		
		//TODO Do you need to compute the default rule again?
		
		return ruleSet;
	}
	
	/**
	 * Induces the rule models. ClusModel.PRUNED = the optimized rule model
	 * ClusModel.DEFAULT = the ensemble tree model.
	 */
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		RowData trainData = (RowData)cr.getTrainingSet();
		getStatManager().getHeuristic().setTrainData(trainData);
		ClusStatistic trainStat = getStatManager().getTrainSetStat(ClusAttrType.ATTR_USE_CLUSTERING);
		// TODO: Are these needed?
//		double value = trainStat.getDispersion(getStatManager().getClusteringWeights(), trainData);
//		getStatManager().getHeuristic().setTrainDataHeurValue(value);
		
		ClusModelInfo default_model = cr.addModelInfo(ClusModel.DEFAULT);
		ClusModel def = ClusDecisionTree.induceDefault(cr);
		default_model.setModel(def);
		default_model.setName("Default");	
		
//		ClusModelInfo model_info = cr.addModelInfo(ClusModel.ORIGINAL);
//		model_info.setName("Original");
//		model_info.setModel(model);
		
		// Only pruned used for rules.	
		ClusModel model = induceSingleUnpruned(cr);
		ClusModelInfo pruned_model = cr.addModelInfo(ClusModel.PRUNED);
		pruned_model.setModel(model);
		pruned_model.setName("Pruned");
		
		
		
	}

}
