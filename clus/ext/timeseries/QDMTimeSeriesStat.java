package clus.ext.timeseries;

import java.text.NumberFormat;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.main.Settings;
import clus.statistic.ClusStatistic;
import clus.statistic.StatisticPrintInfo;
import clus.util.ClusFormat;

public class QDMTimeSeriesStat extends TimeSeriesStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public double calcDistance(TimeSeries t1, TimeSeries t2) {
		// Ljupco's measure if the time series are the same length
		// my proposal if they are not is cyclic, to be defined with Ljupco
		int m = Math.max(t1.length(), t2.length());
		int n = Math.min(t1.length(), t2.length());
		double[] vt1 = t1.getValues();
		double[] vt2 = t2.getValues();
		double distance = 0;
		for (int i = 0; i < m; i++) {
			for (int j = i + 1; j < m; j++) {
				distance += Math.abs(Math.signum(vt1[j] - vt1[i]) - Math.signum(vt2[j % n] - vt2[i % n]));
			}
		}
		distance =distance / (m * (m-1)); 
		return distance;
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
		QDMTimeSeriesStat stat = new QDMTimeSeriesStat();
		stat.cloneFrom(this);
		return stat;
	}

	public void copy(ClusStatistic other) {
		QDMTimeSeriesStat or = (QDMTimeSeriesStat) other;
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
