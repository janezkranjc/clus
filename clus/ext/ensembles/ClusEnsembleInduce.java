package clus.ext.ensembles;

import java.io.IOException;

import clus.Clus;
import clus.algo.induce.*;
import clus.algo.tdidt.ClusDecisionTree;
import clus.main.*;
import clus.data.type.*;
import clus.selection.BaggingSelection;
import clus.util.ClusException;

public class ClusEnsembleInduce extends ClusInduce {
	Clus m_BagClus;
	ClusForest m_OForest;//Forest with the original models
	ClusForest m_DForest;//Forest with stumps (default models)

	public ClusEnsembleInduce(ClusSchema schema, Settings sett, Clus clus) throws ClusException, IOException {
		super(schema, sett);
		m_BagClus = clus;
	}

	public void induceAll(ClusRun cr) throws ClusException, IOException {
		
		switch (cr.getStatManager().getSettings().getEnsembleMethod()){
		case 0: {//Bagging
			induceBagging(cr);
			break;
			}
		case 1: {//RForest
			induceBagging(cr);
			break;
			}
		case 2: {//RSubspaces
			induceSubspaces(cr);
			break;
			}
		case 3: {//Bagging Subspaces
			induceBaggingSubspaces(cr);
			break;
			}
		}
		
		postProcessForest(cr);
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException,
			IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void postProcessForest(ClusRun cr) throws ClusException{
		
		ClusModelInfo def_info = cr.addModelInfo(ClusModels.DEFAULT);
		def_info.setModel(m_DForest);
		def_info.setName("Default");
		
		ClusModelInfo orig_info = cr.addModelInfo(ClusModels.ORIGINAL);
		orig_info.setModel(m_OForest);
		orig_info.setName("Original");
	
	//Application of Thresholds for HMC
	if (cr.getStatManager().getMode() == ClusStatManager.MODE_HIERARCHICAL){
			double[] thresholds = cr.getStatManager().getSettings().getClassificationThresholds().getDoubleVector();
			//setting the printing preferences in the HMC mode
			m_OForest.setPrintModels(Settings.isPrintEnsembleModels());
			m_DForest.setPrintModels(Settings.isPrintEnsembleModels());
			if (thresholds != null){
				for (int i = 0; i < thresholds.length; i++){
					ClusModelInfo pruned_info = cr.addModelInfo(ClusModels.PRUNED + i);
					ClusForest new_forest = m_OForest.cloneForestWithThreshold(thresholds[i]);
					new_forest.setPrintModels(Settings.isPrintEnsembleModels());
					pruned_info.setModel(new_forest);
					pruned_info.setName("T("+thresholds[i]+")");
				}
			}
		}	
	}

	public void induceSubspaces(ClusRun cr) throws ClusException, IOException {
		Settings sett = getStatManager().getSettings();
		int nbsets = sett.getNbBaggingSets();
		m_OForest = new ClusForest(getStatManager());
		m_DForest = new ClusForest(getStatManager());
		
		for (int i = 0; i < nbsets; i++) {
			System.out.println("Bag: "+(i+1));
			ClusRun crSingle = new ClusRun(cr.getTrainingSet(), cr.getSummary());
			ClusForest.selectRandomSubspaces(cr.getStatManager().getSchema().getDescriptiveAttributes(), cr.getStatManager().getSettings().getNbRandomAttrSelected());
			DepthFirstInduce ind = new DepthFirstInduce(this);
			ind.initialize();
			crSingle.getStatManager().initClusteringWeights();
			ClusModel model = ind.induceSingleUnpruned(crSingle);
			ClusModelInfo model_info = crSingle.addModelInfo(ClusModels.ORIGINAL);
			model_info.setModel(model);	
			model_info.setName("Original");

			ClusModel defmod = ClusDecisionTree.induceDefault(crSingle);		
			ClusModelInfo def_info = crSingle.addModelInfo(ClusModels.DEFAULT);
			def_info.setModel(defmod);
			def_info.setName("Default");
			
			m_OForest.addModelToForest(crSingle.getModel(ClusModels.ORIGINAL));
			m_DForest.addModelToForest(crSingle.getModel(ClusModels.DEFAULT));
		}
	}
	
	public void induceBagging(ClusRun cr) throws ClusException, IOException {
		Settings sett = getStatManager().getSettings();
		int nbsets = sett.getNbBaggingSets();
		int nbrows = cr.getTrainingSet().getNbRows();
		m_OForest = new ClusForest(getStatManager());
		m_DForest = new ClusForest(getStatManager());

		for (int i = 0; i < nbsets; i++) {
			System.out.println("Bag: "+(i+1));
			BaggingSelection msel = new BaggingSelection(nbrows);
			ClusRun crSingle = m_BagClus.partitionDataBasic(cr.getTrainingSet(),msel,cr.getSummary(),i + 1);
			DepthFirstInduce ind = new DepthFirstInduce(this);
			ind.initialize();
			crSingle.getStatManager().initClusteringWeights();
			ClusModel model = ind.induceSingleUnpruned(crSingle);
			ClusModelInfo model_info = crSingle.addModelInfo(ClusModels.ORIGINAL);
			model_info.setModel(model);	
			model_info.setName("Original");

			ClusModel defmod = ClusDecisionTree.induceDefault(crSingle);		
			ClusModelInfo def_info = crSingle.addModelInfo(ClusModels.DEFAULT);
			def_info.setModel(defmod);
			def_info.setName("Default");
			
			m_OForest.addModelToForest(crSingle.getModel(ClusModels.ORIGINAL));
			m_DForest.addModelToForest(crSingle.getModel(ClusModels.DEFAULT));
		}
	}
	
	
	public void induceBaggingSubspaces(ClusRun cr) throws ClusException, IOException {
		Settings sett = getStatManager().getSettings();
		int nbsets = sett.getNbBaggingSets();
		int nbrows = cr.getTrainingSet().getNbRows();
		m_OForest = new ClusForest(getStatManager());
		m_DForest = new ClusForest(getStatManager());
		
		for (int i = 0; i < nbsets; i++) {
			System.out.println("Bag: "+(i+1));
			BaggingSelection msel = new BaggingSelection(nbrows);
			ClusRun crSingle = m_BagClus.partitionDataBasic(cr.getTrainingSet(),msel,cr.getSummary(),i + 1);
			ClusForest.selectRandomSubspaces(cr.getStatManager().getSchema().getDescriptiveAttributes(), cr.getStatManager().getSettings().getNbRandomAttrSelected());
			DepthFirstInduce ind = new DepthFirstInduce(this);
			ind.initialize();
			crSingle.getStatManager().initClusteringWeights();
			ind.initializeHeuristic();
			ClusModel model = ind.induceSingleUnpruned(crSingle);
			ClusModelInfo model_info = crSingle.addModelInfo(ClusModels.ORIGINAL);
			model_info.setModel(model);	
			model_info.setName("Original");

			ClusModel defmod = ClusDecisionTree.induceDefault(crSingle);		
			ClusModelInfo def_info = crSingle.addModelInfo(ClusModels.DEFAULT);
			def_info.setModel(defmod);
			def_info.setName("Default");
			
			m_OForest.addModelToForest(crSingle.getModel(ClusModels.ORIGINAL));
			m_DForest.addModelToForest(crSingle.getModel(ClusModels.DEFAULT));
		}
	}
	
	public ClusForest getOriginalForest(){
		if (m_OForest != null)return m_OForest;
		else {
			System.err.println(getClass().getName()+" getForest(): Original Forest is Not created Yet");
			return null;
		}
	}

	public ClusForest getDefaultForest(){
		if (m_DForest != null)return m_DForest;
		else {
			System.err.println(getClass().getName()+" getForest(): Default Forest is Not created Yet");
			return null;
		}
	}
}
