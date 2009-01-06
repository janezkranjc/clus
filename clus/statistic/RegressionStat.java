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

package clus.statistic;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.*;
import java.util.ArrayList;

import jeans.util.StringUtils;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.*;

import clus.main.ClusStatManager;
import clus.main.Settings;
import clus.util.*;
import clus.data.cols.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.data.attweights.*;
import clus.error.ClusNumericError;

public class RegressionStat extends ClusStatistic {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public int m_NbAttrs;
	public double[] m_SumValues;
	public double[] m_SumWeights;
	public double[] m_SumSqValues;
	public double[] m_Means;
	public NumericAttrType[] m_Attrs;

	public RegressionStat(NumericAttrType[] attrs) {
		this(attrs, false);
	}

	public RegressionStat(NumericAttrType[] attrs, boolean onlymean) {
		m_Attrs = attrs;
		m_NbAttrs = attrs.length;
		if (onlymean) {
			m_Means = new double[m_NbAttrs];
		} else {
			m_SumValues = new double[m_NbAttrs];
			m_SumWeights = new double[m_NbAttrs];
			m_SumSqValues = new double[m_NbAttrs];
		}
	}

	public ClusStatistic cloneStat() {
		return new RegressionStat(m_Attrs, false);
	}

	public ClusStatistic cloneSimple() {
		return new RegressionStat(m_Attrs, true);
	}

	/** Clone this statistic by taking the given weight into account.
	 *  This is used for example to get the weighted prediction of default rule. */
	public ClusStatistic cloneWeighted(double weight) {
		RegressionStat newStat = (RegressionStat) cloneSimple();
		for (int iTarget = 0; iTarget < newStat.getNbAttributes(); iTarget++ ){
			newStat.m_Means[iTarget] = weight * newStat.m_Means[iTarget];
		}
		return (ClusStatistic) newStat;
	}

	public int getNbAttributes() {
		return m_NbAttrs;
	}

	public NumericAttrType getAttribute(int idx) {
		return m_Attrs[idx];
	}

