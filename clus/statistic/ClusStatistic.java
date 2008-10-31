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

import clus.main.*;
import clus.util.*;
import clus.data.cols.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.data.attweights.*;
import clus.ext.timeseries.TimeSeries;

import java.io.*;
import java.util.ArrayList;

public abstract class ClusStatistic implements Serializable {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public double m_SumWeight; // the weighted sum of all example

	public int m_nbEx;


	public abstract ClusStatistic cloneStat();

	public ClusStatistic cloneSimple() {
		// Statistic with only memory allocated for storing mean
		// not variance, see e.g. RegressionStat.java
		return cloneStat();
	}

	public int getNbAttributes() {
		return getNbNominalAttributes()+getNbNumericAttributes();
	}

	public int getNbNominalAttributes() {
		return 0;
	}

	public int getNbNumericAttributes() {
		return 0;
	}

	public void printDebug() {
	}

	public void setSumWeight(double weight){
		m_SumWeight = weight;
	}

	public void setSDataSize(int nbex) {
		m_nbEx = nbex;
	}

	public void optimizePreCalc(RowData data) {
	}

	public void showRootInfo() {
	}

	public boolean isValidPrediction() {
		return true;
	}

	public void update(ColTarget target, int idx) {
		System.err.println(getClass().getName()+": update(ColTarget target, int idx): Not yet implemented");
	}

	public abstract void updateWeighted(DataTuple tuple, int idx);

	public void updateWeighted(DataTuple tuple, double weight) {
	}

	public abstract void calcMean();

	public abstract String getString(StatisticPrintInfo info);
	public abstract String getPredictedClassName(int idx);
	public abstract String getArrayOfStatistic();

	public void computePrediction() {
		calcMean();
	}

	public String getString() {
		return getString(StatisticPrintInfo.getInstance());
	}

	public String getPredictString() {
		return getString();
	}

	public abstract void reset();

	public abstract void copy(ClusStatistic other);

	public abstract void addPrediction(ClusStatistic other, double weight);

	public abstract void add(ClusStatistic other);

	public void addData(RowData data) {
		for (int i = 0; i < data.getNbRows(); i++) {
			updateWeighted(data.getTuple(i), 1);
		}
	}

	public void addScaled(double scale, ClusStatistic other) {
		System.err.println(getClass().getName()+": addScaled(): Not yet implemented");
	}

	public void resetToSimple(double weight) {
		System.err.println(getClass().getName()+": resetToSimple(): Not yet implemented");
	}

	public abstract void subtractFromThis(ClusStatistic other);

	public abstract void subtractFromOther(ClusStatistic other);

	public double[] getNumericPred() {
		System.err.println(getClass().getName()+": getNumericPred(): Not yet implemented");
		return null;
	}

	public TimeSeries getTimeSeriesPred() {
		System.err.println(getClass().getName()+": getTimeSeriesPred(): Not yet implemented");
		return null;
	}

	public int[] getNominalPred() {
		System.err.println(getClass().getName()+": getNominalPred(): Not yet implemented");
		return null;
	}

	public String getString2() {
		return "";
	}

	public String getClassString() {
		return getString();
	}

	public String getSimpleString() {
		return ClusFormat.ONE_AFTER_DOT.format(m_SumWeight);
	}

	public double getTotalWeight() {
		return m_SumWeight;
	}

	public String getDebugString() {
		return String.valueOf(m_SumWeight);
	}

	public boolean samePrediction(ClusStatistic other) {
		return false;
	}

	/*
	 * getError() and getErrorDiff() methods
	 * - with scaling
	 * - without scaling (only works for classification now!)
	 **/

	public double getError() {
		return getError(null);
	}

	public double getErrorRel() { // 0<error<1
		return getError(null);
	}

	public double getErrorDiff(ClusStatistic other) {
		return getErrorDiff(null, other);
	}

	public double getError(ClusAttributeWeights scale, RowData data) {
		return getError(scale);
	}

	public double getError(ClusAttributeWeights scale) {
		//System.out.println("ClusStatistic :getError");
		System.err.println(getClass().getName()+": getError(): Not yet implemented");
		return Double.POSITIVE_INFINITY;
	}

	public double getErrorDiff(ClusAttributeWeights scale, ClusStatistic other) {
		System.err.println(getClass().getName()+": getErrorDiff(): Not yet implemented");
		return Double.POSITIVE_INFINITY;
	}

	/*
	 * getSS() and getSSDiff() methods, always with scaling
	 * also version available that needs access to the data
	 **/

	public double getSS(ClusAttributeWeights scale) {
		System.err.println(getClass().getName()+": getSS(): Not yet implemented");
		return Double.POSITIVE_INFINITY;
	}

	public double getSSDiff(ClusAttributeWeights scale, ClusStatistic other) {
		System.err.println(getClass().getName()+": getSSDiff(): Not yet implemented");
		return Double.POSITIVE_INFINITY;
	}

	public double getSS(ClusAttributeWeights scale, RowData data) {
		return getSS(scale);
	}

	public double getSSDiff(ClusAttributeWeights scale, ClusStatistic other, RowData data) {
		return getSSDiff(scale, other);
	}
	
	public double getAbsoluteDistance(DataTuple tuple, ClusAttributeWeights weights) {
		return Double.POSITIVE_INFINITY;
	}

	public static void reset(ClusStatistic[] stat) {
		for (int i = 0; i < stat.length; i++) stat[i].reset();
	}

	public String toString() {
		return getString();
	}

	public String getExtraInfo() {
		return null;
	}

	public void printDistribution(PrintWriter wrt) throws IOException {
		wrt.println(getClass().getName()+" does not implement printDistribution()");
	}

	public static void calcMeans(ClusStatistic[] stats) {
		for (int i = 0; i < stats.length; i++) {
			stats[i].calcMean();
		}
	}

	public void addPredictWriterSchema(String prefix, ClusSchema schema) {
	}

	public String getPredictWriterString() {
		return getPredictString();
	}

	public String getPredictWriterString(DataTuple tuple) {
		return getPredictWriterString();
	}

	public void predictTuple(DataTuple prediction) {
		System.err.println(getClass().getName()+" does not implement predictTuple()");
	}

	public RegressionStat getRegressionStat() {
		return null;
	}

	public ClassificationStat getClassificationStat() {
		return null;
	}

	// In multi-label classification: predicted set of classes is union of
	//                                predictions of individual rules
	public void unionInit() {
	}

	public void unionDone() {
	}

	public void union(ClusStatistic other) {
	}

	public abstract void vote(ArrayList votes);

	public ClusStatistic normalizedCopy() {
		return null;
	}

}
