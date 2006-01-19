
package clus.algo.tdidt;

import java.io.IOException;

import clus.main.*;
import clus.util.*;
import clus.data.rows.*;
import clus.*;

import jeans.resource.*;

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
		node.initTargetStat(cr.getStatManager(), data);
		node.computePrediction();
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

	public void induce(ClusRun cr) throws ClusException, IOException {
		long start_time = ResourceInfo.getTime();
		ClusNode orig = getInduce().induce(cr, m_Clus.getScore());
		cr.setInductionTime(ResourceInfo.getTime()-start_time);
		m_Clus.storeAndPruneModel(cr, orig);		
		if (Settings.VERBOSE > 0) {
			  String cpu = ResourceInfo.isLibLoaded() ? " (CPU)" : "";
				System.out.println("Induction Time: "+(double)cr.getInductionTime()/1000+" sec"+cpu);
				System.out.println("Pruning Time: "+(double)cr.getPruneTime()/1000+" sec"+cpu);
		}		
	}
	
	public void initializeSummary(ClusSummary summ) {	
		ClusModels.DEFAULT = summ.addModel("Default");
		ClusModels.ORIGINAL = summ.addModel("Original");
		// ClusModels.CLASS_PRUNED = m_Summary.addModel("Class pruned");
		int nb_size = getSettings().getSizeConstraintPruningNumber();
		double[] class_thr = getSettings().getClassificationTresholds().getDoubleVector();
		if (nb_size > 0 && getSettings().getPruningMethod() != Settings.PRUNING_METHOD_GAROFALAKIS_VSB) {
			ClusModels.PRUNED = summ.addModel("S("+getSettings().getSizeConstraintPruning(0)+")");
			for (int i = 1; i < nb_size; i++) {
				summ.addModel("S("+getSettings().getSizeConstraintPruning(i)+")");				
			}
		} else if (class_thr != null) {
			ClusModels.PRUNED = summ.addModel("T("+class_thr[0]+")");
			summ.getModelInfo(ClusModels.PRUNED).setShouldSave(false);
			summ.getModelInfo(ClusModels.PRUNED).setPruneInvalid(true);			
			for (int i = 1; i < class_thr.length; i++) {
				int model = summ.addModel("T("+class_thr[i]+")");
				summ.getModelInfo(model).setShouldSave(false);
				summ.getModelInfo(model).setPruneInvalid(true);
			}			
		} else {
			ClusModels.PRUNED = summ.addModel("Pruned");			
		}		
		if (getSettings().rulesFromTree() == Settings.CONVERT_RULES_PRUNED) {
			ClusModels.RULES = summ.addModel("Rules");
		}
		if (getSettings().rulesFromTree() == Settings.CONVERT_RULES_ALL) {
			ClusModels.RULES = summ.getNbModels();			
			for (int i = 0; i < ClusModels.RULES; i++) {
				if (i != ClusModels.DEFAULT) {
					summ.addModel("Rules: "+summ.getModelInfo(i).getName());
				}
			}
		}
	}
}
