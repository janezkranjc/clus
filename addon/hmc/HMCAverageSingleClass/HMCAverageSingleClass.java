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

/*
 * Created on Dec 22, 2005
 */

import java.util.*;

import java.io.*;

import jeans.util.array.*;
import jeans.util.cmdline.*;
import jeans.util.*;

import clus.*;
import clus.algo.tdidt.*;
import clus.data.rows.*;
import clus.data.io.*;
import clus.main.*;
import clus.util.*;
import clus.statistic.*;
import clus.data.type.*;
import clus.model.modelio.*;
import clus.ext.hierarchical.*;
import clus.error.*;

public class HMCAverageSingleClass implements CMDLineArgsProvider {

	private static String[] g_Options = {"models", "nodewise", "stats"};
	private static int[] g_OptionArities = {1, 0, 0};
	
	protected Clus m_Clus;
	protected StringTable m_Table = new StringTable();
	
	//added: keeps prediction results for each threshold
	protected ClusErrorParent[][] m_EvalArray;
	
	public void run(String[] args) throws IOException, ClusException, ClassNotFoundException {
		m_Clus = new Clus();
		Settings sett = m_Clus.getSettings();
		CMDLineArgs cargs = new CMDLineArgs(this);
		cargs.process(args);
		if (cargs.allOK()) {
			sett.setDate(new Date());
			sett.setAppName(cargs.getMainArg(0));
			m_Clus.initSettings(cargs);
			ClusDecisionTree clss = new ClusDecisionTree(m_Clus);
			m_Clus.initialize(cargs, clss);
			ClusStatistic target = createTargetStat();
			target.calcMean();
			if (cargs.hasOption("stats")) {
				computeStats();
				System.exit(0);
			}
			if (cargs.hasOption("models") || cargs.hasOption("nodewise")) {
				//initializing m_EvalArray
				HierClassTresholdPruner pruner = (HierClassTresholdPruner)getStatManager().getTreePruner(null);
				m_EvalArray = new ClusErrorParent[2][pruner.getNbResults()];
				// HierClassWiseAccuracy needs some things				
				ClassHierarchy hier = getStatManager().getHier();
				//initialize each HierClassWiseAccuracy object
				for (int i=0;i<pruner.getNbResults();i++) {
					for (int j = CRParent.TRAINSET; j <= CRParent.TESTSET; j++) {
						m_EvalArray[j][i] = new ClusErrorParent(getStatManager());
						m_EvalArray[j][i].addError(new HierClassWiseAccuracy(m_EvalArray[j][i], hier));
					}
				}
				//load models and update statistics
				ClusRun cr = m_Clus.partitionData();
				if (cargs.hasOption("nodewise")) {
					HMCAverageNodeWiseModels avg = new HMCAverageNodeWiseModels(this);
					avg.processModels(cr);
				} else {
					loadModelPerModel(cargs.getOptionValue("models"), cr);
				}				
				//write output
				ClusOutput output = new ClusOutput(sett.getAppName() + ".combined.out", m_Clus.getSchema(), sett);
				// create default model
				ClusNode def = new ClusNode();
				ClusStatistic stat = createTargetStat();
				stat.calcMean();				
				def.setTargetStat(stat);
				cr.addModelInfo(ClusModels.DEFAULT).setModel(def);
				cr.getModelInfo(ClusModels.DEFAULT).setName("Default");
				m_Clus.calcError(cr, null); // Calc error				
				// add model for each threshold to clusrun
				for (int i = 0; i < pruner.getNbResults(); i++) {
					ClusModelInfo pruned_info = cr.addModelInfo(ClusModels.PRUNED + i);
					pruned_info.setStatManager(getStatManager());
					pruned_info.setName(pruner.getPrunedName(i));
					for (int j = CRParent.TRAINSET; j <= CRParent.TESTSET; j++) {
						m_EvalArray[j][i].setNbExamples(cr.getDataSet(j).getNbRows());
					}
					pruned_info.setTrainError(m_EvalArray[CRParent.TRAINSET][i]);
					pruned_info.setTestError(m_EvalArray[CRParent.TESTSET][i]);
					pruned_info.setModel(new ClusNode());
				}
				output.writeHeader();
				output.writeOutput(cr, true, true);
				output.close();
			} else {
				throw new ClusException("Must specify e.g., -models dirname");
			}
		}
	}
	
