package clus.main;

import jeans.io.ini.*;
import jeans.util.cmdline.*;
import jeans.util.*;

import java.io.*;
import java.util.*;

import clus.heuristic.*;
import clus.data.type.*;
import clus.ext.hierarchical.*;

public class Settings implements Serializable {
	
	public final static long serialVersionUID = 0;
	
	public final static String[] HEURISTICS = 
	{"Default", "ReducedError", "Gain", "SSPD", "MEstimate"};
	
	public final static int HEURISTIC_DEFAULT = 0;
	public final static int HEURISTIC_REDUCED_ERROR = 1;
	public final static int HEURISTIC_GAIN = 2;  
	public final static int HEURISTIC_SSPD = 3;    
	public final static int HEURISTIC_MESTIMATE = 4;
	
	public final static String[] PRUNING_METHODS = 
	{"Default", "None", "C4.5", "M5", "ReducedErrorVSB", "Garofalakis"};
	
	public final static int PRUNING_METHOD_DEFAULT = 0;
	public final static int PRUNING_METHOD_NONE = 1;	
	public final static int PRUNING_METHOD_C45 = 2;
	public final static int PRUNING_METHOD_M5 = 3;
	public final static int PRUNING_METHOD_REDERR_VSB = 4;
	public final static int PRUNING_METHOD_GAROFALAKIS = 5;
	
	public final static String[] HIERMODES = 
	{"TDWEuclid", "TDAbsWEuclid", "XtAXSetDist", "XtAXSetDistDiscrete"};
	
	public final static int HIERMODE_TREE_DIST_WEUCLID = 0;
	public final static int HIERMODE_TREE_DIST_ABS_WEUCLID = 1;	
	public final static int HIERMODE_XTAX_SET_DIST = 2;	
	public final static int HIERMODE_XTAX_SET_DIST_DISCRETE = 3;
		
	public final static long SERIAL_VERSION_ID = 1L;
	
	public final static String NONE = "None";
	
	public static Date m_Date;
	
	public static int FTEST_LEVEL;
	public static double FTEST_VALUE;
	public static boolean GAIN_RATIO;
	public static double MINIMAL_WEIGHT;
	public static boolean IS_MULTISCORE;
	public static double TESTSET_PROPORTION;
	public static int TESTSET_ID;
	public static int XVAL_FOLDS;
	public static String XVAL_FILE;
	public static int XVAL_ID;
	
	public static int BEAM_WIDTH;
	public static double SIZE_PENALTY;
	
	public static String m_AppName;
	public static String m_DirName;
	
	public static boolean SHOW_UNKNOWN_FREQ;
	public static boolean BINARY_SPLIT;
	public static boolean SHOW_BRANCH_FREQ;
	public static boolean SHOW_XVAL_FOREST;
	public static boolean XVAL_OVERLAP = true;
	
	public static boolean ONE_NOMINAL = true;
	public static int VERBOSE = 1;
	public static boolean EXACT_TIME = false;
	
	public static INIFileDouble HIER_W_PARAM;
	public static INIFileBool HIER_SAVE;
	public static INIFileBool HIER_LOAD;
	public static INIFileBool HIER_FLAT;
	public static INIFileInt TREE_MAX_DEPTH;	
	public INIFileNominal m_HierMode;
	
	public static INIFileString m_PruneFile;
	
	INIFileSection m_SectionHierarchical;
	public static INIFileBool HIER_CONT_PROTOTYPE;
	public static INIFileBool HIER_USE_ABUNDANCES;
	public static INIFileBool HIER_NODE_ABUNDANCES;
	public INIFileDouble m_HierPruneInSig;
	
