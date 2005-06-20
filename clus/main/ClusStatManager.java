package clus.main;

import clus.tools.debug.Debug;

import jeans.io.ini.INIFileNominalOrDoubleOrVector;
import jeans.math.matrix.*;

import clus.util.*;
import clus.data.attweights.*;
import clus.data.type.*;
import clus.data.rows.*;
import clus.error.*;
import clus.error.multiscore.*;
import clus.heuristic.*;
import clus.statistic.*;
import clus.pruning.*;

import clus.ext.hierarchical.*;
import clus.ext.sspd.*;
import clus.ext.beamsearch.*;

import clus.algo.rules.*;

import java.io.*;
import java.util.*;

public class ClusStatManager implements Serializable {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;	
	
	public final static int SUBM_DEFAULT = 0;
	
	public final static int MODE_NONE = -1;
	public final static int MODE_CLASSIFY = 0;
	public final static int MODE_REGRESSION = 1;
	public final static int MODE_HIERARCHICAL = 2;
	public final static int MODE_SSPD = 3;
  
	protected int m_Mode = MODE_NONE;
	protected transient ClusHeuristic m_Heuristic;
	protected transient ClusStatistic m_TargetStatistic, m_AllStatistic;
	protected TargetSchema m_Target;
	protected ClusSchema m_Schema;
	protected boolean m_BeamSearch;
	protected boolean m_RuleInduce;
	protected Settings m_Settings;
	protected ClusAttributeWeights m_GlobalTargetWeights;
	protected ClusAttributeWeights m_GlobalWeights;
	protected ClusStatistic m_GlobalTargetStat;
	protected ClusStatistic m_GlobalStat;    
  	protected ClusStatistic[] m_StatisticAttrUse;
  	protected ClusAttributeWeights m_NormalizationWeights;  
  	protected ClusNormalizedAttributeWeights m_ClusteringWeights;
  	protected ClusNormalizedAttributeWeights m_CompactnessWeights;  	
	protected ClassHierarchy m_HierN, m_HierF, m_Hier;
	protected SSPDMatrix m_SSPDMtrx;
	
	public ClusStatManager(ClusSchema schema, Settings sett) throws ClusException, IOException {
		this(schema, sett, true);
	}
	
	public ClusStatManager(ClusSchema schema, Settings sett, boolean docheck) throws ClusException, IOException {
		m_Schema = schema;
		m_Target = schema.getTargetSchema();
		m_Settings = sett;
		if (docheck) {
			check();
			initStructure();
		}
	}	
	
	public Settings getSettings() {
		return m_Settings;
	}	
	
	public final ClusSchema getSchema() {
		return m_Schema;
	}
	
	public final int getMode() {
		return m_Mode;
	}	
	
	public final TargetSchema getTargetSchema() {
		return m_Target;
	}
	
	public final ClassHierarchy getHier() {
//		System.out.println("ClusStatManager.getHier/0 called");
		return m_Hier;
	}
	
	public final ClassHierarchy getNormalHier() {
		return m_HierN;
	}
	
	public void initSH() throws ClusException {
		initWeights();
		initStatistic();
		initHeuristic();
	}
	
	public ClusNormalizedAttributeWeights getClusteringWeights() {
		return m_ClusteringWeights;
	}
	
	public ClusNormalizedAttributeWeights getCompactnessWeights() {
		return m_CompactnessWeights;
	}

	public ClusAttributeWeights getNormalizationWeights() {
		return m_NormalizationWeights;
	}	
	
	public void setGlobalTargetStat(ClusStatistic stat) {
		m_GlobalTargetStat = stat;
	}

