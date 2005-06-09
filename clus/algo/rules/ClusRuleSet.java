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
}
