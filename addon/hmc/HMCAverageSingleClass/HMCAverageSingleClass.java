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

public class HMCAverageSingleClass implements CMDLineArgsProvider {

	private static String[] g_Options = {"models"};
	private static int[] g_OptionArities = {1};
	
	protected Clus m_Clus;
	protected StringTable m_Table = new StringTable();

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
			HMCAverageTreeModel model = new HMCAverageTreeModel(target);
			if (cargs.hasOption("models")) {
				loadModels(cargs.getOptionValue("models"), model);
				ClusRun cr = m_Clus.partitionData();
				evaluateModel(cr, model);
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
	
	public void loadModel(String file, HMCAverageTreeModel model) throws IOException, ClusException, ClassNotFoundException {
		String class_str = getClassStr(file);
		System.out.println("Loading: "+file+" class: "+class_str);
		ClusModelCollectionIO io = ClusModelCollectionIO.load(file);
		ClusModel sub_model = io.getModel("Original");
		if (sub_model == null) {
			throw new ClusException("Error: .model file does not contain model named 'Original'");
		}
		WHTDStatistic stat = createTargetStat();
		stat.calcMean();
		ClassHierarchy hier = stat.getHier();
		ClassesTuple tuple = new ClassesTuple(class_str, hier.getType().getTable());
		tuple.addHierarchyIndices(hier);
		stat.setMeanTuple(tuple);
		model.addSubModel(sub_model, stat, file, m_Table);
	}
	
	public void loadModels(String dir, HMCAverageTreeModel model) throws IOException, ClusException, ClassNotFoundException {
		String[] files = FileUtil.dirList(dir, "model");
		for (int i = 0; i < files.length; i++) {
			loadModel(FileUtil.cmbPath(dir, files[i]), model);
		}
	}
	
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
	
	public void evaluateModelAndUpdateErrors(int class_idx, ClusModel model, ClusRun cr) throws ClusException, IOException {
		RowData data = cr.getTestSet();
		m_Clus.getSchema().attachModel(model);
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			ClusStatistic prediction = model.predictWeighted(tuple);
			double pred_weight = ((WHTDStatistic)prediction).getMean(0);
			ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
			boolean has_class = tp.hasClass(class_idx);
			// for each threshold
			    // update corresponding hierclasswiseacc
		}
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
