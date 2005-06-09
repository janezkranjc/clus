/*
 * Created on May 18, 2005
 */
package clus.statistic;

import java.util.*;

import clus.main.*;
import clus.util.*;

public class NormalizedTargetWeights extends TargetWeightProducer {
	
	protected double[] m_Weights;
	protected boolean[] m_Normalize;	
	
	public NormalizedTargetWeights(ClusStatManager mgr) {
		super(mgr);
	}
	
	public void setNbTarget(int nbtarget) {
		m_Weights = new double[nbtarget];
		m_Normalize = new boolean[nbtarget];
	}
	
	public void setAllNormalize(int nbtarget) {
		m_Weights = null;
		m_Normalize = new boolean[nbtarget];
		Arrays.fill(m_Normalize, true);
	}	
	
	public void setAllFixed(int nbtarget, double weight) {
		m_Weights = new double[nbtarget];
		m_Normalize = new boolean[nbtarget];
		Arrays.fill(m_Weights, weight);		
	}	
	
	public void setWeight(int idx, double wi) {
		m_Weights[idx] = wi;
		m_Normalize[idx] = false;
	}	

	public void setNormalize(int idx, boolean norm) {
		m_Normalize[idx] = norm;
	}	
	
	public void setTotalStat(ClusStatistic stat) {
		RegressionStat rs = (RegressionStat)stat;
		int nbtarget = rs.getNbTarget();
		double[] fac = new double[nbtarget];
		for (int i = 0; i < nbtarget; i++) {
			if (m_Normalize[i]) {
				fac[i] = 1/rs.getVariance(i);				
			} else {
				fac[i] = m_Weights[i];
			}
		}
		setNumWeights(fac);
	}
	
	public String getName() {
		StringBuffer buff = new StringBuffer();
		buff.append("Weights [");
		for (int i = 0; i < m_Normalize.length; i++) {
			if (i != 0) buff.append(",");
			if (m_Normalize[i]) buff.append("N");
			else buff.append(ClusFormat.THREE_AFTER_DOT.format(m_Weights[i]));
		}
		buff.append("]");
		return buff.toString();
	}	
}
