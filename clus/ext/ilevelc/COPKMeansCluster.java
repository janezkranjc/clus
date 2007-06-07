
package clus.ext.ilevelc;

import java.util.*;
import java.io.*;

import clus.data.rows.*;
import clus.data.type.*;
import clus.main.*;

public class COPKMeansCluster implements Serializable {

	protected int m_Index;
	protected ClusStatManager m_Mgr;
	protected ArrayList m_Data = new ArrayList();
	protected ILevelCStatistic m_Center;
	
	public COPKMeansCluster(DataTuple tuple, ClusStatManager mgr) {
		m_Mgr = mgr;
		m_Data.add(tuple);
		m_Center = (ILevelCStatistic)mgr.getStatistic(ClusAttrType.ATTR_USE_CLUSTERING).cloneStat();
		updateCenter();
	}
	
	public ILevelCStatistic getCenter() {
		return m_Center;
	}
	
	public ClusStatManager getStatManager() {
		return m_Mgr;
	}	
	
	public void clearData() {
		m_Data.clear();
	}
	
	public void addData(DataTuple tuple) {
		m_Data.add(tuple);
	}	

	public void updateCenter() {
		m_Center.reset();
		for (int i = 0; i < m_Data.size(); i++) {
			DataTuple tuple = (DataTuple)m_Data.get(i);
			m_Center.updateWeighted(tuple, tuple.getWeight());
		}
		m_Center.calcMean();
	}

	public double computeDistance(DataTuple tuple) {
		double dist = 0.0;
		double[] num = m_Center.getNumericPred();
		for (int j = 0; j < m_Center.getNbAttributes(); j++) {
			NumericAttrType att = m_Center.getAttribute(j);
			double v1 = num[j];
			double v2 = tuple.getDoubleVal(att.getArrayIndex());
			dist += (v1-v2)*(v1-v2);
		}		
		return Math.sqrt(dist);
	}

	public void setIndex(int i) {
		m_Index = i;
		m_Center.setClusterID(i);
	}
}
