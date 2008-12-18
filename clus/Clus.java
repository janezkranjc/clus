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

package clus;

import clus.tools.debug.Debug;

import clus.gui.*;
import clus.io.ClusSerializable;
import clus.algo.ClusInductionAlgorithm;
import clus.algo.ClusInductionAlgorithmType;
import clus.algo.tdidt.*;
import clus.algo.tdidt.processor.NodeExampleCollector;
import clus.algo.tdidt.processor.NodeIDWriter;
import clus.algo.tdidt.tune.CDTTuneFTest;
import clus.algo.tdidt.tune.CDTuneSizeConstrPruning;

import jeans.io.*;
import jeans.util.*;
import jeans.util.cmdline.*;
import jeans.resource.*;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import clus.main.*;
import clus.util.*;
import clus.data.ClusData;
import clus.data.type.*;
import clus.data.io.ARFFFile;
import clus.data.io.ClusReader;
import clus.data.io.ClusView;
import clus.data.rows.*;
import clus.error.*;
import clus.error.multiscore.*;
import clus.statistic.*;
import clus.selection.*;
import clus.ext.hierarchical.*;
import clus.ext.beamsearch.*;
import clus.ext.ensembles.*;
import clus.ext.exhaustivesearch.*;
import clus.ext.constraint.*;
import clus.pruning.*;

import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.model.processor.*;
import clus.model.modelio.*;

import clus.algo.kNN.*;
import clus.algo.rules.*;

// import clus.weka.*;

public class Clus implements CMDLineArgsProvider {

	public final static boolean m_UseHier = true;

	public final static String VERSION = "2.6";

	//exhaustive was added the 1/08/2006
	public final static String[] OPTION_ARGS = {"exhaustive", "xval", "oxval", "target",
			"disable", "silent", "lwise", "c45", "info", "sample", "debug",
			"tuneftest", "load", "soxval", "bag", "obag", "show", "knn",
			"knnTree", "beam", "gui", "fillin", "rules", "weka", "corrmatrix",
			"tunesize", "out2model", "test", "normalize", "tseries", "writetargets", "fold", "forest",
			"copying","sit","tc"};

	public final static int[] OPTION_ARITIES = {0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0,
			0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0,0,0};

	protected Settings m_Sett = new Settings();
	protected ClusSummary m_Summary = new ClusSummary();
	protected ClusSchema m_Schema;
	protected MultiScore m_Score;
	protected ClusInductionAlgorithmType m_Classifier;
	protected ClusInductionAlgorithm m_Induce;
	protected RowData m_Data;
	protected Date m_StartDate = new Date();
	protected boolean isxval = false;
	protected CMDLineArgs m_CmdLine;

	public final void initialize(CMDLineArgs cargs, ClusInductionAlgorithmType clss) throws IOException, ClusException {
		m_CmdLine = cargs;
		m_Classifier = clss;
		// Load resource info (this measures among others CPU time on Linux)
		boolean test = m_Sett.getResourceInfoLoaded() == Settings.RESOURCE_INFO_LOAD_TEST;
		ResourceInfo.loadLibrary(test);
		// Load settings file
		ARFFFile arff = null;
		System.out.println("Loading '" + m_Sett.getAppName() + "'");
		ClusRandom.initialize(m_Sett);
		ClusReader reader = new ClusReader(m_Sett.getDataFile(), m_Sett);
		System.out.println();
		if (cargs.hasOption("c45")) {
			System.out.println("Reading C45 .names/.data");
		} else {
			System.out.println("Reading ARFF Header");
			arff = new ARFFFile(reader);
			m_Schema = arff.read(m_Sett);
		}
		// Count rows and move to data segment
		System.out.println();
		System.out.println("Reading CSV Data");
		// Updata schema based on settings
		m_Sett.updateTarget(m_Schema);
		m_Schema.initializeSettings(m_Sett);
		m_Sett.setTarget(m_Schema.getTarget().toString());
		m_Sett.setDisabled(m_Schema.getDisabled().toString());
		m_Sett.setClustering(m_Schema.getClustering().toString());
		m_Sett.setDescriptive(m_Schema.getDescriptive().toString());
		// Create induce
		m_Induce = clss.createInduce(m_Schema, m_Sett, cargs);
		// Load data from file
		if (ResourceInfo.isLibLoaded()) {
			ClusStat.m_InitialMemory = ResourceInfo.getMemory();
		}
		ClusView view = m_Schema.createNormalView();
		m_Data = view.readData(reader, m_Schema);
		reader.close();
		System.out.println("Found " + m_Data.getNbRows() + " rows");

		if (getSettings().getNormalizeData() != Settings.NORMALIZE_DATA_NONE) {
			System.out.println("Normalizing numerical data");
			m_Data = returnNormalizedData();
		}

		m_Schema.printInfo();
		if (ResourceInfo.isLibLoaded()) {
			ClusStat.m_LoadedMemory = ResourceInfo.getMemory();
		}
		if (getSettings().isRemoveMissingTarget()) {
			m_Data = CriterionBasedSelection.removeMissingTarget(m_Data);
			CriterionBasedSelection.clearMissingFlagTargetAttrs(m_Schema);
		}

		// Preprocess and initialize induce
		m_Sett.update(m_Schema);

		// If not rule induction, reset some settings just to be sure
		// in case rules from trees are used.
		// I.e. this is used if the command line parameter is for decision trees
		// but the transformation for rules is used.
		// It is also possible to use command line parameter -rules and use trees as a covering method.
		if (!m_Induce.getStatManager().isRuleInduce() )
			m_Sett.disableRuleInduceParams();

		// Set XVal field in Settings
		if (isxval) Settings.IS_XVAL = true;
		preprocess(); // necessary in order to link the labels to the class hierarchy in HMC (needs to be before m_Induce.initialize())
		m_Induce.initialize();
		initializeAttributeWeights(m_Data);
		m_Induce.initializeHeuristic();
		loadConstraintFile();
		initializeSummary(clss);
		System.out.println();
		// Sample data
		if (cargs.hasOption("sample")) {
			String svalue = cargs.getOptionValue("sample");
			sample(svalue);
		}
		System.out.println("Has missing values: " + m_Schema.hasMissing());
		if (ResourceInfo.isLibLoaded()) {
			System.out.println("Memory usage: loading data took " + (ClusStat.m_LoadedMemory - ClusStat.m_InitialMemory) +" kB");
		}
	}

	public void initialize(RowData data, ClusSchema schema, Settings sett, ClusInductionAlgorithmType clss) throws IOException, ClusException {
		m_Data = data;
		m_Sett = sett;
		m_Classifier = clss;
		m_Schema = schema;
		m_CmdLine = new CMDLineArgs(this);
	}

	/***
	 * Method for recreating ClusInduce and most other instance variables
	 * Useful for running Clus on different data sets with a similar schema
	 */
	public void recreateInduce(CMDLineArgs cargs, ClusInductionAlgorithmType clss, ClusSchema schema, RowData data) throws ClusException, IOException {
		m_Summary = new ClusSummary();
		m_Schema = schema;
		m_Induce = clss.createInduce(schema, m_Sett, cargs);
		m_Data = data;
		m_Classifier = clss;
		data.setSchema(schema);
		m_Induce.initialize();
		initializeAttributeWeights(data);
		m_Induce.initializeHeuristic();
		initializeSummary(clss);
	}

