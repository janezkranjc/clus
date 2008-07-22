/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.ext.timeseries;

import java.text.NumberFormat;
import java.util.*;

import clus.data.attweights.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.ext.sspd.*;
import clus.main.*;
import clus.statistic.*;
import clus.util.*;

public abstract class TimeSeriesStat extends BitVectorStat implements SSPDDistance {

	public final static int linearParameter = 10;

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	public final static Random m_Random = new Random(0);

	/*
	 * Aco:
	 * m_RepresentativeMean is the time series representing the claster
	*/

	private ArrayList TimeSeriesStack = new ArrayList();
	public TimeSeries m_RepresentativeMean = new TimeSeries("[]");
	public TimeSeries m_RepresentativeMedian = new TimeSeries("[]");

	// public TimeSeries m_RepresentativeQuantitve=new TimeSeries("[]");

	protected double m_Value;
	protected double m_AvgDistances;
	protected double m_AvgSqDistances;

	public double getSS(ClusAttributeWeights scale, RowData data) {
		optimizePreCalc(data);
		return m_Value;
	}

	public void optimizePreCalc(RowData data) {
		if (!m_Modified) return;
		switch (Settings.m_TimeSeriesProtoComlexity.getValue()){
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
		double sumWiDiag = 0.0;
		double sumWiTria = 0.0;
		int nb = m_Bits.size();
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				DataTuple a = data.getTuple(i);
				TimeSeries t1 = (TimeSeries)a.getObjVal(0);
				double a_weight = a.getWeight();
				// sum up elements in upper triangle of matrix (and give double weights)
				for (int j = 0; j < i; j++) {
					if (m_Bits.getBit(j)) {
						DataTuple b = data.getTuple(j);
						TimeSeries t2 = (TimeSeries)b.getObjVal(0);
						double wi = a_weight*b.getWeight();
						double d = calcDistance(t1,t2);
						m_Value += wi * d * d;
						sumWiTria += wi;
					}
				}
				// sum up weights for elements on diagonal (with corresponding zero distances)
				sumWiDiag += a_weight*a_weight;
			}
		}
		m_Value = getTotalWeight() * m_Value / (2 * sumWiTria + sumWiDiag);
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
						double d = calcDistance(t1,t2);
						m_Value += wi * d * d;
						sumWi += wi;
						M++;
					}
					T++;
				}
			}
		}
		m_Value = getTotalWeight() * m_Value / sumWi / 2.0;
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

	    TimeSeriesStack.add(newTimeSeries);
	}

	public void calcSumAndSumSqDistances(TimeSeries prototype) {
		m_AvgDistances = 0.0;
		m_AvgSqDistances = 0.0;
		int count = TimeSeriesStack.size();
		for (int i = 0; i < count; i++){
			double dist = calcDistance(prototype,(TimeSeries)TimeSeriesStack.get(i));
			m_AvgSqDistances += dist * dist;
			m_AvgDistances += dist;
		}
		m_AvgSqDistances /= count;
		m_AvgDistances /= count;
	}

	/*
	 * [Aco]
	 * this is executed in the end
	 * @see clus.statistic.ClusStatistic#calcMean()
	 */
	public void calcMean() {
		// Mean
		for (int i=0; i< m_RepresentativeMean.length();i++){
	    	m_RepresentativeMean.setValue(i,m_RepresentativeMean.getValue(i)/m_SumWeight);
	    }
		// Median
		m_RepresentativeMedian = null;
		double minDistance = Double.POSITIVE_INFINITY;
		for(int i=0; i<TimeSeriesStack.size();i++){
			double crDistance=0.0;
			TimeSeries t1 = (TimeSeries)TimeSeriesStack.get(i);
			for (int j=0; j<TimeSeriesStack.size();j++){
				double dist = calcDistance(t1,(TimeSeries)TimeSeriesStack.get(j));
				crDistance += dist * dist;
			}
			if (crDistance<minDistance) {
				m_RepresentativeMedian=(TimeSeries)TimeSeriesStack.get(i);
				minDistance = crDistance;
			}
		}
		calcSumAndSumSqDistances(m_RepresentativeMedian);

		// Qualitative distance
/*
		double[][] m_RepresentativeQualitativeMatrix = new double[m_RepresentativeMean.length()][m_RepresentativeMean.length()];
		for(int i=0;i<m_RepresentativeMean.length();i++){
			for(int j=0;j<m_RepresentativeMean.length();j++){
				m_RepresentativeQualitativeMatrix[i][j]=0;
			}
		}
		for(int i=0; i<TimeSeriesStack.size();i++){
			TimeSeries newTemeSeries = (TimeSeries)TimeSeriesStack.get(i);
			for (int j = 0; j < newTemeSeries.length(); j++) {
				for (int k = 0; k < newTemeSeries.length(); k++) {
					m_RepresentativeQualitativeMatrix[j][k]+=Math.signum(newTemeSeries.getValue(k) - newTemeSeries.getValue(j));
				}
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
*/
	}

	public void reset() {
		super.reset();
		TimeSeriesStack.clear();
	}

	public double calcDistance(DataTuple d1, DataTuple d2) {
		return calcDistance((TimeSeries)d1.getObjVal(0), (TimeSeries)d2.getObjVal(0));
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
			buf.append(", ");
			buf.append(fr.format(m_AvgDistances));
			buf.append(", ");
			buf.append(fr.format(Math.sqrt(m_AvgSqDistances)));
		}
		buf.append("; ");
/*
		buf.append("Quantitive: ");
		buf.append(m_RepresentativeQuantitve.toString());
		if (info.SHOW_EXAMPLE_COUNT) {
			buf.append(": ");
			buf.append(fr.format(m_SumWeight));
		}
		buf.append("; ");
*/
		return buf.toString();
	}

	public void addPredictWriterSchema(String prefix, ClusSchema schema) {
		schema.addAttrType(new TimeSeriesAttrType(prefix+"-p-TimeSeries"));
		schema.addAttrType(new NumericAttrType(prefix+"-p-Distance"));
		schema.addAttrType(new NumericAttrType(prefix+"-p-Size"));
		schema.addAttrType(new NumericAttrType(prefix+"-p-AvgDist"));
		schema.addAttrType(new NumericAttrType(prefix+"-p-AvgSqDist"));
	}

	public String getPredictWriterString(DataTuple tuple) {
		StringBuffer buf = new StringBuffer();
		buf.append(m_RepresentativeMedian.toString());
		TimeSeries target = (TimeSeries)tuple.getObjVal(0);
		double dist = calcDistance(target, m_RepresentativeMedian);
		buf.append(",");
		buf.append(dist);
		buf.append(",");
		buf.append(getTotalWeight());
		buf.append(",");
		buf.append(m_AvgDistances);
		buf.append(",");
		buf.append(m_AvgSqDistances);
		return buf.toString();
	}

	public TimeSeries getTimeSeriesPred() {
		return m_RepresentativeMedian;
	}
}