	protected INIFileInt m_Verbose;	
	protected INIFile m_Ini = new INIFile();
	protected INIFileString m_Target;
	protected INIFileString m_Disabled;
	protected INIFileString m_Key;
	protected INIFileBool m_Normalize;
	protected INIFileString m_RandomSeed;
	protected INIFileBool m_GainRatio;
	protected static INIFileDouble m_FTest;
	protected INIFileDouble m_MinW;
	protected INIFileDouble m_PruneProp;
	protected INIFileString m_TestPropOrAttr;
	protected INIFileString m_XValFolds;
	protected INIFileInt m_SetsData;
	protected INIFileBool m_OutFoldErr;
	protected INIFileBool m_OutFoldModels;
	protected INIFileBool m_OutTrainErr;
	protected INIFileBool m_ShowForest;
	protected INIFileBool m_ShowBrFreq;
	protected INIFileBool m_ShowUnknown;
	protected INIFileBool m_RulesFromTree;
	protected INIFileBool m_BinarySplit;
	protected INIFileNominal m_PruningMethod;
	protected INIFileString m_MultiScore;
	protected INIFileString m_DataFile;
	protected INIFileString m_TestFile;
	protected INIFileString m_HierSep;
	protected INIFileNominal m_Heuristic;
	protected INIFileDouble m_MEstimate;
	protected INIFileString m_TuneFolds;

	protected INIFileString m_Weights;
	
	protected INIFileString m_SyntacticConstrFile;
	protected static INIFileInt m_MaxSizeConstr;
	protected INIFileInt m_MaxErrorConstr;	

	protected INIFileSection m_SectionBeam;
	protected INIFileDouble m_SizePenalty;
	protected INIFileInt m_BeamWidth;
	protected INIFileInt m_BeamBestN;
	protected INIFileInt m_TreeMaxSize;
	protected INIFileNominal m_BeamAttrHeuristic;
	protected static INIFileBool m_FastBS;
	protected static INIFileBool m_BeamPostPrune;
	protected INIFileBool m_BMRemoveEqualHeur;
	
	protected INIFileBool m_OrderedRules;
	
	INIFileSection m_SectionKNN;	
	public static INIFileInt kNN_k;
	public static INIFileString kNN_vectDist;
	public static INIFileBool kNN_distWeighted;
	public static INIFileBool kNN_normalized;
	public static INIFileBool kNN_attrWeighted;
	
	INIFileSection m_SectionKNNT;	
	public static INIFileInt kNNT_k;
	public static INIFileString kNNT_vectDist;
	public static INIFileBool kNNT_distWeighted;
	public static INIFileBool kNNT_normalized;
	public static INIFileBool kNNT_attrWeighted;
	
