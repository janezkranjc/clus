package clus.main;

import jeans.io.ini.*;
import jeans.util.cmdline.*;
import jeans.util.*;
import jeans.resource.*;

import java.io.*;
import java.util.*;

import clus.statistic.*;
import clus.heuristic.*;
import clus.data.type.*;
import clus.ext.hierarchical.*;

public class Settings implements Serializable {
	
	public final static long serialVersionUID = 1L;
	
	public final static String[] COMPATIBILITY = { "Latest", "CMB05" };
	
	public final static int COMPATIBILITY_LATEST = 0;
	
	public final static int COMPATIBILITY_CMB05 = 1;	
	
	public final static String[] HEURISTICS = { "Default", "ReducedError",
		"Gain", "SSPD", "MEstimate", "Morishita", "DispersionAdt", "DispersionMlt",
		"WRDispersionAdt", "WRDispersionMlt"};
	
	public final static int HEURISTIC_DEFAULT = 0;
	
	public final static int HEURISTIC_REDUCED_ERROR = 1;
	
	public final static int HEURISTIC_GAIN = 2;
	
	public final static int HEURISTIC_SSPD = 3;

	public final static int HEURISTIC_MESTIMATE = 4;

	public final static int HEURISTIC_MORISHITA = 5;

	public final static int HEURISTIC_DISPERSION_ADT = 6;

	public final static int HEURISTIC_DISPERSION_MLT = 7;

	public final static int HEURISTIC_WR_DISPERSION_ADT = 8;

	public final static int HEURISTIC_WR_DISPERSION_MLT = 9;

	public final static String[] WRITE_PRED = { "None", "Test", "Train" };
	
	public final static int[] WRITE_PRED_VALUES = {0};
	
	public final static int WRITE_PRED_NONE = 0;
	
	public final static int WRITE_PRED_TEST = 1;
	
	public final static int WRITE_PRED_TRAIN = 2;
		
	public final static String[] PRUNING_METHODS = { "Default", "None", "C4.5",
		"M5", "M5Multi", "ReducedErrorVSB", "Garofalakis", "GarofalakisVSB", "CartVSB", "CartMaxSize" };
	
	public final static int PRUNING_METHOD_DEFAULT = 0;
	
	public final static int PRUNING_METHOD_NONE = 1;
	
	public final static int PRUNING_METHOD_C45 = 2;
	
	public final static int PRUNING_METHOD_M5 = 3;
	
	public final static int PRUNING_METHOD_M5_MULTI = 4;	
	
	public final static int PRUNING_METHOD_REDERR_VSB = 5;
	
	public final static int PRUNING_METHOD_GAROFALAKIS = 6;
	
	public final static int PRUNING_METHOD_GAROFALAKIS_VSB = 7;
	
	public final static int PRUNING_METHOD_CART_VSB = 8;
	
	public final static int PRUNING_METHOD_CART_MAXSIZE = 9;	
	
	public final static String[] COVERING_METHODS =	{"Standard", "WeightedMultiplicative",
		"WeightedAdditive", "WeightedError", "Union", "BeamRuleDefSet",
		"RandomRuleSet", "StandardBootstrap", "HeurOnly", "RulesFromTree"};
	
	// Standard covering: ordered rules (decision list)
	public final static int COVERING_METHOD_STANDARD = 0;
	
	// 'Weighted' coverings: unordered rules
	public final static int COVERING_METHOD_WEIGHTED_MULTIPLICATIVE = 1;
	
	public final static int COVERING_METHOD_WEIGHTED_ADDITIVE = 2;
	
	public final static int COVERING_METHOD_WEIGHTED_ERROR = 3;
	
	// In multi-label classification: predicted set of classes is union
	// of predictions of individual rules 
	public final static int COVERING_METHOD_UNION = 4;
	
	// Evaluates rules in the context of complete rule set: unordered rules
	// public final static int COVERING_METHOD_RULE_SET = 5;

	// Evaluates rules in the context of complete rule set, checks all rules
	// in the beam: unordered rules
	// public final static int COVERING_METHOD_BEAM_RULE_SET = 6;

	// Evaluates rules in the context of complete rule set, builds default
	// rule first, checks all rules in the beam: unordered rules
	// Obsolete - should be deleted!
	public final static int COVERING_METHOD_BEAM_RULE_DEF_SET = 5; 

	// Evaluates rules in the context of complete rule set, separate rules
	// are constructed randomly: unordered rules
	public final static int COVERING_METHOD_RANDOM_RULE_SET = 6;

	// Repeated standard covering on bootstraped data
	public final static int COVERING_METHOD_STANDARD_BOOTSTRAP = 7;
	
	// No covering, only heuristic
	public final static int COVERING_METHOD_HEURISTIC_ONLY = 8;
	
	// No covering, rules transcribed from tree
	public final static int COVERING_METHOD_RULES_FROM_TREE = 9;
	
	public final static String[] RULE_PREDICTION_METHODS =
	{"DecisionList", "CoverageWeighted", "CovAccWeighted", "Union", "Optimized"};
	
	public final static int RULE_PREDICTION_METHOD_DECISION_LIST = 0;
	
	public final static int RULE_PREDICTION_METHOD_COVERAGE_WEIGHTED = 1;
	
	public final static int RULE_PREDICTION_METHOD_COV_ACC_WEIGHTED = 2;
	
	public final static int RULE_PREDICTION_METHOD_UNION = 3;
	
	public final static int RULE_PREDICTION_METHOD_OPTIMIZED = 4;

	public final static String[] RULE_ADDING_METHODS =	{"Always", "IfBetter", "IfBetterBeam"};

	// Always adds a rule to the rule set 
	public final static int RULE_ADDING_METHOD_ALWAYS = 0;

	// Only adds a rule to the rule set if it improves the rule set performance
	public final static int RULE_ADDING_METHOD_IF_BETTER = 1;

