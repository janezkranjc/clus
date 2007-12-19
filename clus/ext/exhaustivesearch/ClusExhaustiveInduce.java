package clus.ext.exhaustivesearch;

import clus.algo.induce.*;
import clus.main.*;
import clus.util.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.nominal.split.*;
import clus.model.modelio.*;
import clus.ext.beamsearch.*;
import java.io.*;
import clus.algo.tdidt.*;
import java.util.ArrayList;

public class ClusExhaustiveInduce extends ClusInduce {

		protected NominalSplit m_Split;
		protected ClusExhaustiveSearch m_Search;
		
	public ClusExhaustiveInduce(ClusSchema schema, Settings sett, ClusExhaustiveSearch search) throws ClusException, IOException {
			super(schema, sett);
			m_Search = search;
	}
		
	public void initializeHeuristic() {
			m_Search.initializeHeuristic();
	}
		
	
	public ClusData createData() {
		return new RowData(m_Schema);
	}	
	
	public boolean isModelWriter() {
		return true;
	}
	
	public void writeModel(ClusModelCollectionIO strm) throws IOException {
		m_Search.writeModel(strm);
	}

	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		return null;
	}

	public void induceAll(ClusRun cr) throws ClusException, IOException {
		m_Search.exhaustiveSearch(cr);
		ClusModelInfo def_model = cr.addModelInfo(ClusModels.DEFAULT);
		def_model.setModel(ClusDecisionTree.induceDefault(cr));
		def_model.setName("Default");
		ArrayList lst = m_Search.getBeam().toArray();
		for (int i = 0; i < lst.size(); i++) {
			//ClusBeamModel mdl = (ClusBeamModel)lst.get(lst.size()-i-1);
			ClusBeamModel mdl = (ClusBeamModel)lst.get(i);
			ClusNode tree = (ClusNode)mdl.getModel();
			//tree.postProc(null);
			//ClusModelInfo model_info = cr.addModelInfo(i+1);
			ClusModelInfo model_info = cr.addModelInfo(i);
			model_info.setModel(tree);
			model_info.setName("Beam "+(i+1));
			model_info.clearAll();
		}
	}
}

	