	public void create() {		
		INIFileSection settings = new INIFileSection("General");
		settings.addNode(m_RandomSeed = new INIFileString("RandomSeed", "0"));
		settings.addNode(m_Verbose = new INIFileInt("Verbose", 1));
		settings.addNode(m_XValFolds = new INIFileString("XValNumberFolds", "10"));
		
		INIFileSection data = new INIFileSection("Data");
		data.addNode(m_DataFile = new INIFileString("File", NONE));
		data.addNode(m_TestPropOrAttr = new INIFileString("TestProportion", "0"));
		data.addNode(m_PruneProp = new INIFileDouble("PruneProportion"));		
		data.addNode(m_TestFile = new INIFileString("TestFile", NONE));
		data.addNode(m_PruneFile = new INIFileString("PruneFile", NONE));		
		
		INIFileSection attrs = new INIFileSection("Attributes");
		attrs.addNode(m_Target = new INIFileString("Target", NONE));
		attrs.addNode(m_Disabled = new INIFileString("Disable", NONE));
		attrs.addNode(m_Key = new INIFileString("Key", NONE));
		attrs.addNode(m_Weights = new INIFileString("Weights", "Normalize"));
		
		INIFileSection numeric = new INIFileSection("Numeric");
		numeric.addNode(m_FTest = new INIFileDouble("FTest", 1.0));
		numeric.addNode(m_Normalize = new INIFileBool("Normalize", true));
		numeric.addNode(m_MultiScore = new INIFileString("MultiScore", NONE));
		
		INIFileSection nominal = new INIFileSection("Nominal");
		nominal.addNode(m_GainRatio = new INIFileBool("GainRatio"));		
		nominal.addNode(m_MEstimate = new INIFileDouble("MEstimate", 1.0));

		INIFileSection model = new INIFileSection("Model");
		model.addNode(m_MinW = new INIFileDouble("MinimalWeight", 2.0));		
		model.addNode(m_TuneFolds = new INIFileString("ParamTuneNumberFolds", "10"));		
		
		INIFileSection tree = new INIFileSection("Tree");
		tree.addNode(m_Heuristic = new INIFileNominal("Heuristic", HEURISTICS, 0));		
		tree.addNode(TREE_MAX_DEPTH = new INIFileInt("MaxDepth", -1));
		tree.addNode(m_BinarySplit = new INIFileBool("BinarySplit", true));
		tree.addNode(m_PruningMethod = new INIFileNominal("PruningMethod", PRUNING_METHODS, 0));
		tree.addNode(m_RulesFromTree = new INIFileBool("ConvertToRules", false));
				
		INIFileSection rules = new INIFileSection("Rules");
		rules.addNode(m_OrderedRules = new INIFileBool("Ordered", true));		
				
		INIFileSection constr = new INIFileSection("Constraints");
		constr.addNode(m_SyntacticConstrFile = new INIFileString("Syntactic", NONE));
		constr.addNode(m_MaxSizeConstr = new INIFileInt("MaxSize", -1));
		constr.addNode(m_MaxErrorConstr = new INIFileInt("MaxError", -1));		
		
		m_SectionHierarchical = new INIFileSection("Hierarchical");
		m_SectionHierarchical.addNode(HIER_W_PARAM = new INIFileDouble("WParam", 0.75));
		m_SectionHierarchical.addNode(m_HierSep = new INIFileString("HSeparator", "."));
		m_SectionHierarchical.addNode(HIER_LOAD = new INIFileBool("HierLoad", false));
		m_SectionHierarchical.addNode(HIER_SAVE = new INIFileBool("HierSave", false));
		m_SectionHierarchical.addNode(HIER_FLAT = new INIFileBool("HierFlat", false));
		m_SectionHierarchical.addNode(m_HierPruneInSig = new INIFileDouble("PruneInSig", 0.0));
		m_SectionHierarchical.addNode(HIER_CONT_PROTOTYPE = new INIFileBool("ContinueProto", true));
		m_SectionHierarchical.addNode(HIER_USE_ABUNDANCES = new INIFileBool("UseAbundances", false));
		m_SectionHierarchical.addNode(HIER_NODE_ABUNDANCES = new INIFileBool("NodeAbundances", false));
		m_SectionHierarchical.addNode(m_HierMode = new INIFileNominal("Mode", HIERMODES, 0));
		m_SectionHierarchical.setEnabled(false);
		
		INIFileSection output = new INIFileSection("Output");
		output.addNode(m_OutFoldModels = new INIFileBool("AllFoldModels", true));
		output.addNode(m_OutFoldErr = new INIFileBool("AllFoldErrors", false));
		output.addNode(m_OutTrainErr = new INIFileBool("TrainErrors", true));
		output.addNode(m_ShowUnknown = new INIFileBool("UnknownFrequency", false));
		output.addNode(m_ShowBrFreq = new INIFileBool("BranchFrequency", false));
		
		m_SectionKNN = new INIFileSection("kNN");
		m_SectionKNN.addNode(kNN_k = new INIFileInt("k",3));
		m_SectionKNN.addNode(kNN_vectDist = new INIFileString("VectorDistance","Euclidian"));
		m_SectionKNN.addNode(kNN_distWeighted = new INIFileBool("DistanceWeighted",false));
		m_SectionKNN.addNode(kNN_normalized = new INIFileBool("Normalizing",true));
		m_SectionKNN.addNode(kNN_attrWeighted = new INIFileBool("AttributeWeighted",false));
		m_SectionKNN.setEnabled(false);
		
		m_SectionKNNT = new INIFileSection("kNNTree");
		m_SectionKNNT.addNode(kNNT_k = new INIFileInt("k",3));
		m_SectionKNNT.addNode(kNNT_vectDist = new INIFileString("VectorDistance","Euclidian"));
		m_SectionKNNT.addNode(kNNT_distWeighted = new INIFileBool("DistanceWeighted",false));
		m_SectionKNNT.addNode(kNNT_normalized = new INIFileBool("Normalizing",true));
		m_SectionKNNT.addNode(kNNT_attrWeighted = new INIFileBool("AttributeWeighted",false));
		m_SectionKNNT.setEnabled(false);
		
		m_SectionBeam = new INIFileSection("Beam");
		m_SectionBeam.addNode(m_SizePenalty = new INIFileDouble("SizePenalty", 0.1));
		m_SectionBeam.addNode(m_BeamWidth = new INIFileInt("BeamWidth", 10));
		m_SectionBeam.addNode(m_BeamBestN = new INIFileInt("BeamBestN", 5));
		m_SectionBeam.addNode(m_TreeMaxSize = new INIFileInt("MaxSize", -1));
		m_SectionBeam.addNode(m_BeamAttrHeuristic = new INIFileNominal("AttributeHeuristic", HEURISTICS, 0));
		m_SectionBeam.addNode(m_FastBS = new INIFileBool("FastSearch", true));
		m_SectionBeam.addNode(m_BeamPostPrune = new INIFileBool("PostPrune", false));
		m_SectionBeam.addNode(m_BMRemoveEqualHeur = new INIFileBool("RemoveEqualHeur", false));
		m_SectionBeam.setEnabled(false);

		INIFileSection exper = new INIFileSection("Experimental");
		exper.addNode(m_SetsData = new INIFileInt("NumberBags", 25));
		exper.addNode(m_ShowForest = new INIFileBool("XValForest", false));
		exper.setEnabled(false);
		
		m_Ini.addNode(settings);
		m_Ini.addNode(data);
		m_Ini.addNode(attrs);
		m_Ini.addNode(model);		
		m_Ini.addNode(tree);		
		m_Ini.addNode(rules);
		m_Ini.addNode(output);
		m_Ini.addNode(numeric);
		m_Ini.addNode(nominal);
		m_Ini.addNode(constr);		
		m_Ini.addNode(m_SectionBeam);		
		m_Ini.addNode(m_SectionHierarchical);
		// add kNN section
		m_Ini.addNode(m_SectionKNN);
		m_Ini.addNode(m_SectionKNNT);
		m_Ini.addNode(exper);		
	}