	// Only adds a rule to the rule set if it improves the rule set performance.
	// If not, it checks other rules in the beam
	public final static int RULE_ADDING_METHOD_IF_BETTER_BEAM = 2;
	
	public final static String[] HIERMODES = { "TDWEuclid", "TDAbsWEuclid",
		"XtAXSetDist", "XtAXSetDistDiscrete" };
	
	public final static int HIERMODE_TREE_DIST_WEUCLID = 0;
	
	public final static int HIERMODE_TREE_DIST_ABS_WEUCLID = 1;
	
	public final static int HIERMODE_XTAX_SET_DIST = 2;
	
	public final static int HIERMODE_XTAX_SET_DIST_DISCRETE = 3;
	
	// Hierarchical multi-classification now supports both trees and DAGS
	// This was required because Gene Ontology terms are organized in a partial order
	public final static String[] HIERTYPES = { "Tree", "DAG" };
	
	public final static int HIERTYPE_TREE = 0;
	
	public final static int HIERTYPE_DAG = 1;
	
	public final static String[] NORMALIZATIONS = { "Normalize" };
	
	public final static int NORMALIZATION_DEFAULT = 0;
	
	public final static String[] INFINITY = { "Infinity" };
	
	public final static String INFINITY_STRING = "Infinity";	
	
	public final static String[] EMPTY = {};
	
	public final static String[] NUM_NOM_TAR_NTAR_WEIGHTS = { "TargetWeight",
		"NonTargetWeight", "NumericWeight", "NominalWeight" };
	
	public final static int TARGET_WEIGHT = 0;
	
	public final static int NON_TARGET_WEIGHT = 1;
	
	public final static int NUMERIC_WEIGHT = 2;
	
	public final static int NOMINAL_WEIGHT = 3;
	
	public final static double[] FOUR_ONES = { 1.0, 1.0, 1.0, 1.0 };
	
	public final static int INFINITY_VALUE = 0;
	
	public final static long SERIAL_VERSION_ID = 1L;
	
	public final static String NONE = "None";
	
	public final static String[] NONELIST = { "None" };
	
	public final static String DEFAULT = "Default";
	
	public final static String[] RESOURCE_INFO_LOAD = {"Yes", "No", "Test"};
	
	public final static int RESOURCE_INFO_LOAD_YES = 0;
	
	public final static int RESOURCE_INFO_LOAD_NO = 1;
	
	public final static int RESOURCE_INFO_LOAD_TEST = 2;	
	
	public final static String[] SHOW_MODELS = {"Default", "Original", "Pruned", "Others"};
	
	public final static int[] SHOW_MODELS_VALUES = {0,2,3};
	
	public final static int SHOW_MODELS_DEFAULT = 0;
	
	public final static int SHOW_MODELS_ORIGINAL = 1;
	
	public final static int SHOW_MODELS_PRUNED = 2;
	
	public final static int SHOW_MODELS_OTHERS = 3;
	
	public final static String[] SHOW_INFO = {"Count", "Distribution", "Index", "NodePrototypes"};
	
	public final static int[] SHOW_INFO_VALUES = {0};
	
	public final static String[] CONVERT_RULES = { "No", "Pruned", "All" };
	
	public final static int CONVERT_RULES_NONE = 0;
	
	public final static int CONVERT_RULES_PRUNED = 1;
	
	public final static int CONVERT_RULES_ALL = 2;
	
	public final static String[] TIME_SERIES_DISTANCE_MEASURE={"DTW","QDM","TSC"};
	
	public final static int TIME_SERIES_DISTANCE_MEASURE_DTW = 0;
	
	public final static int TIME_SERIES_DISTANCE_MEASURE_QDM = 1;
	
	public final static int TIME_SERIES_DISTANCE_MEASURE_TSC = 2;
		
	/* Filename and date information */
	protected Date m_Date;
	
	protected String m_AppName;
	
	protected String m_DirName;
	
	/* Static constants should be removed later on */
	
	
	public static int FTEST_LEVEL;
	
	public static double FTEST_VALUE;
	
	public static boolean GAIN_RATIO;
	
	public static double MINIMAL_WEIGHT;
	
	public static boolean IS_MULTISCORE;
	
	public static int BEAM_WIDTH;
	
	public static double SIZE_PENALTY;
	
	public static boolean SHOW_UNKNOWN_FREQ;
	
	public static boolean SHOW_BRANCH_FREQ;
	
	public static boolean SHOW_XVAL_FOREST;
	
	public static boolean XVAL_OVERLAP = true;
	
	public static boolean ONE_NOMINAL = true;
	
	public static int VERBOSE = 1;
	
	public static boolean EXACT_TIME = false;
	
	public static boolean IS_RULE_SIG_TESTING = false;
	
	public static boolean IS_XVAL = false;
	
	/* The INI file structure */
	protected INIFile m_Ini = new INIFile();
	
	/* General */
	protected INIFileInt m_Verbose;
	
	protected INIFileString m_RandomSeed;
	
	protected INIFileStringOrInt m_XValFolds;
	
	protected INIFileNominal m_ResourceInfoLoaded;
	
	protected INIFileNominal m_Compatibility;
	
	/* Data */
	protected INIFileString m_DataFile;
	
	protected INIFileStringOrDouble m_TestSet;
	
	protected INIFileStringOrDouble m_PruneSet;
	
	protected INIFileStringOrInt m_PruneSetMax;
	
	/* Attribute */
	protected INIFileString m_Target;
	
	protected INIFileString m_Disabled;
	
	protected INIFileString m_Descriptive;
	
	protected INIFileString m_Clustering;
	
	protected INIFileString m_Key;
	
	protected INIFileNominalOrDoubleOrVector m_Weights;
	
	protected INIFileNominalOrDoubleOrVector m_ClusteringWeights;
	
