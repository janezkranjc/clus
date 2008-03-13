package sit;

import java.io.IOException;
import java.util.Date;

import sit.mtLearner.AvgLearner;
import sit.mtLearner.ClusLearner;
import sit.mtLearner.MTLearner;
import sit.searchAlgorithm.AllTargets;
import sit.searchAlgorithm.GeneticSearch;
import sit.searchAlgorithm.SearchAlgorithm;

import jeans.resource.ResourceInfo;
import jeans.util.IntervalCollection;
import jeans.util.cmdline.CMDLineArgs;
import jeans.util.cmdline.CMDLineArgsProvider;
import clus.algo.ClusInductionAlgorithmType;
import clus.data.io.ARFFFile;
import clus.data.io.ClusReader;
import clus.data.io.ClusView;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;
import clus.data.type.ClusSchema;
import clus.data.type.NumericAttrType;
import clus.main.ClusStat;
import clus.main.Settings;
import clus.util.ClusException;
import clus.util.ClusRandom;

public class Sit implements CMDLineArgsProvider{

	protected Settings m_Sett = new Settings();
	protected ClusSchema m_Schema;
	protected RowData m_Data;
	protected MTLearner m_Learner;
	protected SearchAlgorithm m_Search;
	
	
	private Sit(){} //make sit a singleton
	private static Sit singleton = null;
	
	/**
	 * Returns the one and only Sit instance (singleton pattern)
	 * @return Sit singleton
	 */
	public static Sit getInstance(){
		if(singleton == null){
			singleton = new Sit();
		}
		return singleton;
	}
	
	/**
	 * Initialize:
	 * 	-load settings
	 *  -create schema
	 *  -read in data
	 * @throws IOException
	 * @throws ClusException
	 */
	public void initialize() throws IOException, ClusException {
		// Load settings file
		ARFFFile arff = null;
		System.out.println("Loading '" + m_Sett.getAppName() + "'");
		ClusRandom.initialize(m_Sett);
		ClusReader reader = new ClusReader(m_Sett.getDataFile(), m_Sett);
		System.out.println();
			System.out.println("Reading ARFF Header");
			arff = new ARFFFile(reader);
			m_Schema = arff.read(m_Sett);
		// Count rows and move to data segment
		System.out.println();
		System.out.println("Reading CSV Data");
		m_Schema.setNbRows(reader.countRows());
		System.out.println("Found " + m_Schema.getNbRows() + " rows");
		if (arff != null)
			arff.skipTillData();
		// Updata schema based on settings
		m_Sett.updateTarget(m_Schema);
		m_Schema.initializeSettings(m_Sett);
		m_Sett.setTarget(m_Schema.getTarget().toString());
		m_Sett.setDisabled(m_Schema.getDisabled().toString());
		m_Sett.setClustering(m_Schema.getClustering().toString());
		m_Sett.setDescriptive(m_Schema.getDescriptive().toString());		
		// Load data from file
		if (ResourceInfo.isLibLoaded()) {
			ClusStat.m_InitialMemory = ResourceInfo.getMemory();
		}
		m_Data = new RowData(m_Schema);
		m_Data.resize(m_Schema.getNbRows());
		ClusView view = m_Data.createNormalView(m_Schema);
		view.readData(reader, m_Schema);
		reader.close();
		// Preprocess and initialize induce
		m_Sett.update(m_Schema);
		// Set XVal field in Settings
		Settings.IS_XVAL = true;
		System.out.println("Has missing values: " + m_Schema.hasMissing());		
	}
	
	/**
	 * Returns the current settings object
	 * @return settings
	 */
	public final Settings getSettings() {
		return m_Sett;
	}
	
	/**
	 * Initialize the settings object
	 * @param cargs Commandline arguments
	 * @throws IOException
	 */
	public final void initSettings(CMDLineArgs cargs) throws IOException {
		m_Sett.initialize(cargs, true);
	}
	
	/**
	 * Initialize the MTLearner with the current data and settings.
	 */
	private void InitLearner() {
		//this.m_Learner = new AvgLearner();
		this.m_Learner = new AvgLearner();
		this.m_Learner.init(this.m_Data,this.m_Sett);
		
	}
	/**
	 * Initialize the SearchAlgorithm
	 */
	private void InitSearchAlgorithm() {
		this.m_Search = new GeneticSearch();
		//this.m_Search = new AllTargets();
		this.m_Search.setMTLearner(this.m_Learner);
		
	}
	
	/**
	 * Start the search for the optimal subset using the current learner and search algorithm
	 * @return Targetset The found subset
	 */
	public TargetSet search(){
		
		 int mt = new Integer(m_Sett.getMainTarget())-1;
		 
		ClusAttrType mainTarget = m_Schema.getAttrType(mt);
		IntervalCollection candidates = new IntervalCollection(m_Sett.getTarget());
		TargetSet candidateSet = new TargetSet(m_Schema,candidates);
		return m_Search.search(mainTarget, candidateSet);
		
	}
	
	/*************************************
	 * CMDLineArgsProvider implementation
	 *************************************/
	public final static String[] OPTION_ARGS = {"xval"};
	public final static int[] OPTION_ARITIES = {0};
	
	public int getNbMainArgs() {
		return 1;
	}

	public int[] getOptionArgArities() {
		return OPTION_ARITIES;
	}

	public String[] getOptionArgs() {
		return OPTION_ARGS;
	}

	public void showHelp() {	}
	
	/***************************************
	 * MAIN
	 ***************************************/
	
	/**
	 * @param args
	 * @throws ClusException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, ClusException {
		Sit sit = Sit.getInstance();
		Settings sett = sit.getSettings();
		CMDLineArgs cargs = new CMDLineArgs(sit);
		cargs.process(args);
		if (cargs.getNbMainArgs() == 0) {
			sit.showHelp();
			System.out.println();
			System.out.println("Expected main argument");
			System.exit(0);
		}	
		if (cargs.allOK()) {
			sett.setDate(new Date());
			sett.setAppName(cargs.getMainArg(0));
			sit.initSettings(cargs);
		
		}else{
			System.err.println("Arguments not ok?!");
			
		}
		sit.initialize();
		
		
		/* Init the Learner */
		sit.InitLearner();
		/* Init the Search algorithm */
		sit.InitSearchAlgorithm();
		
		
		/* Search for the optimal subset */
		sit.search();
		
		
	}

	
}
