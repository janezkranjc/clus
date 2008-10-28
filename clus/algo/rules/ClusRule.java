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

/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import jeans.util.*;
import clus.algo.tdidt.ClusNode;
import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;
import clus.model.ClusModel;
import clus.model.test.*;
import clus.util.*;
import clus.data.type.*;
import clus.error.*;

public class ClusRule implements ClusModel, Serializable {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected int m_ID;
	protected Object m_Visitor;
	protected ClusStatistic m_TargetStat;
	protected ClusStatistic m_ClusteringStat;
	protected ArrayList m_Tests = new ArrayList();
	/* Array of tuples covered by this rule */
	protected ArrayList m_Data = new ArrayList();
	protected ClusStatManager m_StatManager;
	/* Combined statistics for training and testing data */
	protected CombStat[] m_CombStat = new CombStat[2];
	/* Number of examples covered by this rule */
	protected double[] m_Coverage = new double[2];
	protected ClusErrorList[] m_Errors;
	/* Average error score of the rule */
	protected double m_TrainErrorScore;
	/* Optimized weight of the rule */
	protected double m_OptWeight;

	public ClusRule(ClusStatManager statManager) {
		m_StatManager = statManager;
		m_TrainErrorScore = -1;
		m_OptWeight = -1;
	}

	public int getID() {
		return m_ID;
	}

	public void setID(int id) {
		m_ID = id;
	}

	public ClusStatistic predictWeighted(DataTuple tuple) {
		return m_TargetStat;
	}

	public void computePrediction() {
		m_TargetStat.calcMean();
		m_ClusteringStat.calcMean();
		// setTrainErrorScore();
	}

	public ClusRule cloneRule() {
		ClusRule new_rule = new ClusRule(m_StatManager);
		for (int i = 0; i < getModelSize(); i++) {
			new_rule.addTest(getTest(i));
		}
		return new_rule;
	}

	public boolean equals(Object other) {
		ClusRule o = (ClusRule)other;
		if (o.getModelSize() != getModelSize()) return false;
		for (int i = 0; i < getModelSize(); i++) {
			boolean has_test = false;
			for (int j = 0; j < getModelSize() && !has_test; j++) {
				if (getTest(i).equals(o.getTest(j))) has_test = true;
			}
			if (!has_test) return false;
		}
		return true;
	}

	public int hashCode() {
		int hashCode = 1234;
		for (int i = 0; i < getModelSize(); i++) {
			hashCode += getTest(i).hashCode();
		}
		return hashCode;
	}

	public boolean covers(DataTuple tuple) {
		for (int i = 0; i < getModelSize(); i++) {
			NodeTest test = getTest(i);
			int res = test.predictWeighted(tuple);
			if (res != ClusNode.YES) return false;
		}
		return true;
	}

	public void simplify() {
		for (int i = getModelSize()-1; i >= 0; i--) {
			boolean found = false;
			NodeTest test_i = getTest(i);
			for (int j = 0; j < i && !found; j++) {
				NodeTest test_j = getTest(j);
				NodeTest simplify = test_j.simplifyConjunction(test_i);
				if (simplify != null) {
					setTest(j, simplify);
					found = true;
				}
			}
			if (found) removeTest(i);
		}
	}