	/* Numeric */
	protected INIFileDouble m_FTest;
	
	protected INIFileString m_MultiScore;
	
	/* Nominal */
	protected INIFileBool m_GainRatio;
	
	protected INIFileDouble m_MEstimate;
	
	/* Model */
	protected INIFileDouble m_MinW;
	
	protected INIFileString m_TuneFolds;
	
//	added by Leander 7-4-2006
	protected INIFileNominalOrDoubleOrVector m_ClassWeight;
	
//	end added by Leander 7-4-2006

	/* Tree */
	protected INIFileNominal m_Heuristic;
	
	public static INIFileInt TREE_MAX_DEPTH;
	
	public static boolean BINARY_SPLIT;
	
	protected INIFileBool m_BinarySplit;
	
	protected INIFileNominal m_PruningMethod;
	
	protected INIFileBool m_1SERule;
	
	protected INIFileNominal m_RulesFromTree;
	
	protected INIFileDouble m_M5PruningMult;
	
	/* Rules */
	protected INIFileNominal m_CoveringMethod;
	
	protected INIFileNominal m_PredictionMethod;
	
	protected INIFileNominal m_RuleAddingMethod;
	
	protected INIFileDouble m_CoveringWeight;

	protected INIFileDouble m_InstCoveringWeightThreshold;
	
	protected INIFileInt m_MaxRulesNb;
	
	protected INIFileDouble m_HeurCoveragePar;
	
	protected INIFileDouble m_CompHeurRuleDistPar;
	
	protected INIFileDouble m_HeurPrototypeDistPar;
	
	protected INIFileDouble m_RuleSignificanceLevel;
	
	protected INIFileInt m_RuleNbSigAtts;
	
	protected INIFileBool m_ComputeCompactness;	
	
	protected INIFileDouble m_NumCompNormWeight;
	
	protected INIFileNominalOrDoubleOrVector m_CompactnessWeights;
	
	protected INIFileInt m_RandomRules;
	
	protected INIFileBool m_RuleWiseErrors;
	
	protected INIFileInt m_OptDEPopSize;
	
	protected INIFileInt m_OptDENumEval;
	
	protected INIFileDouble m_OptDECrossProb;
	
	protected INIFileDouble m_OptDEWeight;
	
	protected INIFileInt m_OptDESeed;

	protected INIFileDouble m_OptRegPar;
	
	protected INIFileDouble m_OptRuleWeightThreshold;
	
	/* Constraints */
	protected INIFileString m_SyntacticConstrFile;
	
	protected INIFileNominalOrIntOrVector m_MaxSizeConstr;
	
	protected INIFileNominalOrDoubleOrVector m_MaxErrorConstr;
	
	/* Output */
	protected INIFileInt m_SetsData;
	
	protected INIFileBool m_OutFoldErr;
	
	protected INIFileBool m_OutFoldModels;
	
	protected INIFileBool m_OutTrainErr;
	
	protected INIFileBool m_ShowForest;
	
	protected INIFileBool m_ShowBrFreq;
	
	protected INIFileBool m_ShowUnknown;
	
	protected INIFileNominal m_ShowInfo;
	
	protected INIFileNominal m_ShowModels;
	
	protected INIFileBool m_PrintModelAndExamples;	
	
	protected INIFileNominal m_WritePredictions;
	
	protected INIFileBool m_ModelIDFiles;
	
	protected INIFileBool m_OutputPythonModel;	
	protected INIFileBool m_OutputDatabaseQueries;	
	
	/* Beam Search For Trees */
	protected INIFileSection m_SectionBeam;
	protected INIFileDouble m_SizePenalty;
	protected INIFileInt m_BeamWidth;
	protected INIFileInt m_BeamBestN;
	protected INIFileInt m_TreeMaxSize;
	protected INIFileNominal m_BeamAttrHeuristic;
	protected INIFileBool m_FastBS;
	protected INIFileBool m_BeamPostPrune;
	protected INIFileBool m_BMRemoveEqualHeur;
	
	/* Exhaustive Search : Elisa 1/08/2006*/
	protected INIFileSection m_SectionExhaustive;
	protected INIFileBool m_exhaustive;
	
	/* Hierarchical Multi-Classification */
	protected INIFileString m_HierSep;

	protected INIFileString m_HierEmptySetIndicator;
	
	public static INIFileDouble HIER_W_PARAM;
	
	public static INIFileBool HIER_SAVE;
	
	public static INIFileBool HIER_LOAD;
	
	public static INIFileBool HIER_FLAT;
	
	public INIFileNominal m_HierMode;
	
	INIFileSection m_SectionHierarchical;
	
	public static INIFileBool HIER_CONT_PROTOTYPE;
	
	public static INIFileBool HIER_USE_ABUNDANCES;
	
	public static INIFileBool HIER_NODE_ABUNDANCES;
	
	public INIFileDouble m_HierPruneInSig;
	
	protected INIFileBool m_HierNoRootPreds;
	
	protected INIFileBool m_HierUseBonferroni;
	
	protected INIFileNominal m_HierType;
	
	protected INIFileString m_DefinitionFile;
	
	protected INIFileNominalOrDoubleOrVector m_HierClassThreshold;
	
	protected INIFileString m_HierEvalClasses;

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
	
	/*Time Series*/
	INIFileSection timeSeries;

	public static INIFileNominal timeSeriesDM;


