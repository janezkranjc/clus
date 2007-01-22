/*
 * Created on Apr 5, 2005
 */
package clus.ext.beamsearch;

import clus.main.*;

import java.util.*;
import java.io.*;

public class ClusBeam {

	TreeMap m_Tree;
	Collection m_Values;
	int m_MaxWidth;
	int m_CrWidth;
	boolean m_RemoveEqualHeur;
	double m_MinValue = Double.NEGATIVE_INFINITY;
	double m_BeamSimilarity;
	
	public ClusBeam(int width, boolean rmEqHeur) {
		m_Tree = new TreeMap(); //trees in the beam
		m_Values = m_Tree.values();
		m_MaxWidth = width;
		m_RemoveEqualHeur = rmEqHeur;
	}	

	//add a tree to the beam if it not already there
	public int addIfNotIn(ClusBeamModel model) {
		Double key = new Double(model.getValue());
		ClusBeamTreeElem found = (ClusBeamTreeElem)m_Tree.get(key);
		if (found == null) {
			m_Tree.put(key, new ClusBeamTreeElem(model));
			return 1;
		} else {
			if (m_RemoveEqualHeur) {
				found.setObject(model);
				return 0;
			} else {
				return found.addIfNotIn(model);
			}
		}
	}
	
	public void removeMin() {
		Object first_key = m_Tree.firstKey();
		ClusBeamTreeElem min_node = (ClusBeamTreeElem)m_Tree.get(first_key);
		if (min_node.hasList()) {
			min_node.removeFirst();
		} else {
			m_Tree.remove(first_key);
		}
	}

	public ClusBeamModel getBestAndSmallestModel() {
		ClusBeamTreeElem elem = (ClusBeamTreeElem)m_Tree.get(m_Tree.lastKey());
		if (elem.hasList()) {
			double value = Double.POSITIVE_INFINITY;
			ClusBeamModel result = null;			
			ArrayList arr = elem.getOthers();
			for (int i = 0; i < arr.size(); i++) {
				ClusBeamModel model = (ClusBeamModel)arr.get(i);
				int size = model.getModel().getModelSize(); 
				if (size < value) {
					value = size;
					result = model;
				}
			}
			return result;
		} else {
			return (ClusBeamModel)elem.getObject();
		}
	}

	public ClusBeamModel getBestModel() {
		ClusBeamTreeElem elem = (ClusBeamTreeElem)m_Tree.get(m_Tree.lastKey());
		return (ClusBeamModel)elem.getAnObject(); 		
	}

	public ClusBeamModel getWorstModel() {
		ClusBeamTreeElem elem = (ClusBeamTreeElem)m_Tree.get(m_Tree.firstKey());
		return (ClusBeamModel)elem.getAnObject();
	}
	
	public double computeMinValue() {
		return ((Double)m_Tree.firstKey()).doubleValue();
	}
	
	public void addModel(ClusBeamModel model) {
		double value = model.getValue();
		if(m_MaxWidth == -1){ //the size ot the beam is infinite
			//System.out.println("try to add model :");
			//ClusNode tree = (ClusNode)model.getModel();
			//tree.printTree();
			m_CrWidth += addIfNotIn(model); 
			//if (addIfNotIn(model) == 1){System.out.println("we add a model");m_CrWidth +=1;}
		}
		else 
		{	
		if (m_CrWidth < m_MaxWidth) {	
			m_CrWidth += addIfNotIn(model); 
			if (m_CrWidth == m_MaxWidth) {
				m_MinValue = computeMinValue();				
			}
		} else if (value >= m_MinValue) {
			if (addIfNotIn(model) == 1) {
				removeMin();
				double min = computeMinValue();
				// System.out.println("*** Removing model: "+min);
				m_MinValue = min;
			}
		}
		}
	}
	
	public void print(PrintWriter wrt, int best_n) {
		ArrayList lst = toArray();
		for (int i = 0; i < Math.min(best_n, lst.size()); i++) {
			if (i != 0) wrt.println();
			ClusBeamModel mdl = (ClusBeamModel)lst.get(lst.size()-i-1);
			ClusNode tree = (ClusNode)mdl.getModel();
			double error = Double.NaN; // tree.estimateError(); 
			wrt.println("Model: "+i+" value: "+mdl.getValue()+" error: "+error+" parent: "+mdl.getParentModelIndex());
			tree.printModel(wrt);
		}
	}
	
