/*
 * Created on May 18, 2005
 */
package clus.statistic;

import java.io.*;

import clus.main.*;

public class TargetWeightProducer implements Serializable {

	public double[] m_NumWeights;
	protected ClusStatManager m_Manager;

	public TargetWeightProducer(ClusStatManager mgr) {
		m_Manager = mgr;
	}
	
	public Settings getSettings() {
		return m_Manager.getSettings();
	}
	
	public void setTotalStat(ClusStatistic stat) {
		// This version should create uniform weights later on
		// stat.createUniformWeights(this);
	}
	
	public double[] getNumWeights() {
		return m_NumWeights;
	}
	
	public void setNumWeights(double[] weights) {
		m_NumWeights = weights;
	}
	
	public String getName() {
		return "Uniform weights";
	}
}