	public void create() {
		INIFileSection settings = new INIFileSection("General");
		settings.addNode(m_Verbose = new INIFileInt("Verbose", 1));
		settings.addNode(m_RandomSeed = new INIFileString("RandomSeed", "0"));
		settings.addNode(m_XValFolds = new INIFileStringOrInt("XVal"));
		settings.addNode(m_ResourceInfoLoaded = new INIFileNominal("ResourceInfoLoaded", RESOURCE_INFO_LOAD, 1));
		settings.addNode(m_Compatibility = new INIFileNominal("Compatibility", COMPATIBILITY, 0));
		m_XValFolds.setIntValue(10);

		INIFileSection data = new INIFileSection("Data");
		data.addNode(m_DataFile = new INIFileString("File", NONE));
		data.addNode(m_TestSet = new INIFileStringOrDouble("TestSet", NONE));
		data.addNode(m_PruneSet = new INIFileStringOrDouble("PruneSet", NONE));
		data.addNode(m_PruneSetMax = new INIFileStringOrInt("PruneSetMax", INFINITY_STRING));
		m_PruneSetMax.setEnabled(false);

		INIFileSection attrs = new INIFileSection("Attributes");
		attrs.addNode(m_Target = new INIFileString("Target", DEFAULT));
		attrs.addNode(m_Disabled = new INIFileString("Disable", NONE));
		attrs.addNode(m_Clustering = new INIFileString("Clustering", DEFAULT));
		attrs.addNode(m_Descriptive = new INIFileString("Descriptive", DEFAULT));
		attrs.addNode(m_Key = new INIFileString("Key", NONE));
		attrs.addNode(m_Weights = new INIFileNominalOrDoubleOrVector("Weights",	NORMALIZATIONS));
		m_Weights.setNominal(NORMALIZATION_DEFAULT);
		attrs.addNode(m_ClusteringWeights = new INIFileNominalOrDoubleOrVector("ClusteringWeights", EMPTY));
		m_ClusteringWeights.setDouble(1.0);
		m_ClusteringWeights.setArrayIndexNames(NUM_NOM_TAR_NTAR_WEIGHTS);

		INIFileSection numeric = new INIFileSection("Numeric");
		numeric.addNode(m_FTest = new INIFileDouble("FTest", 1.0));
		numeric.addNode(m_MultiScore = new INIFileString("MultiScore", NONE));

		INIFileSection nominal = new INIFileSection("Nominal");
		nominal.addNode(m_GainRatio = new INIFileBool("GainRatio"));
		nominal.addNode(m_MEstimate = new INIFileDouble("MEstimate", 1.0));

		INIFileSection model = new INIFileSection("Model");
		model.addNode(m_MinW = new INIFileDouble("MinimalWeight", 2.0));
		model.addNode(m_TuneFolds = new INIFileString("ParamTuneNumberFolds", "10"));
		//added by Leander 7-4-2006
		model.addNode(m_ClassWeight = new INIFileNominalOrDoubleOrVector("ClassWeights", EMPTY));
		//should all weights be initialized to 1 or does this happen automatically? 
		//end added by Leander 7-4-2006

		INIFileSection tree = new INIFileSection("Tree");
		tree.addNode(m_Heuristic = new INIFileNominal("Heuristic", HEURISTICS, 0));
		tree.addNode(TREE_MAX_DEPTH = new INIFileInt("MaxDepth", -1));
		tree.addNode(m_BinarySplit = new INIFileBool("BinarySplit", true));
		tree.addNode(m_PruningMethod = new INIFileNominal("PruningMethod", PRUNING_METHODS, 0));
		tree.addNode(m_1SERule = new INIFileBool("1-SE-Rule", false));
		tree.addNode(m_M5PruningMult = new INIFileDouble("M5PruningMult", 2.0));
		tree.addNode(m_RulesFromTree = new INIFileNominal("ConvertToRules", CONVERT_RULES, 0));

		INIFileSection rules = new INIFileSection("Rules");
    rules.addNode(m_CoveringMethod = new INIFileNominal("CoveringMethod", COVERING_METHODS, 0));
		rules.addNode(m_PredictionMethod = new INIFileNominal("PredictionMethod", RULE_PREDICTION_METHODS, 0));
		rules.addNode(m_RuleAddingMethod = new INIFileNominal("RuleAddingMethod", RULE_ADDING_METHODS, 0));
		rules.addNode(m_CoveringWeight = new INIFileDouble("CoveringWeight", 0.9));
		rules.addNode(m_InstCoveringWeightThreshold = new INIFileDouble("InstCoveringWeightThreshold", 0.1));
		rules.addNode(m_MaxRulesNb = new INIFileInt("MaxRulesNb", 1000));
    rules.addNode(m_HeurCoveragePar = new INIFileDouble("HeurCoveragePar", 1.0));
    rules.addNode(m_CompHeurRuleDistPar = new INIFileDouble("CompHeurRuleDistPar", 0.0));
    rules.addNode(m_HeurPrototypeDistPar = new INIFileDouble("HeurPrototypeDistPar", 0.0));
    rules.addNode(m_RuleSignificanceLevel = new INIFileDouble("RuleSignificanceLevel", 0.05));
    rules.addNode(m_RuleNbSigAtts = new INIFileInt("RuleNbSigAtts", 0));
		rules.addNode(m_ComputeCompactness = new INIFileBool("ComputeCompactness", false));
    rules.addNode(m_NumCompNormWeight = new INIFileDouble("NumCompNormWeight", 4.0));
    rules.addNode(m_CompactnessWeights = new INIFileNominalOrDoubleOrVector("CompactnessWeights", EMPTY));
		m_CompactnessWeights.setArrayIndexNames(NUM_NOM_TAR_NTAR_WEIGHTS);
		m_CompactnessWeights.setDoubleArray(FOUR_ONES);
		m_CompactnessWeights.setArrayIndexNames(true);
    rules.addNode(m_RandomRules = new INIFileInt("RandomRules", 0));
    rules.addNode(m_RuleWiseErrors = new INIFileBool("PrintRuleWiseErrors", false));
    rules.addNode(m_OptDEPopSize = new INIFileInt("OptDEPopSize", 500));
    rules.addNode(m_OptDENumEval = new INIFileInt("OptDENumEval", 10000));
    rules.addNode(m_OptDECrossProb = new INIFileDouble("OptDECrossProb", 0.3));
    rules.addNode(m_OptDEWeight = new INIFileDouble("OptDEWeight", 0.5));
    rules.addNode(m_OptDESeed = new INIFileInt("OptDESeed", 0));
    rules.addNode(m_OptRegPar = new INIFileDouble("OptRegPar", 0.0));
    rules.addNode(m_OptRuleWeightThreshold = new INIFileDouble("OptRuleWeightThreshold", 0.1));

    INIFileSection constr = new INIFileSection("Constraints");
		constr.addNode(m_SyntacticConstrFile = new INIFileString("Syntactic",	NONE));
		constr.addNode(m_MaxSizeConstr = new INIFileNominalOrIntOrVector("MaxSize", INFINITY));
		constr.addNode(m_MaxErrorConstr = new INIFileNominalOrDoubleOrVector("MaxError", INFINITY));
		m_MaxSizeConstr.setNominal(0);
		m_MaxErrorConstr.setDouble(0);

		INIFileSection output = new INIFileSection("Output");
		output.addNode(m_OutFoldModels = new INIFileBool("AllFoldModels", true));
		output.addNode(m_OutFoldErr = new INIFileBool("AllFoldErrors", false));
		output.addNode(m_OutTrainErr = new INIFileBool("TrainErrors", true));
		output.addNode(m_ShowUnknown = new INIFileBool("UnknownFrequency", false));
		output.addNode(m_ShowBrFreq = new INIFileBool("BranchFrequency", false));
		output.addNode(m_ShowInfo = new INIFileNominal("ShowInfo", SHOW_INFO, SHOW_INFO_VALUES));
		output.addNode(m_ShowModels = new INIFileNominal("ShowModels", SHOW_MODELS, SHOW_MODELS_VALUES));		
		output.addNode(m_PrintModelAndExamples = new INIFileBool("PrintModelAndExamples", false));
		output.addNode(m_WritePredictions = new INIFileNominal("WritePredictions", WRITE_PRED, WRITE_PRED_VALUES));
		output.addNode(m_ModelIDFiles = new INIFileBool("ModelIDFiles", false));		
		output.addNode(m_OutputPythonModel = new INIFileBool("OutputPythonModel", false));
		output.addNode(m_OutputDatabaseQueries = new INIFileBool("OutputDatabaseQueries", false));
		
		m_SectionHierarchical = new INIFileSection("Hierarchical");
		m_SectionHierarchical.addNode(m_HierType = new INIFileNominal("Type", HIERTYPES, 0));		
		m_SectionHierarchical.addNode(HIER_W_PARAM = new INIFileDouble("WParam", 0.75));
		m_SectionHierarchical.addNode(m_HierSep = new INIFileString("HSeparator", "."));
		m_SectionHierarchical.addNode(m_HierEmptySetIndicator = new INIFileString("EmptySetIndicator", "n"));
		m_SectionHierarchical.addNode(m_DefinitionFile = new INIFileString("DefinitionFile", NONE)); 
		m_SectionHierarchical.addNode(HIER_LOAD = new INIFileBool("HierLoad", false));
		m_SectionHierarchical.addNode(HIER_SAVE = new INIFileBool("HierSave", false));
		m_SectionHierarchical.addNode(HIER_FLAT = new INIFileBool("HierFlat", false));
		m_SectionHierarchical.addNode(m_HierNoRootPreds = new INIFileBool("NoRootPredictions", false));
		m_SectionHierarchical.addNode(m_HierPruneInSig = new INIFileDouble("PruneInSig", 0.0));
		m_SectionHierarchical.addNode(m_HierUseBonferroni = new INIFileBool("Bonferroni", false));
		m_SectionHierarchical.addNode(m_HierClassThreshold = new INIFileNominalOrDoubleOrVector("ClassificationTreshold", NONELIST));		
		m_HierClassThreshold.setNominal(0);
		m_SectionHierarchical.addNode(m_HierEvalClasses = new INIFileString("EvalClasses", NONE));		
		m_SectionHierarchical.addNode(m_HierMode = new INIFileNominal("Mode", HIERMODES, 0));		
		m_SectionHierarchical.setEnabled(false);

		m_SectionKNN = new INIFileSection("kNN");
		m_SectionKNN.addNode(kNN_k = new INIFileInt("k", 3));
		m_SectionKNN.addNode(kNN_vectDist = new INIFileString("VectorDistance", "Euclidian"));
		m_SectionKNN.addNode(kNN_distWeighted = new INIFileBool("DistanceWeighted", false));
		m_SectionKNN.addNode(kNN_normalized = new INIFileBool("Normalizing", true));
		m_SectionKNN.addNode(kNN_attrWeighted = new INIFileBool("AttributeWeighted", false));
		m_SectionKNN.setEnabled(false);

		m_SectionKNNT = new INIFileSection("kNNTree");
		m_SectionKNNT.addNode(kNNT_k = new INIFileInt("k", 3));
		m_SectionKNNT.addNode(kNNT_vectDist = new INIFileString("VectorDistance", "Euclidian"));
		m_SectionKNNT.addNode(kNNT_distWeighted = new INIFileBool("DistanceWeighted", false));
		m_SectionKNNT.addNode(kNNT_normalized = new INIFileBool("Normalizing", true));
		m_SectionKNNT.addNode(kNNT_attrWeighted = new INIFileBool("AttributeWeighted", false));
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
		
		//added by elisa 1/08/2006
		m_SectionExhaustive = new INIFileSection("Exhaustive");
		m_SectionExhaustive.addNode(m_exhaustive = new INIFileBool("Exhaustive", true));
		
		timeSeries = new INIFileSection("TimeSeries");
		timeSeries.addNode(timeSeriesDM=new INIFileNominal("DistanceMeasure", TIME_SERIES_DISTANCE_MEASURE,0));
		timeSeries.setEnabled(false);
		
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
		m_Ini.addNode(numeric);
		m_Ini.addNode(nominal);
		m_Ini.addNode(constr);
		m_Ini.addNode(output);
		m_Ini.addNode(m_SectionBeam);
		m_Ini.addNode(m_SectionHierarchical);
		// add kNN section
		m_Ini.addNode(m_SectionKNN);
		m_Ini.addNode(m_SectionKNNT);
		m_Ini.addNode(exper);
		m_Ini.addNode(timeSeries);
	}