	public void setSectionBeamEnabled(boolean enable) {
		m_SectionBeam.setEnabled(enable);
	}	
	
	public void setSectionHierarchicalEnabled(boolean enable) {
		m_SectionHierarchical.setEnabled(enable);
	}
	
	public void setSectionKNNEnabled(boolean enable) {
		m_SectionKNN.setEnabled(enable);
	}

	public void setSectionKNNTEnabled(boolean enable) {
		m_SectionKNNT.setEnabled(enable);
	}
	
	public int getPruningMethod() {
		return m_PruningMethod.getValue();
	}
	
	public void setPruningMethod(int method) {
		m_PruningMethod.setSingleValue(method);
	}
	
	public double isHierPruneInSig() {
		return m_HierPruneInSig.getValue();
	}
	
	public boolean isOrderedRules() {
		return m_OrderedRules.getValue();
	}
	
	public int getHierMode() {
		return m_HierMode.getValue();
	}
	
	public String getTuneFolds() {
		return m_TuneFolds.getValue();
	}
	
	public boolean getBeamRemoveEqualHeur() {
		return m_BMRemoveEqualHeur.getValue();
	}

	public double getMEstimate() {
		return m_MEstimate.getValue();
	}
	
	public boolean rulesFromTree() {
		return m_RulesFromTree.getValue();
	}
	
	public boolean hasConstraintFile() {
		return !StringUtils.unCaseCompare(m_SyntacticConstrFile.getValue(), NONE);
	}
	
	public String getConstraintFile() {
		return m_SyntacticConstrFile.getValue();
	}    
	
	public int getBeamAttrHeuristic() {
		return m_BeamAttrHeuristic.getValue();
	} 
	
	public boolean isBeamPostPrune() {
		return m_BeamPostPrune.getValue();
	}
	
	public static boolean isFastBS() {
		return m_FastBS.getValue();
	}
	
	public int getSizeConstraintPruning() {
		return m_MaxSizeConstr.getValue();
	}
	
	public void setSizeConstraintPruning(int size) {
		m_MaxSizeConstr.setValue(size);
	}	
	
	public double getSizePenalty() {
		return m_SizePenalty.getValue();
	}
	
