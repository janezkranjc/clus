/*
 * Created on 2006.3.29
 */
package clus.tools.optimization.de;

import java.util.*;

import clus.main.Settings;
import clus.main.*;

/**
 * Class representing the population.
 * @author Tea Tusar
 */
public class DePop {
	
	public ArrayList m_Inds;
	private DeProbl m_Probl;
	private Random m_Rand;
	private ClusStatManager m_StatMgr;

	public DePop(ClusStatManager stat_mgr, DeProbl probl) {
		m_Probl = probl;
		m_StatMgr = stat_mgr;
		m_Rand = new Random(getSettings().getOptDESeed()); 
		m_Inds = new ArrayList(getSettings().getOptDEPopSize());
		for (int i = 0; i < getSettings().getOptDEPopSize(); i++) {
			DeInd ind = new DeInd();
			m_Inds.add(ind);
		}
	}

	public void createFirstPop() {
		for (int i = 0; i < getSettings().getOptDEPopSize(); i++)
			((DeInd)m_Inds.get(i)).setGenes(m_Probl.getRandVector(m_Rand));
	}

	public int evaluatePop(int num_eval) {
		int result = num_eval;
		for (int i = 0; i < m_Inds.size(); i++) { 
			result = ((DeInd)m_Inds.get(i)).evaluate(m_Probl, result);
		}
		return result;
	}

	public ArrayList getCandidate(int parent) {
	  int i1, i2, i3;
	  int i, i_rand;
	  ArrayList result = new ArrayList(m_Probl.getNumVar());
	  for (int k = 0; k < m_Probl.getNumVar(); k++) {
	  	result.add(k, (new Double(0.0)));
	  }
	  
	  do i1 = (int)(getSettings().getOptDEPopSize() * m_Rand.nextDouble());
	  while (i1 == parent);

	  do i2 = (int)(getSettings().getOptDEPopSize() * m_Rand.nextDouble());
	  while ((i2 == parent) || (i2 == i1));

	  do i3 = (int)(getSettings().getOptDEPopSize() * m_Rand.nextDouble());
	  while ((i3 == parent) || (i3 == i1) || (i3 == i2));

	  i_rand = (int)(m_Probl.getNumVar() * m_Rand.nextDouble());
	  i = i_rand;
	  for (int k = 0; k < m_Probl.getNumVar(); k++) {
	  	if (m_Rand.nextDouble() < (getSettings().getOptDECrossProb()) || (i == i_rand))
	    	result.set(i,new Double(
	    			getSettings().getOptDEWeight() *
				(((Double)((DeInd)m_Inds.get(i1)).getGenes().get(i)).doubleValue() - 
				 ((Double)((DeInd)m_Inds.get(i2)).getGenes().get(i)).doubleValue()) +
				 ((Double)((DeInd)m_Inds.get(i3)).getGenes().get(i)).doubleValue()));
	    else
	      result.set(i,((DeInd)m_Inds.get(parent)).getGenes().get(i));
	    i = ++i % m_Probl.getNumVar();
	  }
	  return m_Probl.getRoundVector(result);
	}
	
	public void sortPopRandom() {
	  int i;
	  ArrayList inds = new ArrayList(getSettings().getOptDEPopSize());
	  ArrayList indexes = new ArrayList(getSettings().getOptDEPopSize());

	  for (i = 0; i < getSettings().getOptDEPopSize(); i++) {
	  	inds.add(new DeInd());
	  	indexes.add(new Integer(i));
	  }

	  int n; 
	  
	  for (i = 0; i < getSettings().getOptDEPopSize(); i++) {
	  	 n = (int)(indexes.size() * m_Rand.nextDouble());
	  	 ((DeInd)inds.get(i)).copy(
	  	 	(DeInd)m_Inds.get(
	  	 		((Integer)indexes.get(n)).intValue()));
	  	 indexes.remove(n);
	  }

	  for (i = 0; i < getSettings().getOptDEPopSize(); i++) {
	  	((DeInd)m_Inds.get(i)).copy((DeInd)inds.get(i));
	  }
	}
	
	public String getPopString() {                          
	  String result = "";
	  for (int i = 0; i < m_Inds.size(); i++) {
	  	result += ((DeInd)m_Inds.get(i)).getIndString();
	  }
	  return result;
	}

	public Settings getSettings() {
		return m_StatMgr.getSettings();
	}

}