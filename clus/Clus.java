package clus;

import clus.tools.debug.Debug;

import clus.gui.*;
import clus.algo.tdidt.*;

import jeans.io.*;
import jeans.util.*;
import jeans.util.cmdline.*;
import jeans.resource.*;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.data.rows.*;
import clus.error.*;
import clus.error.multiscore.*;
import clus.statistic.*;
import clus.algo.induce.*;
import clus.selection.*;
import clus.ext.*;
import clus.ext.optxval.*;
import clus.ext.ootind.*;
import clus.ext.hierarchical.*;
import clus.ext.beamsearch.*;
import clus.ext.constraint.*;
import clus.pruning.*;
import clus.heuristic.*;

import clus.model.processor.*;
import clus.model.modelio.*;

import clus.algo.kNN.*;
import clus.algo.rules.*;

import clus.weka.*;

public class Clus implements CMDLineArgsProvider {

	public final static boolean m_UseHier = true;

	public final static String[] OPTION_ARGS = { "xval", "oxval", "target",
			"disable", "silent", "lwise", "c45", "info", "sample", "debug",
			"tuneftest", "load", "soxval", "bag", "obag", "show", "knn",
			"knnTree", "beam", "gui", "fillin", "rules", "weka", "corrmatrix",
			"tunesize", "out2model", "test", "normalize", "tseries", "writetargets"};

	public final static int[] OPTION_ARITIES = { 0, 0, 1, 1, 0, 0, 0, 0, 1, 0,
			0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0 };

	protected Settings m_Sett = new Settings();

	protected ClusSummary m_Summary = new ClusSummary();

	protected ClusSchema m_Schema;

	protected MultiScore m_Score;

	protected ClusInduce m_Induce;

	protected ClusExtension m_Ext;

	protected ClusData m_Data;

	protected Date m_StartDate = new Date();

	protected boolean isxval = false;

