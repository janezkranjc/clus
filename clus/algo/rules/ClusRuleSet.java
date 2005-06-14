/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import java.util.*;

import jeans.util.*;
import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;
import clus.util.*;

public class ClusRuleSet implements ClusModel, Serializable {

	protected ClusStatistic m_Default;
	protected ArrayList m_Rules = new ArrayList();
  /* Array of tuples covered by the default rule. */
  protected ArrayList m_DefaultData = new ArrayList();
	
	public void add(ClusRule rule) {
		m_Rules.add(rule);
	}
	
	public ClusStatistic predictWeighted(DataTuple tuple) {
		for (int i = 0; i < getModelSize(); i++) {
			ClusRule rule = getRule(i);
			if (rule.covers(tuple)) return rule.getTotalStat();
		}
		return m_Default;
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
	
	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
	}

	public void attachModel(Hashtable table) throws ClusException {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.attachModel(table);
		}
	}
	
	public void printModel(PrintWriter wrt) {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.printModel(wrt);
			wrt.println();
		}
		wrt.println("Default = "+m_Default.getString());
	}
	
	public int getModelSize() {
		return m_Rules.size();
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
}