	/***
	 * Easy to use initialization method to be used from inside add-on
	 * applications supporting Clus (e.g., applications converting data)
	 **/
	public void initializeAddOn(String appname) throws ClusException, IOException {
		Settings sett = getSettings();
		sett.setDate(new Date());
		sett.setAppName(appname);
		initSettings(null);
		ClusDecisionTree clss = new ClusDecisionTree(this);
		initialize(new CMDLineArgs(this), clss);
	}

	public final void loadConstraintFile() throws IOException {
		if (m_Sett.hasConstraintFile()) {
			ClusConstraintFile constr = ClusConstraintFile.getInstance();
			constr.load(m_Sett.getConstraintFile(), m_Schema);
		}
	}

	public final void initSettings(CMDLineArgs cargs) throws IOException {
		m_Sett.initialize(cargs, true);
	}

	public final void initializeSummary(ClusInductionAlgorithmType clss) {
		ClusStatManager mgr = m_Induce.getStatManager();
		ClusErrorList error = mgr.createErrorMeasure(m_Score);
		m_Summary.resetAll();
		m_Summary.setStatManager(mgr);
		m_Summary.setTrainError(error);
		if (hasTestSet()) m_Summary.setTestError(error);
		if (hasPruneSet()) m_Summary.setValidationError(error);
	}

	//added by Leander 7-4-2006
	public final void initializeClassWeights() {
		double[] we = m_Sett.getClassWeight();
		// add the weight to all examples of specific classes (in DataTuple)
		// if there are no weights specified, are they automatically 1? yes
		System.out.println(we);
		ClusAttrType[] classes = m_Schema.getAllAttrUse(ClusAttrType.ATTR_USE_TARGET);
		//int nbClasses = 1;
		//for (int i = 0; i < nbClasses; i++){
		//	ClusAttrType targetclass = classes[i];
		//}
		ClusAttrType targetclass = classes[0];
		RowData data = (RowData) m_Data;
		int nbrows = m_Data.getNbRows();
		for (int i = 0; i < nbrows; i++) {
			DataTuple tuple = data.getTuple(i);
			if (targetclass.getString(tuple).equals("[pos]")) { //tuple is positive
				//System.out.println("Tuple"+tuple.toString()+" Klasse"+targetclass.getString(tuple));
				DataTuple newTuple = tuple.changeWeight(we[0]);
				data.setTuple(newTuple, i);
				//make hash table for mapping classes with their weights?
			}
		}
	}
	//end added by Leander 7-4-2006

	public final void sample(String svalue) {
		ClusSelection sel;
		int nb_rows = m_Data.getNbRows();
		int ps_perc = svalue.indexOf('%');
		if (ps_perc != -1) {
			// FIXME parse string ok?
			double val = Double.parseDouble(svalue.substring(0, ps_perc + 1)) / 100.0;
			if (val < 1.0) {
				sel = new RandomSelection(nb_rows, val);
			} else {
				sel = new OverSample(nb_rows, val);
			}
		} else {
			sel = new RandomSelection(nb_rows, Integer.parseInt(svalue));
		}
		m_Data = (RowData)m_Data.selectFrom(sel);
		int nb_sel = m_Data.getNbRows();
		System.out.println("Sample (" + svalue + ") " + nb_rows + " -> " + nb_sel);
		System.out.println();
	}

	/**
	 * Train the model for given algorithm type.
	 * @param cr Information about this run.
	 * @param clss Type of learning algorithm used.
	 * @throws ClusException
	 * @throws IOException
	 */
	public final void induce(ClusRun cr, ClusInductionAlgorithmType clss) throws ClusException, IOException {
		if (Settings.VERBOSE > 0) {
			System.out.println("Run: " + cr.getIndexString());
			clss.printInfo();
			System.out.println();
		}
		clss.induceAll(cr);
		if (Settings.VERBOSE > 0) {
			System.out.println();
		}
	}

	public final void showTree(String fname) throws ClusException, IOException,
			ClassNotFoundException {
		TreeFrame.showTree(getSettings().getFileAbsolute(fname));
	}

	public final void gui(String lok) throws ClusException, IOException,
			ClassNotFoundException {
		ClusSchema schema = new ClusSchema("Clus");
		ClusStatManager mgr = new ClusStatManager(schema, m_Sett, false);
		TreeFrame.start(mgr, lok);
	}

	public final void postprocModel(ClusModel model, TupleIterator iter,
			ModelProcessorCollection coll) throws IOException, ClusException {
		iter.init();
		ClusSchema mschema = iter.getSchema();
		if (iter.shouldAttach()) {
			System.out.println("Effect of should_attach not implemented in postprocModel");
		}
		coll.initialize(model, mschema);
		DataTuple tuple = iter.readTuple();
		while (tuple != null) {
			model.applyModelProcessors(tuple, coll);
			coll.modelDone();
			tuple = iter.readTuple();
		}
		iter.close();
		coll.terminate(model);
	}

	public final int getNbRows() {
		return m_Data.getNbRows();
	}

	public final ClusData getData() {
		return m_Data;
	}

	public final RowData getRowDataClone() {
		return (RowData) m_Data.cloneData();
	}

	public final MultiScore getMultiScore() {
		return m_Score;
	}

	public final ClusInductionAlgorithm getInduce() {
		return m_Induce;
	}

	public final ClusInductionAlgorithmType getClassifier() {
		return m_Classifier;
	}

	public final ClusStatManager getStatManager() {
		return m_Induce.getStatManager();
	}

	public final MultiScore getScore() {
		return m_Score;
	}

	public final ClusSchema getSchema() {
		return m_Schema;
	}

	public final Settings getSettings() {
		return m_Sett;
	}

	public final ClusSummary getSummary() {
		return m_Summary;
	}

	public final DataPreprocs getPreprocs(boolean single) {
		DataPreprocs pps = new DataPreprocs();
		m_Schema.getPreprocs(pps, single);
		m_Induce.getPreprocs(pps);
		return pps;
	}

	public final void initializeAttributeWeights(ClusData data)	throws IOException, ClusException {
		ClusStatManager mgr = getInduce().getStatManager();
		ClusStatistic allStat = mgr.createStatistic(ClusAttrType.ATTR_USE_ALL);
		data.calcTotalStat(allStat);
		ClusStatistic[] stats = new ClusStatistic[1];
		stats[0] = allStat;
		/*if (!m_Sett.isNullTestFile()) {
			System.out.println("Loading: " + m_Sett.getTestFile());
			updateStatistic(m_Sett.getTestFile(), stats);
		}
		if (!m_Sett.isNullPruneFile()) {
			System.out.println("Loading: " + m_Sett.getPruneFile());
			updateStatistic(m_Sett.getPruneFile(), stats);
		}*/
		mgr.initNormalizationWeights(allStat, data);
		mgr.initClusteringWeights();
		mgr.initDispersionWeights();
		mgr.initHeuristic();
		mgr.initSignifcanceTestingTable();
	}

	public final void preprocess(ClusData data) throws ClusException {
		DataPreprocs pps = getPreprocs(false);
		int nb = pps.getNbPasses();
		for (int i = 0; i < nb; i++) {
			data.preprocess(i, pps);
			pps.done(i);
		}
		if (Debug.HIER_DEBUG) {
			HierMatrixOutput.writeExamples((RowData) data, m_Induce
					.getStatManager().getHier());
		}
	}

