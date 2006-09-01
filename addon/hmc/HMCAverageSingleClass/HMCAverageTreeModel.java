/*
 * Created on Jan 18, 2006
 */

import java.io.*;
import java.util.*;

import jeans.util.MyArray;
import jeans.util.array.*;

import clus.ext.hierarchical.*;
import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;
import clus.util.ClusException;

public class HMCAverageTreeModel implements ClusModel {
	
	protected double m_Threshold;
	protected ClusStatistic m_Target;
	protected ArrayList m_Models = new ArrayList();
	protected ArrayList m_Stats = new ArrayList();
	protected ArrayList m_Names = new ArrayList();
	protected ArrayList m_PosClass = new ArrayList();
	
	public HMCAverageTreeModel(ClusStatistic target) {
		m_Target = target;
	}
	
	public HMCAverageTreeModel createWithThreshold(double thr) {
		HMCAverageTreeModel model = new HMCAverageTreeModel(m_Target);
		model.m_Models = m_Models;
		model.m_Stats = m_Stats;
		model.m_Names = m_Names;
		model.m_PosClass = m_PosClass;
		model.m_Threshold = thr;
		return model;
	}
	
	public ClusStatistic predictWeighted(DataTuple tuple) {
    ClusStatistic stat = m_Target.cloneSimple();
    stat.unionInit();
    for (int i = 0; i < m_Models.size(); i++) {
    	ClusModel model = (ClusModel)m_Models.get(i);
    	WHTDStatistic pred_stat = (WHTDStatistic)model.predictWeighted(tuple);
    	ClassesValue val = (ClassesValue)m_PosClass.get(i);
    	double[] means = pred_stat.getNumericPred();
    	if (means[val.getIndex()] >= m_Threshold/100.0) {
    		stat.union((ClusStatistic)m_Stats.get(i));    	
    	}    	
    }
    stat.unionDone();
    return stat;
	}
	
	public void addSubModel(ClusModel model, ClusStatistic stat, String name, StringTable table) throws ClusException {
		ClusNode root = (ClusNode)model;
		WHTDStatistic root_stat = (WHTDStatistic)root.getTargetStat();
		ClassesValue val = root_stat.getHier().createValueByName("p", table);		
		m_Models.add(model);
		m_Stats.add(stat);
		m_Names.add(name);
		m_PosClass.add(val);
	}
	
	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
	}
	
	public int getModelSize() {
		return 0;
	}
	
	public String getModelInfo() {
		return "";
	}
	
	public void printModel(PrintWriter wrt) {
		for (int i = 0; i < m_Models.size(); i++) {
			wrt.println("Model "+(i+1)+": "+m_Names.get(i));
		}
	}
	
	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		printModel(wrt);
	}	
	
	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
		printModel(wrt);
	}

	public void printModelToPythonScript(PrintWriter wrt) {
	}
		
	public void attachModel(Hashtable table) throws ClusException {
		for (int i = 0; i < m_Models.size(); i++) {
			ClusModel model = (ClusModel)m_Models.get(i);
			model.attachModel(table);
		}
	}
	
	public ClusModel prune(int prunetype) {
		return this;
	}
	
	public int getID() {
		return 0;
	}
	
	public void retrieveStatistics(ArrayList stats) {
	}

	public void printModelToQuery(PrintWriter wrt, ClusRun cr) {
	}
}
