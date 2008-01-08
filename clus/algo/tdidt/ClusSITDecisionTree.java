package clus.algo.tdidt;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Random;

import clus.main.*;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.util.*;
import clus.selection.*;
import clus.algo.ClusInductionAlgorithmType;
import clus.algo.tdidt.ClusDecisionTree;
import clus.data.ClusData;
import clus.data.type.ClusAttrType;
import clus.data.type.NominalAttrType;
import clus.data.type.NumericAttrType;
import clus.error.Accuracy;
import clus.error.ClusError;
import clus.error.ClusErrorList;
import clus.error.RMSError;
import clus.ext.hierarchical.HierClassWiseAccuracy;
import clus.heuristic.*;


public class ClusSITDecisionTree extends ClusDecisionTree{

	
	protected ClusInductionAlgorithmType m_Class;
	
	public ClusSITDecisionTree(ClusInductionAlgorithmType clss) {
		super(clss.getClus());
		m_Class = clss;
	}

	public void printInfo() {
		System.out.println("---------SIT---------");
		System.out.println("Heuristic: "+getStatManager().getHeuristicName());
	}
	
	private final void showFold(int i) {
		if (i != 0) System.out.print(" ");
		System.out.print(String.valueOf(i+1));
		System.out.flush();	
	}
	
	public ClusErrorList createTuneError(ClusStatManager mgr) {
		ClusErrorList parent = new ClusErrorList(mgr);
		if (mgr.getMode() == ClusStatManager.MODE_HIERARCHICAL) {
			parent.addError(new HierClassWiseAccuracy(parent, mgr.getHier()));
			return parent;
		}
		NumericAttrType[] num = mgr.getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom = mgr.getSchema().getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (nom.length != 0) {
			parent.addError(new Accuracy(parent, nom));
		}
		if (num.length != 0) {
			// parent.addError(new PearsonCorrelation(parent, num));
			parent.addError(new RMSError(parent, num));
		}
		return parent;
	}

	
	public ClusError doParamXVal(ClusData trset, ClusData pruneset) throws ClusException, IOException {
		int prevVerb = Settings.enableVerbose(0);
		ClusStatManager mgr = getStatManager();
		ClusSummary summ = new ClusSummary();
		summ.setTestError(createTuneError(mgr));		
//      Next does not always use same partition!		
//		Random random = ClusRandom.getRandom(ClusRandom.RANDOM_PARAM_TUNE);
		Random random = new Random(0);
		int nbfolds = Integer.parseInt(getSettings().getTuneFolds());
		XValMainSelection sel = new XValRandomSelection(trset.getNbRows(), nbfolds, random);
		for (int i = 0; i < nbfolds; i++) {
			//showFold(i);			
			XValSelection msel = new XValSelection(sel, i);
			ClusRun cr = m_Clus.partitionDataBasic(trset, msel, pruneset, summ, i+1);
			ClusModel pruned = m_Class.induceSingle(cr);			
			cr.addModelInfo(ClusModel.PRUNED).setModel(pruned);
			m_Clus.calcError(cr, summ);
/*			System.out.println();
			System.out.println("Model:");
			((ClusNode)pruned).printTree(); */
		}
		ClusModelInfo mi = summ.getModelInfo(ClusModel.PRUNED);
		Settings.enableVerbose(prevVerb);
		ClusError err = mi.getTestError().getFirstError();
		System.out.println();
				
		return err;
	}
	
	
	/**
	 * @param main_target the only weight not set to 0
	 * Set weights to 0, except for the main_target
	 */
	private void resetWeights(int main_target){
		resetWeights();
		getStatManager().getClusteringWeights().m_Weights[main_target] = 1;
	}
	/**
	 * Set weights to 0
	 */
	private void resetWeights(){
		ClusStatManager mgr = getStatManager();
		double[] weights = mgr.getClusteringWeights().m_Weights;
		
		for(int i = 0;i<weights.length;i++){
			weights[i] = 0;
		}
			
	}
	
	/*
	 * find additional support tasks for the given tasks
	 */
	private double findBestSupportTasks(double[] weights,int emc, int[] support_range,ClusData trset, ClusData pruneset) throws ClusException, IOException{
		ClusError err = doParamXVal(trset, pruneset);
		ClusStatManager mgr = getStatManager();
		//variables for holding the current best found weights/err
		double[] best_weights = (double[]) weights.clone();
		
		
		//1 dimensionaal?????
	    System.out.println("err dimensiom:"+err.getDimension());
		double best_err = err.getModelErrorComponent(emc);
		
		
		
		System.out.println("Single Target error: "+best_err);
		System.out.println();
		
		for(int i = support_range[0];i<=support_range[1];i++){
			mgr.getClusteringWeights().m_Weights = (double[]) weights.clone();
			mgr.getClusteringWeights().m_Weights[i] = 1;
		
		
			//Testing new weight vector
			System.out.print("Testing targets: ");
			for(int j = 0;j<weights.length;j++){
				if(mgr.getClusteringWeights().m_Weights[j] == 1){
				System.out.print((j+1)+" ");
				}
			}
			err = doParamXVal(trset, pruneset);
			System.out.println("Correlation: "+err.getModelErrorComponent(emc));
			if(err.getModelErrorComponent(emc)>best_err){
				best_err =  err.getModelErrorComponent(emc);
				best_weights = (double[]) mgr.getClusteringWeights().m_Weights.clone();
			}
			System.out.println();
		}
		
		System.out.println("Best error: "+best_err);
		System.out.print("Best targets:");
		for(int j = 0;j<weights.length;j++){
			if(best_weights[j] == 1){
			System.out.print((j+1)+" ");
			}
		}
		
		//set the weights to the best weights found
		mgr.getClusteringWeights().m_Weights = best_weights;
		
		return best_err;
	}
	
	public void findBestSupportTasks(ClusData trset, ClusData pruneset) throws ClusException, IOException {
		ClusStatManager mgr = getStatManager();
		
		//TODO: make these new settings
		
		int main_target = 139;////we try to optimize for this target. Index! 0 = target 1 
		int support_range[] = {119,149};//try finding optimal support attribute in this range
		int emc = main_target - support_range[0];//error model component of the main target
		boolean recursive = true;
		
		
		/*
		ClusAttrType attr = mgr.getSchema().getAttrType(4-1);
		mgr.getClusteringWeights().setWeight(attr, 0.0); 
		*/
		
		//Optimizing for target 1:
		//set all weights to 0, except the main_target		
		resetWeights(main_target);
		double[] weights = mgr.getClusteringWeights().m_Weights;
		double best_err = findBestSupportTasks((double[])weights.clone(),emc,support_range,trset,pruneset);
		
		if(recursive){
			
			System.out.println("\n---recursive sit---");
			weights = mgr.getClusteringWeights().m_Weights;
			double new_err = findBestSupportTasks((double[])weights.clone(),emc,support_range,trset,pruneset); 
			while(new_err > best_err){
				best_err = new_err;
				weights = mgr.getClusteringWeights().m_Weights;
				new_err = findBestSupportTasks((double[])weights.clone(),emc,support_range,trset,pruneset); 
			}
			
			
		}
		
		
		
		
		
	
		System.out.println();
		
	
	}

	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
	
			// Find optimal F-test value
			findBestSupportTasks(cr.getTrainingSet(), cr.getPruneSet());
			
			// Induce final model
			
			System.out.println("----------Building final model------------");
			m_Class.induceAll(cr);
		
	}
}
