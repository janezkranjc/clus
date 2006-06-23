package clus.ext.timeseries;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.ext.sspd.SSPDMatrix;
import clus.main.Settings;
import clus.statistic.BitVectorStat;
import clus.statistic.ClusStatistic;
import clus.statistic.StatisticPrintInfo;
import clus.util.ClusFormat;

public abstract class TimeSeriesStat extends BitVectorStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	/*
	 * Aco:
	 * m_RepresentativeMean is the time series representing the claster
	*/
	public double m_SumWeightTS=0;
	public TimeSeries m_RepresentativeMean=new TimeSeries("[]");

	private LinkedList TimeSeriesStack = new LinkedList();
	public TimeSeries m_RepresentativeMedian=new TimeSeries("[]");
	
	public TimeSeries m_RepresentativeQuantitve=new TimeSeries("[]");
	
	
	//private double[][] m_RepresentativeQualitativeMatrix;
	
	protected SSPDMatrix m_Matrix;
	protected double m_Value;

	public double getSS(ClusAttributeWeights scale, RowData data) {
		optimizePreCalc(data);
		return m_Value;
	}

	public void optimizePreCalc(RowData data) {
		if (!m_Modified) return;
		m_Value = 0.0;		
		int nb = m_Bits.size();
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				DataTuple a = data.getTuple(i);
				TimeSeries t1 = (TimeSeries)a.getObjVal(0);
				double a_weight = a.getWeight();			
				for (int j = 0; j <= i; j++) {
					if (m_Bits.getBit(j)) {
						DataTuple b = data.getTuple(j);
						TimeSeries t2 = (TimeSeries)b.getObjVal(0);
						m_Value += a_weight*b.getWeight()*calcDistance(t1,t2);
					}	
				}
			}
		}
		m_Modified = false;
	}	
	
	/*
	 * [Aco]
	 * a new timeseries comes, and we calculate something for it
	 * @see clus.statistic.ClusStatistic#updateWeighted(clus.data.rows.DataTuple, int)
	 */
	public void updateWeighted(DataTuple tuple, int idx){
		super.updateWeighted(tuple,idx);

		//if different length we first resize it
	    TimeSeries newTimeSeries= ((TimeSeries)tuple.m_Objects[0]);
	    if (m_RepresentativeMean.length()<newTimeSeries.length()) {
	    	if (m_RepresentativeMean.length()==0) {
	    		m_RepresentativeMean = new TimeSeries(newTimeSeries.getValues());
	    	}
	    	else{
	    		m_RepresentativeMean.resize(newTimeSeries.length(),"linear");
	    	}
	    }
	    
	    if (newTimeSeries.length()<m_RepresentativeMean.length()){
	    	newTimeSeries.resize(m_RepresentativeMean.length(),"linear");
	    }
	    //and add to the representative (in calc mean we are going to devide)
	    for (int i=0; i< m_RepresentativeMean.length();i++){
	    	m_RepresentativeMean.setValue(i,m_RepresentativeMean.getValue(i)+newTimeSeries.getValue(i));
	    }
	    m_SumWeightTS += tuple.getWeight();
	    
	    TimeSeriesStack.add(newTimeSeries);
	    
	}
	
	/*
	 * [Aco]
	 * this is executed in the end
	 * @see clus.statistic.ClusStatistic#calcMean()
	 */
	public void calcMean() {
		for (int i=0; i< m_RepresentativeMean.length();i++){
	    	m_RepresentativeMean.setValue(i,m_RepresentativeMean.getValue(i)/m_SumWeightTS);
	    }
		
		//first generate matrix of zeros
		double[][] m_RepresentativeQualitativeMatrix = new double[m_RepresentativeMean.length()][m_RepresentativeMean.length()];
		for(int i=0;i<m_RepresentativeMean.length();i++){
			for(int j=0;j<m_RepresentativeMean.length();j++){
				m_RepresentativeQualitativeMatrix[i][j]=0;
			}
		}
		
		double minDistance = Double.POSITIVE_INFINITY;
		for(int i=0; i<TimeSeriesStack.size();i++){
			double tmpDistance=0;
			
			//for diferent length time series we need resizing
			TimeSeries newTemeSeries = (TimeSeries)TimeSeriesStack.get(i);
			if (newTemeSeries.length()<m_RepresentativeMean.length()){
				newTemeSeries.resize(m_RepresentativeMean.length(),"linear");
			}
			// the quantitive becomes euclidian :)
			for (int j = 0; j < newTemeSeries.length(); j++) {
				for (int k = 0; k < newTemeSeries.length(); k++) {
					m_RepresentativeQualitativeMatrix[j][k]+=Math.signum(newTemeSeries.getValue(k) - newTemeSeries.getValue(j));
				}
			}
			
			for (int j=0; j<TimeSeriesStack.size();j++){
				tmpDistance+=calcDistance((TimeSeries)TimeSeriesStack.get(i),(TimeSeries)TimeSeriesStack.get(j));
			}
			
			if (tmpDistance<minDistance){
				m_RepresentativeMedian=(TimeSeries)TimeSeriesStack.get(i);
			}
		}
		
		double tmpMaxValue=(double)(m_RepresentativeQualitativeMatrix.length - 1);
		m_RepresentativeQuantitve=new TimeSeries(m_RepresentativeQualitativeMatrix.length);
		for (int i=0;i<m_RepresentativeQualitativeMatrix.length;i++){
			int numSmaller=0;
			int numBigger=0;
			for (int j=0; j<m_RepresentativeQualitativeMatrix.length;j++){
				if (m_RepresentativeQualitativeMatrix[i][j]>0){numBigger++;}
				if (m_RepresentativeQualitativeMatrix[i][j]<0){numSmaller++;}
			}
			
			m_RepresentativeQuantitve.setValue(i,((double)(numSmaller+tmpMaxValue-numBigger))/2);
			
		}
		m_RepresentativeQuantitve.rescale(m_RepresentativeMedian.min(),m_RepresentativeMedian.max());
		
//		System.err.println();
//		System.err.println("-------------");
//		for (int i=0;i<m_RepresentativeQualitativeMatrix.length; i++){
//			for (int j=0;j<m_RepresentativeQualitativeMatrix.length; j++){
//				System.err.print(m_RepresentativeQualitativeMatrix[i][j]+" ");
//			}
//			System.err.println();
//		}
		
		
	}
	
	public abstract double calcDistance(TimeSeries t1, TimeSeries t2);
	
	public String getString(){
		return getString(StatisticPrintInfo.getInstance());
	}
	
	/*
	 * [Aco]
	 * for printing in the nodes
	 * @see clus.statistic.ClusStatistic#getString(clus.statistic.StatisticPrintInfo)
	 */
	public String getString(StatisticPrintInfo info){
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		StringBuffer buf = new StringBuffer();
		buf.append("Mean: ");
		buf.append(m_RepresentativeMean.toString());
		if (info.SHOW_EXAMPLE_COUNT) {
			buf.append(": ");		
			buf.append(fr.format(m_SumWeight));
		}		
		buf.append("; ");

		buf.append("Median: ");
		buf.append(m_RepresentativeMedian.toString());
		if (info.SHOW_EXAMPLE_COUNT) {
			buf.append(": ");		
			buf.append(fr.format(m_SumWeight));
		}		
		buf.append("; ");
		
		buf.append("Quantitive: ");
		buf.append(m_RepresentativeQuantitve.toString());
		if (info.SHOW_EXAMPLE_COUNT) {
			buf.append(": ");		
			buf.append(fr.format(m_SumWeight));
		}		
		buf.append("; ");
		
		
		return buf.toString();
		//java.lang.Double.toString(m_SumWeight);
	}

	
}
