package clus.main;

import clus.tools.debug.Debug;

import jeans.io.ini.INIFileNominalOrDoubleOrVector;
import jeans.math.matrix.*;

import clus.util.*;
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

public class ClusStatManager implements Serializable {
	
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
	protected TargetWeightProducer m_GlobalTargetWeights;
  protected TargetWeightProducer m_GlobalWeights;
  protected ClusStatistic m_GlobalTargetStat;
  protected ClusStatistic m_GlobalStat;
  
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
	
  public void setGlobalTargetStat(ClusStatistic stat) {
    m_GlobalTargetWeights.setTotalStat(stat);
    m_GlobalTargetStat = stat;
  }

  public void setGlobalStat(ClusStatistic stat) {
    m_GlobalWeights.setTotalStat(stat);
    m_GlobalStat = stat;
  }

  /**
   * Returns the global weights of target attributes.
   * @return the weights
   */
  public TargetWeightProducer getGlobalTargetWeights() {
    return m_GlobalTargetWeights;
  }
	
  /**
   * Returns the global weights of all attributes.
   * @return the weights
   */
  public TargetWeightProducer getGlobalWeights() {
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
	
	public TargetWeightProducer createTargetWeightProducer() throws ClusException {
		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			if (hiermode == Settings.HIERMODE_TREE_DIST_WEUCLID) {
				return new HierarchicalTargetWeightProducer(this, m_Hier);
			} 
			break;
		case MODE_REGRESSION:
			int nb_num = m_Target.getNbNum();			
			INIFileNominalOrDoubleOrVector winfo = getSettings().getTargetWeights();						
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
		return new TargetWeightProducer(this);		
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
	
	public void initWeights() throws ClusException {
		m_GlobalTargetWeights = createTargetWeightProducer();
    NormalizedTargetWeights weights = new NormalizedTargetWeights(this);
    weights.setAllNormalize(m_Schema.m_NbDoubles);
    m_GlobalWeights = weights;
  }
	
	public void initStatistic() throws ClusException {

    // TODO: This is a temporary solution!    
    m_AllStatistic = new CombStat(this);
      
		switch (m_Mode) {
		case MODE_CLASSIFY:
			m_TargetStatistic = new ClassificationStat(m_Target);
			break;
		case MODE_REGRESSION:
			m_TargetStatistic = new RegressionStat(m_Target.getNbNum());
			break;
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			switch (hiermode) {
			case Settings.HIERMODE_TREE_DIST_ABS_WEUCLID:
				m_TargetStatistic = new WAHNDStatistic(m_Hier); break;
			case Settings.HIERMODE_TREE_DIST_WEUCLID:
				m_TargetStatistic = new WHTDStatistic(m_Hier); break;
			case Settings.HIERMODE_XTAX_SET_DIST:
				m_TargetStatistic = new SPMDStatistic(m_Hier); break;
			case Settings.HIERMODE_XTAX_SET_DIST_DISCRETE:
				m_TargetStatistic = new DHierStatistic(m_Hier); break;
			}
			break;
		case MODE_SSPD:
			m_TargetStatistic = new SSPDStatistic(m_SSPDMtrx);
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
					m_Heuristic = new ClusRuleHeuristicError(createTargetWeightProducer());									
				} else {
					m_Heuristic = new ClusRuleHeuristicMEstimate(getSettings().getMEstimate());
				}				
			} else {
				m_Heuristic = new ClusRuleHeuristicError(createTargetWeightProducer());
			}
			return;
		}
		if (isBeamSearch()) {
			if (getSettings().getHeuristic() == Settings.HEURISTIC_REDUCED_ERROR) {
				m_Heuristic = new ClusBeamHeuristicError(createStatistic());
			} else if (getSettings().getHeuristic() == Settings.HEURISTIC_MESTIMATE) {
				m_Heuristic = new ClusBeamHeuristicMEstimate(createStatistic(), getSettings().getMEstimate());
			} else {
				m_Heuristic = new ClusBeamHeuristicSS(createStatistic(), createTargetWeightProducer());    			
			}
			return;
		}
		switch (m_Mode) {
		case MODE_CLASSIFY:
			if (getSettings().getHeuristic() == Settings.HEURISTIC_REDUCED_ERROR) {
				m_Heuristic = new ReducedErrorHeuristic(createStatistic());
			} else {
				m_Heuristic = new GainHeuristic();
			}
			break;
		case MODE_REGRESSION:
			m_Heuristic = new SSReductionHeuristic(createTargetWeightProducer());
			break;
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			switch (hiermode) {
			case Settings.HIERMODE_TREE_DIST_ABS_WEUCLID:
				name = "Weighted Absolute Hierarchical Tree Distance";
			m_Heuristic = new SSDHeuristic(name, createStatistic(), createTargetWeightProducer(), getSettings().isHierNoRootPreds()); break;				
			case Settings.HIERMODE_TREE_DIST_WEUCLID:
				name = "Weighted Hierarchical Tree Distance";
			m_Heuristic = new SSDHeuristic(name, createStatistic(), createTargetWeightProducer(), getSettings().isHierNoRootPreds()); break;
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
			return new SizeConstraintPruning(sett.getTreeMaxSize(), createTargetWeightProducer());			
		}
		int size_nb = sett.getSizeConstraintPruningNumber();
		if (size_nb > 0) {
			int[] sizes = new int[size_nb];
			for (int i = 0; i < size_nb; i++) {
				sizes[i] = sett.getSizeConstraintPruning(i);
			}			
			sett.setPruningMethod(Settings.PRUNING_METHOD_GAROFALAKIS);
			return new SizeConstraintPruning(sizes, createTargetWeightProducer());
		}
		if (m_Mode == MODE_REGRESSION) {
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_DEFAULT ||
				sett.getPruningMethod() == Settings.PRUNING_METHOD_M5) {
					sett.setPruningMethod(Settings.PRUNING_METHOD_M5);
					return new M5Pruner(createTargetWeightProducer());
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
		
	public ClusStatistic createStatistic() {
		return m_TargetStatistic.cloneStat();
	}

  /**
   * @param attType 0 - descriptive, 1 - clustering,2 - target, -1 - all attributes 
   * @return the statistic
   */
  public ClusStatistic createStatistic(int attType) {
    switch (attType) {
    case ClusAttrType.ATTR_USE_ALL:
      return m_AllStatistic.cloneStat();
    /* case ClusAttrType.ATTR_USE_CLUSTERING:
      return m_TargetStatistic.cloneStat();
    case ClusAttrType.ATTR_USE_DESCRIPTIVE:
      return m_TargetStatistic.cloneStat(); */
    case ClusAttrType.ATTR_USE_TARGET:
      return m_TargetStatistic.cloneStat();
    default:
      return m_AllStatistic.cloneStat();
    }
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
