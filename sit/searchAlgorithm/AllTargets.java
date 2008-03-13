package sit.searchAlgorithm;

import java.util.ArrayList;
import java.util.Collection;

import sit.TargetSet;
import sit.mtLearner.MTLearner;

import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;
import clus.data.type.ClusSchema;
import clus.data.type.NumericAttrType;
import clus.error.ClusErrorList;
import clus.error.PearsonCorrelation;
import clus.error.RMSError;
import clus.error.SpearmanRankCorrelation;

public class AllTargets implements SearchAlgorithm {

	protected MTLearner m_MTLearner;
	
	/**
	 * This class will always return the full target candidates set.
	 */
	public TargetSet search(ClusAttrType mainTarget, TargetSet candidates) {
		//init the XVal
		int nbFolds = m_MTLearner.initLOOXVal();
		//learn a model for each fold
		ArrayList<RowData[]> folds = new ArrayList<RowData[]>();
		
		for(int f = 0;f<nbFolds;f++){
			folds.add(m_MTLearner.LearnModel(candidates,f));
		}
		
		
		
		calcError(folds);
		/*
		 * By design returns back the full candidates set
		 */
		return new TargetSet(candidates);
	}

	public void setMTLearner(MTLearner learner) {
		this.m_MTLearner = learner;
	}
	
	private void calcError(ArrayList<RowData[]> folds){
		RowData[] temp = (RowData[]) folds.get(0);
		ClusSchema schema = temp[0].getSchema();
		ClusErrorList parent = new ClusErrorList();
		NumericAttrType[] num = schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		SpearmanRankCorrelation error = new SpearmanRankCorrelation(parent, num);
		parent.addError(error);
		
		//System.out.println(folds.size());
			
		
		for(int f=0;f<folds.size();f++){
			RowData[] fold = folds.get(f);
			
		//	System.out.println("-----fold:"+f);
			for(int t=0;t<fold[0].getNbRows();t++){
			//	System.out.println("--tuple:"+t);
				DataTuple tuple_real = fold[0].getTuple(t);
				DataTuple tuple_prediction = fold[1].getTuple(t);
				
				//System.out.println("real: "+tuple_real);
				//System.out.println("pred: "+tuple_prediction);
				parent.addExample(tuple_real, tuple_prediction);
				
			}
		}
		
			
		System.out.println(error.getAvgRankCorr()+"+-"+Math.sqrt(error.getRankCorrVariance()));
		System.out.println(error.getHarmonicAvgRankCorr()+"+-"+Math.sqrt(error.getHarmonicRankCorrVariance()));
		

		
		
		
		
	}
}