	public RowData removeCovered(RowData data) {
		int covered = 0;
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			if (covers(tuple)) covered++;
		}
		int idx = 0;
		RowData res = new RowData(data.getSchema(), data.getNbRows()-covered);
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			if (!covers(tuple)) res.setTuple(tuple, idx++);
		}
		return res;
	}

	public RowData computeCovered(RowData data) {
		int covered = 0;
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			if (covers(tuple)) covered++;
		}
		int idx = 0;
		RowData res = new RowData(data.getSchema(), covered);
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			if (covers(tuple)) res.setTuple(tuple, idx++);
		}
		return res;
	}

	/**
	 * Removes the examples that have been covered by enough rules,
	 * i.e., rules that have weights below the threshold.
	 */
	// TODO: Clean up!
	public RowData removeCoveredEnough(RowData data) {
		// double MAX_TIMES_COVERED = 5;
		// double threshold;
		// if ((getSettings().getCoveringMethod() == Settings.COVERING_METHOD_WEIGHTED_ERROR)){
		// 	threshold = 0.01;
		// } else {
		// 	threshold = 1 / (MAX_TIMES_COVERED + 1 + 1);
		// }
		double threshold = getSettings().getInstCoveringWeightThreshold();
		// if (!getSettings().isCompHeurRuleDist()) { // TODO: check if this is ok!
		if (true) {
			double covered = 0;
			for (int i = 0; i < data.getNbRows(); i++) {
				DataTuple tuple = data.getTuple(i);
				if (tuple.m_Weight < threshold) {
					covered++;
				}
			}
			int idx = 0;
			RowData res;
			res = new RowData(data.getSchema(), (int)(data.getNbRows() - covered));
			for (int i = 0; i < data.getNbRows(); i++) {
				DataTuple tuple = data.getTuple(i);
				if (!(tuple.m_Weight < threshold)) {
					res.setTuple(tuple, idx++);
				}
			}
			return res;
		} else {	// Don't remove, just set the weights to zero.
							// TODO: Check if this causes any problems
			for (int i = 0; i < data.getNbRows(); i++) {
				DataTuple tuple = data.getTuple(i);
				if (tuple.m_Weight < threshold) {
					tuple.changeWeight(0.0);
				}
			}
			return data;
		}
	}

	/**
	 * Reweighs all the examples covered by this rule.
	 */
	public RowData reweighCovered(RowData data) throws ClusException {
		int method = getSettings().getCoveringMethod();
		double gamma = getSettings().getCoveringWeight();
		int nb_rows = data.getNbRows();
		RowData result = new RowData(data.getSchema(), nb_rows);
		double old_weight, new_weight;
		boolean cls_mode = true;
		if (m_TargetStat instanceof RegressionStat) {
			cls_mode = false;
		} else if (m_TargetStat instanceof CombStat){
			throw new ClusException("Error weighted covering not yet supported for mixed classification/regression!");
		}
		if ((gamma >= 1) || (gamma < 0)) {
			throw new ClusException("Error weighted covering: covering weight should be between 0 and 1!");
		}
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			old_weight = tuple.getWeight();
			if (method == Settings.COVERING_METHOD_WEIGHTED_ADDITIVE) {
				// double olditer = (old_weight != 1) ? ((1 - old_weight) / old_weight) : 0;
				// new_weight = 1 / (olditer + 1 + 1);
				new_weight = gamma * old_weight / (old_weight + 1);
			} else if (method == Settings.COVERING_METHOD_WEIGHTED_MULTIPLICATIVE) {
				new_weight = old_weight * gamma;
			} else { // COVERING_METHOD_WEIGHTED_ERROR
				// DONE: weighted by a proportion of incorrectly classified target attributes.
				// TODO: weighted by a distance to a prototype of examples covered by this rule.
				if (cls_mode) { // Classification
					int[] predictions = predictWeighted(tuple).getNominalPred();
					NominalAttrType[] targetAttrs = data.getSchema().getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
					if (predictions.length > 1) { // Multiple target
						double prop_true = 0;
						for (int j = 0; j < predictions.length; j++) {
							int true_value = targetAttrs[j].getNominal(tuple);
							if (predictions[j] == true_value) {
								prop_true++;
							}
						}
						prop_true = prop_true != 0.0 ? prop_true / predictions.length : 0.0;
						new_weight = old_weight * (1 + prop_true * (gamma - 1));
					} else { // Single target
						int prediction = predictions[0];
						int true_value = targetAttrs[0].getNominal(tuple);
						if (prediction == true_value) {
							new_weight = old_weight * gamma;
						} else {
							new_weight = old_weight;
						}
					}
				} else {  // Regression
					double[] predictions = predictWeighted(tuple).getNumericPred();
					NumericAttrType[] targetAttrs = data.getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
					if (predictions.length > 1) { // Multiple target
						double[] true_values = new double[predictions.length];
						ClusStatistic stat = m_StatManager.getTrainSetStat();
						int[] target_idx = new int[predictions.length];
						double[] variance = new double[predictions.length];
						double[] coef = new double[predictions.length];
						for (int j = 0; j < true_values.length; j++) {
							true_values[j] = targetAttrs[j].getNumeric(tuple);
							variance[j] = ((CombStat)stat).getRegressionStat().getVariance(j);
							coef[j] = gamma * Math.abs(predictions[j] - true_values[j]) / Math.sqrt(variance[j]); // Add /2 or /4 here?
						}
						double mean_coef = 0;
						for (int j = 0; j < true_values.length; j++) {
							mean_coef += coef[j];
						}
						mean_coef /= coef.length;
						if (mean_coef > 1) { // Limit max weight to 1
							mean_coef = 1;
						}
						new_weight = old_weight * mean_coef;
					} else { // Single target
						double prediction = predictions[0];
						double true_value = targetAttrs[0].getNumeric(tuple);
						ClusStatistic stat = m_StatManager.getTrainSetStat();
						double variance = ((CombStat)stat).getRegressionStat().getVariance(0);
						double coef = gamma * Math.abs(prediction - true_value) / Math.sqrt(variance); // Add /2 or /4 here?
						if (coef > 1) { // Limit max weight to 1
							coef = 1;
						}
						new_weight = old_weight * coef;
					}
				}
			}
			if (covers(tuple)) {
				result.setTuple(tuple.changeWeight(new_weight), i);
			} else {
				result.setTuple(tuple, i);
			}
		}
		return removeCoveredEnough(result);
	}

	public void setVisitor(Object visitor) {
		m_Visitor = visitor;
	}

	public Object getVisitor() {
		return m_Visitor;
	}

	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
	}

	public void attachModel(HashMap table) throws ClusException {
		for (int i = 0; i < m_Tests.size(); i++) {
			NodeTest test = (NodeTest)m_Tests.get(i);
			test.attachModel(table);
		}
	}

	public void printModel() {
		PrintWriter wrt = new PrintWriter(System.out);
		printModel(wrt);
		wrt.flush();
	}

	public void printModel(PrintWriter wrt) {
		printModel(wrt, StatisticPrintInfo.getInstance());
	}

	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		wrt.print("IF ");
		if (m_Tests.size() == 0) {
			wrt.print("true");
		} else {
			for (int i = 0; i < m_Tests.size(); i++) {
				NodeTest test = (NodeTest)m_Tests.get(i);
				if (i != 0) {
					wrt.println(" AND");
					wrt.print("   ");
				}
				wrt.print(test.getString());
			}
		}
		wrt.println();
		wrt.print("THEN "+m_TargetStat.getString(info));
		if (getID() != 0 && info.SHOW_INDEX) wrt.println(" ("+getID()+")");
		else wrt.println();
		String extra = m_TargetStat.getExtraInfo();
		if (extra != null) {
			// Used, e.g., in hierarchical multi-classification
			wrt.println();
			wrt.print(extra);
		}
		if (getSettings().computeDispersion() && (m_CombStat[ClusModel.TRAIN] != null)) {
			if (getSettings().getRulePredictionMethod() == Settings.RULE_PREDICTION_METHOD_OPTIMIZED) {
				wrt.println("\n   Rule weight        : " + fr.format(getOptWeight()));
			}
			wrt.println("   Dispersion (train): " + m_CombStat[ClusModel.TRAIN].getDispersionString());
			wrt.println("   Coverage   (train): " + fr.format(m_Coverage[ClusModel.TRAIN]));
			wrt.println("   Cover*Disp (train): " + fr.format((m_CombStat[ClusModel.TRAIN].dispersionCalc()*m_Coverage[ClusModel.TRAIN])));
			if (m_CombStat[ClusModel.TEST] != null) {
				wrt.println("   Dispersion (test):  " + m_CombStat[ClusModel.TEST].getDispersionString());
				wrt.println("   Coverage   (test):  " + fr.format(m_Coverage[ClusModel.TEST]));
				wrt.println("   Cover*Disp (test):  " + fr.format((m_CombStat[ClusModel.TEST].dispersionCalc()*m_Coverage[ClusModel.TEST])));
			}
		}
		if (hasErrors()) {
			// Enable with setting PrintRuleWiseErrors = Yes
			ClusErrorList train_err = getError(ClusModel.TRAIN);
			if (train_err != null) {
				wrt.println();
				wrt.println("Training error");
				train_err.showError(wrt);
			}
			ClusErrorList test_err = getError(ClusModel.TEST);
			if (test_err != null) {
				wrt.println();
				wrt.println("Testing error");
				test_err.showError(wrt);
			}
		}
	}

	public void printModelToPythonScript(PrintWriter wrt) {
	}

	public void printModelToQuery(PrintWriter wrt, ClusRun cr, int starttree, int startitem, boolean ex) {
	}
	
	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
	}

	public Settings getSettings() {
		return m_StatManager.getSettings();
	}

	public boolean isEmpty() {
		return getModelSize() == 0;
	}

	public int getModelSize() {
		return m_Tests.size();
	}

	public NodeTest getTest(int i) {
		return (NodeTest)m_Tests.get(i);
	}

	public void setTest(int i, NodeTest test) {
		m_Tests.set(i, test);
	}

	public void addTest(NodeTest test) {
		m_Tests.add(test);
	}

	public void removeTest(int i) {
		m_Tests.remove(i);
	}

	public double[] getCoverage() {
		return m_Coverage;
	}

	public void setCoverage(double[] coverage) {
		m_Coverage = coverage;
	}

	public ClusStatistic getTargetStat() {
		return m_TargetStat;
	}

	public ClusStatistic getClusteringStat() {
		return m_ClusteringStat;
	}

	public void setTargetStat(ClusStatistic stat) {
		m_TargetStat = stat;
	}

	public void setClusteringStat(ClusStatistic stat) {
		m_ClusteringStat = stat;
	}

	public void postProc() {
		m_TargetStat.calcMean();
	}

	public String getModelInfo() {
		return "Tests = "+getModelSize();
	}

	public double getTrainErrorScore() {
		return m_TrainErrorScore;
	}	

	/** Calculates the error score - average of error rates
	 *  across all target attributes on the current training set
	 *  i.e., the data that the rule was trained on and not the 
	 *  entire training set (kind of ...).
	 */
	public void setTrainErrorScore() {
		int nb_tar = m_TargetStat.getNbAttributes();
		double sum_err = 0;
		if (m_TargetStat instanceof ClassificationStat) {
			for (int i = 0; i < nb_tar; i++) {
				int maj_class = ((ClassificationStat)m_TargetStat).getNominalPred()[i];
				double err = 1 - ((ClassificationStat)m_TargetStat).getCount(i,maj_class) / m_TargetStat.m_SumWeight;
				sum_err += err;
			}
			m_TrainErrorScore = sum_err / nb_tar;
		} else { // if (m_TargetStat instanceof RegressionStat) {
			double norm = getSettings().getNumCompNormWeight();
			for (int i = 0; i < nb_tar; i++) {
				double weight = m_StatManager.getClusteringWeights().getWeight(
						((RegressionStat)m_TargetStat).getAttribute(i));
				sum_err += ((RegressionStat)m_TargetStat).getVariance(i) * weight / (norm*norm);
			}
			m_TrainErrorScore = sum_err / nb_tar;
		}
		if (m_TrainErrorScore > 1) { // Limit the error store to 1 
			m_TrainErrorScore = 1;
		}
	}

	/** Calculates the error score - average of error rates
	 *  across all target attributes on the all training data 
	 *  covered by this rule (kind of ...).
	 */
	public void setErrorScore() {
		int nb_rows = m_Data.size();
		int nb_tar = m_TargetStat.getNbAttributes();
		if (m_TargetStat instanceof ClassificationStat) {
			int[] true_counts = new int[nb_tar];
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = (DataTuple)m_Data.get(i);
				int[] prediction = predictWeighted(tuple).getNominalPred();
				int[] true_value = tuple.m_Ints;
				for (int j = 0; j < nb_tar; j++) {
					if (prediction[j] == true_value[j]) {
						true_counts[j]++;
					}
				}
			}
			double sum_err = 0;
			for (int j = 0; j < nb_tar; j++) {
				sum_err += (double)(nb_rows - true_counts[j]) / nb_rows;
			}
			m_TrainErrorScore = sum_err / nb_tar;
		} else { // if (m_TargetStat instanceof RegressionStat) {
			double norm = getSettings().getNumCompNormWeight();
			ClusStatistic stat = m_StatManager.getTrainSetStat();
			NumericAttrType[] target_atts = m_StatManager.getSchema().
												getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
			int[] target_idx = new int[nb_tar];
			double[] variance = new double[nb_tar];
			double[] diff = new double[nb_tar];
			for (int j = 0; j < nb_tar; j++) {
				target_idx[j] = target_atts[j].getArrayIndex();
				variance[j] = ((CombStat)stat).getRegressionStat().getVariance(target_idx[j]);
			}
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = (DataTuple)m_Data.get(i);
				double[] prediction = predictWeighted(tuple).getNumericPred();
				double[] true_value = tuple.m_Doubles;
				for (int j = 0; j < nb_tar; j++) {
					diff[j] += Math.abs(prediction[j] - true_value[j]);				
				}
			}
			double sum_diff = 0;
			for (int j = 0; j < nb_tar; j++) {
				sum_diff += diff[j] / nb_rows / Math.sqrt(variance[j]) / (norm*norm);
			}
			// Should this attribute weight below also be included above?
			// double weight = m_StatManager.getClusteringWeights().getWeight(
			// 		((RegressionStat)m_TargetStat).getAttribute(i));
			m_TrainErrorScore = sum_diff / nb_tar;
			if (m_TrainErrorScore > 1) { // Limit the error store to 1 
				m_TrainErrorScore = 1;
			}
		}
	}

	public double getOptWeight() {
		if (m_OptWeight != -1) {
		  return m_OptWeight;
		} else {
			System.err.println("Warning: Optimal rule weight not initialized!");
			return 0.0;
		}
	}

	public void setOptWeight(double weight) {
		m_OptWeight = weight;
	}

	/**
	 * Computes the dispersion of data tuples covered by this rule.
	 * @param mode 0 for train set, 1 for test set
	 */
	public void computeDispersion(int mode) {
		CombStat combStat = (CombStat)m_StatManager.createStatistic(ClusAttrType.ATTR_USE_ALL);
		for (int i = 0; i < m_Data.size(); i++) {
			combStat.updateWeighted((DataTuple)m_Data.get(i), 0); // second parameter does nothing!
		}
		combStat.calcMean();
		m_CombStat[mode] = combStat;
		// save the coverage
		m_Coverage[mode] = m_Data.size();
	}

	/**
	 * Adds the tuple to the m_Data array
	 * @param tuple
	 */
	public void addDataTuple(DataTuple tuple) {
		m_Data.add(tuple);
	}

	/**
	 * Removes the data tuples from the m_Data array
	 */
	public void removeDataTuples() {
		m_Data.clear();
	}

	public ArrayList getData() {
		return m_Data;
	}

	/**
	 * For computation of rule-wise error measures
	 */
	public void setError(ClusErrorList error, int subset) {
		if (m_Errors == null) m_Errors = new ClusErrorList[2];
		m_Errors[subset] = error;
	}

	public ClusErrorList getError(int subset) {
		if (m_Errors == null) return null;
		return m_Errors[subset];
	}

	public boolean hasErrors() {
		return m_Errors != null;
	}

	public boolean hasPrediction() {
		// Sometimes no valid prediction can be derived from a prototype (e.g., in HMC)
		return m_TargetStat.isValidPrediction();
	}

	public void computeCoverStat(RowData data, ClusStatistic stat) {
		int nb = data.getNbRows();
		stat.setSDataSize(nb);
		for (int i = 0; i < nb; i++) {
			DataTuple tuple = data.getTuple(i);
			if (covers(tuple)) {
				stat.updateWeighted(tuple, i);
			}
		}
		stat.optimizePreCalc(data);
	}

	public ClusModel prune(int prunetype) {
		return this;
	}

  public void retrieveStatistics(ArrayList list) {
  }
}
