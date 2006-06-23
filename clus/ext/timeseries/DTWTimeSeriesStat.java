package clus.ext.timeseries;

import java.text.NumberFormat;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.ext.sspd.SSPDStatistic;
import clus.main.Settings;
import clus.statistic.BitVectorStat;
import clus.statistic.ClusStatistic;
import clus.statistic.StatisticPrintInfo;
import clus.util.ClusFormat;

public class DTWTimeSeriesStat extends TimeSeriesStat {
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public double calcDistance(TimeSeries t1, TimeSeries t2, int adjustmentWindow){
		
		int m = t1.length();
		int n = t2.length();
		double[][] wrappingPathMatrix = new double[m][n];
		double[] vt1 = t1.getValues();
		double[] vt2 = t2.getValues();
		wrappingPathMatrix[0][0]=Math.abs((vt1[0]-vt2[0]))*2;
		int aw = Math.min(m,adjustmentWindow);
		
		for (int i=1;i<aw;i++){
			wrappingPathMatrix[i][0]=wrappingPathMatrix[i-1][0]+Math.abs((vt1[i]-vt2[0]));
		}

		for (int i=aw;i<m;i++){
			wrappingPathMatrix[i][0]=Double.POSITIVE_INFINITY;
		}
		
		aw = Math.min(n,adjustmentWindow);
		for (int i=1;i<aw;i++){
			wrappingPathMatrix[0][i]=wrappingPathMatrix[0][i-1]+Math.abs((vt1[0]-vt2[i]));
		}
		
		for (int i=aw;i<n;i++){
			wrappingPathMatrix[0][i]=Double.POSITIVE_INFINITY;
		}

		for (int k=2;k<m+n-1;k++){
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
	
	public double getSS(ClusAttributeWeights scale, RowData data) {
		// TODO Auto-generated method stub
		return super.getSS(scale, data);
	}
	
	public void optimizePreCalc(RowData data) {
		// TODO Auto-generated method stub
		super.optimizePreCalc(data);
	}

	public ClusStatistic cloneStat() {
		DTWTimeSeriesStat stat = new DTWTimeSeriesStat();
		stat.cloneFrom(this);
		return stat;
	}
	
	public void copy(ClusStatistic other) {
		DTWTimeSeriesStat or = (DTWTimeSeriesStat)other;
		super.copy(or);
		m_Value = or.m_Value;
	}

	public double getError(ClusAttributeWeights scale) {
		// TODO Auto-generated method stub
		return Double.POSITIVE_INFINITY;
	}	
	
	/*
	 * [Aco]
	 * for printing in the nodes
	 * @see clus.statistic.ClusStatistic#getString(clus.statistic.StatisticPrintInfo)
	 */
	public String getString(StatisticPrintInfo info){
		return super.getString(info);
	}
	
	/*
	 * [Aco]
	 * a new timeseries comes, and we calculate something for it
	 * @see clus.statistic.ClusStatistic#updateWeighted(clus.data.rows.DataTuple, int)
	 */
	public void updateWeighted(DataTuple tuple, int idx){
	    super.updateWeighted(tuple,idx);
	}
	
	/*
	 * [Aco]
	 * this is executed in the end
	 * @see clus.statistic.ClusStatistic#calcMean()
	 */
	public void calcMean() {
		super.calcMean();
	}

}