	public int getBeamBestN() {
		return m_BeamBestN.getValue();
	} 
	
	public int getBeamWidth() {
		return m_BeamWidth.getValue();
	}
	
	public int getTreeMaxSize() {
		return m_TreeMaxSize.getValue();
	}
	
	public boolean isShowBranchFreq() {
		return m_ShowBrFreq.getValue();
	}
	
	public boolean isBinarySplit() {
		return m_BinarySplit.getValue();
	}
	
	public boolean isOutFoldError() {
		return m_OutFoldErr.getValue();
	}
	
	public boolean isOutFoldTree() {
		return m_OutFoldModels.getValue();
	}
	
	public boolean isOutTrainError() {
		return m_OutTrainErr.getValue();
	}
	
	public boolean isShowUnknown() {
		return m_ShowUnknown.getValue();
	}
	
	public boolean isShowXValForest() {
		return m_ShowForest.getValue();
	}
	
	public boolean isNullTarget() {
		return StringUtils.unCaseCompare(m_Target.getValue(), NONE);
	}
	
	public boolean isNullFile() {
		return StringUtils.unCaseCompare(m_DataFile.getValue(), NONE);
	}
	
	public boolean isNullTestFile() {
		return StringUtils.unCaseCompare(m_TestFile.getValue(), NONE);
	}
	
	public boolean isNullPruneFile() {
		return StringUtils.unCaseCompare(m_PruneFile.getValue(), NONE);
	}	
	
	public boolean checkHeuristic(String value) {
		return m_Heuristic.getStringSingle().equals(value);
	}
	
	public int getHeuristic() {
		return m_Heuristic.getValue();
	}    
	
	public String getDataFile() {
		return m_DataFile.getValue();
	}
	
	public String getTestFile() {
		return m_TestFile.getValue();
	}
	
	public int getBaggingSets() {
		return m_SetsData.getValue();
	}
	
	public void updateTarget(ClusSchema schema) {
		int nb = schema.getNbAttributes();
		if (isNullTarget()) {
			if (checkHeuristic("SSPD")) {
				schema.addAttrType(new IntegerAttrType("SSPD"));
				nb++;
			}
			m_Target.setValue(String.valueOf(nb));
		}
	}
	
	public void updateDataFile(String fname) {
		if (isNullFile()) m_DataFile.setValue(fname);
	}
	
	public void initialize(CMDLineArgs cargs, boolean loads) throws IOException {		
		create();
		if (cargs != null) preprocess(cargs);
		if (loads) {
			try {
				String fname = getFileAbsolute(getAppName()+".s");
				m_Ini.load(fname, '%');
			} catch (FileNotFoundException e) {
				System.out.println("No settings file found");			
			}
		}
		if (cargs != null) process(cargs);
		updateDataFile(getAppName()+".arff");
		initTestSet();		
		initHierarchical();
	}
	
	public void preprocess(CMDLineArgs cargs) {
		if (cargs.hasOption("xval")) {
			m_OutTrainErr.setValue(false);
		}
	}
	
	public void process(CMDLineArgs cargs) {
		if (cargs.hasOption("target")) {
			m_Target.setValue(cargs.getOptionValue("target"));
		}
		if (cargs.hasOption("disable")) {
			String disarg = cargs.getOptionValue("disable");
			String orig = m_Disabled.getValue();
			if (StringUtils.unCaseCompare(orig, NONE)) {
				m_Disabled.setValue(disarg);
			} else {
				m_Disabled.setValue(orig + "," + disarg);
			}
		}
		if (cargs.hasOption("silent")) {
			VERBOSE = 0;
		}
	}
	
	public boolean isMultiScore() {
		return !StringUtils.unCaseCompare(m_MultiScore.getValue(), NONE);
	}
	
	public void disableMultiScore() {
		m_MultiScore.setValue(NONE);
		IS_MULTISCORE = false;
	}
	
	public String getMultiScore() {
		return m_MultiScore.getValue();
	}
	
	public boolean isGainRatio() {
		return m_GainRatio.getValue();
	}
	
	public boolean isPresetRandom() {
		return !StringUtils.unCaseCompare(m_RandomSeed.getValue(), NONE);
	}
	
