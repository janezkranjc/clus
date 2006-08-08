
package clus.algo.tdidt;

import java.io.*;
import java.util.*;

import clus.main.*;
import clus.util.*;
import clus.selection.*;
import clus.error.ClusError;
import clus.heuristic.*;

//added 18-05-06
//import clus.ext.hierarchical.*;

public class CDTTuneFTest extends ClusDecisionTree {

	protected ClusClassifier m_Class;

	public CDTTuneFTest(ClusClassifier clss) {
		super(clss.getClus());
		m_Class = clss;
	}

	public void printInfo() {
		System.out.println("TDIDT (Tuning F-Test)");
		System.out.println("Heuristic: "+getStatManager().getHeuristicName());
	}
	
	private final void showFold(int i) {
		if (i != 0) System.out.print(" ");
		System.out.print(String.valueOf(i+1));
		System.out.flush();	
	}
	
	public double doParamXVal(ClusData trset, ClusData pruneset) throws ClusException, IOException {
		int prevVerb = Settings.enableVerbose(0);
		ClusStatManager mgr = getStatManager();
		ClusSummary summ = new ClusSummary();
		summ.setTestError(mgr.createTuneError());		
//      Next does not always use same partition!		
//		Random random = ClusRandom.getRandom(ClusRandom.RANDOM_PARAM_TUNE);
		Random random = new Random(0);
		int nbfolds = Integer.parseInt(getSettings().getTuneFolds());
		XValMainSelection sel = new XValRandomSelection(trset.getNbRows(), nbfolds, random);
		for (int i = 0; i < nbfolds; i++) {
			showFold(i);			
			XValSelection msel = new XValSelection(sel, i);
			ClusRun cr = m_Clus.partitionDataBasic(trset, msel, pruneset, summ, i+1);
			ClusModel pruned = m_Class.induceSingle(cr);			
			cr.getModelInfo(ClusModels.PRUNED).setModel(pruned);
			m_Clus.calcError(cr, summ);
/*			System.out.println();
			System.out.println("Model:");
			((ClusNode)pruned).printTree(); */
		}
		ClusModelInfo mi = summ.getModelInfo(ClusModels.PRUNED);
		Settings.enableVerbose(prevVerb);
		ClusError err = mi.getTestError().getFirstError();
		System.out.println();
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(System.out));
		wrt.print("Error:");			
		err.showModelError(wrt, 1);
		wrt.flush();		
		return err.getModelError();
	}
	
//	public final static double[] FTEST_SIG = {1.0, 0.1, 0.05, 0.01, 0.005, 0.001};	
	
	public void findBestFTest(ClusData trset, ClusData pruneset) throws ClusException, IOException {
		int best_value = 0;
		boolean low = getStatManager().createTuneError().getFirstError().shouldBeLow();
		double best_error = low ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
//		System.out.println("best error is"+best_error);
		for (int i = 0; i < 6; i++) {
			Settings.FTEST_LEVEL = i;
			System.out.println("Try for: "+FTest.FTEST_SIG[i]);
			double err = doParamXVal(trset, pruneset);
			System.out.print(" -> "+err);
			if (low) {
				if (err <= best_error) {
					best_error = err;
					best_value = i;
					System.out.println(" *");
				} else {
					System.out.println();
				}				
			} else {
				if (err >= best_error) {
					best_error = err;
					best_value = i;
					System.out.println(" *");
				} else {
					System.out.println();
				}				
			}
		}
		double best_f = FTest.FTEST_SIG[best_value];
		getSettings().setFTest(best_f);
		System.out.println("Best was: "+best_f);
	}

	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		try {
			// Find optimal F-test value
			findBestFTest(cr.getTrainingSet(), cr.getPruneSet());
			System.out.println();
			// Induce final model
			m_Class.induceAll(cr);
		} catch (ClusException e) {
		    System.err.println("Error: "+e);
		} catch (IOException e) {
		    System.err.println("IO Error: "+e);
		}
	}
}
