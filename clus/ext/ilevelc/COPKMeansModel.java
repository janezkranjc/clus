package clus.ext.ilevelc;

import java.io.PrintWriter;

import clus.data.rows.DataTuple;
import clus.main.*;
import clus.statistic.ClusStatistic;
import clus.statistic.StatisticPrintInfo;

public class COPKMeansModel extends ClusNode {

	protected int m_K, m_Iterations, m_CSets, m_AvgIter;
	protected boolean m_Illegal;
	protected double m_RandIndex;
	protected COPKMeansCluster[] m_Clusters;

	public void setK(int k) {
		m_K = k;
	}
	
	public int getModelSize() {
		return m_K;
	}
		
	public ClusStatistic predictWeighted(DataTuple tuple) {
		if (m_Illegal) {
			return null;
		} else {
			int best_cl = -1;
			double min_dist = Double.POSITIVE_INFINITY;
			for (int j = 0; j < m_K; j++) {
				double dist = m_Clusters[j].computeDistance(tuple);
				if (dist < min_dist) {
					best_cl = j;
					min_dist = dist;
				}
			}
			return m_Clusters[best_cl].getCenter();
		}
	}
	
	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		wrt.println("COPKMeans("+m_K+", iter = "+m_Iterations+", max = "+m_AvgIter+", csets = "+m_CSets+")");
		if (m_Illegal) {
			wrt.println("   Illegal");
		} else {
			for (int j = 0; j < m_K; j++) {
				wrt.println("  "+m_Clusters[j].getCenter().getString(info));
			}
		}
	}
	
	public String getModelInfo() {
		if (m_Illegal) {
			return "Rand Index = ?";
		} else {
			return "Rand Index = "+m_RandIndex;
		}
	}
	
	public void setCSets(int sets) {
		m_CSets = sets;
	}
	
	public int getCSets() {
		return m_CSets;
	}
	
	public void setAvgIter(int avg) {
		m_AvgIter = avg;
	}
	
	public void setIllegal(boolean illegal) {
		m_Illegal = illegal;
	}
	
	public void setRandIndex(double value) {
		m_RandIndex = value;		
	}

	public void setClusters(COPKMeansCluster[] clusters) {
		m_Clusters = clusters;
	}

	public void setIterations(int i) {
		m_Iterations = i;
	}

	public int getIterations() {
		return m_Iterations;
	}
	
	public boolean isIllegal() {
		return m_Illegal;
	}
}
