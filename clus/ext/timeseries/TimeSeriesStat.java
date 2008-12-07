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

public abstract class TimeSeriesStat extends BitVectorStat implements ClusDistance {

	public final static int linearParameter = 10;

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	public final static Random m_Random = new Random(0);

	// m_RepresentativeMean is the time series representing the cluster

	// TODO: Investigate the usage of median vs. mean?

	protected TimeSeriesAttrType m_Attr;
	private ArrayList m_TimeSeriesStack = new ArrayList();
	public TimeSeries m_RepresentativeMean = new TimeSeries("[]");
	public TimeSeries m_RepresentativeMedian = new TimeSeries("[]");

	// public TimeSeries m_RepresentativeQuantitve=new TimeSeries("[]");

	protected double m_Value;
	protected double m_AvgDistances;
	protected double m_AvgSqDistances;
	
	public TimeSeriesStat(TimeSeriesAttrType attr) {
		m_Attr = attr;
	}
	
	public void copy(ClusStatistic other) {
		TimeSeriesStat or = (TimeSeriesStat)other;
		super.copy(or);
		//m_Value = or.m_Value;
		//m_AvgDistances = or.m_AvgDistances;
		//m_AvgSqDistances = or.m_AvgSqDistances;
		m_TimeSeriesStack.clear();
		m_TimeSeriesStack.addAll(or.m_TimeSeriesStack);
		// m_RepresentativeMean = or.m_RepresentativeMean;
		// m_RepresentativeMedian = or.m_RepresentativeMedian;		
	}

	/**
	 * Used for combining weighted predictions. 
	 */
	public TimeSeriesStat normalizedCopy() {
		TimeSeriesStat copy = (TimeSeriesStat)cloneSimple();
		copy.m_nbEx = 0;
		copy.m_SumWeight = 1;
		copy.m_TimeSeriesStack.add(getTimeSeriesPred());
		copy.m_RepresentativeMean.setValues(m_RepresentativeMean.getValues());
		copy.m_RepresentativeMedian.setValues(m_RepresentativeMedian.getValues());
		return copy;
	}

	public void addPrediction(ClusStatistic other, double weight) {
		TimeSeriesStat or = (TimeSeriesStat)other;
		m_SumWeight += weight*or.m_SumWeight;
		TimeSeries pred = new TimeSeries(or.getTimeSeriesPred());
		pred.setTSWeight(weight);
		m_TimeSeriesStack.add(pred);
	}
	
	/*
	 * Add a weighted time series to the statistic.
	 */
	public void updateWeighted(DataTuple tuple, int idx){
		super.updateWeighted(tuple,idx);
	    TimeSeries newTimeSeries = new TimeSeries((TimeSeries)tuple.m_Objects[0]);
	    newTimeSeries.setTSWeight(tuple.getWeight());
	    m_TimeSeriesStack.add(newTimeSeries);
	}

	public double getSVarS(ClusAttributeWeights scale, RowData data) {
		optimizePreCalc(data);
		return m_Value;
	}
	
	/**
	 * Currently only used to compute the default dispersion within rule heuristics.
	 */
	public double getDispersion(ClusAttributeWeights scale, RowData data) {
		return getSVarS(scale, data);
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
	
	public double getAbsoluteDistance(DataTuple tuple, ClusAttributeWeights weights) {
		int idx = m_Attr.getIndex();
		TimeSeries actual = (TimeSeries)tuple.getObjVal(0);
		return calcDistance(m_RepresentativeMean, actual) * weights.getWeight(idx);
	}

	public void initNormalizationWeights(ClusAttributeWeights weights, boolean[] shouldNormalize) {
		int idx = m_Attr.getIndex();
		if (shouldNormalize[idx]) {
			double var = m_Value / getTotalWeight();
			double norm = var > 0 ? 1/var : 1; // No normalization if variance = 0;
			weights.setWeight(m_Attr, norm);
		}
	}
	
	public void calcSumAndSumSqDistances(TimeSeries prototype) {
		m_AvgDistances = 0.0;
		m_AvgSqDistances = 0.0;
		int count = m_TimeSeriesStack.size();
		for (int i = 0; i < count; i++){
			double dist = calcDistance(prototype,(TimeSeries)m_TimeSeriesStack.get(i));
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
		// Median
		m_RepresentativeMedian = null;
		double minDistance = Double.POSITIVE_INFINITY;
		for(int i=0; i<m_TimeSeriesStack.size(); i++){
			double crDistance = 0.0;
			TimeSeries t1 = (TimeSeries)m_TimeSeriesStack.get(i);
			for (int j=0; j<m_TimeSeriesStack.size(); j++){
				TimeSeries t2 = (TimeSeries)m_TimeSeriesStack.get(j);
				double dist = calcDistance(t1, t2);
				crDistance += dist * dist * t2.geTSWeight();
			}
			if (crDistance<minDistance) {
				m_RepresentativeMedian = (TimeSeries)m_TimeSeriesStack.get(i);
				minDistance = crDistance;
			}
		}
		calcSumAndSumSqDistances(m_RepresentativeMedian);
		// Mean
		m_RepresentativeMean.setSize(m_RepresentativeMedian.length());
		for (int i=0; i< m_RepresentativeMean.length(); i++){
			double sum = 0.0;
			for(int j=0; j<m_TimeSeriesStack.size(); j++){
				TimeSeries t1 = (TimeSeries)m_TimeSeriesStack.get(j);
				sum += t1.getValue(i) * t1.geTSWeight();
			}
	    	m_RepresentativeMean.setValue(i, sum/m_SumWeight);
	    }
		
		double sumwi = 0.0;
		for(int j=0; j<m_TimeSeriesStack.size(); j++){
			TimeSeries t1 = (TimeSeries)m_TimeSeriesStack.get(j);
			sumwi += t1.geTSWeight();
		}
		double diff = Math.abs(m_SumWeight-sumwi);
		if (diff > 1e-6) {
			System.err.println("Error: Sanity check failed! - "+diff);
		}
		
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
		m_TimeSeriesStack.clear();
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

	public TimeSeries getRepresentativeMean() {
		return m_RepresentativeMean;
	}
	
	public TimeSeries getRepresentativeMedian() {
		return m_RepresentativeMedian;
	}
	
	public TimeSeries getTimeSeriesPred() {
		return m_RepresentativeMedian;
	}
	
}