	public static boolean hasBitEqualToOne(boolean[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i]) return true;
		}
		return false;
	}
	
	public void initWeights(ClusNormalizedAttributeWeights result, NumericAttrType[] num, NominalAttrType[] nom, INIFileNominalOrDoubleOrVector winfo) throws ClusException {
		result.setAllWeights(1.0);
		int nbattr = result.getNbAttributes();
		if (winfo.hasArrayIndexNames()) {
			// Weights given for target, non-target, numeric and nominal
			double target_weight = winfo.getDouble(Settings.TARGET_WEIGHT);
			double non_target_weight = winfo.getDouble(Settings.NON_TARGET_WEIGHT);
			double num_weight = winfo.getDouble(Settings.NUMERIC_WEIGHT);
			double nom_weight = winfo.getDouble(Settings.NOMINAL_WEIGHT);
			System.out.println("  Target weight     = "+target_weight);
			System.out.println("  Non target weight = "+non_target_weight);
			System.out.println("  Numeric weight    = "+num_weight);
			System.out.println("  Nominal weight    = "+nom_weight);
			for (int i = 0; i < num.length; i++) {
				NumericAttrType cr_num = num[i];
				double tw = cr_num.getStatus() == ClusAttrType.STATUS_TARGET ? target_weight : non_target_weight;
				result.setWeight(cr_num, num_weight * tw);
			}
			for (int i = 0; i < nom.length; i++) {
				NominalAttrType cr_nom = nom[i];
				double tw = cr_nom.getStatus() == ClusAttrType.STATUS_TARGET ? target_weight : non_target_weight;
				result.setWeight(cr_nom, nom_weight * tw);
			}
		} else if (winfo.isVector()) {
			// Explicit vector of weights given
			if (nbattr != winfo.getVectorLength()) {
				throw new ClusException("Number of attributes is "+nbattr+" but weight vector has only "+winfo.getVectorLength()+" components");
			}
			for (int i = 0; i < nbattr; i++) {
				result.setWeight(i, winfo.getDouble(i));
			}
		} else {
			// One single constant weight given
			result.setAllWeights(winfo.getDouble());
		}
	}
	
	public void initCompactnessWeights() throws ClusException {
		NumericAttrType[] num = m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_ALL);		
		NominalAttrType[] nom = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_ALL);
		initWeights(m_CompactnessWeights, num, nom, getSettings().getCompactnessWeights());
		System.out.println("Compactness:   "+m_CompactnessWeights.getName(m_Schema.getAllAttrUse(ClusAttrType.ATTR_USE_ALL)));
	}

	public void initClusteringWeights() throws ClusException {
		NumericAttrType[] num = m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);		
		NominalAttrType[] nom = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_CLUSTERING);
		initWeights(m_ClusteringWeights, num, nom, getSettings().getClusteringWeights());		
		System.out.println("Clustering: "+m_ClusteringWeights.getName(m_Schema.getAllAttrUse(ClusAttrType.ATTR_USE_CLUSTERING)));
	}
	
	public void initNormalizationWeights(ClusStatistic stat) throws ClusException {
		m_GlobalStat = stat;		
		int nbattr = m_Schema.getNbAttributes();
		m_NormalizationWeights.setAllWeights(1.0);		
		boolean[] shouldNormalize = new boolean[nbattr];		
		INIFileNominalOrDoubleOrVector winfo = getSettings().getNormalizationWeights();						
		if (winfo.isVector()) {				
			if (nbattr != winfo.getVectorLength()) {
				throw new ClusException("Number of attributes is "+nbattr+" but weight vector has only "+winfo.getVectorLength()+" components");
			}
			for (int i = 0; i < nbattr; i++) {
				if (winfo.isNominal(i)) shouldNormalize[i] = true;
				else m_NormalizationWeights.setWeight(i, winfo.getDouble(i));
			}				
		} else {
			if (winfo.isNominal() && winfo.getNominal() == Settings.NORMALIZATION_DEFAULT) {
				Arrays.fill(shouldNormalize, true);
			} else {				
				m_NormalizationWeights.setAllWeights(winfo.getDouble());
			}
		}
		if (hasBitEqualToOne(shouldNormalize)) {
			CombStat cmb = (CombStat)stat;
			RegressionStat rstat = cmb.getRegressionStat();			
			rstat.initNormalizationWeights(m_NormalizationWeights, shouldNormalize);
		}
	}

	public void initWeights() {
		int nbattr = m_Schema.getNbAttributes();
		m_NormalizationWeights = new ClusAttributeWeights(nbattr);
		m_NormalizationWeights.setAllWeights(1.0);		
		m_ClusteringWeights = new ClusNormalizedAttributeWeights(m_NormalizationWeights);
		m_CompactnessWeights = new ClusNormalizedAttributeWeights(m_NormalizationWeights);		
	}
	
  /**
   * Returns the global weights of target attributes.
   * @return the weights
   */
  public ClusAttributeWeights getGlobalTargetWeights() {
    return m_GlobalTargetWeights;
  }
	
  /**
   * Returns the global weights of all attributes.
   * @return the weights
   */
  public ClusAttributeWeights getGlobalWeights() {
    return m_GlobalWeights;
  }  
  
	public ClusStatistic getGlobalStat() {
		return m_GlobalTargetStat;
	}
	
	public void check() throws ClusException {
		int nb_types = 0;
		int nb_nom = m_Target.getNbNom();
		int nb_num = m_Target.getNbNum();
		int nb_int = m_Target.getNbType(IntegerAttrType.THIS_TYPE);
		if (nb_nom > 0) {
			m_Mode = MODE_CLASSIFY;
			nb_types++;
		}
		if (nb_num > 0) {
			m_Mode = MODE_REGRESSION;
			nb_types++;
		}
		int nb_class = m_Target.getNbType(ClassesAttrType.THIS_TYPE);
		if (nb_class > 0) {
			m_Mode = MODE_HIERARCHICAL;
			getSettings().setSectionHierarchicalEnabled(true);
			nb_types++;
		}
		if (nb_int > 0) {
			m_Mode = MODE_SSPD;
			nb_types++;
		}
		if (nb_types == 0) {
			System.err.println("No target value defined");
		}
		if (nb_types > 1) throw new ClusException("Can't mix different type target values");
	}
	
	public ClusAttributeWeights createClusAttributeWeights() throws ClusException {
/*		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			if (hiermode == Settings.HIERMODE_TREE_DIST_WEUCLID) {
				return new HierarchicalClusAttributeWeights(this, m_Hier);
			} 
			break;
		case MODE_REGRESSION:
			int nb_num = m_Target.getNbNum();			
			INIFileNominalOrDoubleOrVector winfo = getSettings().getNormalizationWeights();						
			NormalizedTargetWeights wi = new NormalizedTargetWeights(this);
			if (winfo.isVector()) {				
				wi.setNbTarget(nb_num);
				if (nb_num != winfo.getVectorLength()) {
					throw new ClusException("Number of numeric targets is "+nb_num+" but weight vector has only "+winfo.getVectorLength()+" components");
				}
				for (int i = 0; i < nb_num; i++) {
					if (winfo.isNominal(i)) wi.setNormalize(i, true);
					else wi.setWeight(i, winfo.getDouble(i));
				}				
			} else {
				if (winfo.isNominal() && winfo.getNominal() == Settings.NORMALIZATION_DEFAULT) {
					wi.setAllNormalize(nb_num); 
				} else {				
					wi.setAllFixed(nb_num, winfo.getDouble());
				}
			}
			return wi;
		}
		return new ClusAttributeWeights(this);		*/
		return getClusteringWeights();
	}
	
	public void initStructure() throws IOException {
		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			createHierarchy();
			break;
		case MODE_SSPD:
			m_SSPDMtrx = SSPDMatrix.read(getSettings().getAppName()+".dist", getSettings());
			break;
		}
	}
		
	public void initStatistic() throws ClusException {
		m_StatisticAttrUse = new ClusStatistic[ClusAttrType.NB_ATTR_USE];
		
		if (m_Mode == MODE_CLASSIFY || m_Mode == MODE_REGRESSION) {
			// TODO: This is a temporary solution!    
			NumericAttrType[] num = m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_ALL);
			NominalAttrType[] nom = m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_ALL);			
			m_StatisticAttrUse[ClusAttrType.ATTR_USE_ALL] = new CombStat(this, num, nom);
		}      
    
		switch (m_Mode) {
		case MODE_CLASSIFY:
			setTargetStatistic(new ClassificationStat(m_Schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET)));
			break;
		case MODE_REGRESSION:
			setTargetStatistic(new RegressionStat(m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET)));
			break;
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			switch (hiermode) {
			case Settings.HIERMODE_TREE_DIST_ABS_WEUCLID:
				setTargetStatistic(new WAHNDStatistic(m_Hier)); break;
			case Settings.HIERMODE_TREE_DIST_WEUCLID:
				setTargetStatistic(new WHTDStatistic(m_Hier)); break;
			case Settings.HIERMODE_XTAX_SET_DIST:
				setTargetStatistic(new SPMDStatistic(m_Hier)); break;
/*			case Settings.HIERMODE_XTAX_SET_DIST_DISCRETE:
				setTargetStatistic(DHierStatistic(m_Hier)); break; */
			}
			break;
		case MODE_SSPD:
			setTargetStatistic(new SSPDStatistic(m_SSPDMtrx));
			break;
		}
	}
	
	public ClusHeuristic createHeuristic(int type) {
		switch (type) {
		case Settings.HEURISTIC_GAIN:
			return new GainHeuristic();
		default:
			return null;    			
		}
	}
	
	public void initHeuristic() throws ClusException {
		String name;
		if (isRuleInduce()) {
			if (m_Mode == MODE_CLASSIFY) {
				if (getSettings().getHeuristic() == Settings.HEURISTIC_REDUCED_ERROR) {
					m_Heuristic = new ClusRuleHeuristicError(createClusAttributeWeights());									
				} else {
					m_Heuristic = new ClusRuleHeuristicMEstimate(getSettings().getMEstimate());
				}				
			} else {
				m_Heuristic = new ClusRuleHeuristicError(createClusAttributeWeights());
			}
			return;
		}
		if (isBeamSearch()) {
			if (getSettings().getHeuristic() == Settings.HEURISTIC_REDUCED_ERROR) {
				m_Heuristic = new ClusBeamHeuristicError(createTargetStatistic());
			} else if (getSettings().getHeuristic() == Settings.HEURISTIC_MESTIMATE) {
				m_Heuristic = new ClusBeamHeuristicMEstimate(createTargetStatistic(), getSettings().getMEstimate());
			} else {
				m_Heuristic = new ClusBeamHeuristicSS(createTargetStatistic(), createClusAttributeWeights());    			
			}
			return;
		}
		switch (m_Mode) {
		case MODE_CLASSIFY:
			if (getSettings().getHeuristic() == Settings.HEURISTIC_REDUCED_ERROR) {
				m_Heuristic = new ReducedErrorHeuristic(createTargetStatistic());
			} else {
				m_Heuristic = new GainHeuristic();
			}
			break;
		case MODE_REGRESSION:
			m_Heuristic = new SSReductionHeuristic(createClusAttributeWeights(), m_Schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET));
			break;
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			switch (hiermode) {
			case Settings.HIERMODE_TREE_DIST_ABS_WEUCLID:
				name = "Weighted Absolute Hierarchical Tree Distance";
			m_Heuristic = new SSDHeuristic(name, createTargetStatistic(), createClusAttributeWeights(), getSettings().isHierNoRootPreds()); break;				
			case Settings.HIERMODE_TREE_DIST_WEUCLID:
				name = "Weighted Hierarchical Tree Distance";
			m_Heuristic = new SSDHeuristic(name, createTargetStatistic(), createClusAttributeWeights(), getSettings().isHierNoRootPreds()); break;
			case Settings.HIERMODE_XTAX_SET_DIST:
				m_Heuristic = new SPMDHeuristic(m_Hier); break;
			case Settings.HIERMODE_XTAX_SET_DIST_DISCRETE:
				m_Heuristic = new DHierHeuristic(m_Hier); break;
			}
			break;
		case MODE_SSPD:
			m_Heuristic = new SSPDHeuristic(m_SSPDMtrx);
			break;
		}
	}
	
	public ClusErrorParent createErrorMeasure(MultiScore score) {
		ClusErrorParent parent = new ClusErrorParent(m_Target, this);
		switch (m_Mode) {
		case MODE_CLASSIFY:
			parent.addError(new ContingencyTable(parent, m_Target));
			break;
		case MODE_REGRESSION:
			parent.addError(new AbsoluteError(parent));
			parent.addError(new RMSError(parent));
			if (getSettings().hasNonTrivialWeights()) {
				parent.addError(new RMSError(parent, m_GlobalTargetWeights));
			}
			parent.addError(new PearsonCorrelation(parent));			
			if (Settings.IS_MULTISCORE) {
				TargetSchema nts = MultiScoreWrapper.createTarSchema(m_Target);
				ContingencyTable ct = new ContingencyTable(parent, nts);
				parent.addError(new MultiScoreWrapper(ct));
				// parent.addError(new MultiScoreError(parent, score));
			}
			break;
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			switch (hiermode) {
			case Settings.HIERMODE_TREE_DIST_WEUCLID:
				parent.addError(new HierRMSError(parent, m_GlobalTargetWeights, true, true, m_Hier));				
				parent.addError(new HierRMSError(parent, m_GlobalTargetWeights, true, false, m_Hier));							
				parent.addError(new HierLevelAccuracy(parent, m_Hier));				
				parent.addError(new HierClassWiseAccuracy(parent, m_Hier));				
				break;
			case Settings.HIERMODE_TREE_DIST_ABS_WEUCLID:
				if (Debug.HIER_JANS_PAPER) {
					HierNodeWeights ws = new HierNodeWeights();
					double widec = Settings.HIER_W_PARAM.getValue();
					ws.initExponentialDepthWeights(m_HierN, widec);
					parent.addError(new WAHNDSqError(parent, m_HierN.getType(), false, ws));
					parent.addError(new WAHNDSqError(parent, m_HierN.getType(), true, ws));
					parent.addError(new HierBinNodeAccuracy(parent, m_HierN.getType()));
				} else {
					double[] widecs = {1.0,0.75,0.5,0.25};
					m_HierN.calcErrorWeights(widecs);
					parent.addError(new WAHNDError(parent,1.0));
					parent.addError(new WAHNDError(parent,0.75));
					parent.addError(new WAHNDError(parent,0.50));
					parent.addError(new WAHNDError(parent,0.25));
				}
			break;
			case Settings.HIERMODE_XTAX_SET_DIST:
			case Settings.HIERMODE_XTAX_SET_DIST_DISCRETE:
				int depth = m_HierN.getMaxDepth();
			MSymMatrix km1 = m_HierN.calcMatrix(new HierWeightSPath(depth, 1.0));
			MSymMatrix km2 = m_HierN.calcMatrix(new HierWeightSPath(depth, 0.75));
			MSymMatrix km3 = m_HierN.calcMatrix(new HierWeightSPath(depth, 0.5));
			MSymMatrix km4 = m_HierN.calcMatrix(new HierWeightSPath(depth, 0.25));
			parent.addError(new HierXtAXError(parent, km1, "1.0"));
			parent.addError(new HierXtAXError(parent, km2, "0.75"));
			parent.addError(new HierXtAXError(parent, km3, "0.5"));
			parent.addError(new HierXtAXError(parent, km4, "0.25"));
			break;
			}			
		}
		return parent;
	}
	
	public ClusErrorParent createEvalError() {
		ClusErrorParent parent = new ClusErrorParent(m_Target, this);
		switch (m_Mode) {
		case MODE_REGRESSION:
			parent.addError(new RMSError(parent));
			// parent.addError(new PearsonCorrelation(parent));
			break;
		case MODE_CLASSIFY:
			parent.addError(new Accuracy(parent));	    
			break;
		case MODE_HIERARCHICAL:
			if (Debug.HIER_JANS_PAPER) {
				HierNodeWeights ws = new HierNodeWeights();
				double widec = Settings.HIER_W_PARAM.getValue();
				ws.initExponentialDepthWeights(m_Hier, widec);
				parent.addError(new WAHNDSqError(parent, m_Hier.getType(), true, ws));
			} else {
				parent.addError(new HierXtAXError(parent, m_Hier.getKMatrix(), "prune"));
			}
			break;
		}
		return parent;
	}
	
	public ClusErrorParent createAdditiveError() {
		ClusErrorParent parent = new ClusErrorParent(m_Target, this);
		switch (m_Mode) {
		case MODE_REGRESSION:
			parent.addError(new MSError(parent));
			// parent.addError(new PearsonCorrelation(parent));
			break;
		case MODE_CLASSIFY:
			parent.addError(new Accuracy(parent));	    
			break;
		case MODE_HIERARCHICAL:
			parent.addError(new HierClassWiseAccuracy(parent, m_Hier));
			break;
		}
		return parent;
	}
	
	public ClusErrorParent createTuneError() {
		ClusErrorParent parent = new ClusErrorParent(m_Target, this);
		switch (m_Mode) {
		case MODE_REGRESSION:
			parent.addError(new PearsonCorrelation(parent));
			break;
		case MODE_CLASSIFY:
			parent.addError(new Accuracy(parent));	    
			break;
		case MODE_HIERARCHICAL:
			parent.addError(new HierClassWiseAccuracy(parent, m_Hier));			
			break;
		}
		return parent;
	}
	
	public PruneTree getTreePrunerNoVSB() throws ClusException {
		Settings sett = getSettings();
		if (isBeamSearch() && sett.isBeamPostPrune()) {
			sett.setPruningMethod(Settings.PRUNING_METHOD_GAROFALAKIS);
			return new SizeConstraintPruning(sett.getTreeMaxSize(), createClusAttributeWeights());			
		}
		int size_nb = sett.getSizeConstraintPruningNumber();
		if (size_nb > 0) {
			int[] sizes = new int[size_nb];
			for (int i = 0; i < size_nb; i++) {
				sizes[i] = sett.getSizeConstraintPruning(i);
			}			
			sett.setPruningMethod(Settings.PRUNING_METHOD_GAROFALAKIS);
			return new SizeConstraintPruning(sizes, createClusAttributeWeights());
		}
		if (m_Mode == MODE_REGRESSION) {
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_DEFAULT ||
				sett.getPruningMethod() == Settings.PRUNING_METHOD_M5) {
					sett.setPruningMethod(Settings.PRUNING_METHOD_M5);
					return new M5Pruner(createClusAttributeWeights());
			}		
		}
		if (m_Mode == MODE_HIERARCHICAL) {
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_M5) {
				return new M5Pruner(m_GlobalTargetWeights);
			}			
		}
		sett.setPruningMethod(Settings.PRUNING_METHOD_NONE);
		return new DummyPruner();
	}
	
	public PruneTree getTreePruner(ClusData pruneset) throws ClusException {
		Settings sett = getSettings();
		if (m_Mode == MODE_HIERARCHICAL) {
			PruneTree pruner = getTreePrunerNoVSB(); 
			HierRemoveInsigClasses hierpruner = new HierRemoveInsigClasses(pruneset, pruner, m_Hier);
			hierpruner.setSignificance(sett.isHierPruneInSig());
			hierpruner.setNoRootPreds(sett.isHierNoRootPreds());
			hierpruner.setNoRootAfterInSigPreds(sett.isHierPruneInSigTree());
			return hierpruner;
		}
		if (pruneset != null) {
			ClusErrorParent parent = createEvalError();
			sett.setPruningMethod(Settings.PRUNING_METHOD_REDERR_VSB);
			return new VSBPruning(parent, (RowData)pruneset);
		} else {
			return getTreePrunerNoVSB();
		}
	}
		
	public void setTargetStatistic(ClusStatistic stat) {
		m_StatisticAttrUse[ClusAttrType.ATTR_USE_TARGET] = stat;
	}
	
	public ClusStatistic createTargetStatistic() {
		return m_StatisticAttrUse[ClusAttrType.ATTR_USE_TARGET].cloneStat();
	}

  /**
   * @param attType 0 - descriptive, 1 - clustering,2 - target, -1 - all attributes 
   * @return the statistic
   */
	public ClusStatistic createStatistic(int attType) {
		return m_StatisticAttrUse[attType].cloneStat();
	}
  
	public ClusStatistic getStatistic() {
		return m_TargetStatistic;
	}
	
	public ClusHeuristic getHeuristic() {
		return m_Heuristic;
	}
	
	public String getHeuristicName() {
		return m_Heuristic.getName();
	}
	
	public void getPreprocs(DataPreprocs pps) {
	}
	
	public boolean needsHierarchyProcessors(){
		if (m_Mode == MODE_SSPD) return false;
		else return true;
	}
	
	public void setRuleInduce(boolean rule) {
		m_RuleInduce = rule;
	}
	
	public boolean isRuleInduce() {
		return m_RuleInduce;
	}
	
	public void setBeamSearch(boolean beam) {
		m_BeamSearch = beam;
	}
	
	public boolean isBeamSearch() {
		return m_BeamSearch;
	}
	
	private void createHierarchy() {
		int idx = 0;
		for (int i = 0; i < m_Schema.getNbAttributes(); i++) {
			ClusAttrType type = m_Schema.getAttrType(i);
			if (type instanceof ClassesAttrType) {
				ClassesAttrType cltype = (ClassesAttrType)type;
				System.out.println("Classes type: "+type.getName());
				if (idx == 0) m_HierN = cltype.getHier();
				else m_HierF = cltype.getHier();
				idx++;
			}
		}
		if (Settings.HIER_FLAT.getValue()) m_Hier = m_HierF;
		else m_Hier = m_HierN;
	}
}