	public final void preprocSingle(RowData data) throws ClusException {
		DataPreprocs pps = getPreprocs(true);
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			pps.preprocSingle(tuple);
		}
	}

	public final void preprocess() throws ClusException {
		preprocess(m_Data);
		/*
		 * ClusTarget target = m_Data.getTarget(); target.initTransformation();
		 * if (m_Sett.shouldNormalize()) { System.out.println("Normalizing
		 * data"); target.normalize(); }
		 */
	}

	public final boolean hasTestSet() {
		if (!m_Sett.isNullTestFile())
			return true;
		if (m_Sett.getTestProportion() != 0.0)
			return true;
		if (isxval)
			return true;
		return false;
	}

	public final boolean hasPruneSet() {
		if (!m_Sett.isNullPruneFile())
			return true;
		if (m_Sett.getPruneProportion() != 0.0)
			return true;
		return false;
	}

	public final RowData loadDataFile(String fname) throws IOException,
			ClusException {
		ClusReader reader = new ClusReader(fname, m_Sett);
		if (Settings.VERBOSE > 0)
			System.out.println("Reading: " + fname);
		ARFFFile arff = new ARFFFile(reader);
		// FIXME - test if schema equal
		arff.read(m_Sett); // Read schema, but ignore :-)
		// FIXME - hack with number of rows
		ClusView view = m_Schema.createNormalView();
		RowData data = view.readData(reader, m_Schema);
		reader.close();
		if (Settings.VERBOSE > 0)
			System.out.println("Found " + data.getNbRows() + " rows");
		preprocSingle(data);
		return data;
	}

	public final ClusRun partitionData() throws IOException, ClusException {
		boolean testfile = false;
		boolean writetest = false;
		ClusSelection sel = null;
		if (!m_Sett.isNullTestFile()) {
			testfile = true;
			writetest = true;
		} else {
			double test = m_Sett.getTestProportion();
			if (test != 0.0) {
				int nbtot = m_Data.getNbRows();
				sel = new RandomSelection(nbtot, test);
				writetest = true;
			}
		}
		return partitionData(m_Data, sel, testfile, writetest, m_Summary, 1);
	}

	public final ClusRun partitionData(ClusSelection sel, int idx) throws IOException, ClusException {
		return partitionData(m_Data, sel, false, false, m_Summary, idx);
	}

	public final ClusRun partitionData(ClusData data, ClusSelection sel, boolean testfile, boolean writetest, ClusSummary summary, int idx) throws IOException,	ClusException {
		// cloning the data is done in partitionDataBasic()
		String test_fname = m_Sett.getAppName();
		ClusRun cr = partitionDataBasic(data, sel, summary, idx);
		boolean hasMissing = m_Schema.hasMissing();
		if (testfile) {
			test_fname = m_Sett.getTestFile();
			MyClusInitializer init = new MyClusInitializer();
			TupleIterator iter = new DiskTupleIterator(test_fname, init, getPreprocs(true), m_Sett);
			iter.setShouldAttach(true);
			cr.setTestSet(iter);
		}
		if (writetest) {
			if (m_Sett.isWriteModelIDPredictions()) {
				String tname = FileUtil.removePath(StringUtils.removeSuffix(FileUtil.getName(test_fname), ".arff"));
				ClusModelInfo mi = cr.addModelInfo(ClusModel.ORIGINAL);
				mi.addModelProcessor(ClusModelInfo.TEST_ERR, new NodeIDWriter(tname + ".id", hasMissing, m_Sett));
			}
			if (m_Sett.isWriteTestSetPredictions()) {
				ClusModelInfo allmi = cr.getAllModelsMI();
				String tname = StringUtils.removeSuffix(FileUtil.getName(test_fname), ".arff");
				allmi.addModelProcessor(ClusModelInfo.TEST_ERR, new PredictionWriter(tname + ".pred.arff", m_Sett, getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET)));
			}
		}
		if (m_Sett.isWriteTrainSetPredictions()) {
			String tr_name = m_Sett.getAppName() + ".train." + idx + ".pred.arff";
			ClusModelInfo allmi = cr.getAllModelsMI();
			allmi.addModelProcessor(ClusModelInfo.TRAIN_ERR, new PredictionWriter(tr_name, m_Sett, getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET)));
		}
		if (m_Sett.isWriteModelIDPredictions()) {
			ClusModelInfo mi = cr.addModelInfo(ClusModel.ORIGINAL);
			String id_tr_name = m_Sett.getAppName() + ".train." + idx + ".id";
			mi.addModelProcessor(ClusModelInfo.TRAIN_ERR, new NodeExampleCollector(id_tr_name, hasMissing, m_Sett));
		}
		return cr;
	}

	public final ClusRun partitionDataBasic(RowData train) throws IOException, ClusException {
		ClusSummary summary = new ClusSummary();
		return partitionDataBasic(train, null, null, summary, 1);
	}

	public final ClusRun partitionDataBasic(ClusData data, ClusSelection sel, ClusSummary summary, int idx) throws IOException, ClusException {
		return partitionDataBasic(data, sel, null, summary, idx);
	}

	public final ClusRun partitionDataBasic(ClusData data, ClusSelection sel, ClusData prunefile, ClusSummary summary, int idx) throws IOException, ClusException {
		ClusRun cr = new ClusRun(data.cloneData(), summary);
		if (sel != null) {
			if (sel.changesDistribution()) {
				((RowData) cr.getTrainingSet()).update(sel);
			} else {
				ClusData val = cr.getTrainingSet().select(sel);
				cr.setTestSet(((RowData) val).getIterator());
			}
		}
		int pruning_max = m_Sett.getPruneSetMax();
		double vsb = m_Sett.getPruneProportion();
		if (vsb != 0.0) {
			ClusData train = cr.getTrainingSet();
			int nbtot = train.getNbRows();
			int nbsel = (int)Math.round((double)vsb*nbtot);
			if (nbsel > pruning_max) nbsel = pruning_max;
			RandomSelection prunesel = new RandomSelection(nbtot, nbsel);
			cr.setPruneSet(train.select(prunesel), prunesel);
			if (Settings.VERBOSE > 0) System.out.println("Selecting pruning set: " + nbsel);
		}
		if (!m_Sett.isNullPruneFile()) {
			String prset = m_Sett.getPruneFile();
			if (prunefile != null) {
				cr.setPruneSet(prunefile, null);
			} else {
				ClusData prune = loadDataFile(prset);
				cr.setPruneSet(prune, null);
				if (Settings.VERBOSE > 0)
					System.out.println("Selecting pruning set: " + prset);
			}
		}
		cr.setIndex(idx);
		cr.createTrainIter();
		return cr;
	}

	public final void attachModels(ClusSchema schema, ClusRun cr) throws ClusException {
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModel model = cr.getModel(i);
			if (model != null) schema.attachModel(model);
		}
	}

	public final static double calcModelError(ClusStatManager mgr, RowData data, ClusModel model) throws ClusException, IOException {
		ClusSchema schema = data.getSchema();
		/* create error measure */
		ClusErrorList error = new ClusErrorList();
		NumericAttrType[] num = schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom = schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		TimeSeriesAttrType[] ts = schema.getTimeSeriesAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (nom.length != 0) {
			error.addError(new Accuracy(error, nom));
		} else if (num.length != 0) {
			error.addError(new PearsonCorrelation(error, num));
		} else if (ts.length != 0){
			error.addError(new PearsonCorrelation(error, num));
		}
		/* attach model to given schema */
		schema.attachModel(model);
		/* iterate over tuples and compute error */
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			ClusStatistic pred = model.predictWeighted(tuple);
			error.addExample(tuple, pred);
		}
		/* return the error */
		double err = error.getFirstError().getModelError();