	public int getPresetRandom() {
		return Integer.parseInt(m_RandomSeed.getValue());
	}
	
	public double getPruneSetProportion() {
		return m_PruneProp.getValue();
	}
	
	public boolean shouldNormalize() {
		return m_Normalize.getValue();
	}
	
	public void initTestSet() {
		XVAL_ID = -1;
		TESTSET_ID = -1;
		XVAL_FOLDS = 10;
		TESTSET_PROPORTION = 0.0;
		try {
			String testset = m_TestPropOrAttr.getValue();
			if (testset.charAt(0) == 'A' && testset.length() > 1) {
				TESTSET_ID = Integer.parseInt(testset.substring(1))-1;
			} else {
				TESTSET_PROPORTION = Double.parseDouble(testset);
			}
			String xvfolds = m_XValFolds.getValue();
			if (xvfolds.length() > 2 && xvfolds.charAt(0) == 'A' && Character.isDigit(xvfolds.charAt(1))) {
				XVAL_ID = Integer.parseInt(xvfolds.substring(1))-1;
			} else if (xvfolds.length() > 1 && Character.isDigit(xvfolds.charAt(0))) {
				XVAL_FOLDS = Integer.parseInt(xvfolds);
			} else {
				XVAL_FILE = xvfolds;
				XVAL_FOLDS = 0;
			}
		} catch (NumberFormatException e) {}
	}
	
	public static void setFTest(double ftest) {
		FTEST_VALUE = ftest;
		FTEST_LEVEL = FTest.getLevel(ftest);
		m_FTest.setValue(ftest);
	}
	
	public void update(ClusSchema schema) {
		setFTest(getFTest());
		MINIMAL_WEIGHT = getMinimalWeight();
		GAIN_RATIO = isGainRatio();
		IS_MULTISCORE = isMultiScore();
		SHOW_UNKNOWN_FREQ = isShowUnknown();
		SHOW_XVAL_FOREST = isShowXValForest();
		BINARY_SPLIT = isBinarySplit();
		SHOW_BRANCH_FREQ = isShowBranchFreq();
		ONE_NOMINAL = (schema.getNbTarNom() == 1 && schema.getNbTarNum() == 0);
		SIZE_PENALTY = getSizePenalty();
		BEAM_WIDTH = m_BeamWidth.getValue();
		VERBOSE = m_Verbose.getValue();
	}
	
	public void initHierarchical() {
		ClassesValue.setHSeparator(m_HierSep.getValue());
	}
	
	public static int enableVerbose(int talk) {
		int prev = VERBOSE;
		VERBOSE = talk;
		return prev;
	}
	
	public void setFolds(int folds) {
		XVAL_FOLDS = folds;
	}
	
	public String getTarget() {
		return m_Target.getValue();
	}
	
	public String getDisabled() {
		return m_Disabled.getValue();
	}
	
	public String getKey() {
		return m_Key.getValue();
	}
	
	public double getFTest() {
		return m_FTest.getValue();
	}
	
	public double getMinimalWeight() {
		return m_MinW.getValue();
	}
	
	public void show(PrintWriter where) throws IOException {
		m_Ini.save(where);
	}
	
	public static void setAppName(String file) {
		file = StringUtils.removeSuffix(file, ".gz");
		file = StringUtils.removeSuffix(file, ".arff");
		file = StringUtils.removeSuffix(file, ".s");		
		file = StringUtils.removeSuffix(file, ".");		
		m_AppName = FileUtil.removePath(file);
		m_DirName = FileUtil.getPath(file);
	}
	
	public static void setDate(Date date) {
		m_Date = date;
	}
	
	public static Date getDate() {
		return m_Date;
	}
	
	public static String getFileAbsolute(String fname) {
		if (m_DirName == null) return fname;
		return m_DirName + File.separator + fname;
	}
	
	public static PrintWriter getFileAbsoluteWriter(String fname) throws FileNotFoundException {
		String path = getFileAbsolute(fname);
		return new PrintWriter(new OutputStreamWriter(new FileOutputStream(path)));
	}
	
	public String getAppName() {
		return m_AppName;
	}
}
