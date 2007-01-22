package clus.ext.beamsearch;

import clus.Clus;
import clus.algo.induce.*;
import clus.algo.tdidt.*;
import clus.data.rows.RowData;
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
		root.updateTree();
		return root;
	}
	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		m_Search.beamSearch(cr);
		ClusModelInfo def_model = cr.addModelInfo(ClusModels.DEFAULT);
		def_model.setModel(ClusDecisionTree.induceDefault(cr));
		def_model.setName("Default");
		ArrayList lst = m_Search.getBeam().toArray();
		if (cr.getStatManager().getSettings().getBeamSortOnTrainParameter())sortModels(cr, lst);
		for (int i = 0; i < lst.size(); i++) {
			ClusBeamModel mdl = (ClusBeamModel)lst.get(lst.size()-i-1);
			ClusNode tree = (ClusNode)mdl.getModel();			
			tree.updateTree();
			ClusModelInfo model_info = cr.addModelInfo(i+1);
			model_info.setModel(tree);
			model_info.setName("Beam "+(i+1));
			model_info.clearAll();
		}
	}	
	
	/**Dragi, JSI
	 * Sorts the beam according to train accuracy/correlation in descending order
	 * In case of equal train accuracy/correlation 
	 * then the tree with greater heuristic score are put higher
	 * @param cr - Clus Run
	 * @param arr	- The list with the beam
	 * @throws ClusException
	 * @throws IOException
	 */
	
	public void sortModels(ClusRun cr, ArrayList arr) throws ClusException, IOException{
		int size = arr.size();
		ClusBeamModel[] models = new ClusBeamModel[size];
		double[]err = new double [size];
		double[]heur = new double [size];
		for (int i = 0 ; i < size; i++){
			models[i] = (ClusBeamModel)arr.get(i);
			err[i] = Clus.calcModelError(cr.getStatManager(), (RowData)cr.getTrainingSet(), models[i].getModel());
			heur[i] = models[i].getValue();
		}
		ClusBeamModel cbm;
		double tmp;
		for (int j = 0; j < size -1; j++)
			for (int k = j+1; k < size; k++){
				if (err[j]>err[k]){
					cbm = models [j];
					models [j] = models [k];
					models [k] = cbm;
					tmp = err [j];
					err [j] = err [k];
					err [k] = tmp;
					tmp = heur [j];
					heur [j] = heur [k];
					heur [k] = tmp;
				}
				else if (err[j]==err[k]){
						if (heur[j]<heur[k]){
							cbm = models [j];
							models [j] = models [k];
							models [k] = cbm;
							tmp = err [j];
							err [j] = err [k];
							err [k] = tmp;
							tmp = heur [j];
							heur [j] = heur [k];
							heur [k] = tmp;
						} 
				}
			}
		arr.clear();
		for (int m = 0; m < size; m++)arr.add(models[m]);
	}
}
