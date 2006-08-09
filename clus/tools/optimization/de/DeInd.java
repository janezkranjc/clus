/*
 * Created on 2006.3.29
 */
package clus.tools.optimization.de;

import java.text.NumberFormat;
import java.util.*;

import clus.util.ClusFormat;

/**
 * Class representing a DE individual
 * @author Tea Tusar
 */
public class DeInd {
	
	private ArrayList m_Genes;
	public double m_Fitness;

	public DeInd() {
		m_Genes = new ArrayList();
	}
	
	public void setGenes(ArrayList genes) {
		m_Genes = genes;
	}
	               		
	public ArrayList getGenes() {
		return m_Genes;
	}
	
	public int evaluate(DeProbl probl, int num_eval) {
		m_Fitness = probl.calcFitness(m_Genes);
		return (num_eval + 1);
	}
	
	public String getIndString() {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		int i;
		String result = "";
		result += fr.format(m_Fitness) + "\t";
		for (i = 0; i < m_Genes.size(); i++) {
			result += fr.format(m_Genes.get(i)) + "\t";
		}
	  return result;
	}
	
	public DeInd copy(DeInd original) {
		m_Fitness = original.m_Fitness;
		m_Genes = original.m_Genes;
		return this; 		
	}

}