//		System.out.println("Error: "+err);
		return err;
	}

	/** Compute either test or train error */
	public final void calcError(TupleIterator iter, int type, ClusRun cr) throws IOException, ClusException {
		iter.init();
		ClusSchema mschema = iter.getSchema();
		if (iter.shouldAttach()) attachModels(mschema, cr);
		cr.initModelProcessors(type, mschema);
		ModelProcessorCollection allcoll = cr.getAllModelsMI().getAddModelProcessors(type);
		DataTuple tuple = iter.readTuple();
		while (tuple != null) {
			allcoll.exampleUpdate(tuple);
			for (int i = 0; i < cr.getNbModels(); i++) {
				ClusModelInfo mi = cr.getModelInfo(i);
				if (mi != null && mi.getModel() != null) {
					ClusModel model = mi.getModel();
					ClusStatistic pred = model.predictWeighted(tuple);
					ClusErrorList err = mi.getError(type);
					if (err != null) err.addExample(tuple, pred);
					ModelProcessorCollection coll = mi.getModelProcessors(type);
					if (coll != null) {
						if (coll.needsModelUpdate()) {
							model.applyModelProcessors(tuple, coll);
							coll.modelDone();
						}
						coll.exampleUpdate(tuple, pred);
					}
				}
			}
			allcoll.exampleDone();
			tuple = iter.readTuple();
		}
		iter.close();
		cr.termModelProcessors(type);
	}

	public void addModelErrorMeasures(ClusRun cr) {
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModelInfo info = cr.getModelInfo(i);
			// Compute rule-wise error measures
			if (info != null && info.getModel() instanceof ClusRuleSet && m_Sett.isRuleWiseErrors()) {
				ClusRuleSet ruleset = (ClusRuleSet) info.getModel();
				ruleset.setError(info.getTrainingError(), ClusModel.TRAIN);
				ruleset.setError(info.getTestError(), ClusModel.TEST);
				info.addModelProcessor(ClusModelInfo.TRAIN_ERR,	new ClusCalcRuleErrorProc(ClusModel.TRAIN, info.getTrainingError()));
				info.addModelProcessor(ClusModelInfo.TEST_ERR, new ClusCalcRuleErrorProc(ClusModel.TEST, info.getTestError()));
			}
		}
	}

	public final void calcExtraTrainingSetErrors(ClusRun cr) {
		ClusErrorList parent = getStatManager().createExtraError(ClusModelInfo.TRAIN_ERR);
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModelInfo info = cr.getModelInfo(i);
			if (info != null) {
				ClusErrorList parent_cl = parent.getErrorClone();
				parent_cl.compute((RowData)cr.getTrainingSet(), info.getModel());
				info.setExtraError(ClusModelInfo.TRAIN_ERR, parent_cl);
				if (info.getModel() instanceof ClusRuleSet && m_Sett.isRuleWiseErrors()) {
					ClusRuleSet ruleset = (ClusRuleSet) info.getModel();
					for (int j = 0; j < ruleset.getModelSize(); j++) {
						ClusErrorList rule_error = parent.getErrorClone();
						rule_error.compute((RowData)cr.getTrainingSet(), ruleset.getRule(j));
						ruleset.getRule(j).addError(rule_error, ClusModelInfo.TRAIN_ERR);
					}
				}
			}
		}
	}

	public final void calcError(ClusRun cr, ClusSummary summary) throws IOException, ClusException {
		cr.copyAllModelsMIs();
		if (Settings.VERBOSE > 0) System.out.println("Computing training error");
		calcError(cr.getTrainIter(), ClusModelInfo.TRAIN_ERR, cr);
		if (Settings.VERBOSE > 0) System.out.println("Computing testing error");
		TupleIterator tsiter = cr.getTestIter();
		if (tsiter != null) calcError(tsiter, ClusModelInfo.TEST_ERR, cr);
		if (Settings.VERBOSE > 0) System.out.println("Computing validation error");
		if (cr.getPruneSet() != null) calcError(cr.getPruneIter(), ClusModelInfo.VALID_ERR, cr);
		if (summary != null) summary.addSummary(cr);
	}

	public final void out2model(String fname) throws IOException, ClusException {
		String model_name = FileUtil.getName(fname) + ".model";
		ClusTreeReader rdr = new ClusTreeReader();
		ClusNode node = rdr.loadOutTree(fname, m_Schema, "Original Model");
		if (node == null) {
			// If original model not in .out, go with pruned version
			node = rdr.loadOutTree(fname, m_Schema, "Pruned Model");
		}
		if (node == null) {
			throw new ClusException("Unable to find original tree in .out file");
		}
		ClusRun cr = partitionData();
		ConstraintDFInduce induce = new ConstraintDFInduce(m_Induce);
		ClusNode orig = induce.fillInInduce(cr, node, getScore());
		orig.numberTree();
		PruneTree pruner = induce.getStatManager().getTreePruner(cr.getPruneSet());
		pruner.setTrainingData((RowData) cr.getTrainingSet());
		ClusNode pruned = (ClusNode) orig.cloneTree();
		pruner.prune(pruned);
		pruned.numberTree();
		System.out.println();
		System.out.println("Tree read from .out:");
		orig.printTree();
		System.out.println();
		if (rdr.getLineAfterTree() != null) {
			System.out.println("First line after tree: '"	+ rdr.getLineAfterTree() + "'");
			System.out.println();
		}
		ClusModelCollectionIO io = new ClusModelCollectionIO();
		ClusModelInfo pruned_info = new ClusModelInfo("Pruned");
		pruned_info.setModel(pruned);
		io.addModel(pruned_info);
		ClusModelInfo orig_info = new ClusModelInfo("Original");
		orig_info.setModel(orig);
		io.addModel(orig_info);
		io.save(model_name);
	}


	/** Normalize the data so that most of the variables are within [-1,1] range. Using Zenko, 2007 suggestion.
	 * TODO also for nominal attributes
	 * @throws IOException
	 * @throws ClusException
	 */
	public final RowData returnNormalizedData() throws IOException, ClusException {
		RowData data = m_Data;

		NumericAttrType[] numTypes = getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_ALL);
		NominalAttrType[] nomTypes = getSchema().getNominalAttrUse(ClusAttrType.ATTR_USE_ALL);

		// Variance and mean for numeric types.
		double[] variance = new double[numTypes.length];
		double[] mean = new double[numTypes.length];
		//** Some of the values are not valid. These should not be used for computing variance etc. *//
		double[] nbOfValidValues = new double[numTypes.length];

		// Computing the means
		for (int iRow = 0; iRow < data.getNbRows(); iRow++) {
			DataTuple tuple = data.getTuple(iRow);

			for (int jNumAttrib = 0; jNumAttrib < numTypes.length; jNumAttrib++) {
				double value = numTypes[jNumAttrib].getNumeric(tuple);
				if (!Double.isNaN(value) && !Double.isInfinite(value)) {// Value not given
					mean[jNumAttrib] += value;
					nbOfValidValues[jNumAttrib]++;
				}
			}
		}

		// Divide with the number of examples
		for (int jNumAttrib = 0; jNumAttrib < numTypes.length; jNumAttrib++) {
			mean[jNumAttrib] /= nbOfValidValues[jNumAttrib];
		}

		// Computing the variances
		for (int iRow = 0; iRow < data.getNbRows(); iRow++) {
			DataTuple tuple = data.getTuple(iRow);

			for (int jNumAttrib = 0; jNumAttrib < numTypes.length; jNumAttrib++) {
				double value = numTypes[jNumAttrib].getNumeric(tuple);
				if (!Double.isNaN(value) && !Double.isInfinite(value)) // Value not given
					variance[jNumAttrib] += Math.pow(value - mean[jNumAttrib], 2.0);
			}
		}

		// Divide with the number of examples
		for (int jNumAttrib = 0; jNumAttrib < numTypes.length; jNumAttrib++) {
			variance[jNumAttrib] /= nbOfValidValues[jNumAttrib];
		}

		ArrayList<DataTuple> normalized = new ArrayList<DataTuple>();
		// Make the alterations
		for (int iRow = 0; iRow < data.getNbRows(); iRow++) {
			DataTuple tuple = data.getTuple(iRow).deepCloneTuple();

//			if (getSettings().getNormalizeData() == Settings.NORMALIZE_DATA_ALL) {
//				for (int jNomAttrib = 0; jNomAttrib < tuple.m_Ints.length; jNomAttrib++) {
//				NominalAttrType type = nomTypes[jNomAttrib];
//				int value = type.getNominal(tuple);
//
////				value -= median[jNomAttrib];
////				value /= Math.sqrt(variance[jNomAttrib]);
//				type.setNominal(tuple, value);
//				}
//			}

			for (int jNumAttrib = 0; jNumAttrib < tuple.m_Doubles.length; jNumAttrib++) {
				NumericAttrType type = numTypes[jNumAttrib];

				double value = type.getNumeric(tuple);
				if (!Double.isNaN(value) && !Double.isInfinite(value)) {// Value not given
					value -= mean[jNumAttrib]; // Putting the mean to 0
					// Suggestion by Zenko 2007  p. 22 4*standard deviation. However we are also moving the data to
					// zero mean. Thus we take only 2 * standard deviation.
					value /= 2*Math.sqrt(variance[jNumAttrib]);
					value += 0.5; // Putting the mean to 0.5

					// After this transformation the mean should be about 0.5 and variance about 0.25
					// (and standard deviation 0.5). Thus 95% of values should be between [0,1]
				}
				type.setNumeric(tuple, value);

			}
			normalized.add(tuple);
		}


		RowData normalized_data = new RowData(normalized, getSchema());
		System.out.println("Normalized number of examples: "+normalized_data.getNbRows());


		return normalized_data;
	}

	/** Does not necessarily do the same as returnNormalizedData. However, stores
	 * the normalized data to file. Kept (and not modified) for compatibility purposes.*/
	public final void normalizeDataAndWriteToFile() throws IOException, ClusException {
		RowData data = (RowData)m_Data;
		CombStat cmb = (CombStat)getStatManager().getTrainSetStat(ClusAttrType.ATTR_USE_ALL);
		RegressionStat rstat = cmb.getRegressionStat();
		NumericAttrType[] numtypes = getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_ALL);
		int tcnt = 0;
		for (int j = 0; j < numtypes.length; j++) {
			NumericAttrType type = numtypes[j];
			if (type.isTarget()) {
				tcnt++;
				NumberFormat format = ClusFormat.THREE_AFTER_DOT;
				System.out.print(StringUtils.printStr("T"+tcnt+" ", 5));
				System.out.print(StringUtils.printStr(type.getName()+" ", 30));
				System.out.print(StringUtils.printStr(format.format(rstat.getMean(j)), 10));
				System.out.print(StringUtils.printStr(format.format(Math.sqrt(rstat.getVariance(j))), 10));
				System.out.println();
			}
		}
		ArrayList normalized = new ArrayList();
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i).deepCloneTuple();
			for (int j = 0; j < numtypes.length; j++) {
				NumericAttrType type = numtypes[j];
				if (type.isTarget()) {
					double value = type.getNumeric(tuple);
					value -= rstat.getMean(j);
					value /= Math.sqrt(rstat.getVariance(j));
					type.setNumeric(tuple, value);
				}
			}
			normalized.add(tuple);
		}
		RowData normalized_data = new RowData(normalized, getSchema());
		System.out.println("Size: "+normalized_data.getNbRows());

		String fname = m_Sett.getFileAbsolute(FileUtil.getName(m_Sett.getDataFile())+"_norm.arff");
		ARFFFile.writeArff(fname, normalized_data);
	}

	public final void testModel(String fname) throws IOException,	ClusException, ClassNotFoundException {
		ClusModelCollectionIO io = ClusModelCollectionIO.load(fname);
		ClusNode res = (ClusNode) io.getModel("Original");
		String test_name = m_Sett.getAppName() + ".test";
		ClusOutput out = new ClusOutput(test_name, m_Schema, m_Sett);
		ClusRun cr = partitionData();
		getStatManager().updateStatistics(res);
		getSchema().attachModel(res);
		getClassifier().pruneAll(cr);
		getClassifier().postProcess(cr);
		calcError(cr, null);
		out.writeHeader();
		out.writeOutput(cr, true, true);
		out.close();
	}


	public final void showModel(String fname) throws IOException,	ClusException, ClassNotFoundException {
		ClusModelCollectionIO io = ClusModelCollectionIO.load(fname);
		ClusNode res = (ClusNode) io.getModel("Pruned");
		System.out.println("Tree read from .model:");
		System.out.println();
		res.inverseTests();
		res.printTree();
	}

	public final void saveModels(ClusRun models, ClusModelCollectionIO io) throws IOException {
		if (getInduce().isModelWriter()) {
			getInduce().writeModel(io);
		}
		int pos = 0;
		for (int i = models.getNbModels() - 1; i >= 0; i--) {
			ClusModelInfo info = models.getModelInfo(i);
			if (info != null && info.shouldSave()) {
				io.insertModel(pos++, info);
			}
		}
	}

	public ClusRun train(RowData train) throws ClusException, IOException {
		m_Induce = getClassifier().createInduce(train.getSchema(), m_Sett, m_CmdLine);
		ClusRun cr = partitionDataBasic(train);
		m_Induce.initialize();
		initializeAttributeWeights(m_Data);
		m_Induce.initializeHeuristic();
		getStatManager().computeTrainSetStat((RowData)cr.getTrainingSet());
		induce(cr, getClassifier());
		return cr;
	}

	public final void singleRun(ClusInductionAlgorithmType clss) throws IOException, ClusException {
		ClusModelCollectionIO io = new ClusModelCollectionIO();
		m_Summary.setTotalRuns(1);
		ClusRun run = singleRunMain(clss, null);
		saveModels(run, io);
//		io.save(getSettings().getFileAbsolute(m_Sett.getAppName() + ".model"));
		if (ClusEnsembleInduce.isOptimized()&&(m_Sett.getNbBaggingSets().getVectorLength() > 1))
			io.save(getSettings().getFileAbsolute(m_Sett.getAppName()+"_"+ ClusEnsembleInduce.getMaxNbBags()+"_.model"));
		else
			io.save(getSettings().getFileAbsolute(m_Sett.getAppName() + ".model"));

	}

	/* Run the prediction algorithm session once: train and possibly test and exit.
	 */
	public final ClusRun singleRunMain(ClusInductionAlgorithmType clss, ClusSummary summ)	throws IOException, ClusException {
//		ClusOutput output = new ClusOutput(m_Sett.getAppName() + ".out", m_Schema, m_Sett);
		ClusOutput output;
		if (ClusEnsembleInduce.isOptimized() && (m_Sett.getNbBaggingSets().getVectorLength() > 1))
			output = new ClusOutput(m_Sett.getAppName() +"_"+ClusEnsembleInduce.getMaxNbBags()+"_.out", m_Schema, m_Sett);
		else
			output = new ClusOutput(m_Sett.getAppName() + ".out", m_Schema, m_Sett);

		ClusRun cr = partitionData();
		// Compute statistic on training data
		getStatManager().computeTrainSetStat((RowData)cr.getTrainingSet());
		// Used for exporting data to CN2 and Orange formats
		/* ARFFFile.writeCN2Data("train-all.exs", (RowData)cr.getTrainingSet());
		   ARFFFile.writeOrangeData("train-all.tab", (RowData)cr.getTrainingSet());
	       ARFFFile.writeFRSHead("header.pl", (RowData)cr.getTrainingSet(), true);
	       ARFFFile.writeFRSData("train-all.pl", (RowData)cr.getTrainingSet(), true); */
		// Induce model
		induce(cr, clss);
		if (summ == null) {
			// E.g., rule-wise error measures
			addModelErrorMeasures(cr);
		}
		// Calc error
		calcError(cr, null);
		if (summ != null) {
			for (int i = 0; i < cr.getNbModels(); i++) {
				ClusModelInfo info = cr.getModelInfo(i);
				ClusModelInfo summ_info = summ.getModelInfo(i);
				ClusErrorList test_err = summ_info.getTestError();
				info.setTestError(test_err);
			}
		}
		calcExtraTrainingSetErrors(cr);
		output.writeHeader();
		output.writeOutput(cr, true, true);
		output.close();
		clss.saveInformation(m_Sett.getAppName());
		return cr;
	}

	public final XValMainSelection getXValSelection() throws IOException, ClusException {
		if (m_Sett.isLOOXVal()) {
			return new XValRandomSelection(m_Data.getNbRows(), m_Data.getNbRows());
		} else if (m_Sett.isNullXValFile()) {
			return m_Schema.getXValSelection(m_Data);
		} else {
			return XValDataSelection.readFoldsFile(m_Sett.getXValFile(), m_Data.getNbRows());
		}
	}

	public final void combineAllFoldRuns(ClusInductionAlgorithmType clss) throws IOException, ClusException {
		ClusOutput output = new ClusOutput(m_Sett.getAppName() + ".xval", m_Schema, m_Sett);
		output.writeHeader();
		XValMainSelection sel = getXValSelection();
		m_Summary.setTotalRuns(sel.getNbFolds());
		for (int fold = 0; fold < sel.getNbFolds(); fold++) {
			String dat_fname = "folds/" + m_Sett.getAppName() + ".fold."+fold;
			System.out.println("Reading: "+dat_fname);
			ObjectLoadStream strm = new ObjectLoadStream(new FileInputStream(dat_fname));
			try {
				m_Summary.addSummary((ClusRun)strm.readObject());
				output.print((String)strm.readObject());
			} catch (ClassNotFoundException e) {}
			strm.close();
		}
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream(m_Sett.getAppName() + ".test.pred")));
		for (int fold = 0; fold < sel.getNbFolds(); fold++) {
			String pw_fname = "folds/" + m_Sett.getAppName() + ".test.pred."+fold;
			System.out.println("Combining: "+pw_fname);
			LineNumberReader rdr = new LineNumberReader(new InputStreamReader(new FileInputStream(pw_fname)));
			String line = rdr.readLine();
			if (fold != 0) {
				// Only copy header from fold 0
				while (line != null && !line.equals("@DATA")) {
					line = rdr.readLine();
				}
				line = rdr.readLine();
			}
			while (line != null) {
				wrt.println(line);
				line = rdr.readLine();
			}
			rdr.close();
		}
		wrt.close();
		output.writeSummary(m_Summary);
		output.close();
		/* Cross-validation now includes a single run */
		ClusRandom.initialize(m_Sett);
		singleRunMain(clss, m_Summary);
	}

	public final void oneFoldRun(ClusInductionAlgorithmType clss, int fold) throws IOException, ClusException {
		if (fold == 0) {
			combineAllFoldRuns(clss);
		} else {
			fold = fold - 1;
			FileUtil.mkdir("folds");
			ClusOutput output = new ClusOutput(m_Schema, m_Sett);
			ClusStatistic target = getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
			String pw_fname = "folds/" + m_Sett.getAppName() + ".test.pred."+fold;
			PredictionWriter wrt = new PredictionWriter(pw_fname, m_Sett, target);
			wrt.globalInitialize(m_Schema);
			XValMainSelection sel = getXValSelection();
			ClusModelCollectionIO io = new ClusModelCollectionIO();
			m_Summary.setTotalRuns(sel.getNbFolds());
			ClusRun cr = doOneFold(fold, clss, sel, io, wrt, output,null);
			wrt.close();
			output.close();
			// Write summary of this run to a file
			// cr.deleteDataAndModels();
			cr.deleteData();
			String dat_fname = "folds/" + m_Sett.getAppName() + ".fold."+fold;
			ObjectSaveStream strm = new ObjectSaveStream(new FileOutputStream(dat_fname));
			strm.writeObject(cr);
			strm.writeObject(output.getString());
			strm.close();
		}
	}

	public final ClusRun doOneFold(int fold, ClusInductionAlgorithmType clss, XValMainSelection sel, ClusModelCollectionIO io, PredictionWriter wrt, ClusOutput output, ClusErrorOutput errOutput) throws IOException, ClusException {
		XValSelection msel = new XValSelection(sel, fold);
		ClusRun cr = partitionData(msel, fold + 1);
		// Create statistic for the training set
		getStatManager().computeTrainSetStat((RowData)cr.getTrainingSet());
		if (wrt != null) {
			wrt.println("! Fold = " + fold);
			cr.getAllModelsMI().addModelProcessor(ClusModelInfo.TEST_ERR, wrt);
		}
		// Used for exporting data to CN2 and Orange formats
		/* ARFFFile.writeCN2Data("test-"+i+".exs", cr.getTestSet());
		   ARFFFile.writeCN2Data("train-"+i+".exs", (RowData)cr.getTrainingSet());
		   ARFFFile.writeOrangeData("test-"+fold+".tab", cr.getTestSet());
		   ARFFFile.writeOrangeData("train-"+fold+".tab", (RowData)cr.getTrainingSet()); */
		// Induce tree
		induce(cr, clss);
		if (m_Sett.isRuleWiseErrors()) {
			addModelErrorMeasures(cr);
		}
		// Calc error
		calcError(cr, m_Summary);
		if (errOutput != null) {
			// Write errors to ARFF file
			errOutput.writeOutput(cr, false, false, getStatManager().getClusteringWeights().m_Weights);
		}
		if (m_Sett.isOutputFoldModels()) {
			// Write output to file and also store in .model file
			output.writeOutput(cr, false);
			if (!Settings.m_EnsembleMode) {
				ClusModelInfo mi = cr.getModelInfo(ClusModel.PRUNED);
				if (mi != null) io.addModel(mi);
			}
		} else {
			output.writeBrief(cr);
		}
		return cr;
	}


	public final void xvalRun(ClusInductionAlgorithmType clss) throws IOException, ClusException {
		ClusErrorOutput errFileOutput = null;
		if (getSettings().isWriteErrorFile()) {
			errFileOutput = new ClusErrorOutput(m_Sett.getAppName() + ".err", m_Schema,m_Sett);
			errFileOutput.writeHeader();
		}
		PredictionWriter testPredWriter = null;
		if (getSettings().isWriteTestSetPredictions()) {
			ClusStatistic target = getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
			testPredWriter = new PredictionWriter(m_Sett.getAppName() + ".test.pred.arff", m_Sett, target);
			testPredWriter.globalInitialize(m_Schema);
		}
		/* Perform cross-validation */
		ClusOutput output = new ClusOutput(m_Sett.getAppName() + ".xval", m_Schema, m_Sett);
		output.writeHeader();
		XValMainSelection sel = getXValSelection();
		ClusModelCollectionIO io = new ClusModelCollectionIO();
		for (int fold = 0; fold < sel.getNbFolds(); fold++) {
			doOneFold(fold, clss, sel, io, testPredWriter, output, errFileOutput);
		}
		output.writeSummary(m_Summary);
		output.close();
		if (testPredWriter != null) testPredWriter.close();
		if (errFileOutput != null) errFileOutput.close();
		/* Cross-validation now includes a single run */
		ClusRandom.initialize(m_Sett);
		ClusRun run = singleRunMain(clss, m_Summary);
		saveModels(run, io);
		io.save(getSettings().getFileAbsolute(m_Sett.getAppName() + ".model"));
	}



	public final void baggingRun(ClusInductionAlgorithmType clss) throws IOException, ClusException {
		ClusOutput output = new ClusOutput(m_Sett.getAppName() + ".bag", m_Schema, m_Sett);
		output.writeHeader();
		ClusStatistic target = getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
		PredictionWriter wrt = new PredictionWriter(m_Sett.getAppName()	+ ".test.pred.arff", m_Sett, target);
		wrt.globalInitialize(m_Schema);
		int nbsets = m_Sett.getBaggingSets();
		int nbrows = m_Data.getNbRows();
		for (int i = 0; i < nbsets; i++) {
			BaggingSelection msel = new BaggingSelection(nbrows);
			ClusRun cr = partitionData(msel, i + 1);
			ClusModelInfo mi = cr.getModelInfo(ClusModel.PRUNED);
			mi.addModelProcessor(ClusModelInfo.TEST_ERR, wrt);
			induce(cr, clss); // Induce tree
			calcError(cr, m_Summary); // Calc error
			if (m_Sett.isOutputFoldModels())
				output.writeOutput(cr, false); // Write output to file
		}
		wrt.close();
		output.writeSummary(m_Summary);
		output.close();
	}
	/* clss is the object on which the induce methode is
	 * called :in our case it is a ClusDecisionTree
	 *
	 * Modify to have more than one model as an output !*/
	public final void exhaustiveRun(ClusInductionAlgorithmType clss) throws IOException, ClusException {
		ClusOutput output = new ClusOutput(m_Sett.getAppName() + ".all", m_Schema, m_Sett);
		output.writeHeader();
		ClusRun cr = partitionData();
		induce(cr, clss); // Induce model
		output.writeOutput(cr, false); // Write output to file
		output.writeSummary(m_Summary);
		output.close();
	}

	private class MyClusInitializer implements ClusSchemaInitializer {

		public void initSchema(ClusSchema schema) throws ClusException, IOException {
			if (getSettings().getHeuristic() == Settings.HEURISTIC_SSPD) {
				schema.addAttrType(new IntegerAttrType("SSPD"));
			}
			schema.setTarget(new IntervalCollection(m_Sett.getTarget()));
			schema.setDisabled(new IntervalCollection(m_Sett.getDisabled()));
			schema.setClustering(new IntervalCollection(m_Sett.getClustering()));
			schema.setDescriptive(new IntervalCollection(m_Sett.getDescriptive()));
			schema.setKey(new IntervalCollection(m_Sett.getKey()));
			schema.updateAttributeUse();
			schema.initializeFrom(m_Schema);
		}
	}

	public void writeTargets() throws ClusException, IOException {
		ClassHierarchy hier = getStatManager().getHier();
		if (hier != null) {
			hier.writeTargets((RowData)m_Data, m_Schema, m_Sett.getAppName());
		}
	}

	public void showInfo() throws ClusException, IOException {
		RowData data = (RowData) m_Data;
		System.out.println("Name            #Rows      #Missing  #Nominal #Numeric #Target  #Classes");
		System.out.print(StringUtils.printStr(m_Sett.getAppName(), 16));
		System.out.print(StringUtils.printInt(data.getNbRows(), 11));
		//double perc = -1; // (double)m_Schema.getTotalInputNbMissing()/data.getNbRows()/m_Schema.getNbNormalAttr()*100.0;
		double perc = (double)m_Schema.getTotalInputNbMissing()/data.getNbRows()/m_Schema.getNbDescriptiveAttributes()*100.0;
		System.out.print(StringUtils.printStr(ClusFormat.TWO_AFTER_DOT.format(perc) + "%", 10));
		System.out.print(StringUtils.printInt(m_Schema.getNbNominalDescriptiveAttributes(), 9));
		System.out.print(StringUtils.printInt(m_Schema.getNbNumericDescriptiveAttributes(), 9));
		System.out.print(StringUtils.printInt(m_Schema.getNbAllAttrUse(ClusAttrType.ATTR_USE_TARGET), 9));
		NominalAttrType[] tarnom = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (tarnom != null && tarnom.length >= 1) {
			if (tarnom.length == 1) System.out.println(tarnom[0].getNbValues());
			else System.out.println("M:"+tarnom.length);
		} else {
			System.out.println("(num)");
		}
		System.out.println();
		m_Schema.showDebug();
		if (getStatManager().hasClusteringStat()) {
			ClusStatistic[] stats = new ClusStatistic[2];
			stats[0] = getStatManager().createClusteringStat();
			stats[1] = getStatManager().createStatistic(ClusAttrType.ATTR_USE_ALL);
			m_Data.calcTotalStats(stats);
			if (!m_Sett.isNullTestFile()) {
				System.out.println("Loading: " + m_Sett.getTestFile());
				updateStatistic(m_Sett.getTestFile(), stats);
			}
			if (!m_Sett.isNullPruneFile()) {
				System.out.println("Loading: " + m_Sett.getPruneFile());
				updateStatistic(m_Sett.getPruneFile(), stats);
			}
			ClusStatistic.calcMeans(stats);
			MyFile statf = new MyFile(getSettings().getAppName() + ".distr");
			statf.log("** Target:");
			stats[0].printDistribution(statf.getWriter());
			statf.log("** All:");
			stats[1].printDistribution(statf.getWriter());
			statf.close();
		}
	}

	public void updateStatistic(String fname, ClusStatistic[] stats) throws ClusException, IOException {
		MyClusInitializer init = new MyClusInitializer();
		TupleIterator iter = new DiskTupleIterator(fname, init,	getPreprocs(true), m_Sett);
		iter.init();
		DataTuple tuple = iter.readTuple();
		while (tuple != null) {
			for (int i = 0; i < stats.length; i++) {
				stats[i].updateWeighted(tuple, 1.0);
			}
			tuple = iter.readTuple();
		}
		iter.close();
	}

	public void setFolds(int folds) {
		m_Sett.setXValFolds(folds);
	}

	public void showDebug() {
		m_Schema.showDebug();
	}

	public void showHelp() {
		ClusOutput.showHelp();
	}

	public String[] getOptionArgs() {
		return OPTION_ARGS;
	}

	public int[] getOptionArgArities() {
		return OPTION_ARITIES;
	}

	public int getNbMainArgs() {
		return 1;
	}

	public String getAppName() {
		return m_Sett.getAppName();
	}

	public static void main(String[] args) {
		try {
			ClusOutput.printHeader();
			Clus clus = new Clus();
			Settings sett = clus.getSettings();
			CMDLineArgs cargs = new CMDLineArgs(clus);
			cargs.process(args);
			if (cargs.hasOption("copying")) {
				ClusOutput.printGPL();
				System.exit(0);
			} else if (cargs.getNbMainArgs() == 0) {
				clus.showHelp();
				System.out.println();
				System.out.println("Expected main argument");
				System.exit(0);
			}
			if (cargs.allOK()) {
				sett.setDate(new Date());
				sett.setAppName(cargs.getMainArg(0));
				clus.initSettings(cargs);
				ClusInductionAlgorithmType clss = null;

				/** There are two groups of command line parameters of type -<parameter>.
				 *  From both of them at most one can be used.
				 *  The first group is the learning method.
				 *  Options are knn, knnTree, rules, weka (for Weka workbench),
				 *  tuneftest, tunesize, beam (beam search induction tree), exhaustive,
				 *  sit (inductive transfer learning), forest (ensemble trees).
				 *  If the parameter is not given, a single decision tree is used.
				 *  TODO What do the other parameter values mean? (e.g. tuneftest, exhaustive)
				 *  TODO There should be a command line help for these. For example with -help.
				 */
				if (cargs.hasOption("knn")) {
					clus.getSettings().setSectionKNNEnabled(true);
					clus.getSettings().setSectionTreeEnabled(false);
					clss = new KNNClassifier(clus);
				} else if (cargs.hasOption("knnTree")) { //new
					clus.getSettings().setSectionKNNTEnabled(true);
					clss = new KNNTreeClassifier(clus);
				} else if (cargs.hasOption("rules")) {
					clus.getSettings().setSectionBeamEnabled(true);
					// clus.getSettings().setSectionTreeEnabled(false);
					clss = new ClusRuleClassifier(clus);
				} else if (cargs.hasOption("weka")) {
					// clss = new ClusWekaClassifier(clus, cargs.getOptionValue("weka"));
				} else if (cargs.hasOption("tuneftest")) {
					clss = new ClusDecisionTree(clus);
					clss = new CDTTuneFTest(clss);
				} else if (cargs.hasOption("tunesize")) {
					clss = new ClusDecisionTree(clus);
					clss = new CDTuneSizeConstrPruning(clss);
				} else if (cargs.hasOption("beam")) {
					clus.getSettings().setSectionBeamEnabled(true);
					clss = sett.isFastBS() ? new ClusFastBeamSearch(clus) : new ClusBeamSearch(clus);
				} else if (cargs.hasOption("exhaustive")) {
					// new part added by elisa 1/08/2006
					clus.getSettings().setSectionExhaustiveEnabled(true);
					clss = new ClusExhaustiveDFSearch(clus);
				} else if (cargs.hasOption("sit")) {
					//new part by beau
					clss = new ClusDecisionTree(clus);
					clss = new ClusSITDecisionTree(clss);
				} else if (cargs.hasOption("forest")) {
					sett.setEnsembleMode(true);
					clss = new ClusEnsembleClassifier(clus);
					if (sett.getFTestArray().isVector()) clss = new CDTTuneFTest(clss, sett.getFTestArray().getDoubleVector());
				} else {
					clss = new ClusDecisionTree(clus);
					if (sett.getFTestArray().isVector()) clss = new CDTTuneFTest(clss, sett.getFTestArray().getDoubleVector()); 
				}

				/** The second group of command line parameters is for miscellaneous action.
				 *  The options are corrmatrix, info, writetargets, out2model, test, normalize, debug,
				 *  xval (test error estimation via K-fold cross validation), fold, bag (originally bagging, may not be used)
				 *  show, gui, tseries
				 *  TODO What do these mean?
				 */
				if (cargs.hasOption("corrmatrix")) {
					clus.initialize(cargs, clss);
					CorrelationMatrixComputer cmp = new CorrelationMatrixComputer();
					cmp.compute((RowData) clus.m_Data);
					cmp.printMatrixTeX();
				} else if (cargs.hasOption("info")) {
					clus.initialize(cargs, clss);
					clus.showInfo();
				} else if (cargs.hasOption("writetargets")) {
					clus.initialize(cargs, clss);
					clus.writeTargets();
				} else if (cargs.hasOption("out2model")) {
					clus.initialize(cargs, clss);
					clus.out2model(cargs.getOptionValue("out2model"));
				} else if (cargs.hasOption("test")) {
					clus.initialize(cargs, clss);
					clus.testModel(cargs.getOptionValue("test"));
				} else if (cargs.hasOption("normalize")) {
					clus.initialize(cargs, clss);
					clus.normalizeDataAndWriteToFile();
				} else if (cargs.hasOption("debug")) {
					clus.initialize(cargs, clss);
					clus.showDebug();
				} else if (cargs.hasOption("xval")) { // cross validation
					clus.isxval = true;
					clus.initialize(cargs, clss);
					clus.xvalRun(clss);
				} else if (cargs.hasOption("fold")) {
					clus.isxval = true;
					clus.initialize(cargs, clss);
					clus.oneFoldRun(clss, cargs.getOptionInteger("fold"));
				} else if (cargs.hasOption("bag")) {
					clus.isxval = true;
					clus.initialize(cargs, clss);
					clus.baggingRun(clss);
				} else if (cargs.hasOption("show")) {
					// clus.showModel(clus.getAppName());
					clus.showTree(clus.getAppName());
				} else if (cargs.hasOption("gui")) {
					clus.gui(cargs.getMainArg(0));
				} else if (cargs.hasOption("tseries")) {
					clus.getSettings().setSectionTimeSeriesEnabled(true);
					clus.initialize(cargs, clss);
					clus.singleRun(clss);
				} else {
					clus.initialize(cargs, clss);
					clus.singleRun(clss);
				}
			}
			if (Debug.debug == 1) ClusStat.show();
			DebugFile.close();
		} catch (ClusException e) {
			System.err.println();
			System.err.println("Error: " + e);
		} catch (IllegalArgumentException e) {
			System.err.println();
			System.err.println("Error: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.err.println();
			System.err.println("File not found: " + e);
		} catch (IOException e) {
			System.err.println();
			System.err.println("IO Error: " + e);
		} catch (ClassNotFoundException e) {
			System.err.println();
			System.err.println("Class not found" + e);
		}
	}
}
