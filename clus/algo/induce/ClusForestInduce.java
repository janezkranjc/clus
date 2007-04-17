package clus.algo.induce;

import java.io.IOException;

import clus.Clus;
import clus.algo.tdidt.ClusDecisionTree;
import clus.main.*;
import clus.selection.BaggingSelection;
import clus.util.ClusException;

public class ClusForestInduce extends ClusInduce {
	Clus m_BagClus;
	ClusForest m_Forest_ORIGINAL;
	ClusForest m_Forest_DEFAULT;
	
	public ClusForestInduce(ClusSchema schema, Settings sett, Clus clus) throws ClusException, IOException {
		super(schema, sett);
		m_BagClus = clus;
	}

	public void induceAll(ClusRun cr) throws ClusException, IOException {
		Settings sett = getStatManager().getSettings();
		int nbsets = sett.getNbBaggingSets();
		int nbrows = cr.getTrainingSet().getNbRows();
		m_Forest_ORIGINAL = new ClusForest(getStatManager().getSchema());
		m_Forest_DEFAULT = new ClusForest(getStatManager().getSchema());

		for (int i = 0; i < nbsets; i++) {
			System.out.println();
			System.out.println("Bag: "+(i+1));
			BaggingSelection msel = new BaggingSelection(nbrows);
			ClusRun crSingle = m_BagClus.partitionDataBasic(cr.getTrainingSet(),msel,cr.getSummary(),i + 1);
			DepthFirstInduce ind = new DepthFirstInduce(this);
			ind.initialize();
			ind.initializeHeuristic();
			ClusModel model = ind.induceSingleUnpruned(crSingle);
			
			ClusModelInfo model_info = crSingle.addModelInfo(ClusModels.ORIGINAL);
			model_info.setModel(model);	
			model_info.setName("Original");

			ClusModel defmod = ClusDecisionTree.induceDefault(crSingle);		
			ClusModelInfo def_info = crSingle.addModelInfo(ClusModels.DEFAULT);
			def_info.setModel(defmod);
			def_info.setName("Default");
			
			m_Forest_ORIGINAL.addModelToForest(crSingle.getModel(ClusModels.ORIGINAL));
			m_Forest_DEFAULT.addModelToForest(crSingle.getModel(ClusModels.DEFAULT));
		}
		postProcessForest(cr);
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException,
			IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void postProcessForest(ClusRun cr){
		
		ClusModelInfo def_info = cr.addModelInfo(ClusModels.DEFAULT);
		def_info.setModel(m_Forest_DEFAULT);
		def_info.setName("Default");
		
		ClusModelInfo orig_info = cr.addModelInfo(ClusModels.ORIGINAL);
		orig_info.setModel(m_Forest_ORIGINAL);
		orig_info.setName("Original");
	}
	
	public ClusForest getOriginalForest(){
		if (m_Forest_ORIGINAL != null)return m_Forest_ORIGINAL;
		else {
			System.err.println(getClass().getName()+" getForest(): Original Forest is Not created Yet");
			return null;
		}
	}

	public ClusForest getDefaultForest(){
		if (m_Forest_DEFAULT != null)return m_Forest_DEFAULT;
		else {
			System.err.println(getClass().getName()+" getForest(): Default Forest is Not created Yet");
			return null;
		}
	}
	}
