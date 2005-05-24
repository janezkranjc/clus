/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import java.util.*;

import jeans.io.*;
import jeans.util.*;
import clus.data.rows.*;
import clus.ext.*;
import clus.main.*;
import clus.statistic.*;
import clus.model.test.*;
import clus.util.*;

public class ClusRule implements ClusModel, Serializable {

	protected Object m_Visitor;
	protected ClusStatistic m_Default;	
	protected ArrayList m_Tests = new ArrayList();
	
	public ClusStatistic predictWeighted(DataTuple tuple) {
		return m_Default;
	}
	
	public ClusRule cloneRule() {
		ClusRule new_rule = new ClusRule();
		for (int i = 0; i < getModelSize(); i++) {
			new_rule.addTest(getTest(i));
		}		
		return new_rule;
	}
	
	public boolean equals(Object other) {
		ClusRule o = (ClusRule)other;
		if (o.getModelSize() != getModelSize()) return false;
		for (int i = 0; i < getModelSize(); i++) {
			boolean has_test = false;
			for (int j = 0; j < getModelSize() && !has_test; j++) {
				if (getTest(i).equals(o.getTest(j))) has_test = true;
			}
			if (!has_test) return false;
		}
		return true;
	}
	
	public int hashCode() {
		int hashCode = 1234;
		for (int i = 0; i < getModelSize(); i++) {
			hashCode += getTest(i).hashCode();
		}
		return hashCode;
	}
	
	public boolean covers(DataTuple tuple) {
		for (int i = 0; i < getModelSize(); i++) {
			NodeTest test = getTest(i);
			int res = test.predictWeighted(tuple);
			if (res != ClusNode.YES) return false;
		}
		return true;
	}
	
	public void simplify() {
		for (int i = getModelSize()-1; i >= 0; i--) {
			boolean found = false;
			NodeTest test_i = getTest(i);
			for (int j = 0; j < i && !found; j++) {
				NodeTest test_j = getTest(j);
				NodeTest simplify = test_j.simplifyConjunction(test_i);
				if (simplify != null) {
					setTest(j, simplify);
					found = true;
				}				
			}
			if (found) removeTest(i);
		}
	}
		
	public RowData removeCovered(RowData data) {
		int covered = 0;
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			if (covers(tuple)) covered++;
		}
		int idx = 0;
		RowData res = new RowData(data.getSchema(), data.getNbRows()-covered);
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			if (!covers(tuple)) res.setTuple(tuple, idx++);
		}
		return res;
	}
	
	public ClusStatistic getTotalStat() {
		return m_Default;
	}
	
	public void setVisitor(Object visitor) {
		m_Visitor = visitor;
	}
	
	public Object getVisitor() {
		return m_Visitor;
	}	
	
	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
	}
	
	public void saveModel(ObjectSaveStream strm) throws IOException {
		strm.writeObject(new Integer(ClusExtension.RULE));
		strm.writeObject(this);		
	}
	
	public void attachModel(Hashtable table) throws ClusException {
		for (int i = 0; i < m_Tests.size(); i++) {
			NodeTest test = (NodeTest)m_Tests.get(i);
			test.attachModel(table);
		}
	}
	
	public void printModel() {
		PrintWriter wrt = new PrintWriter(System.out);
		printModel(wrt);
		wrt.flush();
	}
	
	public void printModel(PrintWriter wrt) {
		wrt.print("IF ");
		if (m_Tests.size() == 0) {
			wrt.print("true");
		} else {
			for (int i = 0; i < m_Tests.size(); i++) {
				NodeTest test = (NodeTest)m_Tests.get(i);
				if (i != 0) {
					wrt.println(" AND");
					wrt.print("   ");
				}
				wrt.print(test.getString());
			}
		}
		wrt.println();
		wrt.println("THEN "+m_Default.getString());
	}
	
	public boolean isEmpty() {
		return getModelSize() == 0;
	}
	
	public int getModelSize() {
		return m_Tests.size();
	}
	
	public NodeTest getTest(int i) {
		return (NodeTest)m_Tests.get(i);
	}
	
	public void setTest(int i, NodeTest test) {
		m_Tests.set(i, test);
	}
	
	public void addTest(NodeTest test) {
		m_Tests.add(test);
	}
	
	public void removeTest(int i) {
		m_Tests.remove(i);
	}
	
	public void setDefaultStat(ClusStatistic def) {
		m_Default = def;
	}
	
	public void postProc() {
		m_Default.calcMean();
	}
	
	public String getModelInfo() {
		return "Tests = "+getModelSize();
	}		
}