	public ClusStatManager getStatManager() {
		return m_Clus.getStatManager();
	}
	
	public Settings getSettings() {
		return m_Clus.getSettings();
	}
	
	public Clus getClus() {
		return m_Clus;
	}
	
	public ClusErrorParent getEvalArray(int traintest, int j) {
		return m_EvalArray[traintest][j];
	}	
	
	public WHTDStatistic createTargetStat() {
		return (WHTDStatistic)m_Clus.getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
	}
	
	public String getClassStr(String file) {
		int cnt = 0;
		String result = "";		
		String value = FileUtil.getName(FileUtil.removePath(file));
		String[] elems = value.split("-");
		int pos = elems.length - 1;
		while (pos > 0 /*&& StringUtils.isInteger(elems[pos])*/) {
				if (cnt != 0) result = "/" + result;
				result = "" + elems[pos] + result;
				cnt++; pos--;
		}	
		return result;
	}
	
	public int getClassIndex(String file) throws ClusException {
		String class_str = getClassStr(file);
		ClassHierarchy hier = getStatManager().getHier();
		ClassesValue val = new ClassesValue(class_str, hier.getType().getTable());
		return hier.getClassTerm(val).getIndex();
	}
	
	public ClusModel loadModel(String file) throws IOException, ClusException, ClassNotFoundException {
		String class_str = getClassStr(file);
		System.out.println("Loading: "+file+" class: "+class_str);
		ClusModelCollectionIO io = ClusModelCollectionIO.load(file);
		ClusModel sub_model = io.getModel("Original");
		if (sub_model == null) {
			throw new ClusException("Error: .model file does not contain model named 'Original'");
		}
		return sub_model;
	}	
	
	public void loadModelPerModel(String dir, ClusRun cr) throws IOException, ClusException, ClassNotFoundException {
		String[] files = FileUtil.dirList(dir, "model");
		for (int i = 0; i < files.length; i++) {
			ClusModel model = loadModel(FileUtil.cmbPath(dir, files[i]));
			int class_idx = getClassIndex(files[i]);
			// voor iedere threshold 1
			for (int j = CRParent.TRAINSET; j <= CRParent.TESTSET; j++) {
				evaluateModelAndUpdateErrors(j, class_idx, model, cr);
			}
		}		
	}
	
