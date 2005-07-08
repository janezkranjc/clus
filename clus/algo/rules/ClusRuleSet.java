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

	protected ClusStatistic m_Default;
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
    if (getSettings().getCoveringMethod() == Settings.COVERING_METHOD_STANDARD) {
      for (int i = 0; i < getModelSize(); i++) {
        ClusRule rule = getRule(i);
        if (rule.covers(tuple)) return rule.getTotalStat();
      }
      return m_Default;
    } else {
      boolean covered = false;
      ClusStatistic stat = m_Default.cloneSimple();
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
        return m_Default;
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
    boolean headers = getSettings().computeCompactness() || hasRuleErrors();
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
      }
			rule.printModel(wrt);
			wrt.println();
		}
    if (headers) {
      wrt.println("Default rule:");
      wrt.println("=============");
    }
		wrt.println("Default = "+(m_Default == null ? "N/A" : m_Default.getString()));
	}
	
	public void printModelAndExamples(PrintWriter wrt, ClusSchema schema) {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.printModel(wrt);
			wrt.println();
			wrt.println("Covered examples:");
			ArrayList data = rule.getData();
			ClusAttrType[] attrs = schema.getAllAttrUse(ClusAttrType.ATTR_USE_TARGET);
			ClusAttrType[] key = schema.getAllAttrUse(ClusAttrType.ATTR_USE_KEY);
			for (int k = 0; k < data.size(); k++) {
				DataTuple tuple = (DataTuple)data.get(k);
				wrt.print(String.valueOf(k)+": ");
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
		wrt.println("Default = "+m_Default.getString());
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
	
	public void setDefaultStat(ClusStatistic def) {
		m_Default = def;
	}
	
	public void postProc() {
		m_Default.calcMean();
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
}
