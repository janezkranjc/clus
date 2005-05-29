package clus.main;

import clus.tools.debug.Debug;

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
	protected transient ClusStatistic m_Statistic;
	protected TargetSchema m_Target;
	protected ClusSchema m_Schema;
	protected boolean m_BeamSearch;
	protected boolean m_RuleInduce;
	protected Settings m_Settings;
	protected TargetWeightProducer m_GlobalWeights;
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
	
	public final int getMode() {
		return m_Mode;
	}
	
	public final ClusSchema getSchema() {
		return m_Schema;
	}
	
	public final TargetSchema getTargetSchema() {
		return m_Target;
	}
	
	public final ClassHierarchy getHier() {
		System.out.println("ClusStatManager.getHier/0 called");
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
	
	public void setGlobalStat(ClusStatistic stat) {
		m_GlobalWeights.setTotalStat(stat);
		m_GlobalStat = stat;
	}
	
	public ClusStatistic getGlobalStat() {
		return m_GlobalStat;
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
	
	public TargetWeightProducer createTargetWeightProducer() {
		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			if (hiermode == Settings.HIERMODE_TREE_DIST_WEUCLID) {
				return new HierarchicalTargetWeightProducer(this, m_Hier);
			} 
			break;
		case MODE_REGRESSION:
			return new NormalizedTargetWeights(this);
		}
		return new TargetWeightProducer(this);		
	}
	
	public void initStructure() throws IOException {
		switch (m_Mode) {
		case MODE_HIERARCHICAL:
			createHierarchy();
			break;
		case MODE_SSPD:
			m_SSPDMtrx = SSPDMatrix.read(getSettings().getAppName()+".dist");
			break;
		}
	}
	
	public void initWeights() {
		m_GlobalWeights = createTargetWeightProducer();
	}
	
	public void initStatistic() throws ClusException {
		switch (m_Mode) {
		case MODE_CLASSIFY:
			m_Statistic = new ClassificationStat(m_Target);
			break;
		case MODE_REGRESSION:
			m_Statistic = new RegressionStat(m_Target.getNbNum());
			break;
		case MODE_HIERARCHICAL:
			int hiermode = getSettings().getHierMode();
			switch (hiermode) {
			case Settings.HIERMODE_TREE_DIST_ABS_WEUCLID:
				m_Statistic = new WAHNDStatistic(m_Hier); break;
			case Settings.HIERMODE_TREE_DIST_WEUCLID:
				m_Statistic = new WHTDStatistic(m_Hier); break;
			case Settings.HIERMODE_XTAX_SET_DIST:
				m_Statistic = new SPMDStatistic(m_Hier); break;
			case Settings.HIERMODE_XTAX_SET_DIST_DISCRETE:
				m_Statistic = new DHierStatistic(m_Hier); break;
			}
			break;
		case MODE_SSPD:
			m_Statistic = new SSPDStatistic(m_SSPDMtrx);
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
	
	public void initHeuristic() {
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
			m_Heuristic = new SSDHeuristic(name, createStatistic(), createTargetWeightProducer()); break;				
			case Settings.HIERMODE_TREE_DIST_WEUCLID:
				name = "Weighted Hierarchical Tree Distance";
			m_Heuristic = new SSDHeuristic(name, createStatistic(), createTargetWeightProducer()); break;
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
			if (getSettings().shouldNormalize()) {
				parent.addError(new RMSError(parent, m_GlobalWeights));
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
				parent.addError(new HierRMSError(parent, m_GlobalWeights, true, true, m_Hier));				
				parent.addError(new HierRMSError(parent, m_GlobalWeights, true, false, m_Hier));							
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
			int hiermode = getSettings().getHierMode();
			switch (hiermode) {
			case Settings.HIERMODE_TREE_DIST_WEUCLID:
				parent.addError(new HierLevelAccuracy(parent, m_Hier));
				// parent.addError(new HierRMSError(parent, m_GlobalWeights, false, true, m_Hier));
				break;
			}
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
	
	public PruneTree getTreePrunerNoVSB() {
		int maxsize = -1;
		Settings sett = getSettings();
		if (isBeamSearch() && sett.isBeamPostPrune()) {
			maxsize = sett.getTreeMaxSize();
		} else {
			maxsize = sett.getSizeConstraintPruning();
		}
		if (maxsize != -1) {
			sett.setPruningMethod(Settings.PRUNING_METHOD_GAROFALAKIS);
			return new SizeConstraintPruning(maxsize, createTargetWeightProducer());
		}
		if (m_Mode == MODE_REGRESSION) {
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_DEFAULT ||
				sett.getPruningMethod() == Settings.PRUNING_METHOD_M5) {
					sett.setPruningMethod(Settings.PRUNING_METHOD_M5);
					return new M5Pruner(createTargetWeightProducer());
			}		
		}
		if (m_Mode == MODE_HIERARCHICAL) {
			if (sett.getPruningMethod() == Settings.PRUNING_METHOD_DEFAULT ||
					sett.getPruningMethod() == Settings.PRUNING_METHOD_M5) {
				sett.setPruningMethod(Settings.PRUNING_METHOD_M5);
				return new M5Pruner(m_GlobalWeights);
			}
		}
		sett.setPruningMethod(Settings.PRUNING_METHOD_NONE);
		return new DummyPruner();
	}
	
	public PruneTree getTreePruner(ClusData pruneset) {
		Settings sett = getSettings();
		if (pruneset != null) {
			if (sett.isHierPruneInSig() != 0.0) {
				PruneTree pruner = getTreePrunerNoVSB(); 
				return new HierRemoveInsigClasses(sett.isHierPruneInSig(), pruneset, pruner, m_Hier);
			} else {				
				ClusErrorParent parent = createEvalError();
				sett.setPruningMethod(Settings.PRUNING_METHOD_REDERR_VSB);
				return new VSBPruning(parent, (RowData)pruneset);
			}
		} else {
			return getTreePrunerNoVSB();
		}
	}
	
	public Settings getSettings() {
		return m_Settings;
	}
	
	public ClusStatistic createStatistic() {
		return m_Statistic.cloneStat();
	}
	
	public ClusStatistic getStatistic() {
		return m_Statistic;
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
