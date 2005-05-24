
package clus.algo.tdidt;

import clus.main.*;
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

	public ClusModel induceSingle(ClusRun cr) {
		ClusNode orig = getInduce().induce(cr, m_Clus.getScore());
		return m_Clus.pruneTree(orig, cr.getPruneSet());
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) {
		return getInduce().induce(cr, m_Clus.getScore());
	}

	public void induce(ClusRun cr) {
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
		ClusModels.PRUNED = summ.addModel("Pruned");
		if (getSettings().rulesFromTree()) {
			ClusModels.RULES = summ.addModel("Rules");
		}
	}
}
