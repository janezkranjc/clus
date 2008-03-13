package sit.mtLearner;

import java.io.IOException;

import sit.TargetSet;
import clus.Clus;
import clus.algo.ClusInductionAlgorithm;
import clus.algo.ClusInductionAlgorithmType;
import clus.algo.tdidt.ClusDecisionTree;
import clus.algo.tdidt.DepthFirstInduce;
import clus.data.rows.RowData;
import clus.main.Settings;
import clus.util.ClusException;

public class ClusLearner implements MTLearner{

	public void init(RowData data, Settings sett) {
		Clus clus = new Clus();
		ClusInductionAlgorithmType clss = new ClusDecisionTree(clus);
		ClusInductionAlgorithm induce = null;
		try {
			induce = new DepthFirstInduce(data.m_Schema, sett);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		
		
		
		
		
		
		
		try {
			clus.singleRun(clss);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public RowData[] LearnModel(TargetSet targets, int foldNr) {
		// TODO Auto-generated method stub
		return null;
	}

	public RowData[] LearnModel(TargetSet targets) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	

	public int initLOOXVal() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void initXVal(int nrFolds) {
		// TODO Auto-generated method stub
		
	}

	public void setTestData(RowData test) {
		// TODO Auto-generated method stub
		
	}

}
