/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.algo.tdidt.tune;

import java.io.*;
import java.util.*;

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

//added 18-05-06
//import clus.ext.hierarchical.*;

public class CDTTuneFTest extends ClusDecisionTree {

	protected ClusInductionAlgorithmType m_Class;

	public CDTTuneFTest(ClusInductionAlgorithmType clss) {
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

	public ClusErrorList createTuneError(ClusStatManager mgr) {
		ClusErrorList parent = new ClusErrorList();
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

	public double doParamXVal(ClusData trset, ClusData pruneset) throws ClusException, IOException {
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
			showFold(i);
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
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(System.out));
		// wrt.print("Error:"); err.showModelError(wrt, 1);
		wrt.flush();
		return err.getModelError();
	}

//	public final static double[] FTEST_SIG = {1.0, 0.1, 0.05, 0.01, 0.005, 0.001};

	public void findBestFTest(ClusData trset, ClusData pruneset) throws ClusException, IOException {
		int best_value = 0;
		boolean low = createTuneError(getStatManager()).getFirstError().shouldBeLow();
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
