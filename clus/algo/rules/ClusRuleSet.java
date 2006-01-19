/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import java.util.*;

import jeans.util.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.error.ClusErrorParent;
import clus.main.*;
import clus.statistic.*;
import clus.util.*;

public class ClusRuleSet implements ClusModel, Serializable {
  
  public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected ClusStatistic m_TargetStat;
	protected ArrayList m_Rules = new ArrayList();
  /* Array of tuples covered by the default rule. */
  protected ArrayList m_DefaultData = new ArrayList();
  protected ClusStatManager m_StatManager;
  protected boolean m_HasRuleErrors;
  
  /**
   * Constructor for this class.
   * @param statmanager
   */
  public ClusRuleSet(ClusStatManager statmanager) {
    m_StatManager = statmanager;
  }
	
	public void add(ClusRule rule) {
		m_Rules.add(rule);
	}
	
  /** 
   * Returns the statistic for (prediction of) a given tuple.
   */
  public ClusStatistic predictWeighted(DataTuple tuple) {
  	int cover_method = getSettings().getCoveringMethod();
    if (cover_method == Settings.COVERING_METHOD_STANDARD) {
      for (int i = 0; i < getModelSize(); i++) {
        ClusRule rule = getRule(i);
        if (rule.covers(tuple)) return rule.getTargetStat();
      }
      return m_TargetStat;
    } else if (cover_method == Settings.COVERING_METHOD_UNION) {
    	// In multi-label classification: predicted set of classes is union of predictions by individual rules 
      boolean covered = false;
      ClusStatistic stat = m_TargetStat.cloneSimple();
      stat.unionInit();
      for (int i = 0; i < getModelSize(); i++) {
        ClusRule rule = getRule(i);
        if (rule.covers(tuple)) {
          stat.union(rule.getTargetStat());
          covered = true;         
        }
      }
      stat.unionDone();
      return covered ? stat : m_TargetStat;
    } else {
      boolean covered = false;
      ClusStatistic stat = m_TargetStat.cloneSimple();
      for (int i = 0; i < getModelSize(); i++) {
        ClusRule rule = getRule(i);
        if (rule.covers(tuple)) {
          ClusStatistic rulestat = rule.predictWeighted(tuple);
          stat.addPrediction(rulestat, 1);
          // TODO: Use weights different from 1 above! Probably different for
          // classification and for regression (and for mixed case)
          covered = true;         
        }
      }
      stat.calcMean();
      if (covered) {
        return stat;  
      } else {
        return m_TargetStat;
      }
    }
	}
	
	public void removeEmptyRules() {
		for (int i = getModelSize()-1; i >= 0; i--) {
			if (getRule(i).isEmpty()) {
				m_Rules.remove(i);
			}
		}
	}	
	
	public void simplifyRules() {
		for (int i = getModelSize()-1; i >= 0; i--) {
			getRule(i).simplify();
		}
	}
	
