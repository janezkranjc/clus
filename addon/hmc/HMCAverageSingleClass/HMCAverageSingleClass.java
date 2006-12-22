/*
 * Created on Dec 22, 2005
 */

import java.util.Date;

import java.io.*;

import jeans.util.array.*;
import jeans.util.cmdline.*;
import jeans.util.*;

import clus.*;
import clus.algo.tdidt.*;
import clus.data.rows.*;
import clus.ext.*;
import clus.main.*;
import clus.util.*;
import clus.statistic.*;
import clus.data.type.*;
import clus.model.modelio.ClusModelCollectionIO;
import clus.ext.hierarchical.*;
import clus.error.*;

public class HMCAverageSingleClass implements CMDLineArgsProvider {

	private static String[] g_Options = {"models"};
	private static int[] g_OptionArities = {1};
	
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
			if (cargs.hasOption("models")) {
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
				loadModelPerModel(cargs.getOptionValue("models"), cr);
				
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
