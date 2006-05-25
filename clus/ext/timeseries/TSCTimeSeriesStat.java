package clus.ext.timeseries;

import java.text.NumberFormat;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.main.Settings;
import clus.statistic.ClusStatistic;
import clus.statistic.StatisticPrintInfo;
import clus.util.ClusFormat;

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

	/*
	 * [Aco]
	 * for printing in the nodes
	 * @see clus.statistic.ClusStatistic#getString(clus.statistic.StatisticPrintInfo)
	 */
	public String getString(StatisticPrintInfo info){
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		StringBuffer buf = new StringBuffer();		
		buf.append(m_RepresentativeTS.toString());
		if (info.SHOW_EXAMPLE_COUNT) {
			buf.append(": ");		
			buf.append(fr.format(m_SumWeight));
		}		
		return buf.toString();
		//java.lang.Double.toString(m_SumWeight);
	}
	
	/*
	 * [Aco]
	 * a new timeseries comes, and we calculate something for it
	 * @see clus.statistic.ClusStatistic#updateWeighted(clus.data.rows.DataTuple, int)
	 */
	public void updateWeighted(DataTuple tuple, int idx){
	    super.updateWeighted(tuple,idx);
	    TimeSeries newTimeSeries= ((TimeSeries)tuple.m_Objects[0]);
	    if (m_RepresentativeTS.length()<newTimeSeries.length()) {
	    	if (m_RepresentativeTS.length()==0) {
	    		m_RepresentativeTS = new TimeSeries(newTimeSeries.getValues());
	    	}
	    	else{
	    		m_RepresentativeTS.resize(newTimeSeries.length(),"linear");
	    	}
	    }
	    
	    if (newTimeSeries.length()<m_RepresentativeTS.length()){
	    	newTimeSeries.resize(m_RepresentativeTS.length(),"linear");
	    }
	    	
	    //this must be changed in near future
	    for (int i=0; i< m_RepresentativeTS.length();i++){
	    	m_RepresentativeTS.setValue(i,m_RepresentativeTS.getValue(i)+newTimeSeries.getValue(i));
	    }
	    m_SumWeightTS += tuple.getWeight();
	}
	
	/*
	 * [Aco]
	 * this is executed in the end
	 * @see clus.statistic.ClusStatistic#calcMean()
	 */
	public void calcMean() {
		for (int i=0; i< m_RepresentativeTS.length();i++){
	    	m_RepresentativeTS.setValue(i,m_RepresentativeTS.getValue(i)/m_SumWeightTS);
	    }
	}

}
