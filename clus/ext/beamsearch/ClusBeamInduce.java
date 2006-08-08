package clus.ext.beamsearch;

import clus.algo.induce.*;
import clus.algo.tdidt.*;
import clus.main.*;
import clus.util.*;
import clus.nominal.split.*;
import clus.model.modelio.*;

import java.io.*;
import java.util.ArrayList;

public class ClusBeamInduce extends ClusInduce {
	
	protected NominalSplit m_Split;
	protected ClusBeamSearch m_Search;
	
	public ClusBeamInduce(ClusSchema schema, Settings sett, ClusBeamSearch search) throws ClusException, IOException {
		super(schema, sett);
		m_Search = search;
	}
	
	public void initializeHeuristic() {
		m_Search.initializeHeuristic();
	}
		
	public boolean isModelWriter() {
		return true;
	}
	
	public void writeModel(ClusModelCollectionIO strm) throws IOException {
		m_Search.writeModel(strm);
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		ClusNode root = m_Search.beamSearch(cr);
		root.postProc(null);
		return root;
	}
	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		m_Search.beamSearch(cr);
		ClusModelInfo def_model = cr.addModelInfo(ClusModels.DEFAULT);
		def_model.setModel(ClusDecisionTree.induceDefault(cr));
		def_model.setName("Default");
		ArrayList lst = m_Search.getBeam().toArray();
		for (int i = 0; i < lst.size(); i++) {
			ClusBeamModel mdl = (ClusBeamModel)lst.get(lst.size()-i-1);
			ClusNode tree = (ClusNode)mdl.getModel();
			tree.postProc(null);
			ClusModelInfo model_info = cr.addModelInfo(i+1);
			model_info.setModel(tree);
			model_info.setName("Beam "+(i+1));
			model_info.clearAll();
		}
	}	
}