	public final void initialize(CMDLineArgs cargs, ClusClassifier clss)
			throws IOException, ClusException {
		// Load resource info lib
		boolean test = m_Sett.getResourceInfoLoaded() == Settings.RESOURCE_INFO_LOAD_TEST;
		ResourceInfo.loadLibrary(test);
		// Load settings file
		ARFFFile arff = null;
		System.out.println("Loading '" + m_Sett.getAppName() + "'");
		ClusRandom.initialize(m_Sett);
		// Load header
		//		String hdr_ext = cargs.hasOption("c45") ? ".names" : ".arff";
		ClusReader reader = new ClusReader(m_Sett.getDataFile(), m_Sett);
		System.out.println();
		if (cargs.hasOption("c45")) {
			System.out.println("Reading C45 .names/.data");
			//			C45File c45f = new C45File(reader);
			//			m_Schema = c45f.read();
		} else {
			System.out.println("Reading ARFF Header");
			arff = new ARFFFile(reader);
			m_Schema = arff.read();
		}
		// Count rows and move to data segment
		m_Schema.setSettings(m_Sett);
		m_Schema.setTestSet(-1); /* Support ID for XVAL attribute later on? */
		System.out.println();
		System.out.println("Reading CSV Data");
		m_Schema.setNbRows(reader.countRows());
		System.out.println("Found " + m_Schema.getNbRows() + " rows");
		if (arff != null)
			arff.skipTillData();
		// Updata schema based on settings
		m_Sett.updateTarget(m_Schema);
		m_Schema.setTarget(new IntervalCollection(m_Sett.getTarget()));
		m_Schema.setDisabled(new IntervalCollection(m_Sett.getDisabled()));
		m_Schema.setClustering(new IntervalCollection(m_Sett.getClustering()));
		m_Schema.setDescriptive(new IntervalCollection(m_Sett.getDescriptive()));
		m_Schema.setKey(new IntervalCollection(m_Sett.getKey()));
		m_Schema.updateAttributeUse();
		m_Sett.setTarget(m_Schema.getTarget().toString());
		m_Sett.setDisabled(m_Schema.getDisabled().toString());
		m_Sett.setClustering(m_Schema.getClustering().toString());
		m_Sett.setDescriptive(m_Schema.getDescriptive().toString());
		// FIXME - move somewhere else - MyInitializer?
		if (Settings.HIER_FLAT.getValue()) {
			ClusAttrType attr = m_Schema
					.getAttrType(m_Schema.getNbAttributes() - 1);
			ClusAttrType adda = new FlatClassesAttrType("FLAT",
					(ClassesAttrType) attr);
			adda.setStatus(ClusAttrType.STATUS_TARGET);
			m_Schema.addAttrType(adda);
		}
		// Create induce
		m_Induce = m_Ext.createInduce(m_Schema, m_Sett, cargs);
		clss.initializeInduce(m_Induce, cargs);
		// Load data from file
		m_Data = m_Induce.createData();
		m_Data.resize(m_Schema.getNbRows());
		ClusView view = m_Data.createNormalView(m_Schema);
		view.readData(reader, m_Schema);
		reader.close();
		// Preprocess and initialize induce
		m_Sett.update(m_Schema);
		// Multiscore?
		if (Settings.IS_MULTISCORE) {
			if (m_Schema.isRegression())
				m_Score = new MultiScore(m_Schema, m_Sett);
			else
				m_Sett.disableMultiScore();
		}
		// Preprocess() should become for m_Induce.initialize()
		// -> e.g., for hierarchical multi-classification
		preprocess();
		m_Induce.initialize();
		initializeAttributeWeights(m_Data);
		m_Induce.initializeHeuristic();
		loadConstraintFile();
		initializeSummary(clss);
		System.out.println();
		removeMissingTarget();
		
		//added 7-4-2006
		//initializePosExampleWeights();
		//end added 7-4-2006
		
		// Initialize F-Test table
		FTest.initializeFTable(Settings.FTEST_LEVEL);
		// Sample data
		if (cargs.hasOption("sample")) {
			String svalue = cargs.getOptionValue("sample");
			sample(svalue);
		}
		System.out.println("Has missing values: " + m_Schema.hasMissing());
		ClusStat.m_LoadedMemory = ResourceInfo.getMemory();
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

	public final void initializeSummary(ClusClassifier clss) {
		ClusStatManager mgr = m_Induce.getStatManager();
		ClusErrorParent error = mgr.createErrorMeasure(m_Score);
		m_Summary.setStatManager(mgr);
		if (!Settings.HIER_FLAT.getValue())
			m_Summary.setTrainError(error);
		if (hasTestSet())
			m_Summary.setTestError(error);
		if (hasPruneSet())
			m_Summary.setValidationError(error);
		clss.initializeSummary(m_Summary);
	}

	public final void removeMissingTarget() {
		RowData data = (RowData) m_Data;
		int nbrows = m_Data.getNbRows();
		TargetSchema schema = m_Schema.getTargetSchema();
		BitMapSelection sel = new BitMapSelection(nbrows);
		for (int i = 0; i < nbrows; i++) {
			DataTuple tuple = data.getTuple(i);
			if (!schema.isMissingTarget(tuple))
				sel.select(i);
		}
		if (sel.getNbSelected() != nbrows) {
			System.out.println("Tuples with missing target: "
					+ (nbrows - sel.getNbSelected()));
			m_Data = m_Data.selectFrom(sel);
			m_Schema.setNbRows(m_Data.getNbRows());
		}
	}
	
	//added by Leander 7-4-2006
	
	public final void initializePosExampleWeights() {
		
		double we = m_Sett.getPosExampleWeight();
		// add the weight to all positive examples (in DataTuple)
		RowData data = (RowData) m_Data;
		int nbrows = m_Data.getNbRows();
		for (int i = 0; i < nbrows; i++) {
			DataTuple tuple = data.getTuple(i);
			if (false) { //tuple is positive
				tuple = tuple.changeWeight(we);
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
		m_Data = m_Data.selectFrom(sel);
		int nb_sel = m_Data.getNbRows();
		m_Schema.setNbRows(nb_sel);
		System.out.println("Sample (" + svalue + ") " + nb_rows + " -> " + nb_sel);
		System.out.println();
	}

	public final ClusNode pruneTree(ClusNode orig, ClusData pruneset,
			ClusData trainset) throws ClusException {
		ClusNode pruned = (ClusNode) orig.cloneTree();
		PruneTree pruner = m_Induce.getStatManager().getTreePruner(pruneset);
		pruner.setTrainingData((RowData) trainset);
		pruner.prune(pruned);
		return pruned;
	}

	public final static ClusNode pruneToRoot(ClusNode orig) {
		ClusNode pruned = (ClusNode) orig.cloneNode();
		pruned.makeLeaf();
		return pruned;
	}
	
	public void convertToRules(ClusRun cr, int tree, int rules, boolean check) throws ClusException, IOException {
		ClusNode tree_root = (ClusNode)cr.getModel(tree);
		ClusRulesFromTree rft = new ClusRulesFromTree(true);
		ClusRuleSet rule_set = null;
		if (getSettings().computeCompactness()) {
			rule_set = rft.constructRules(cr, tree_root, getStatManager());
		} else {
			rule_set = rft.constructRules(tree_root, getStatManager());
		}
		ClusModelInfo rules_info = cr.getModelInfo(rules);
		rules_info.setModel(rule_set);
		rules_info.setStatManager(getStatManager());
		// Be sure that we have found the correct model info
		if (check && !rules_info.getName().equals("Rules: "+cr.getModelName(tree))) {
			throw new ClusException("Rules and tree names do not match: '"+rules_info.getName()+"' and '"+cr.getModelName(tree)+"'");
		}
	}

	public final void storeAndPruneModel(ClusRun cr, ClusNode orig)	throws ClusException, IOException {
		orig.numberTree();
		// Postprocess tree on training set
		ClusModelInfo orig_info = cr.getModelInfo(ClusModels.ORIGINAL);
		orig_info.setStatManager(getStatManager());
		orig_info.setModel(orig);
		ClusNode defmod = pruneToRoot(orig);
		ClusModelInfo def_info = cr.getModelInfo(ClusModels.DEFAULT);
		def_info.setStatManager(getStatManager());
		def_info.setModel(defmod);		
		long start_time = ResourceInfo.getTime();		
		PruneTree pruner = m_Induce.getStatManager().getTreePruner(cr.getPruneSet());
		pruner.setTrainingData((RowData) cr.getTrainingSet());
		for (int i = 0; i < pruner.getNbResults(); i++) {
			ClusNode pruned = (ClusNode) orig.cloneTree();
			pruner.prune(i, pruned);
			pruned.numberTree();
			ClusModelInfo pruned_info = cr.getModelInfo(ClusModels.PRUNED + i);
			pruned_info.setModel(pruned);
			pruned_info.setStatManager(getStatManager());
		}
		cr.setPruneTime(ResourceInfo.getTime()-start_time);		
		if (getSettings().rulesFromTree() == Settings.CONVERT_RULES_PRUNED) {
			convertToRules(cr, ClusModels.PRUNED, ClusModels.RULES, false);
		}
		if (getSettings().rulesFromTree() == Settings.CONVERT_RULES_ALL) {
			for (int i = 0; i < ClusModels.RULES; i++) {
				if (i != ClusModels.DEFAULT) {
					convertToRules(cr, i, ClusModels.RULES+i-1, true);					
				}
			}
		}
	}

	public final void induce(ClusRun cr, ClusClassifier clss)
			throws ClusException, IOException {
		if (Settings.VERBOSE > 0) {
			System.out.println("Run: " + cr.getIndexString());
			clss.printInfo();
			System.out.println();
		}
		clss.induce(cr);
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
		return m_Schema.getNbRows();
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

	public final ClusInduce getInduce() {
		return m_Induce;
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

	public final TargetSchema getTargetSchema() {
		return m_Schema.getTargetSchema();
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

	public final void initializeAttributeWeights(ClusData data)
			throws IOException, ClusException {
		ClusStatManager mgr = getInduce().getStatManager();
		ClusStatistic allStat = mgr.createStatistic(ClusAttrType.ATTR_USE_ALL);
		data.calcTotalStat(allStat);
		ClusStatistic[] stats = new ClusStatistic[1];
		stats[0] = allStat;
		if (!m_Sett.isNullTestFile()) {
			System.out.println("Loading: " + m_Sett.getTestFile());
			updateStatistic(m_Sett.getTestFile(), stats);
		}
		if (!m_Sett.isNullPruneFile()) {
			System.out.println("Loading: " + m_Sett.getPruneFile());
			updateStatistic(m_Sett.getPruneFile(), stats);
		}
		mgr.initNormalizationWeights(allStat);
		mgr.initClusteringWeights();
		mgr.initCompactnessWeights();
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
		if (m_Sett.getPruneProportion() != 0.0)
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
		arff.read(); // Read schema, but ignore :-)
		int nbrows = reader.countRows();
		if (Settings.VERBOSE > 0)
			System.out.println("Found " + nbrows + " rows");
		arff.skipTillData();
		RowData data = (RowData) m_Induce.createData();
		data.resize(nbrows);
		// FIXME - hack with number of rows
		int oldrows = m_Schema.getNbRows();
		m_Schema.setNbRows(nbrows);
		ClusView view = data.createNormalView(m_Schema);
		view.readData(reader, m_Schema);
		reader.close();
		m_Schema.setNbRows(oldrows);
		preprocSingle(data);
		return data;
	}

	public final ClusRun partitionData() throws IOException, ClusException {
		boolean testfile = false;
		ClusSelection sel = null;
		if (!m_Sett.isNullTestFile()) {
			testfile = true;
		} else {
			double test = m_Sett.getTestProportion();
			if (test != 0.0) {
				int nbtot = m_Data.getNbRows();
				sel = new RandomSelection(nbtot, test);
			}
		}
		return partitionData(m_Data, sel, testfile, m_Summary, 1);
	}

	public final ClusRun partitionData(ClusSelection sel, int idx) throws IOException, ClusException {
		return partitionData(m_Data, sel, false, m_Summary, idx);
	}

	public final ClusRun partitionData(ClusData data, ClusSelection sel, boolean testfile, ClusSummary summary, int idx) throws IOException,	ClusException {
		ClusRun cr = partitionDataBasic(data, sel, summary, idx);
		boolean hasMissing = m_Schema.hasMissing();
		if (testfile) {
			String fname = m_Sett.getTestFile();
			MyClusInitializer init = new MyClusInitializer();
			TupleIterator iter = new DiskTupleIterator(fname, init,	getPreprocs(true), m_Sett);
			iter.setShouldAttach(true);
			cr.setTestSet(iter);
			if (m_Sett.isWriteModelIDPredictions()) {
				String tname = FileUtil.getName(fname);
				ClusModelInfo mi = cr.getModelInfo(ClusModels.PRUNED);
				mi.addModelProcessor(ClusModelInfo.TEST_ERR, new NodeIDWriter(tname + ".id", hasMissing, m_Sett));
			}
			if (m_Sett.isWriteTestSetPredictions()) {
				String tname = FileUtil.getName(fname);
				ClusModelInfo mi = cr.getModelInfo(ClusModels.PRUNED);
				mi.addModelProcessor(ClusModelInfo.TEST_ERR,
						new PredictionWriter(tname + ".pred", m_Sett, getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET)));
			}
		}
		if (m_Sett.isWriteTrainSetPredictions()) {
			ClusModelInfo mi = cr.getModelInfo(ClusModels.PRUNED);
			String tr_name = m_Sett.getAppName() + ".train." + idx + ".pred";
			mi.addModelProcessor(ClusModelInfo.TRAIN_ERR,
					new PredictionWriter(tr_name, m_Sett, getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET)));
		}
		if (m_Sett.isWriteModelIDPredictions()) {
			ClusModelInfo mi = cr.getModelInfo(ClusModels.PRUNED);
			String id_tr_name = m_Sett.getAppName() + ".train." + idx + ".id";
			mi.addModelProcessor(ClusModelInfo.TRAIN_ERR, new NodeExampleCollector(id_tr_name, hasMissing, m_Sett));
		}
		if (Settings.HIER_FLAT.getValue()) {
			ClassHierarchy hier = m_Induce.getStatManager().getNormalHier();
			CalcStatisticProcessor proc = new CalcStatisticProcessor(new WAHNDStatistic(hier));
			cr.getModelInfo(ClusModels.DEFAULT).addModelProcessor(ClusModelInfo.TRAIN_ERR, proc);
			cr.getModelInfo(ClusModels.ORIGINAL).addModelProcessor(ClusModelInfo.TRAIN_ERR, proc);
			cr.getModelInfo(ClusModels.PRUNED).addModelProcessor(ClusModelInfo.TRAIN_ERR, proc);
		}
		return cr;
	}

	public final ClusRun partitionDataBasic(ClusData data, ClusSelection sel,	ClusSummary summary, int idx) throws IOException, ClusException {
		return partitionDataBasic(data, sel, null, summary, idx);
	}

	public final ClusRun partitionDataBasic(ClusData data, ClusSelection sel,	ClusData prunefile, ClusSummary summary, int idx)	throws IOException, ClusException {
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
			if (Settings.VERBOSE > 0)	System.out.println("Selecting pruning set: " + nbsel);
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

	public final void calcError(TupleIterator iter, int type, ClusRun cr)	throws IOException, ClusException {
		iter.init();
		ClusSchema mschema = iter.getSchema();
		if (iter.shouldAttach()) attachModels(mschema, cr);
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModelInfo mi = cr.getModelInfo(i);
			mi.initModelProcessors(type, mschema);
		}
		DataTuple tuple = iter.readTuple();
		while (tuple != null) {
			for (int i = 0; i < cr.getNbModels(); i++) {
				ClusModelInfo mi = cr.getModelInfo(i);
				ClusModel model = mi.getModel();
				if (model != null) {
					ClusStatistic pred = model.predictWeighted(tuple);
					ClusErrorParent err = mi.getError(type);
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
			tuple = iter.readTuple();
		}
		iter.close();
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModelInfo mi = cr.getModelInfo(i);
			mi.termModelProcessors(type);
		}
	}

	public void addModelErrorMeasures(ClusRun cr) {
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModelInfo info = cr.getModelInfo(i);
			// Compute rule-wise error measures
			if (info.getModel() instanceof ClusRuleSet && m_Sett.isRuleWiseErrors()) {
				ClusRuleSet ruleset = (ClusRuleSet) info.getModel();
				ruleset.setError(info.getTrainingError(), ClusModel.TRAIN);
				ruleset.setError(info.getTestError(), ClusModel.TEST);
				info.addModelProcessor(ClusModelInfo.TRAIN_ERR,	new ClusCalcRuleErrorProc(ClusModel.TRAIN, info.getTrainingError()));
				info.addModelProcessor(ClusModelInfo.TEST_ERR, new ClusCalcRuleErrorProc(ClusModel.TEST, info.getTestError()));
			}
		}
	}

	public final void calcError(ClusRun cr, ClusSummary summary) throws IOException, ClusException {
		calcError(cr.getTrainIter(), ClusModelInfo.TRAIN_ERR, cr);
		TupleIterator tsiter = cr.getTestIter();
		if (tsiter != null) calcError(tsiter, ClusModelInfo.TEST_ERR, cr);
		if (cr.getPruneSet() != null)	calcError(cr.getPruneIter(), ClusModelInfo.VALID_ERR, cr);
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

	public final void normalizeData() throws IOException {
		RowData data = (RowData)m_Data;
		CombStat cmb = (CombStat)getStatManager().getGlobalStat();
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
		storeAndPruneModel(cr, res);		
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

	public final void singleRun(ClusClassifier clss) throws IOException, ClusException {
		ClusModelCollectionIO io = new ClusModelCollectionIO();
		m_Summary.setTotalRuns(1);
		ClusRun run = singleRunMain(clss, null);
		saveModels(run, io);
		io.save(getSettings().getFileAbsolute(m_Sett.getAppName() + ".model"));
	}

	public final ClusRun singleRunMain(ClusClassifier clss, ClusSummary summ)	throws IOException, ClusException {
		ClusOutput output = new ClusOutput(m_Sett.getAppName() + ".out", m_Schema, m_Sett);
		ClusRun cr = partitionData();
		induce(cr, clss); // Induce model
		if (summ == null) {
			// E.g., rule-wise error measures
			addModelErrorMeasures(cr);
		}
		calcError(cr, null); // Calc error
		if (summ != null) {
			for (int i = 0; i < cr.getNbModels(); i++) {
				ClusModelInfo info = cr.getModelInfo(i);
				ClusModelInfo summ_info = summ.getModelInfo(i);
				ClusErrorParent test_err = summ_info.getTestError();
				info.setTestError(test_err);
			}
		}
		output.writeHeader();
		output.writeOutput(cr, true, true);
		output.close();
		clss.saveInformation(m_Sett.getAppName());
		return cr;
	}

	public final XValMainSelection getXValSelection() throws IOException,
			ClusException {
		if (m_Sett.isNullXValFile()) {
			return m_Schema.getXValSelection(m_Data);
		} else {
			return XValDataSelection.readFoldsFile(m_Sett.getXValFile(), m_Data
					.getNbRows());
		}
	}

	public final void xvalRun(ClusClassifier clss) throws IOException, ClusException {
		ClusOutput output = new ClusOutput(m_Sett.getAppName() + ".xval", m_Schema, m_Sett);
		output.writeHeader();
		ClusStatistic target = getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
		PredictionWriter wrt = new PredictionWriter(m_Sett.getAppName()	+ ".test.pred", m_Sett, target);
		wrt.globalInitialize(m_Schema);
		XValMainSelection sel = getXValSelection();
		ClusModelCollectionIO io = new ClusModelCollectionIO();
		m_Summary.setTotalRuns(sel.getNbFolds());
		for (int i = 0; i < sel.getNbFolds(); i++) {
			wrt.getWrt().println("! Fold = " + i);
			XValSelection msel = new XValSelection(sel, i);
			ClusRun cr = partitionData(msel, i + 1);
			ClusModelInfo mi = cr.getModelInfo(ClusModels.PRUNED);
			mi.addModelProcessor(ClusModelInfo.TEST_ERR, wrt);
			induce(cr, clss);			// Induce tree
      // TODO: Check if this is ok!
      if (m_Sett.isRuleWiseErrors()) {
        addModelErrorMeasures(cr);
      }
      calcError(cr, m_Summary);	// Calc error
			if (m_Sett.isOutputFoldModels())	{
				// Write output to file and also store in .model file
				output.writeOutput(cr, false);
				mi.setName("Fold: " + (i + 1));
				io.addModel(mi);
			} else {
				output.writeBrief(cr);
			}
		}
		wrt.close();
		output.writeSummary(m_Summary);
		output.close();
		/* Cross-validation now includes a single run */
		ClusRandom.initialize(m_Sett);
		ClusRun run = singleRunMain(clss, m_Summary);
		saveModels(run, io);
		io.save(getSettings().getFileAbsolute(m_Sett.getAppName() + ".model"));
	}

	public final void baggingRun(ClusClassifier clss) throws IOException, ClusException {
		ClusOutput output = new ClusOutput(m_Sett.getAppName() + ".bag", m_Schema, m_Sett);
		output.writeHeader();
		ClusStatistic target = getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
		PredictionWriter wrt = new PredictionWriter(m_Sett.getAppName()	+ ".test.pred", m_Sett, target);
		wrt.globalInitialize(m_Schema);
		int nbsets = m_Sett.getBaggingSets();
		int nbrows = m_Data.getNbRows();
		for (int i = 0; i < nbsets; i++) {
			BaggingSelection msel = new BaggingSelection(nbrows);
			ClusRun cr = partitionData(msel, i + 1);
			ClusModelInfo mi = cr.getModelInfo(ClusModels.PRUNED);
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

	private class MyClusInitializer implements ClusSchemaInitializer {

		public void initSchema(ClusSchema schema) throws ClusException {
			if (getSettings().getHeuristic() == Settings.HEURISTIC_SSPD) {
				schema.addAttrType(new IntegerAttrType("SSPD"));
			}
			if (Settings.HIER_FLAT.getValue()) {
				ClusAttrType attr = schema
						.getAttrType(schema.getNbAttributes() - 1);
				ClusAttrType adda = new FlatClassesAttrType("FLAT",
						(ClassesAttrType) attr);
				adda.setStatus(ClusAttrType.STATUS_TARGET);
				schema.addAttrType(adda);
			}
			schema.setTarget(new IntervalCollection(m_Sett.getTarget()));
			schema.setDisabled(new IntervalCollection(m_Sett.getDisabled()));
			schema
					.setClustering(new IntervalCollection(m_Sett
							.getClustering()));
			schema.setDescriptive(new IntervalCollection(m_Sett
					.getDescriptive()));
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
		System.out.println("Name            #Rows      #Missing  #Nominal #Numeric #Target    #Classes");
		System.out.print(StringUtils.printStr(m_Sett.getAppName(), 16));
		System.out.print(StringUtils.printInt(data.getNbRows(), 11));
		double perc = -1; // (double)m_Schema.getTotalInputNbMissing()/data.getNbRows()/m_Schema.getNbNormalAttr()*100.0;
		System.out.print(StringUtils.printStr(ClusFormat.TWO_AFTER_DOT
				.format(perc)
				+ "%", 10));
		System.out.print(StringUtils.printInt(m_Schema
				.getNbNominalDescriptiveAttributes(), 9));
		System.out.print(StringUtils.printInt(m_Schema
				.getNbNumericDescriptiveAttributes(), 9));
		System.out.print(StringUtils.printInt(m_Schema
				.getNbAllAttrUse(ClusAttrType.ATTR_USE_TARGET), 5));
		/*
		 * if (schema.getNbTarNom() > 0) { System.out.print("(nom) ");
		 * NominalAttrType ctype = schema.getTarNom(0);
		 * System.out.println(ctype.getNbValues()); } else {
		 * System.out.println("(num)"); }
		 */
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

	public void updateStatistic(String fname, ClusStatistic[] stats)
			throws ClusException, IOException {
		MyClusInitializer init = new MyClusInitializer();
		TupleIterator iter = new DiskTupleIterator(fname, init,
				getPreprocs(true), m_Sett);
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

	public void setExtension(ClusExtension ext) {
		m_Ext = ext;
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
		if (Debug.debug == 1)
			ClusStat.m_InitialMemory = ResourceInfo.getMemorySize();
		try {
			ClusOutput.printHeader();
			Clus clus = new Clus();
			Settings sett = clus.getSettings();
			CMDLineArgs cargs = new CMDLineArgs(clus);
			cargs.process(args);
			if (cargs.allOK()) {
				sett.setDate(new Date());
				sett.setAppName(cargs.getMainArg(0));
				clus.initSettings(cargs);
				clus.setExtension(new DefaultExtension());
				ClusClassifier clss = null;
				if (cargs.hasOption("knn")) {
					clus.getSettings().setSectionKNNEnabled(true);
					clss = new KNNClassifier(clus);
				} else if (cargs.hasOption("knnTree")) { //new
					clus.getSettings().setSectionKNNTEnabled(true);
					clss = new KNNTreeClassifier(clus);
				} else if (cargs.hasOption("rules")) {
					clus.getSettings().setSectionBeamEnabled(true);
					clss = new ClusRuleClassifier(clus);
				} else if (cargs.hasOption("weka")) {
					clss = new ClusWekaClassifier(clus, cargs
							.getOptionValue("weka"));
				} else {
					clss = new ClusDecisionTree(clus);
				}
				if (cargs.hasOption("tuneftest")) {
					clss = new CDTTuneFTest(clss);
				} else if (cargs.hasOption("tunesize")) {
					clss = new CDTuneSizeConstrPruning(clss);
				}
				if (cargs.hasOption("beam")) {
					clus.getSettings().setSectionBeamEnabled(true);
					ClusBeamSearch search = sett.isFastBS() ? new ClusFastBeamSearch(
							clus)
							: new ClusBeamSearch(clus);
					clus.setExtension(search);
				}
				
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
					clus.normalizeData();
				} else if (cargs.hasOption("debug")) {
					clus.initialize(cargs, clss);
					clus.showDebug();
				} else if (cargs.hasOption("xval")) {
					clus.isxval = true;
					clus.initialize(cargs, clss);
					clus.xvalRun(clss);
				} else if (cargs.hasOption("bag")) {
					clus.isxval = true;
					clus.initialize(cargs, clss);
					clus.baggingRun(clss);
				} else if (cargs.hasOption("oxval")) {
					OptXVal optx = new OptXVal(clus);
					clus.setExtension(optx);
					clus.initialize(cargs, clss);
					//		    optx.xvalRun(clss);
				} else if (cargs.hasOption("soxval")) {
					OOTInd optx = new OOTInd(clus);
					clus.setExtension(optx);
					clus.initialize(cargs, clss);
					//		    optx.xvalRun(clss);
				} else if (cargs.hasOption("obag")) {
					OOTInd optx = new OOTInd(clus);
					clus.setExtension(optx);
					clus.initialize(cargs, clss);
					//		    optx.baggingRun(clss);
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
			if (Debug.debug == 1)
				ClusStat.show();
		} catch (ClusException e) {
			System.err.println("Error: " + e);
		} catch (IllegalArgumentException e) {
			System.err.println("Error: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + e);
		} catch (IOException e) {
			System.err.println("IO Error: " + e);
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found" + e);
		}
	}
}