	public void initNamedValues() {
		TREE_MAX_DEPTH.setNamedValue(-1, "Infinity");
		m_TreeMaxSize.setNamedValue(-1, "Infinity");
	}

	public void setSectionBeamEnabled(boolean enable) {
		m_SectionBeam.setEnabled(enable);
	}
	
	//new Elisa 1/08/2006
	public void setSectionExhaustiveEnabled(boolean enable) {
		m_SectionExhaustive.setEnabled(enable);
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

	public void setSectionTimeSeriesEnabled(boolean enable) {
		timeSeries.setEnabled(enable);
	}
	
	
	public boolean isSectionTimeSeriesEnabled() {
		return timeSeries.isEnabled();
	}
	
	public boolean isHierNoRootPreds() {
		return m_HierNoRootPreds.getValue();
	}

	public boolean isUseBonferroni() {
		return m_HierUseBonferroni.getValue();
	}
	
	public INIFileNominalOrDoubleOrVector getClassificationThresholds() {
		return m_HierClassThreshold;
	}
	
	public String getHierEvalClasses() {
		return m_HierEvalClasses.getValue();
	}	
		
	public boolean hasHierEvalClasses() {
		return !StringUtils.unCaseCompare(m_HierEvalClasses.getValue(), NONE);
	}	

	public int getPruningMethod() {
		return m_PruningMethod.getValue();
	}

	public String getPruningMethodName() {
		return m_PruningMethod.getStringValue();
	}	
	
	public boolean get1SERule() {
		return m_1SERule.getValue();
	}
	
	public double getM5PruningMult() {
		return m_M5PruningMult.getValue();
	}

	public void setPruningMethod(int method) {
		m_PruningMethod.setSingleValue(method);
	}

	public double isHierPruneInSig() {
		return m_HierPruneInSig.getValue();
	}
	
  public boolean isRandomRules() {
    return (m_RandomRules.getValue() > 0);
  }
  
  public int nbRandomRules() {
    return m_RandomRules.getValue();
  }
  
  public boolean isRuleWiseErrors() {
  	return m_RuleWiseErrors.getValue();
  }

  public int getCoveringMethod() {
    return m_CoveringMethod.getValue();
  }
  
  public void setCoveringMethod(int method) {
    m_CoveringMethod.setSingleValue(method);
  }

  public int getRulePredictionMethod() {
    return m_PredictionMethod.getValue();
  }
  
  public void setRulePredictionMethod(int method) {
    m_PredictionMethod.setSingleValue(method);
  }

  public int getRuleAddingMethod() {
    return m_RuleAddingMethod.getValue();
  }
  
  public void setRuleAddingMethod(int method) {
    m_RuleAddingMethod.setSingleValue(method);
  }
  
  public double getCoveringWeight() {
    return m_CoveringWeight.getValue();
  }

  public void setCoveringWeight(double weight) {
    m_CoveringWeight.setValue(weight);
  }
 
  public double getInstCoveringWeightThreshold() {
    return m_InstCoveringWeightThreshold.getValue();
  }

  public void setInstCoveringWeightThreshold(double thresh) {
  	m_InstCoveringWeightThreshold.setValue(thresh);
  }
  
  public int getMaxRulesNb() {
    return m_MaxRulesNb.getValue();
  }
  
  public void setMaxRulesNb(int nb) {
    m_MaxRulesNb.setValue(nb);
  }
  
  public double getRuleSignificanceLevel() {
    return m_RuleSignificanceLevel.getValue();
  }

  public int getRuleNbSigAtt() {
    return m_RuleNbSigAtts.getValue();
  }

  public boolean isRuleSignificanceTesting() {
    return m_RuleNbSigAtts.getValue() != 0;
  }

  public double getHeurCoveragePar() {
    return m_HeurCoveragePar.getValue();
  }
 
  public double getCompHeurRuleDistPar() {
    return m_CompHeurRuleDistPar.getValue();
  }
  
  public void setCompHeurRuleDistPar(double value) {
    m_CompHeurRuleDistPar.setValue(value);
  }
  
  public boolean isCompHeurRuleDist() {
    return m_CompHeurRuleDistPar.getValue() > 0;
  }

  public double getHeurPrototypeDistPar() {
    return m_HeurPrototypeDistPar.getValue();
  }
  
  public void setHeurPrototypeDistPar(double value) {
  	m_HeurPrototypeDistPar.setValue(value);
  }
  
  public boolean isHeurPrototypeDistPar() {
    return m_HeurPrototypeDistPar.getValue() > 0;
  }

	public void disableRuleInduceParams() {
		setCompHeurRuleDistPar(0.0);
		setRulePredictionMethod(RULE_PREDICTION_METHOD_DECISION_LIST);
		setCoveringMethod(COVERING_METHOD_RULES_FROM_TREE);
	}

  public boolean computeCompactness() {
    return m_ComputeCompactness.getValue();
  }
	
  public double getNumCompNormWeight() {
    return m_NumCompNormWeight.getValue();
  }
  
	public double getOptDECrossProb() {
		return m_OptDECrossProb.getValue();
	}

	public int getOptDENumEval() {
		return m_OptDENumEval.getValue();
	}

	public int getOptDEPopSize() {
		return m_OptDEPopSize.getValue();
	}

	public int getOptDESeed() {
		return m_OptDESeed.getValue();
	}

	public double getOptDEWeight() {
		return m_OptDEWeight.getValue();
	}

	public double getOptRegPar() {
		return m_OptRegPar.getValue();
	}
	
	public double getOptRuleWeightThreshold() {
		return m_OptRegPar.getValue();
	}
  
	public int getHierMode() {
		return m_HierMode.getValue();
	}
	
	public int getHierType() {
		return m_HierType.getValue();
	}
	
	public boolean hasDefinitionFile() {
		return !StringUtils.unCaseCompare(m_DefinitionFile.getValue(), NONE);
	}
	
	public String getDefinitionFile() {
		return m_DefinitionFile.getValue();
	}

	public String getTuneFolds() {
		return m_TuneFolds.getValue();
	}
	
	//added by Leander 7-4-2006
	public double[] getClassWeight() {
		return m_ClassWeight.getDoubleVector();
	}
	//end added by Leander 7-4-2006

	public boolean getBeamRemoveEqualHeur() {
		return m_BMRemoveEqualHeur.getValue();
	}

	public double getMEstimate() {
		return m_MEstimate.getValue();
	}
	
	public int rulesFromTree() {
		return m_RulesFromTree.getValue();
	}	

	public boolean hasConstraintFile() {
		return !StringUtils.unCaseCompare(m_SyntacticConstrFile.getValue(),	NONE);
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

	
	public boolean isFastBS() {
		return m_FastBS.getValue();
	}
	
	//added by elisa 1/08/2006
	public boolean isExhaustiveSearch() {
		return m_exhaustive.getValue();
	}
	//
	
	public int getMaxSize() {
		return getSizeConstraintPruning(0);
	}
	
	public int getSizeConstraintPruning(int idx) {
		if (m_MaxSizeConstr.isNominal(idx)) {
			return -1;
		} else {
			return m_MaxSizeConstr.getInt(idx);
		}
	}

	public int getSizeConstraintPruningNumber() {
		int len = m_MaxSizeConstr.getVectorLength();
		if (len == 1 && m_MaxSizeConstr.getNominal() == INFINITY_VALUE)
			return 0;
		else
			return len;
	}

	public int[] getSizeConstraintPruningVector() {
		int size_nb = getSizeConstraintPruningNumber();
		int[] sizes = new int[size_nb];
		for (int i = 0; i < size_nb; i++) {
			sizes[i] = getSizeConstraintPruning(i);
		}
		return sizes;
	}
	
	public void setSizeConstraintPruning(int size) {
		m_MaxSizeConstr.setInt(size);
	}
	
	public double getMaxErrorConstraint(int idx) {
		if (m_MaxErrorConstr.isNominal(idx)) {
			return Double.POSITIVE_INFINITY;
		} else {
			return m_MaxErrorConstr.getDouble(idx);
		}
	}

	public int getMaxErrorConstraintNumber() {
		int len = m_MaxErrorConstr.getVectorLength();
		if (len == 1 && m_MaxErrorConstr.getDouble(0) == 0.0)
			return 0;
		else
			return len;
	}
	
	public double[] getMaxErrorConstraintVector() {
		int error_nb = getMaxErrorConstraintNumber();
		double[] max_error = new double[error_nb];
		for (int i = 0; i < error_nb; i++) {
			max_error[i] = getMaxErrorConstraint(i);
		}
		return max_error;
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

	public int getBeamTreeMaxSize() {
		return m_TreeMaxSize.getValue();
	}
	
	
	public boolean isShowBranchFreq() {
		return m_ShowBrFreq.getValue();
	}

	public boolean isWriteTestSetPredictions() {
		return m_WritePredictions.contains(WRITE_PRED_TEST);
	}
	
	public boolean isWriteTrainSetPredictions() {
		return m_WritePredictions.contains(WRITE_PRED_TRAIN);
	}	

	public boolean isWriteModelIDPredictions() {
		return m_ModelIDFiles.getValue();
	}
	
	public boolean isOutputPythonModel() {
		return m_OutputPythonModel.getValue();
	}
	
	//added by elisa 06/2006
	public boolean isOutputDatabaseQueries() {
		return m_OutputDatabaseQueries.getValue();
	}
	
	public boolean isPrintModelAndExamples() {
		return m_PrintModelAndExamples.getValue();
	}

	public boolean isBinarySplit() {
		return m_BinarySplit.getValue();
	}

	public boolean isOutFoldError() {
		return m_OutFoldErr.getValue();
	}

	public boolean isOutputFoldModels() {
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
	
	public boolean isDefaultTarget() {
		return StringUtils.unCaseCompare(m_Target.getValue(), DEFAULT);
	}	

	public boolean isNullFile() {
		return StringUtils.unCaseCompare(m_DataFile.getValue(), NONE);
	}

	public boolean isNullTestFile() {
		return m_TestSet.isDoubleOrNull(NONE);
	}

	public boolean isNullPruneFile() {
		return m_PruneSet.isDoubleOrNull(NONE);
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
		return m_TestSet.getValue();
	}

	public String getPruneFile() {
		return m_PruneSet.getValue();
	}

	public double getTestProportion() {
		if (!m_TestSet.isDouble())
			return 0.0;
		return m_TestSet.getDoubleValue();
	}

	public double getPruneProportion() {
		if (!m_PruneSet.isDouble())
			return 0.0;
		return m_PruneSet.getDoubleValue();
	}
	
	public int getPruneSetMax() {
		if (m_PruneSetMax.isString(INFINITY_STRING)) return Integer.MAX_VALUE;
		else return m_PruneSetMax.getIntValue();
	}

	public boolean isNullXValFile() {
		return m_XValFolds.isIntOrNull(NONE);
	}

	public String getXValFile() {
		return m_XValFolds.getValue();
	}

	public int getXValFolds() {
		return m_XValFolds.getIntValue();
	}

	public void setXValFolds(int folds) {
		m_XValFolds.setIntValue(folds);
	}

	public int getBaggingSets() {
		return m_SetsData.getValue();
	}

	public INIFileNominalOrDoubleOrVector getNormalizationWeights() {
		return m_Weights;
	}

	public INIFileNominalOrDoubleOrVector getClusteringWeights() {
		return m_ClusteringWeights;
	}

	public INIFileNominalOrDoubleOrVector getCompactnessWeights() {
		return m_CompactnessWeights;
	}

	public boolean hasNonTrivialWeights() {
		for (int i = 0; i < m_Weights.getVectorLength(); i++) {
			if (m_Weights.isNominal(i))
				return true;
			else if (m_Weights.getDouble(i) != 1.0)
				return true;
		}
		return false;
	}

	public void updateTarget(ClusSchema schema) {
		if (checkHeuristic("SSPD")) {
			schema.addAttrType(new IntegerAttrType("SSPD"));
			int nb = schema.getNbAttributes();			
			m_Target.setValue(String.valueOf(nb));
		}
	}

	public void updateDataFile(String fname) {
		if (isNullFile())
			m_DataFile.setValue(fname);
	}

	public void initialize(CMDLineArgs cargs, boolean loads) throws IOException {
		create();
		initNamedValues();
		if (cargs != null)
			preprocess(cargs);
		if (loads) {
			try {
				String fname = getFileAbsolute(getAppName() + ".s");
				m_Ini.load(fname, '%');
			} catch (FileNotFoundException e) {
				System.out.println("No settings file found");
			}
		}
		if (cargs != null)
			process(cargs);
		updateDataFile(getAppName() + ".arff");
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

	public void setFTest(double ftest) {
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
		ONE_NOMINAL = (schema.getNbNominalTargetAttributes() == 1 && schema
				.getNbNumericTargetAttributes() == 0);
		SIZE_PENALTY = getSizePenalty();
		BEAM_WIDTH = m_BeamWidth.getValue();
		VERBOSE = m_Verbose.getValue();
	}

	public void initHierarchical() {
		ClassesValue.setHSeparator(m_HierSep.getValue());
		ClassesValue.setEmptySetIndicator(m_HierEmptySetIndicator.getValue());
	}

	public static int enableVerbose(int talk) {
		int prev = VERBOSE;
		VERBOSE = talk;
		return prev;
	}

	public String getTarget() {
		return m_Target.getValue();
	}

	public String getDisabled() {
		return m_Disabled.getValue();
	}

	public String getClustering() {
		return m_Clustering.getValue();
	}

	public String getDescriptive() {
		return m_Descriptive.getValue();
	}

	public String getKey() {
		return m_Key.getValue();
	}

	public void setTarget(String str) {
		m_Target.setValue(str);
	}

	public void setDisabled(String str) {
		m_Disabled.setValue(str);
	}

	public void setClustering(String str) {
		m_Clustering.setValue(str);
	}

	public void setDescriptive(String str) {
		m_Descriptive.setValue(str);
	}

	public double getFTest() {
		return m_FTest.getValue();
	}

	public double getMinimalWeight() {
		return m_MinW.getValue();
	}
	
	public void setMinimalWeight(double val) {
		m_MinW.setValue(val);
	}
	
	public int getResourceInfoLoaded() {
		return m_ResourceInfoLoaded.getValue();
	}
	
	public int getCompatibility() {
		return m_Compatibility.getValue();
	}

	public void updateDisabledSettings() {
		int pruning = getPruningMethod();
		m_M5PruningMult.setEnabled(pruning == PRUNING_METHOD_M5 || pruning == PRUNING_METHOD_M5_MULTI);
		m_PruneSetMax.setEnabled(!m_PruneSet.isString(NONE));
		m_1SERule.setEnabled(pruning == PRUNING_METHOD_GAROFALAKIS_VSB);
		if (ResourceInfo.isLibLoaded()) m_ResourceInfoLoaded.setSingleValue(RESOURCE_INFO_LOAD_YES);
		else m_ResourceInfoLoaded.setSingleValue(RESOURCE_INFO_LOAD_NO);
	}

	public void show(PrintWriter where) throws IOException {
		updateDisabledSettings();
		m_Ini.save(where);
	}

	public void setAppName(String file) {
		file = StringUtils.removeSuffix(file, ".gz");
		file = StringUtils.removeSuffix(file, ".arff");
		file = StringUtils.removeSuffix(file, ".s");
		file = StringUtils.removeSuffix(file, ".");
		m_AppName = FileUtil.removePath(file);
		m_DirName = FileUtil.getPath(file);
	}

	public StatisticPrintInfo getStatisticPrintInfo() {
		StatisticPrintInfo info = new StatisticPrintInfo();
		info.SHOW_EXAMPLE_COUNT = m_ShowInfo.contains(0);  
		info.SHOW_DISTRIBUTION = m_ShowInfo.contains(1);
		info.SHOW_INDEX = m_ShowInfo.contains(2);
		info.INTERNAL_DISTR = m_ShowInfo.contains(3);
		return info;
	}
	
	public boolean getShowModel(int i) {
		return m_ShowModels.contains(i);  
	}
	
	public void setDate(Date date) {
		m_Date = date;
	}

	public Date getDate() {
		return m_Date;
	}

	public String getFileAbsolute(String fname) {
		// System.out.println("Dir name: '"+m_DirName+"'");
		// System.out.println("File name: '"+fname+"'");
		if (m_DirName == null) {
			return fname;
		} else {
			if (FileUtil.isAbsolutePath(fname)) {
				return fname;
			} else {
				return m_DirName + File.separator + fname;
			}
		}
	}

	public PrintWriter getFileAbsoluteWriter(String fname)
			throws FileNotFoundException {
		String path = getFileAbsolute(fname);
		return new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(path)));
	}

	public String getAppName() {
		return m_AppName;
	}

}