	public Iterator getIterator() {
		return m_Values.iterator();
	}
	
	public ArrayList toArray() {
		ArrayList lst = new ArrayList();
		Iterator iter = m_Values.iterator();
		while (iter.hasNext()) {
			ClusBeamTreeElem elem = (ClusBeamTreeElem)iter.next();
			elem.addAll(lst);
		}
		return lst;
	}
	
	public int getMaxWidth() {
		return m_MaxWidth;
	}
	
	public int getCrWidth() {
		return m_CrWidth;
	}
	
	public double getMinValue() {
		return m_MinValue;
	}
	
	public void print() {
/*		System.out.println("Beam:");
		m_Tree.printStructure();
		System.out.println("All:");
		m_Tree.printTree();
		System.out.println("Done"); */
	}
	
	/**Dragi
	 * 
	 * @param model - candidate model
	 * @return 0 - no change in the beam (the candidate didn't entered the beam)
	 * @return 1 - change in the beam (the candidate entered the beam)
	 */
	public int removeMinUpdated(ClusBeamModel model){
		// until we reach the Beam Width we put all models in
		if (m_CrWidth < m_MaxWidth) {	
			m_CrWidth += addIfNotIn(model); 
			return 1;
		}
		double currentMin = model.getValue() - Settings.BEAM_SIMILARITY * model.getSimilarityWithBeam();
		double currentMinSimilarity = model.getSimilarityWithBeam();
		ArrayList arr = toArray();
		double modelUpdatedHeur,modelSimilarity;
		int bsize = arr.size();
		int min_pos = bsize;
		for (int i = 0; i < bsize; i++){
			modelSimilarity = ((ClusBeamModel)arr.get(i)).getSimilarityWithBeam();
			modelUpdatedHeur = ((ClusBeamModel)arr.get(i)).getValue() - Settings.BEAM_SIMILARITY * modelSimilarity;
			if (currentMin == modelUpdatedHeur){		
				//this is for handling the case when the updated heuristic is equal
				//we select the model with smaller similarity
				if (currentMinSimilarity > modelSimilarity){
					min_pos = i;
					currentMinSimilarity = modelSimilarity;
				}
			}
			else if (currentMin > modelUpdatedHeur){
					min_pos = i;
					currentMin = modelUpdatedHeur;
					currentMinSimilarity = modelSimilarity;
				}
		}

		if (min_pos != bsize){
			TreeMap temp = new TreeMap();
			ClusBeamModel cbm;
			ClusBeamTreeElem found;
			for (int j = 0; j <= bsize; j++){
				if (j != min_pos){
					if (j !=bsize)	cbm = (ClusBeamModel)arr.get(j);
					else cbm = model; 
					found = (ClusBeamTreeElem) temp.get(Double.valueOf(cbm.getValue()));
					if (found == null) temp.put(Double.valueOf(cbm.getValue()), new ClusBeamTreeElem(cbm));
					else found.addIfNotIn(cbm);
				}
			}
			m_Tree = temp;
			m_Values = m_Tree.values();
			m_MinValue = computeMinValue();
			return 1;
		}
		return 0;
	}
	
	//this checks if the same tree is already in the beam
	public boolean modelAlreadyIn(ClusBeamModel model){
		ArrayList arr = toArray();
		ClusBeamModel bmodel;
		for (int k = 0; k < arr.size(); k++){
			bmodel = (ClusBeamModel)arr.get(k);
			if (((ClusNode)bmodel.getModel()).equals(((ClusNode)model.getModel())))return true;
		}
		return false;
	}
	
	/**Dragi
	 * Sets the Current Beam Similarity
	 * @param similarity
	 */
	public void setBeamSimilarity(double similarity){
		m_BeamSimilarity = similarity;
	}
	
	public double getBeamSimilarity(){
		return m_BeamSimilarity;
	}
}
