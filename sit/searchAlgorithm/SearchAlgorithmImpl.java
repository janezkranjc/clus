package sit.searchAlgorithm;

import java.util.ArrayList;

import sit.Evaluator;
import sit.TargetSet;
import sit.mtLearner.MTLearner;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;

/**
 * Abstract implementation of the SearchAlgo interface.
 * Provides some basic functions needed by most implementations.
 * @author beau
 *
 */
public abstract class SearchAlgorithmImpl implements SearchAlgorithm{

	protected MTLearner learner;

	public void setMTLearner(MTLearner learner) {
		this.learner = learner;		
	}

	protected double eval(TargetSet tset, ClusAttrType mainTarget){
		//create a few folds
		int nbFolds = 10;
		learner.initXVal(nbFolds);
		//learn a model for each fold
		ArrayList<RowData[]> folds = new ArrayList<RowData[]>();
		for(int f = 0;f<nbFolds;f++){
			folds.add(learner.LearnModel(tset,f));
		}
		return Evaluator.getPearsonCorrelation(folds,mainTarget.getArrayIndex());
	}
	
	
}
