package clus.ext.timeseries;

import clus.main.Settings;

public class DTWTimeSeriesStat extends TimeSeriesStat {
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public double calcDistance(TimeSeries t1, TimeSeries t2, int adjustmentWindow){
		int m = t1.length();
		int n = t2.length();
		double[][] wrappingPathMatrix = new double[m][n];
		double[] vt1 = t1.getValues();
		double[] vt2 = t2.getValues();
		wrappingPathMatrix[0][0]=Math.abs((vt1[0]-vt2[0]))*2;
		for (int k=1;k<m+n-1;k++){
			for (int i=Math.max(k-n+1,1);i<Math.min(k, m);i++){
				if (Math.abs(2*i-k)<=adjustmentWindow){
					double dfk = Math.abs(vt1[i]-vt2[k-i]);
					wrappingPathMatrix[i][k-i]=Math.min(wrappingPathMatrix[i][k-i-1]+dfk, Math.min(wrappingPathMatrix[i-1][k-i]+dfk, wrappingPathMatrix[i-1][k-i-1]+dfk*2));
				}else{
					wrappingPathMatrix[i][k-i]=Double.POSITIVE_INFINITY;
				}
			}
		}
		return wrappingPathMatrix[m-1][n-1]/(m+n);
	}
	public double calcDistance(TimeSeries t1, TimeSeries t2) {
		return calcDistance(t1,t2,Math.max(t1.length(),t2.length())/2);
	}
}