	public void reset() {
		m_SumWeight = 0.0;
		m_NbExamples = 0;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] = 0.0;
			m_SumValues[i] = 0.0;
			m_SumSqValues[i] = 0.0;
		}
	}

	public void copy(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight = or.m_SumWeight;
		m_NbExamples = or.m_NbExamples;
		System.arraycopy(or.m_SumWeights, 0, m_SumWeights, 0, m_NbAttrs);
		System.arraycopy(or.m_SumValues, 0, m_SumValues, 0, m_NbAttrs);
		System.arraycopy(or.m_SumSqValues, 0, m_SumSqValues, 0, m_NbAttrs);
	}

	/**
	 * Used for combining weighted predictions.
	 */
	public RegressionStat normalizedCopy() {
		RegressionStat copy = (RegressionStat)cloneSimple();
		copy.m_NbExamples = 0;
		copy.m_SumWeight = 1;
		calcMean(copy.m_Means);
		return copy;
	}

	public void addPrediction(ClusStatistic other, double weight) {
		RegressionStat or = (RegressionStat)other;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_Means[i] += weight*or.m_Means[i];
		}
	}

	public void add(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight += or.m_SumWeight;
		m_NbExamples += or.m_NbExamples;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] += or.m_SumWeights[i];
			m_SumValues[i] += or.m_SumValues[i];
			m_SumSqValues[i] += or.m_SumSqValues[i];
		}
	}

	public void addScaled(double scale, ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight += scale * or.m_SumWeight;
		m_NbExamples += or.m_NbExamples;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] += scale * or.m_SumWeights[i];
			m_SumValues[i] += scale * or.m_SumValues[i];
			m_SumSqValues[i] += scale * or.m_SumSqValues[i];
		}
	}

	public void subtractFromThis(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight -= or.m_SumWeight;
		m_NbExamples -= or.m_NbExamples;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] -= or.m_SumWeights[i];
			m_SumValues[i] -= or.m_SumValues[i];
			m_SumSqValues[i] -= or.m_SumSqValues[i];
		}
	}

	public void subtractFromOther(ClusStatistic other) {
		RegressionStat or = (RegressionStat)other;
		m_SumWeight = or.m_SumWeight - m_SumWeight;
		m_NbExamples = or.m_NbExamples - m_NbExamples;
		for (int i = 0; i < m_NbAttrs; i++) {
			m_SumWeights[i] = or.m_SumWeights[i] - m_SumWeights[i];
			m_SumValues[i] = or.m_SumValues[i] - m_SumValues[i];
			m_SumSqValues[i] = or.m_SumSqValues[i] - m_SumSqValues[i];
		}
	}

	public void updateWeighted(DataTuple tuple, int idx) {
		updateWeighted(tuple, tuple.getWeight());
	}

	public void updateWeighted(DataTuple tuple, double weight) {
		m_NbExamples++;
		m_SumWeight += weight;
		for (int i = 0; i < m_NbAttrs; i++) {
			double val = m_Attrs[i].getNumeric(tuple);
			if (val != Double.POSITIVE_INFINITY) {
				m_SumWeights[i] += weight;
				m_SumValues[i] += weight*val;
				m_SumSqValues[i] += weight*val*val;
			}
		}
	}

	public void computePrediction() {
		// do not need to call calcMean() here
	}

	public void calcMean(double[] means) {
		for (int i = 0; i < m_NbAttrs; i++) {
			// If divider zero, return zero
			means[i] = m_SumWeights[i] != 0.0 ? m_SumValues[i] / m_SumWeights[i] : 0.0;
		}
	}

	public void calcMean() {
		if (m_Means == null) m_Means = new double[m_NbAttrs];
		calcMean(m_Means);
	}
	
	public void setMeans(double[] means) {
		m_Means = means;
	}
	
	public double getMean(int i) {
		return m_SumWeights[i] != 0.0 ? m_SumValues[i] / m_SumWeights[i] : 0.0;
	}

	public double getSumValues(int i) {
		return m_SumValues[i];
	}

	public double getSumWeights(int i) {
		return m_SumWeights[i];
	}

	public double getSS(int i) {
		double n_tot = m_SumWeight;
		double k_tot = m_SumWeights[i];
		double sv_tot = m_SumValues[i];
		double ss_tot = m_SumSqValues[i];
		return (k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0;
	}

	public double getScaledSS(int i, ClusAttributeWeights scale) {
		return getSS(i)*scale.getWeight(getAttribute(i));
	}

	public double getVariance(int i) {
		return m_SumWeight != 0.0 ? getSS(i) / m_SumWeight : 0.0;
	}

	public double getScaledVariance(int i, ClusAttributeWeights scale) {
		return getVariance(i)*scale.getWeight(getAttribute(i));
	}

	public double getRootScaledVariance(int i, ClusAttributeWeights scale) {
		return Math.sqrt(getScaledVariance(i, scale));
	}

	public double[] getRootScaledVariances(ClusAttributeWeights scale) {
		int nb = getNbAttributes();
		double[] res = new double[nb];
		for (int i = 0; i < res.length; i++) {
			res[i] = getRootScaledVariance(i, scale);
		}
		return res;
	}

	public double getStandardDeviation(int i) {
		double n_tot = m_SumWeight;
		double k_tot = m_SumWeights[i];
		double sv_tot = m_SumValues[i];
		double ss_tot = m_SumSqValues[i];
		double var = (k_tot > 1.0) ? ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot : 0.0;
		return Math.sqrt(var / (n_tot - 1));
	}

	/**
	 * Currently only used to compute the default dispersion within rule heuristics.
	 */
	public double getDispersion(ClusAttributeWeights scale, RowData data) {
		System.err.println(getClass().getName()+": getDispersion(): Not yet implemented!");
		return Double.POSITIVE_INFINITY;
	}

	/**
	 * Computes a 2-sample t statistic, without the hypothesis of equal sub-population variances,
	 * and returns the p-value of a t test.
	 * t = (m1 - m2) / sqrt(var1/n1 + var2/n2);
	 * @param att attribute index
	 * @return t p-value
	 * @throws MathException
	 */
	public double getTTestPValue(int att, ClusStatManager stat_manager) throws MathException {
		double global_mean = ((CombStat)stat_manager.getTrainSetStat()).m_RegStat.getMean(att);
		double global_var = ((CombStat)stat_manager.getTrainSetStat()).m_RegStat.getVariance(att);
		double global_n = ((CombStat)stat_manager.getTrainSetStat()).getTotalWeight();
		double local_mean = getMean(att);
		double local_var = getVariance(att);
		double local_n = getTotalWeight();
		double t = Math.abs(local_mean - global_mean) / Math.sqrt(local_var/local_n + global_var/global_n);
		double degreesOfFreedom = 0;
		degreesOfFreedom = df(local_var, global_var, local_n, global_n);
		DistributionFactory distributionFactory = DistributionFactory.newInstance();
		TDistribution tDistribution = distributionFactory.createTDistribution(degreesOfFreedom);
		return 1.0 - tDistribution.cumulativeProbability(-t, t);
	}

	/**
	 * Computes approximate degrees of freedom for 2-sample t-test.
	 * source: math.commons.stat.inference.TTestImpl
	 *
	 * @param v1 first sample variance
	 * @param v2 second sample variance
	 * @param n1 first sample n
	 * @param n2 second sample n
	 * @return approximate degrees of freedom
	 */
	protected double df(double v1, double v2, double n1, double n2) {
		return (((v1 / n1) + (v2 / n2)) * ((v1 / n1) + (v2 / n2))) /
			((v1 * v1) / (n1 * n1 * (n1 - 1d)) + (v2 * v2) / (n2 * n2 * (n2 - 1d)));
	}

	/**
	 * @return Array for all the attributes.
	 */
	public double[] getNumericPred() {
		return m_Means;
	}

	public String getPredictedClassName(int idx) {
		return "";
	}

	public int getNbNumericAttributes() {
		return m_NbAttrs;
	}

	public double getError(ClusAttributeWeights scale) {
		return getSVarS(scale);
	}

	public double getErrorDiff(ClusAttributeWeights scale, ClusStatistic other) {
		return getSVarSDiff(scale, other);
	}

	public double getSVarS(ClusAttributeWeights scale) {
		double result = 0.0;
		for (int i = 0; i < m_NbAttrs; i++) {
			double n_tot = m_SumWeight;
			double k_tot = m_SumWeights[i];
			double sv_tot = m_SumValues[i];
			double ss_tot = m_SumSqValues[i];
			if (k_tot == n_tot) {
				result += (ss_tot - sv_tot*sv_tot/n_tot)*scale.getWeight(m_Attrs[i]);	
			} else {
				result += (ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot)*scale.getWeight(m_Attrs[i]);
			}
		}
		return result / m_NbAttrs;
	}

	public double getRMSE(ClusAttributeWeights scale) {
		return Math.sqrt(getSVarS(scale)/getTotalWeight());
	}

	public double getSVarSDiff(ClusAttributeWeights scale, ClusStatistic other) {
		double result = 0.0;
		RegressionStat or = (RegressionStat)other;
		for (int i = 0; i < m_NbAttrs; i++) {
			double n_tot = m_SumWeight - or.m_SumWeight;
			double k_tot = m_SumWeights[i] - or.m_SumWeights[i];
			double sv_tot = m_SumValues[i] - or.m_SumValues[i];
			double ss_tot = m_SumSqValues[i] - or.m_SumSqValues[i];
			if (k_tot == n_tot) {
				result += (ss_tot - sv_tot*sv_tot/n_tot)*scale.getWeight(m_Attrs[i]);	
			} else {
				result += (ss_tot * (n_tot - 1) / (k_tot - 1) - n_tot * sv_tot/k_tot*sv_tot/k_tot)*scale.getWeight(m_Attrs[i]);
			}
		}
		return result / m_NbAttrs;
	}

	public void initNormalizationWeights(ClusAttributeWeights weights, boolean[] shouldNormalize) {
		for (int i = 0; i < m_NbAttrs; i++) {
			int idx = m_Attrs[i].getIndex();
			if (shouldNormalize[idx]) {
				double var = getVariance(i);
				double norm = var > 0 ? 1/var : 1; // No normalization if variance = 0;
				//if (m_NbAttrs < 15) System.out.println("  Normalization for: "+m_Attrs[i].getName()+" = "+norm);
				weights.setWeight(m_Attrs[i], norm);
			}
		}
	}

	/*
	 * Compute squared Euclidean distance between tuple's target attributes and this statistic's mean.
	 **/
	public double getSquaredDistance(DataTuple tuple, ClusAttributeWeights weights) {
		double sum = 0.0;
		for (int i = 0; i < getNbAttributes(); i++) {
			NumericAttrType type = getAttribute(i);
			double dist = type.getNumeric(tuple) - m_Means[i];
			sum += dist * dist * weights.getWeight(type);
		}
		return sum / getNbAttributes();
	}

	public String getArrayOfStatistic(){
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		for (int i = 0; i < m_NbAttrs; i++) {
			if (i != 0) buf.append(",");
			buf.append(fr.format(m_Means[i]));
		}
		buf.append("]");
		return buf.toString();
	}

	public String getString(StatisticPrintInfo info) {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		for (int i = 0; i < m_NbAttrs; i++) {
			if (i != 0) buf.append(",");
			double tot = getSumWeights(i);
			if (tot == 0) buf.append("?");
			else buf.append(fr.format(getSumValues(i)/tot));
		}
		buf.append("]");
		if (info.SHOW_EXAMPLE_COUNT_BYTARGET) {
			buf.append(": [");
			for (int i = 0; i < m_NbAttrs; i++) {
				if (i != 0) buf.append(",");
				buf.append(fr.format(m_SumWeights[i]));
			}
			buf.append("]");
		} else if (info.SHOW_EXAMPLE_COUNT) {
			buf.append(": ");
			buf.append(fr.format(m_SumWeight));
		}
		return buf.toString();
	}

	public String getPredictString() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < m_NbAttrs; i++) {
			if (i != 0) buf.append(",");
			buf.append(String.valueOf(m_Means[i]));
		}
		return buf.toString();
	}

	public String getDebugString() {
		NumberFormat fr = ClusFormat.THREE_AFTER_DOT;
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		for (int i = 0; i < m_NbAttrs; i++) {
			if (i != 0) buf.append(",");
			buf.append(fr.format(getMean(i)));
		}
		buf.append("]");
		buf.append("[");
		for (int i = 0; i < m_NbAttrs; i++) {
			if (i != 0) buf.append(",");
			buf.append(fr.format(getVariance(i)));
		}
		buf.append("]");
		return buf.toString();
	}

	public void printDebug() {
		for (int i = 0; i < getNbAttributes(); i++) {
			double n_tot = m_SumWeight;
			double k_tot = m_SumWeights[i];
			double sv_tot = m_SumValues[i];
			double ss_tot = m_SumSqValues[i];
			System.out.println("n: "+n_tot+" k: "+k_tot);
			System.out.println("sv: "+sv_tot);
			System.out.println("ss: "+ss_tot);
			double mean = sv_tot / n_tot;
			double var = ss_tot - n_tot*mean*mean;
			System.out.println("mean: "+mean);
			System.out.println("var: "+var);
		}
		System.out.println("err: "+getError());
	}

	public void printDistribution(PrintWriter wrt) throws IOException {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		for (int i = 0; i < m_Attrs.length; i++) {
			wrt.print(StringUtils.printStr(m_Attrs[i].getName(), 35));
			wrt.print(" [");
			wrt.print(fr.format(getMean(i)));
			wrt.print(",");
			wrt.print(fr.format(getVariance(i)));
			wrt.println("]");
		}
	}

	public void addPredictWriterSchema(String prefix, ClusSchema schema) {
		for (int i = 0; i < m_NbAttrs; i++) {
			ClusAttrType type = m_Attrs[i].cloneType();
			type.setName(prefix+"-p-"+type.getName());
			schema.addAttrType(type);
		}
	}

	public String getPredictWriterString() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < m_NbAttrs; i++) {
			if (i != 0) buf.append(",");
			if (m_Means != null) {
				buf.append(""+m_Means[i]);
			} else {
				buf.append("?");
			}
		}
		return buf.toString();
	}

	public void predictTuple(DataTuple prediction) {
		for (int i = 0; i < m_NbAttrs; i++) {
			NumericAttrType type = m_Attrs[i];
			type.setNumeric(prediction, m_Means[i]);
		}
	}

	public void vote(ArrayList votes) {
		reset();
		m_Means = new double[m_NbAttrs];
		RegressionStat vote;
		int nb_votes = votes.size();
		for (int j = 0; j < nb_votes; j++){
			vote = (RegressionStat) votes.get(j);
			for (int i = 0; i < m_NbAttrs; i++){
				m_Means[i] += vote.m_Means[i] / nb_votes;
			}
		}
	}

	public RegressionStat getRegressionStat() {
		return this;
	}
}
