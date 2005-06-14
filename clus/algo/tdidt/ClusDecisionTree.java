
package clus.algo.tdidt;

import clus.main.*;
import clus.util.*;
import clus.data.rows.*;
import clus.*;

public class ClusDecisionTree extends ClusClassifier {

	public ClusDecisionTree(Clus clus) {
		super(clus);
	}

	public void printInfo() {
		System.out.println("TDIDT");
		System.out.println("Heuristic: "+getStatManager().getHeuristicName());
	}
	
	public static ClusModel induceDefault(ClusRun cr) {
		ClusNode node = new ClusNode();
		RowData data = (RowData)cr.getTrainingSet();
		node.initTotalStat(cr.getStatManager(), data);
		node.getTotalStat().calcMean();
		node.makeLeaf();		
		return node;
	}

	public ClusModel induceSingle(ClusRun cr) throws ClusException {
		ClusNode orig = getInduce().induce(cr, m_Clus.getScore());
		return m_Clus.pruneTree(orig, cr.getPruneSet(), cr.getTrainingSet());
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException {
		return getInduce().induce(cr, m_Clus.getScore());
	}

	public void induce(ClusRun cr) throws ClusException {
		long start_time = System.currentTimeMillis();
		ClusNode orig = getInduce().induce(cr, m_Clus.getScore());
		m_Clus.storeAndPruneModel(cr, orig);
		long time = (System.currentTimeMillis()-start_time);
		if (Settings.VERBOSE > 0) System.out.println("Time: "+(double)time/1000+" sec");
		cr.setInductionTime(time);		
	}
	
	public void initializeSummary(ClusSummary summ) {	
		ClusModels.DEFAULT = summ.addModel("Default");
		ClusModels.ORIGINAL = summ.addModel("Original");
		// ClusModels.CLASS_PRUNED = m_Summary.addModel("Class pruned");
		int nb_size = getSettings().getSizeConstraintPruningNumber();
		if (nb_size > 0) {
			ClusModels.PRUNED = summ.addModel("S("+getSettings().getSizeConstraintPruning(0)+")");
			for (int i = 1; i < nb_size; i++) {
				summ.addModel("S("+getSettings().getSizeConstraintPruning(i)+")");				
			}
		} else {
			ClusModels.PRUNED = summ.addModel("Pruned");			
		}		
		if (getSettings().rulesFromTree()) {
			ClusModels.RULES = summ.addModel("Rules");
		}
	}
}
