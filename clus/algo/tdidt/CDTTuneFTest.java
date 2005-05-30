
package clus.algo.tdidt;

import java.io.*;
import java.util.*;

import clus.main.*;
import clus.util.*;
import clus.selection.*;
import clus.heuristic.*;

public class CDTTuneFTest extends ClusClassifier {

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
	
	public double doParamXVal(ClusData trset) throws ClusException, IOException {
		int prevVerb = Settings.enableVerbose(0);
		ClusStatManager mgr = getStatManager();
		ClusSummary summ = new ClusSummary();
		summ.setTestError(mgr.createTuneError());
		int prmodel = summ.addModel("Pruned");
//      Next does not always use same partition!		
//		Random random = ClusRandom.getRandom(ClusRandom.RANDOM_PARAM_TUNE);
		Random random = new Random(0);
		int nbfolds = Integer.parseInt(getSettings().getTuneFolds());
		XValMainSelection sel = new XValRandomSelection(trset.getNbRows(), nbfolds, random);
		for (int i = 0; i < nbfolds; i++) {
			showFold(i);			
			XValSelection msel = new XValSelection(sel, i);
			ClusRun cr = m_Clus.partitionData2(trset, msel, summ, i+1);
			ClusModel pruned = m_Class.induceSingle(cr);			
			cr.getModelInfo(prmodel).setModel(pruned);
			m_Clus.calcError(cr, summ);
		}
		ClusModelInfo mi = summ.getModelInfo(prmodel);
		Settings.enableVerbose(prevVerb);
		return mi.getTestError().getFirstError().getModelError();
	}
	
//	public final static double[] FTEST_SIG = {1.0, 0.1, 0.05, 0.01, 0.005, 0.001};	
	
	public void findBestFTest(ClusData trset) throws ClusException, IOException {
		int best_value = 0;
		boolean low = getStatManager().createTuneError().getFirstError().shouldBeLow();
		double best_error = low ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		for (int i = 1; i < 6; i++) {
			Settings.FTEST_LEVEL = i;
			System.out.println("Try for: "+FTest.FTEST_SIG[i]);
			double err = doParamXVal(trset);
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
		Settings.setFTest(best_f);
		System.out.println("Best was: "+best_f);
	}

	public void induce(ClusRun cr) {
		try {
			// Find optimal F-test value
			findBestFTest(cr.getTrainingSet());
			System.out.println();
			// Induce final model
			m_Class.induce(cr);
		} catch (ClusException e) {
		    System.err.println("Error: "+e);
		} catch (IOException e) {
		    System.err.println("IO Error: "+e);
		}
	}
	
	public void initializeSummary(ClusSummary summ) {	
		m_Class.initializeSummary(summ);
	}
}