	public void attachModel(Hashtable table) throws ClusException {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.attachModel(table);
		}
	}
	
	public void printModel(PrintWriter wrt) {
		printModel(wrt, StatisticPrintInfo.getInstance());
	}	
	
	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
    boolean headers = getSettings().computeCompactness() || hasRuleErrors();
    // [train/test][comb/num/nom]
    double[][] avg_compactness = new double[2][3];
    double[] avg_coverage = new double[2];
    double[][] avg_prod = new double[2][3];
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
      if (headers) {
        String head = new String("Rule " + (i + 1) + ":");
        char[] underline = new char[head.length()];
        for (int j = 0; j < head.length(); j++) {
          underline[j] = '=';
        }
        wrt.println(head);
        wrt.println(new String(underline));
        // Added this test so that PrintRuleWiseErrors also works in HMC setting (02/01/06)
        if (getSettings().computeCompactness()) {
	        avg_compactness[0][0] += rule.m_CombStat[0].compactnessCalc();
	        avg_compactness[0][1] += rule.m_CombStat[0].compactnessNumCalc();
	        avg_compactness[0][2] += rule.m_CombStat[0].compactnessNomCalc();
	        avg_coverage[0] += rule.m_Coverage[0];
	        avg_prod[0][0] += rule.m_CombStat[0].compactnessCalc()*rule.m_Coverage[0];
	        avg_prod[0][1] += rule.m_CombStat[0].compactnessNumCalc()*rule.m_Coverage[0];
	        avg_prod[0][2] += rule.m_CombStat[0].compactnessNomCalc()*rule.m_Coverage[0];
	        if (rule.m_CombStat[1] != null) {
	          avg_compactness[1][0] += rule.m_CombStat[1].compactnessCalc();
	          avg_compactness[1][1] += rule.m_CombStat[1].compactnessNumCalc();
	          avg_compactness[1][2] += rule.m_CombStat[1].compactnessNomCalc();
	          avg_coverage[1] += rule.m_Coverage[1];
	          avg_prod[1][0] += rule.m_CombStat[1].compactnessCalc()*rule.m_Coverage[1];
	          avg_prod[1][1] += rule.m_CombStat[1].compactnessNumCalc()*rule.m_Coverage[1];
	          avg_prod[1][2] += rule.m_CombStat[1].compactnessNomCalc()*rule.m_Coverage[1];
	        }
        }
      }
			rule.printModel(wrt, info);
			wrt.println();
		}
		if (m_TargetStat != null && m_TargetStat.isValidPrediction()) {
      if (headers) {
        wrt.println("Default rule:");
        wrt.println("=============");
      }
      wrt.println("Default = "+m_TargetStat.getString());
		}
    if (headers && getSettings().computeCompactness()) {
      wrt.println("\n   Avg_Compactness (train): " + (avg_compactness[0][0]/m_Rules.size()) + " = " + (avg_compactness[0][1]/m_Rules.size()) + " + " + (avg_compactness[0][2]/m_Rules.size()) );
      wrt.println("   Avg_Coverage    (train): " + (avg_coverage[0]/m_Rules.size()));
      wrt.println("   Avg_Cover*Comp  (train): " + (avg_prod[0][0]/m_Rules.size()) + " = " + (avg_prod[0][1]/m_Rules.size()) + " + " + (avg_prod[0][2]/m_Rules.size()));
      wrt.println("   Avg_Compactness (test):  " + (avg_compactness[1][0]/m_Rules.size()) + " = " + (avg_compactness[1][1]/m_Rules.size()) + " + " + (avg_compactness[1][2]/m_Rules.size()));
      wrt.println("   Avg_Coverage    (test):  " + (avg_coverage[1]/m_Rules.size()));
      wrt.println("   Avg_Cover*Comp  (test):  " + (avg_prod[1][0]/m_Rules.size()) + " = " + (avg_prod[1][1]/m_Rules.size()) + " + " + (avg_prod[1][2]/m_Rules.size()));
    }
  }
	
	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, ClusSchema schema) {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.printModel(wrt, info);
			wrt.println();
			wrt.println("Covered examples:");
			ArrayList data = rule.getData();
			ClusAttrType[] attrs = schema.getAllAttrUse(ClusAttrType.ATTR_USE_TARGET);
			ClusAttrType[] key = schema.getAllAttrUse(ClusAttrType.ATTR_USE_KEY);
			for (int k = 0; k < data.size(); k++) {
				DataTuple tuple = (DataTuple)data.get(k);
				wrt.print(String.valueOf(k+1)+": ");
				boolean hasval = false;
				for (int j = 0; j < key.length; j++) {
					if (hasval) wrt.print(",");
					wrt.print(key[j].getString(tuple));
					hasval = true;
				}
				for (int j = 0; j < attrs.length; j++) {
					if (hasval) wrt.print(",");
					wrt.print(attrs[j].getString(tuple));
					hasval = true;
				}
				wrt.println();
			}
			wrt.println();			
		}
		wrt.println("Default = "+(m_TargetStat == null ? "None" : m_TargetStat.getString()));
	}	
	
	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
		addDataToRules(examples);
		printModelAndExamples(wrt, info, examples.getSchema());
		removeDataFromRules();		
	}	

	public void printModelToPythonScript(PrintWriter wrt) {
	}
	
	public int getModelSize() {
		return m_Rules.size();
	}
	
  public Settings getSettings() {
    return m_StatManager.getSettings();
  }

	public ClusRule getRule(int i) {
		return (ClusRule)m_Rules.get(i);
	}
	
	public int getNbLiterals() {
		int count = 0;
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			count += rule.getModelSize();
		}
		return count;
	}
	
	public void setTargetStat(ClusStatistic def) {
		m_TargetStat = def;
	}
	
	public void postProc() {
		m_TargetStat.calcMean();
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.postProc();
		}
	}
	
	public String getModelInfo() {
		return "Rules = "+getModelSize()+" (Tests: "+getNbLiterals()+")";
	}		

  /** 
   * Computes the compactness of data tuples covered by each rule.
   * @param mode 0 for train set, 1 for test set
   */
  public void computeCompactness(int mode) {
    for (int i = 0; i < m_Rules.size(); i++) {
      ClusRule rule = (ClusRule)m_Rules.get(i);
      rule.computeCompactness(mode);
    }
  }

  /** 
   * Adds the tuple to the rule which covers it.
   * @param tuple the data tuple
   * @return true if tuple is covered by any rule in this RuleSet,
   *         false otherwise
   */
  public boolean addDataToRules(DataTuple tuple) {
    boolean covered = false;
    for (int i = 0; i < m_Rules.size(); i++) {
      ClusRule rule = (ClusRule)m_Rules.get(i);
      if (rule.covers(tuple)) {
        rule.addDataTuple(tuple);
        covered = true;
      }
    }
    return covered;
  }

  /**
   * Adds the data tuples to rules which cover them. Noncovered
   * tuples are added to the m_DefaultData array. 
   * @param data the data 
   */
  public void addDataToRules(RowData data) {
    for (int i = 0; i < data.getNbRows(); i++) {
       DataTuple tuple = data.getTuple(i);
       if (!addDataToRules(tuple)) {
         m_DefaultData.add(tuple);
       }
    }
  }
  
  /**
   * Removes the data tuples from rules and from m_DefaultData array. 
   */
  public void removeDataFromRules() {
    for (int i = 0; i < m_Rules.size(); i++) {
      ((ClusRule)m_Rules.get(i)).removeDataTuples();
    }
    m_DefaultData.clear();
  }

	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
    for (int i = 0; i < getModelSize(); i++) {
      ClusRule rule = getRule(i);
      if (rule.covers(tuple)) {
      	for (int j = 0; j < mproc.size(); j++) {
      		ClusModelProcessor proc = (ClusModelProcessor)mproc.elementAt(j);
      		proc.modelUpdate(tuple, rule);
      	}      	
      }
    }
	}
    
	public final void applyModelProcessor(DataTuple tuple, ClusModelProcessor proc) throws IOException {
      for (int i = 0; i < getModelSize(); i++) {
        ClusRule rule = getRule(i);
        if (rule.covers(tuple)) proc.modelUpdate(tuple, rule);
      }
	}
  
  public void setError(ClusErrorParent error, int subset) {
  	m_HasRuleErrors = true;  	
  	for (int i = 0; i < m_Rules.size(); i++) {  		
  		ClusRule rule = getRule(i);
  		if (error != null) rule.setError(error.getErrorClone(), subset);
  		else rule.setError(null, subset);
  	}  	
  }
  
  public boolean hasRuleErrors() {
  	return m_HasRuleErrors;
  }
  
  public int getID() {
  	return 0;
  }
  
  public ClusModel prune(int prunetype) {
		return this;
  }
}
