package clus.ext.timeseries;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.RowData;
import clus.main.Settings;
import clus.statistic.ClusStatistic;

public class TSCTimeSeriesStat extends TimeSeriesStat{

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public double calcDistance(TimeSeries t1, TimeSeries t2) {
		//this calculates the Correlation coefficient of two TimeSeries
		//the two TimeSeries have same length
		double[] ts1 = t1.getValues();
		double[] ts2 = t2.getValues();
		double mean_ts1 = calculateMean(ts1);
		double mean_ts2 = calculateMean(ts2);
		double cc = 0;
		double sum_ts1_sqr = 0;
		double sum_ts2_sqr = 0;
		double sum_ts1_ts2 = 0;
		if (ts1.length != ts2.length){
			System.err.println("TimeSeriesCorrelation applies only to Time Series with equal length");
	//		throw new ArrayIndexOutOfBoundsException("TimeSeriesCorrelation applies only to Time Series with equal length");
			System.exit(1);
		}
		for (int k = 0; k < ts1.length; k++){
			sum_ts1_ts2 += (ts1[k] - mean_ts1) * (ts2[k] - mean_ts2);
			sum_ts1_sqr += Math.pow((ts1[k] - mean_ts1),2);
			sum_ts2_sqr += Math.pow((ts2[k] - mean_ts2),2);
		}
		cc = 1 - Math.abs(sum_ts1_ts2/Math.sqrt(sum_ts1_sqr*sum_ts2_sqr));
		return cc;
	}
	/*
	public double calcDistance(TimeSeries t1, TimeSeries t2) {
		//this calculates the Correlation coefficient of two TimeSeries
		//the two TimeSeries may have different lengths - Cycle Arrays
		
		double[] ts1=t1.getValues();
		double[] ts2=t2.getValues();
		double mean_ts1=calculateMean(ts1);
		double mean_ts2=calculateMean(ts2);
		double cc=0;
		double sum_ts1_sqr=0;
		double sum_ts2_sqr=0;
		double sum_ts1_ts2=0;
		boolean run=true;
		int k=0;
		while (run){
			sum_ts1_ts2+=(ts1[k%ts1.length]-mean_ts1)*(ts2[k%ts2.length]-mean_ts2);
			sum_ts1_sqr+=Math.pow((ts1[k%ts1.length]-mean_ts1),2);
			sum_ts2_sqr+=Math.pow((ts2[k%ts2.length]-mean_ts2),2);
			k++;
			if ((k%ts1.length==0)&&(k%ts2.length==0))run=false;
		}
		cc=1-Math.abs(sum_ts1_ts2/Math.sqrt(sum_ts1_sqr*sum_ts2_sqr));
		return cc;
	}*/
	
	public double getSS(ClusAttributeWeights scale, RowData data) {
		// TODO Auto-generated method stub
		return super.getSS(scale, data);
	}

	public void optimizePreCalc(RowData data) {
		// TODO Auto-generated method stub
		super.optimizePreCalc(data);
	}

	public ClusStatistic cloneStat() {
		TSCTimeSeriesStat stat = new TSCTimeSeriesStat();
		stat.cloneFrom(this);
		return stat;
	}

	public void copy(ClusStatistic other) {
		TSCTimeSeriesStat or = (TSCTimeSeriesStat) other;
		super.copy(or);
		m_Value = or.m_Value;
	}

	public double getError(ClusAttributeWeights scale) {
		// TODO Auto-generated method stub
		return Double.POSITIVE_INFINITY;
	}
	
	public double calculateMean(double[] ts){
		double sum = 0;
		for (int k = 0; k < ts.length; k++)sum += ts[k];
		sum = sum / ts.length;
		return sum;
	}
}
