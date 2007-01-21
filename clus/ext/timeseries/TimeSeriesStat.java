package clus.ext.timeseries;

import java.text.NumberFormat;
import java.util.*;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.ext.sspd.SSPDMatrix;
import clus.main.Settings;
import clus.statistic.BitVectorStat;
import clus.statistic.StatisticPrintInfo;
import clus.util.*;

public abstract class TimeSeriesStat extends BitVectorStat {

	public final static int linearParameter = 10;
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	public final static Random m_Random = new Random();
	
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
		switch (Settings.timeSeriesProtoComlexity.getValue()){
		case 1:
			//System.out.println("Log");
			optimizeLogPreCalc(data);
			break;
		case 2:
			//System.out.println("Linear");
			optimizeLinearPreCalc(data);
			break;
		case 4:
			optimizeLinearPreCalc(data);
			double linval = m_Value / getTotalWeight();
			/*optimizeLogPreCalc(data);
			double logval = m_Value / getTotalWeight();			
			optimizePairwiseLinearPreCalc(data);
			double pairval = m_Value / getTotalWeight();*/
			optimizePreCalcDefault(data);
			double exactval = m_Value / getTotalWeight();			
			// String str = ""+exactval+","+linval+","+logval+","+pairval+","+getTotalWeight();
			String str = ""+exactval+","+linval+","+getTotalWeight();
			DebugFile.log(str);
			System.out.println("Next: "+str);
		default :
			//System.out.println("N^2");
			optimizePreCalcDefault(data);
			break;			
		}
		m_Modified = false;
	}
	
	public void optimizePreCalcDefault(RowData data) {
		//long t = Calendar.getInstance().getTimeInMillis();
		m_Value = 0.0;		
		double sumWi = 0.0;
		int nb = m_Bits.size();		
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				DataTuple a = data.getTuple(i);
				TimeSeries t1 = (TimeSeries)a.getObjVal(0);
				double a_weight = a.getWeight();
				for (int j = 0; j < i; j++) {
					if (m_Bits.getBit(j)) {
						DataTuple b = data.getTuple(j);
						TimeSeries t2 = (TimeSeries)b.getObjVal(0);
						double wi = a_weight*b.getWeight();
						m_Value += wi * Math.pow(calcDistance(t1,t2),2);
						sumWi += wi;
					}	
				}
			}
		}
		m_Value = getTotalWeight() * m_Value / sumWi;
	}	
	
	public final static int Sampling_K_Random(int a, int b) {
		/* return value in interval a ... b (inclusive) */
		return a + m_Random.nextInt(b + 1);
	}

	public void optimizeLinearPreCalc(RowData data) {
		optimizeLinearPreCalc(data, linearParameter);
	}
	
	//linear random
	public void optimizeLinearPreCalc(RowData data, int samplenb) {
		//long t = Calendar.getInstance().getTimeInMillis();
		/* reset value */
		m_Value = 0.0;		
		int nb = m_Bits.size();
		/* create index */
		int nb_total = 0;
		int[] indices = new int[nb];
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) indices[nb_total++] = i; 
		}
		if (nb_total < samplenb) {
			/* less examples than sample size, use default method */
			optimizePreCalcDefault(data);
			return;
		}
		/* compute SSPD */
		double sumWi = 0.0;
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				DataTuple a = data.getTuple(i);
				TimeSeries t1 = (TimeSeries)a.getObjVal(0);
				double a_weight = a.getWeight();
				/* Knuth's SAMPLING_K */
				int T = 0;
				int M = 0;
				while (M < samplenb) {
					if (Sampling_K_Random(0, nb_total - T - 1) < samplenb - M) {
						DataTuple b = data.getTuple(indices[T]);
						TimeSeries t2 = (TimeSeries)b.getObjVal(0);
						double wi = a_weight*b.getWeight();
						m_Value += wi * Math.pow(calcDistance(t1,t2),2);
						sumWi += wi;
						M++;
					}
					T++;
				}
			}
		}
		m_Value = getTotalWeight() * m_Value / sumWi;
	}
	
	public void optimizePairwiseLinearPreCalc(RowData data) {
		/* reset value */
		m_Value = 0.0;		
		int nb = m_Bits.size();
		/* create index */
		int nb_total = 0;
		int[] indices = new int[nb];
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) indices[nb_total++] = i; 
		}
		/* compute SSPD */
		double sumWi = 0.0;
		for (int i = 0; i < nb_total; i++) {
			/* get first tuple */
			int a = Sampling_K_Random(0, nb_total-1); 
			DataTuple dt1 = data.getTuple(indices[a]);
			TimeSeries t1 = (TimeSeries)dt1.getObjVal(0);
			/* get second tuple */
			int b = Sampling_K_Random(0, nb_total-1);
			DataTuple dt2 = data.getTuple(indices[b]);
			TimeSeries t2 = (TimeSeries)dt2.getObjVal(0);
			/* update sspd formula */
			double wi = dt1.getWeight()*dt2.getWeight();
			m_Value += wi * Math.pow(calcDistance(t1,t2),2);
			sumWi += wi;
		}
		m_Value = getTotalWeight() * m_Value / sumWi;
	}
	
	// N*LogN random
	public void optimizeLogPreCalc(RowData data) {
		int nb = getNbTuples();
		int lognb = (int)Math.floor(Math.log(nb)/Math.log(2))+1;
		optimizeLinearPreCalc(data, lognb);
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

	public TimeSeries getTimeSeriesPred() {
		return m_RepresentativeMedian;
	}

	
	
	
}