	// evaluate tree for one class on all examples and update errors
	public void evaluateModelAndUpdateErrors(int train_or_test, int class_idx, ClusModel model, ClusRun cr) throws ClusException, IOException {
		RowData data = cr.getDataSet(train_or_test);
		m_Clus.getSchema().attachModel(model);
		HierClassTresholdPruner pruner = (HierClassTresholdPruner)getStatManager().getTreePruner(null);
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			ClusStatistic prediction = model.predictWeighted(tuple);
			double[] predicted_distr = prediction.getNumericPred();
			ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
			boolean actually_has_class = tp.hasClass(class_idx);
			for (int j = 0; j < pruner.getNbResults(); j++) {
			    // update corresponding hierclasswiseacc
				boolean predicted_class = predicted_distr[0] >= pruner.getThreshold(j)/100.0;
				HierClassWiseAccuracy acc = (HierClassWiseAccuracy)m_EvalArray[train_or_test][j].getError(0);
				acc.nextPrediction(class_idx, predicted_class, actually_has_class);
			}			
		}
	}

	// Older version of the code
	
	void evaluateModel(ClusRun cr, HMCAverageTreeModel model) throws IOException, ClusException {
		Settings sett = m_Clus.getSettings();
		ClusOutput output = new ClusOutput(sett.getAppName() + ".combined.out", m_Clus.getSchema(), sett);		
		HierClassTresholdPruner pruner = (HierClassTresholdPruner)getStatManager().getTreePruner(null);		
		for (int i = 0; i < pruner.getNbResults(); i++) {
			ClusModel pruned = model.createWithThreshold(pruner.getThreshold(i));			
			ClusModelInfo pruned_info = cr.addModelInfo(ClusModels.PRUNED + i);
			pruned_info.setModel(pruned);
			pruned_info.setStatManager(getStatManager());
			pruned_info.setName(pruner.getPrunedName(i));
		}
		ClusNode def = new ClusNode();
		ClusStatistic stat = createTargetStat();
		stat.calcMean();
		def.setTargetStat(stat);
		cr.getModelInfo(ClusModels.DEFAULT).setModel(def);
		m_Clus.calcError(cr, null); // Calc error		
		output.writeHeader();
		output.writeOutput(cr, true, true);
		output.close();
	}	
	
	public String[] getOptionArgs() {
		return g_Options;
	}
	
	public int[] getOptionArgArities() {
		return g_OptionArities;
	}
	
	public int getNbMainArgs() {
		return 1;
	}

	public void showHelp() {
	}
	
	public void countClasses(RowData data, double[] counts) {
		ClassHierarchy hier = getStatManager().getHier();
		int sidx = hier.getType().getArrayIndex();
		boolean[] arr = new boolean[hier.getTotal()];
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
			// count with parents
			Arrays.fill(arr, false);
			tp.fillBoolArrayNodeAndAncestors(arr);
			for (int j = 0; j < arr.length; j++) {
				if (arr[j]) counts[0] += 1.0;
			}
			// count without parents
			hier.removeParentNodes(arr);
			for (int j = 0; j < arr.length; j++) {
				if (arr[j]) counts[1] += 1.0;
			}			
		}
	}
		
	public void computeStats() throws ClusException, IOException {
		ClusRun cr = m_Clus.partitionData();
		RegressionStat stat = (RegressionStat)getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
		RowData train = (RowData)cr.getTrainingSet();
		RowData valid = (RowData)cr.getPruneSet();
		RowData test = (RowData)cr.getTestSet();
		train.calcTotalStat(stat);
		if (valid != null) valid.calcTotalStat(stat);
		if (test != null) test.calcTotalStat(stat);
		stat.calcMean();
		ClassHierarchy hier = getStatManager().getHier();
		PrintWriter wrt = getSettings().getFileAbsoluteWriter(getSettings().getAppName() + "-hmcstat.arff");
		ClusSchema schema = new ClusSchema("HMC-Statistics");
		schema.addAttrType(new StringAttrType("Class"));
		schema.addAttrType(new NumericAttrType("Weight"));
		schema.addAttrType(new NumericAttrType("MinDepth"));
		schema.addAttrType(new NumericAttrType("MaxDepth"));
		schema.addAttrType(new NumericAttrType("Frequency"));
		double total = stat.getTotalWeight();
		double[] classCounts = new double[2];
		countClasses(train, classCounts);
		countClasses(valid, classCounts);
		countClasses(test, classCounts);
		wrt.println();
		wrt.println("% Number of examples: "+total);
		wrt.println("% Number of classes: "+hier.getTotal());
		wrt.println("% Number of labels/example: "+classCounts[0]+" (most specific: "+classCounts[1]+")");
		wrt.println("% Hierarchy depth: "+hier.getDepth());		
		wrt.println();
		ARFFFile.writeArffHeader(wrt, schema);
		wrt.println("@DATA");
		for (int i = 0; i < hier.getTotal(); i++) {
			ClassTerm term = hier.getTermAt(i);
			int index = term.getIndex();
			wrt.print(term.toStringHuman(hier));
			wrt.print(","+hier.getWeight(index));
			wrt.print(","+term.getMinDepth());			
			wrt.print(","+term.getMaxDepth());			
			wrt.print(","+stat.getSumValues(index));
			wrt.println();
		}
		wrt.close();
	}
	
	public static void main(String[] args) {
		try {
			HMCAverageSingleClass avg = new HMCAverageSingleClass();
			avg.run(args);
		} catch (IOException io) {
			System.out.println("IO Error: "+io.getMessage());
		} catch (ClusException cl) {
			System.out.println("Error: "+cl.getMessage());
		} catch (ClassNotFoundException cn) {
			System.out.println("Error: "+cn.getMessage());
		}
	}
}
