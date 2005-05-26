/*
 * Created on May 26, 2005
 */
package clus.ext.hierarchical;

import java.util.*;

import clus.pruning.*;
import clus.statistic.ClusStatistic;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.main.*;

public class HierRemoveInsigClasses implements PruneTree {

	PruneTree m_Pruner;
	ClusData m_PruneSet;
	ClassHierarchy m_Hier;
	double[] m_Distribution;
	double m_SigLevel;	
	
	public HierRemoveInsigClasses(double siglevel, ClusData pruneset, PruneTree other, ClassHierarchy hier) {
		m_Pruner = other;
		m_SigLevel = siglevel;
		m_PruneSet = pruneset;
		m_Hier = hier;
		m_Distribution = new double[hier.getTotal()];
	}
	
	public void prune(ClusNode node) {
		m_Pruner.prune(node);
		executeRecursive(node, (WHTDStatistic)node.getTotalStat(), (RowData)m_PruneSet);
	}
	
	public static double factorial(int fac) {
		double res = 1.0;
		if (fac < 0) {
			System.err.println("Error, factorial less than zero: "+fac);
		}
		for (int i = 2; i <= fac; i++) {
			res *= i;
		}
		System.out.println("Fac: "+res);
		return res;
	}
	
	public static double comb(int n, int k) {		
		return factorial(n)/factorial(n-k)/factorial(k);
	}
	
	public double computeStatistic(int nbcorr, int nbsamp, int globalclass, int nbglobal) {
		double stat = 0.0;
		for (int i = nbcorr; i < nbsamp; i++) {
			double a1 = comb(globalclass, i) * comb(nbglobal - globalclass, nbsamp - i);
			double a2 = comb(nbglobal, nbsamp);
			System.out.println("A1 = "+a1+" A2 = "+a2);
			if (a2 != 0.0) stat +=  a1/a2;
		}		
		return stat;
	}

	public void executeRecursive(ClusNode node, WHTDStatistic global, RowData data) {
		if (node.atBottomLevel()) {
			Arrays.fill(m_Distribution, 0.0);
			for (int i = 0; i < data.getNbRows(); i++) {
				DataTuple tuple = data.getTuple(i);
				int sidx = m_Hier.getType().getSpecialIndex();
				ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
				tp.updateDistribution(m_Distribution, 1.0);
			}
			WHTDStatistic pred = (WHTDStatistic)node.getTotalStat();
			double[] predarr = ((WHTDStatistic)pred).getDiscretePred();
			for (int i = 0; i < predarr.length; i++) {
				if (predarr[i] > 0.5) {
					/* Predicted class i, check sig? */
					int nb_correct = (int)m_Distribution[i];
					int nbsamp = data.getNbRows();
					int nbglobal = (int)global.getTotalWeight();
					int globalclass = (int)(global.getTotalWeight()*global.m_Means[i]);
					System.out.println("Global total = "+nbglobal);
					System.out.println("Sample total = "+nbsamp);
					System.out.println("Global class = "+globalclass);
					System.out.println("Sample class = "+nb_correct);
					double stat = computeStatistic(nb_correct, nbsamp, globalclass, nbglobal);
					System.out.println("Stat = "+stat);
				}
			}
		}
		int arity = node.getNbChildren();
		for (int i = 0; i < arity; i++) {
			RowData subset = data.applyWeighted(node.getTest(), i);
			executeRecursive((ClusNode)node.getChild(i), global, subset);
		}
	}
}



