
package clus.ext.ilevelc;

import java.io.PrintWriter;

import clus.data.rows.DataTuple;
import clus.data.type.ClusAttrType;
import clus.main.*;
import clus.statistic.ClusStatistic;
import clus.statistic.StatisticPrintInfo;

public class SimpleClusterModel extends ClusNode {
	
	protected int[] m_Assign;
	protected ClusStatManager m_Manager;

	public SimpleClusterModel(int[] assign, ClusStatManager mgr) {
		m_Assign = assign;
		m_Manager = mgr;
	}
	
	public ClusStatistic predictWeighted(DataTuple tuple) {
		int idx = tuple.getIndex();
		int cl = m_Assign[idx];
		ILevelCStatistic stat = (ILevelCStatistic)m_Manager.getStatistic(ClusAttrType.ATTR_USE_CLUSTERING).cloneStat();
		stat.setClusterID(cl);
		stat.calcMean();
		return stat;
	}

	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		wrt.println("MPCKMeans()");
		if (m_Assign == null) {
			wrt.println("   Illegal");
		} else {
			wrt.println("   "+m_Assign.length);
		}
	}
}