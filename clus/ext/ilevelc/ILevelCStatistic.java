package clus.ext.ilevelc;

import java.util.*;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.*;
import clus.data.type.NumericAttrType;
import clus.statistic.*;

public class ILevelCStatistic extends RegressionStat {
	
	protected NumericAttrType[] m_Numeric;
	protected int m_ClusterID = -1;
	
	public ILevelCStatistic(NumericAttrType[] num) {
		super(num);
		m_Numeric = num;
	}
	
	public void setClusterID(int id) {
		m_ClusterID = id;
	}
	
	public int getClusterID() {
		return m_ClusterID;
	}
	
	public ClusStatistic cloneStat() {
		return new ILevelCStatistic(m_Numeric);
	}
	
	public String getString(StatisticPrintInfo info) {
		String res = super.getString(info);
		return res + " L=" + getClusterID();
	}
	
	public String getPredictWriterString(DataTuple tuple) {
		return "";
	}	
	
	public void assignInstances(RowData data, int[] clusters) {
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			clusters[tuple.getIndex()] = getClusterID();
		}
	}
}
