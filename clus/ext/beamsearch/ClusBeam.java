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
	
	public ClusBeam(int width, boolean rmEqHeur) {
		m_Tree = new TreeMap();
		m_Values = m_Tree.values();
		m_MaxWidth = width;
		m_RemoveEqualHeur = rmEqHeur;
	}	

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
}
