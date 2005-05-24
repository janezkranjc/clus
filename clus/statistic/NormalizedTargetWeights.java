/*
 * Created on May 18, 2005
 */
package clus.statistic;

import clus.main.*;

public class NormalizedTargetWeights extends TargetWeightProducer {
	
	public NormalizedTargetWeights(ClusStatManager mgr) {
		super(mgr);
	}
	
	public void setTotalStat(ClusStatistic stat) {
		RegressionStat rs = (RegressionStat)stat;
		int nbtarget = rs.getNbTarget();
		double[] fac = new double[nbtarget];
		if (getSettings().shouldNormalize()) {
			for (int i = 0; i < nbtarget; i++) {
				fac[i] = 1/rs.getVariance(i);
			}
		} else {
			for (int i = 0; i < nbtarget; i++) {
				fac[i] = 1.0;
			}
		}
		setNumWeights(fac);
	}
	
	public String getName() {
		return "Normalized weights";
	}	
}
