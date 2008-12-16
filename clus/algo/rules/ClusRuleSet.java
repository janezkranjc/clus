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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import jeans.util.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.error.ClusErrorList;
import clus.ext.hierarchical.WHTDStatistic;
import clus.main.*;
import clus.model.ClusModel;
import clus.model.processor.ClusModelProcessor;
import clus.statistic.*;
import clus.tools.optimization.OptProbl;
import clus.tools.optimization.de.DeAlg;
import clus.tools.optimization.de.DeProbl; // Optimization information (data, predictions)
import clus.util.*;

/**
 * Set of predictive rules.
 * 
 *
 */
public class ClusRuleSet implements ClusModel, Serializable {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	/** Default prediction if no other rule covers the instance. */
	protected ClusStatistic m_TargetStat;
	
	/** If we have created an all examples covering rule that is in the first position of rules 
	 * Used for weight optimization.*/
	protected boolean m_allCoveringRuleExists = false;
	
	protected ArrayList<ClusRule> m_Rules = new ArrayList<ClusRule>();
	/** Array of tuples covered by the default rule. */
	protected ArrayList m_DefaultData = new ArrayList();
	protected ClusStatManager m_StatManager;
	protected boolean m_HasRuleErrors;
	protected String m_Comment;

	public ClusRuleSet(ClusStatManager statmanager) {
		m_StatManager = statmanager;
	}

	/**
	 * Clones the rule set so that it points to the same rules.
	 * @return Clone of this rule set.
	 */
	public ClusRuleSet cloneRuleSet() {
		ClusRuleSet new_ruleset = new ClusRuleSet(m_StatManager);
		for (int i = 0; i < getModelSize(); i++) {
			new_ruleset.add(getRule(i));
		}
		return new_ruleset;
	}
	
	/**
	 * Deep clones this ruleset, so that it points to different rules that
	 * have target statistics with the given threshold.
	 * @return Clone of this ruleset with rules with the given treshold.
	 */
	public ClusRuleSet cloneRuleSetWithThreshold(double threshold) {
		ClusRuleSet new_ruleset = new ClusRuleSet(m_StatManager);
		for (int i = 0; i < getModelSize(); i++) {
			ClusRule newRule = getRule(i).cloneRule();
			WHTDStatistic stat = (WHTDStatistic)getRule(i).getTargetStat();
			WHTDStatistic new_stat = (WHTDStatistic)stat.cloneStat();
			new_stat.copyAll(stat);
			new_stat.setThreshold(threshold);
			new_stat.calcMean();
			newRule.setTargetStat(new_stat);
			new_ruleset.add(newRule);
		}
		return new_ruleset;
	}

	/** Add given rule to this rule set
	 * @param rule Added rule
	 */
	public void add(ClusRule rule) {
		if (getSettings().isWeightedCovering()) {
			// Only add unique rules for weighted covering
			if (unique(rule)) {
				m_Rules.add(rule);
			}
		} else {
			m_Rules.add(rule);
		}
	}

	public void removeLastRule() {
		m_Rules.remove(getModelSize()-1);
	}
	
	/** Add given rule if it is not already in the rule set.
	 * @param rule Added rule
	 * @return True if the rule was unique.
	 */
	public boolean addIfUnique(ClusRule rule) {
		if (unique(rule)) {
			m_Rules.add(rule);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Tests if the rule is already in the rule set.
	 * Rules are equal if their tests are same! 
	 */
	public boolean unique(ClusRule rule) {
		boolean res = true;
		for (int i = 0; i < m_Rules.size(); i++) {
			if (((ClusRule)m_Rules.get(i)).equals(rule)) {
				res = false;
			}
		}
		return res;
	}

	/**
	 * Returns the statistic (prediction) for a given tuple.
	 */
	public ClusStatistic predictWeighted(DataTuple tuple) {
		boolean covered = false;
		int pred_method = getSettings().getRulePredictionMethod();
		if (pred_method == Settings.RULE_PREDICTION_METHOD_DECISION_LIST) {
			for (int i = 0; i < getModelSize(); i++) {
				ClusRule rule = getRule(i);
				if (rule.covers(tuple)) {
					return rule.getTargetStat();
				}
			}
			return m_TargetStat;
		} else if (pred_method == Settings.RULE_PREDICTION_METHOD_UNION) {
			// In multi-label classification: predicted set of classes is
			// union of predictions by individual rules
			// TODO: Check if this is obsolete/move/reuse for hierarchical MLC ...
			ClusStatistic stat = m_TargetStat.cloneSimple();
			stat.unionInit();
			for (int i = 0; i < getModelSize(); i++) {
				ClusRule rule = getRule(i);
				if (rule.covers(tuple)) {
					stat.union(rule.getTargetStat());
					covered = true;
				}
			}
			stat.unionDone();
			
			// If covered, return stat, else default prediction
			return covered ? stat : m_TargetStat;
			
		} else if (getSettings().isRulePredictionOptimized()) {
			// Optimized weights 
			// The rules are considered as base predictors of ensemble
			
			// Get the weighted prediction of the rule that covers all the 
			// instances (former default rule)
			ClusRule firstRule = getRule(0);
			ClusStatistic prediction = (firstRule.predictWeighted(tuple)).normalizedCopy()
										.cloneWeighted(getAppropriateWeight(firstRule));
		
			for (int iBaseRule = 1; iBaseRule < getModelSize(); iBaseRule++) {
				ClusRule rule = getRule(iBaseRule);
				
				// If the rule covers tuple (indicator function is nonzero)
				if (rule.covers(tuple)) {
					ClusStatistic rulestat = rule.predictWeighted(tuple);
					
					// For regression normalization does not do anything. 
					// For classification the values are normalized over the the counts.
					ClusStatistic norm_rulestat = rulestat.normalizedCopy();
					prediction.addPrediction(norm_rulestat, getAppropriateWeight(rule));
				}
			}
			
			// Is always covered because default rule is used
			return prediction;
			
		} else {  // Unordered rules (with different weighting schemes)
			ClusStatistic stat = m_TargetStat.cloneSimple();
			double weight_sum = 0.0;
			
			// Get the overall sum of weights for all the rules that cover the example
			for (int i = 0; i < getModelSize(); i++) {
				ClusRule rule = getRule(i);
				if (rule.covers(tuple)) {
					weight_sum += getAppropriateWeight(rule);
				}
			}
			
			// Take the prediction. Normalise
			for (int i = 0; i < getModelSize(); i++) {
				ClusRule rule = getRule(i);
				if (rule.covers(tuple)) {
					ClusStatistic rulestat = rule.predictWeighted(tuple);
					double weight = getAppropriateWeight(rule)/weight_sum;
					ClusStatistic norm_rulestat = rulestat.normalizedCopy();
					stat.addPrediction(norm_rulestat, weight);
					covered = true;
				}
			}
			if (covered) {
				stat.computePrediction();
				return stat;
			} else {
				// If not covered, return default prediction
				return m_TargetStat;
			}
		}
	}
			
	public double getAppropriateWeight(ClusRule rule) {
		switch (getSettings().getRulePredictionMethod()) {
		case Settings.RULE_PREDICTION_METHOD_COVERAGE_WEIGHTED:
			return rule.m_TargetStat.m_SumWeight;
		case Settings.RULE_PREDICTION_METHOD_TOT_COVERAGE_WEIGHTED:
			return rule.getCoverage()[ClusModel.TRAIN];
		case Settings.RULE_PREDICTION_METHOD_ACC_COV_WEIGHTED:
			return rule.m_TargetStat.m_SumWeight*(1-rule.getTrainErrorScore());
		case Settings.RULE_PREDICTION_METHOD_ACCURACY_WEIGHTED:
			return 1-rule.getTrainErrorScore();
		case Settings.RULE_PREDICTION_METHOD_OPTIMIZED:
		case Settings.RULE_PREDICTION_METHOD_GD_OPTIMIZED:
			return rule.getOptWeight();
		default:
			System.err.println("getAppropriateWeight(): Unknown weighted prediction method!");
			return Double.NEGATIVE_INFINITY;
		}
	}

	public void removeEmptyRules() {
		for (int i = getModelSize()-1; i >= 0; i--) {
			if (getRule(i).isEmpty()) {
				m_Rules.remove(i);
			}
		}
	}

	/**
	 * Remove rules that have less weight than OptRuleWeightThreshold set in .s file.
	 */
	public void removeLowWeightRules() {
		double threshold = getSettings().getOptRuleWeightThreshold();
		int nb_rules = getModelSize();
		for (int i = nb_rules-1; i >= 0; i--) {
			
			// If the first rule is all covering, it is not removed (can predict just 0 if that is the weight)
			// This is used instead of default rule if the weights are optimized.
			if (m_allCoveringRuleExists && i == 0)
				continue;
			
			if (getRule(i).getOptWeight() < threshold || getRule(i).getOptWeight() == 0.0) {
				// If optimization is used, the last rule covers all the instances. This should not be removed 
				m_Rules.remove(i);
			}
		}
		if (Settings.VERBOSE > 0) {
			System.out.println("Rules left: " + getModelSize() + " out of " + nb_rules);
		}
	}

	public void simplifyRules() {
		for (int i = getModelSize()-1; i >= 0; i--) {
			getRule(i).simplify();
		}
	}

	public void attachModel(HashMap table) throws ClusException {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.attachModel(table);
		}
	}

	public void printModel(PrintWriter wrt) {
		printModel(wrt, StatisticPrintInfo.getInstance());
	}

	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		// boolean headers = getSettings().computeDispersion() || hasRuleErrors();
		boolean headers = true;
		// [train/test][comb/num/nom]
		double[][] avg_dispersion = new double[2][3];
		double[] avg_coverage = new double[2];
		double[][] avg_prod = new double[2][3];
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			if (headers) {
				String head = new String("Rule " + (i + 1) + ":");
				char[] underline = new char[head.length()];
				for (int j = 0; j < head.length(); j++) {
					underline[j] = '=';
				}
				wrt.println(head);
				wrt.println(new String(underline));
				// Added this test so that PrintRuleWiseErrors also works in HMC setting (02/01/06)
				if (getSettings().computeDispersion()) {
					avg_dispersion[0][0] += rule.m_CombStat[0].dispersionCalc();
					avg_dispersion[0][1] += rule.m_CombStat[0].dispersionNum(1);
					avg_dispersion[0][2] += rule.m_CombStat[0].dispersionNom(1);
					avg_coverage[0] += rule.m_Coverage[0];
					avg_prod[0][0] += rule.m_CombStat[0].dispersionCalc()*rule.m_Coverage[0];
					avg_prod[0][1] += rule.m_CombStat[0].dispersionNum(1)*rule.m_Coverage[0];
					avg_prod[0][2] += rule.m_CombStat[0].dispersionNom(1)*rule.m_Coverage[0];
					if (rule.m_CombStat[1] != null) {
						avg_dispersion[1][0] += rule.m_CombStat[1].dispersionCalc();
						avg_dispersion[1][1] += rule.m_CombStat[1].dispersionNum(1);
						avg_dispersion[1][2] += rule.m_CombStat[1].dispersionNom(1);
						avg_coverage[1] += rule.m_Coverage[1];
						avg_prod[1][0] += rule.m_CombStat[1].dispersionCalc()*rule.m_Coverage[1];
						avg_prod[1][1] += rule.m_CombStat[1].dispersionNum(1)*rule.m_Coverage[1];
						avg_prod[1][2] += rule.m_CombStat[1].dispersionNom(1)*rule.m_Coverage[1];
					}
				}
			}
			rule.printModel(wrt, info);
			wrt.println();
		}
		if (m_TargetStat != null && m_TargetStat.isValidPrediction()) {
			if (headers) {
				if (m_Comment == null) {
					wrt.println("Default rule:");
					wrt.println("=============");
				} else {
					wrt.println("Default rule"+m_Comment+":");
					wrt.println("=============");
				}
			}
			wrt.println("Default = "+(m_TargetStat == null ? "N/A" : m_TargetStat.getString()));			
		}
		if (headers && getSettings().computeDispersion()) {
			wrt.println("\n\nRule set dispersion:");
			wrt.println("=====================");
			avg_dispersion[0][0] = avg_dispersion[0][0] == 0 ? 0 : avg_dispersion[0][0]/m_Rules.size();
			avg_dispersion[0][1] = avg_dispersion[0][1] == 0 ? 0 : avg_dispersion[0][1]/m_Rules.size();
			avg_dispersion[0][2] = avg_dispersion[0][2] == 0 ? 0 : avg_dispersion[0][2]/m_Rules.size();
			avg_coverage[0] = avg_coverage[0] == 0 ? 0 : avg_coverage[0]/m_Rules.size();
			avg_prod[0][0] = avg_prod[0][0] == 0 ? 0 : avg_prod[0][0]/m_Rules.size();
			avg_prod[0][1] = avg_prod[0][1] == 0 ? 0 : avg_prod[0][1]/m_Rules.size();
			avg_prod[0][2] = avg_prod[0][2] == 0 ? 0 : avg_prod[0][2]/m_Rules.size();
			avg_dispersion[1][0] = avg_dispersion[1][0] == 0 ? 0 : avg_dispersion[1][0]/m_Rules.size();
			avg_dispersion[1][1] = avg_dispersion[1][1] == 0 ? 0 : avg_dispersion[1][1]/m_Rules.size();
			avg_dispersion[1][2] = avg_dispersion[1][2] == 0 ? 0 : avg_dispersion[1][2]/m_Rules.size();
			avg_coverage[1] = avg_coverage[1] == 0 ? 0 : avg_coverage[1]/m_Rules.size();
			avg_prod[1][0] = avg_prod[1][0] == 0 ? 0 : avg_prod[1][0]/m_Rules.size();
			avg_prod[1][1] = avg_prod[1][1] == 0 ? 0 : avg_prod[1][1]/m_Rules.size();
			avg_prod[1][2] = avg_prod[1][2] == 0 ? 0 : avg_prod[1][2]/m_Rules.size();
			wrt.println("   Avg_Dispersion  (train): " + fr.format(avg_dispersion[0][0]) + " = " + fr.format(avg_dispersion[0][1]) + " + " + fr.format(avg_dispersion[0][2]));
			wrt.println("   Avg_Coverage    (train): " + fr.format(avg_coverage[0]));
			wrt.println("   Avg_Cover*Disp  (train): " + fr.format(avg_prod[0][0]) + " = " + fr.format(avg_prod[0][1]) + " + " + fr.format(avg_prod[0][2]));
			wrt.println("   Avg_Dispersion  (test):  " + fr.format(avg_dispersion[1][0]) + " = " + fr.format(avg_dispersion[1][1]) + " + " + fr.format(avg_dispersion[1][2]));
			wrt.println("   Avg_Coverage    (test):  " + fr.format(avg_coverage[1]));
			wrt.println("   Avg_Cover*Disp  (test):  " + fr.format(avg_prod[1][0]) + " = " + fr.format(avg_prod[1][1]) + " + " + fr.format(avg_prod[1][2]));
		}
	}

	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, ClusSchema schema) {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.printModel(wrt, info);
			wrt.println();
			wrt.println("Covered examples:");
			ArrayList data = rule.getData();
			ClusAttrType[] attrs = schema.getAllAttrUse(ClusAttrType.ATTR_USE_TARGET);
			ClusAttrType[] key = schema.getAllAttrUse(ClusAttrType.ATTR_USE_KEY);
			for (int k = 0; k < data.size(); k++) {
				DataTuple tuple = (DataTuple)data.get(k);
				wrt.print(String.valueOf(k+1)+": ");
				boolean hasval = false;
				for (int j = 0; j < key.length; j++) {
					if (hasval) wrt.print(",");
					wrt.print(key[j].getString(tuple));
					hasval = true;
				}
				for (int j = 0; j < attrs.length; j++) {
					if (hasval) wrt.print(",");
					wrt.print(attrs[j].getString(tuple));
					hasval = true;
				}
				wrt.println();
			}
			wrt.println();
		}
		wrt.println("Default = "+(m_TargetStat == null ? "None" : m_TargetStat.getString()));
	}

	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
		addDataToRules(examples);
		// setTrainErrorScore(); // Is this needed?
		printModelAndExamples(wrt, info, examples.getSchema());
		removeDataFromRules();
	}

	public void printModelToPythonScript(PrintWriter wrt) {
	}

	public void printModelToQuery(PrintWriter wrt, ClusRun cr, int starttree, int startitem, boolean ex) {
	}
	
	/**
	 * How many rules in the set?
	 */
	public int getModelSize() {
		return m_Rules.size();
	}

	public Settings getSettings() {
		return m_StatManager.getSettings();
	}

	public ClusRule getRule(int i) {
		return (ClusRule)m_Rules.get(i);
	}

	/**
	 * How many literals in all of the rules.
	 */
	public int getNbLiterals() {
		int count = 0;
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			count += rule.getModelSize();
		}
		return count;
	}

	public void setTargetStat(ClusStatistic def) {
		m_TargetStat = def;
	}

	/**
	 * Post process the rule set rule by rule.
	 * Post processing is only calculating the means for each of the target statistics. 
	 */
	public void postProc() {
		m_TargetStat.calcMean();
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.postProc();
		}
	}

	public String getModelInfo() {
		return "Rules = "+getModelSize()+" (Tests: "+getNbLiterals()+")";
	}

	/**
	 * Computes the dispersion of data tuples covered by each rule.
	 * @param mode 0 for train set, 1 for test set
	 */
	public void computeDispersion(int mode) {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.computeDispersion(mode);
		}
	}

	/**
	 * Computes the error score of the rule set. To be used for
	 * deciding whether to add more rules to the rule set or not.
	 */
	// TODO: finish
	public double computeErrorScore(RowData data) {
		ClusStatistic tar_stat = m_StatManager.getStatistic(ClusAttrType.ATTR_USE_TARGET);
		// Average error rate over all target attributes
		if (tar_stat instanceof ClassificationStat) {
			double result = 0;
			int nb_rows = data.getNbRows();
			int nb_tar = tar_stat.getNbNominalAttributes();
			int[] nb_right = new int[nb_tar];
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				int[] predictions = predictWeighted(tuple).getNominalPred();
				int true_value;
				NominalAttrType[] targetAttrs = data.getSchema().getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
				for (int j = 0; j < nb_tar; j++) {
					true_value = targetAttrs[j].getNominal(tuple);
					if (predictions[j] == true_value) {
						nb_right[j]++;
					}
				}
			}
			for (int j = 0; j < nb_tar; j++) {
				result += (1.0 * nb_rows - nb_right[j]) / nb_rows;
			}
			result /= nb_tar;
			return result;
		// Average RMSE over all target attributes
		} else if (tar_stat instanceof RegressionStat) {
			double result = 0;
			int nb_rows = data.getNbRows();
			int nb_tar = tar_stat.getNbNumericAttributes();
			double[] sum_sqr_err = new double[nb_tar];
			NumericAttrType[] targetAttrs = data.getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				double[] predictions = ((RegressionStat)predictWeighted(tuple)).getNumericPred();
				for (int j = 0; j < nb_tar; j++) {
					double diff = predictions[j] - targetAttrs[j].getNumeric(tuple); 
					sum_sqr_err[j] += diff * diff;
				}
			}
			for (int j = 0; j < nb_tar; j++) {
				result += Math.sqrt(sum_sqr_err[j]/nb_rows);
			}
			result /= nb_tar;
			return result;
		} else { // Mixed classification and regression not yet implemented
			return -1;
		}
	}
	
	/**
	 * Sets TrainErrorScore in all rules which is to be used in some schemes
	 * (AccuracyWeighted) for combining predictions of multiple  (unordered) rules.
	 * COMPATIBILITY NOTE: This used to be in addDataToRules(DataTuple) ...
	 */
	public void setTrainErrorScore() {
		for (int i = 0; i < m_Rules.size(); i++) {
			((ClusRule)m_Rules.get(i)).setTrainErrorScore();
		}
	}
	
	/**
	 * Adds tuple to the rule which covers it. Returns true if the
	 * tuple is covered by any rule in this RuleSet and false otherwise.
	 */
	public boolean addDataToRules(DataTuple tuple) {
		boolean covered = false;
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			if (rule.covers(tuple)) {
				rule.addDataTuple(tuple);
				covered = true;
			}
		}
		// COMPATIBILITY NOTE: Here used to be a call to set(Train)ErrorScore()
		// for each rule - no idea why here ... moved outside.
		return covered;
	}

	/**
	 * Adds the data tuples to rules which cover them. Non-covered
	 * tuples are added to the m_DefaultData array.
	 */
	public void addDataToRules(RowData data) {
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			if (!addDataToRules(tuple)) {
				m_DefaultData.add(tuple);
			}
		}
	}

	/**
	 * Removes the data tuples from rules and from m_DefaultData array.
	 */
	public void removeDataFromRules() {
		for (int i = 0; i < m_Rules.size(); i++) {
			((ClusRule)m_Rules.get(i)).removeDataTuples();
		}
		m_DefaultData.clear();
	}
	
	public ArrayList getDefaultData() {
		return m_DefaultData;		
	}

	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
		for (int i = 0; i < getModelSize(); i++) {
			ClusRule rule = getRule(i);
			if (rule.covers(tuple)) {
				for (int j = 0; j < mproc.size(); j++) {
					ClusModelProcessor proc = (ClusModelProcessor)mproc.elementAt(j);
					proc.modelUpdate(tuple, rule);
				}
			}
		}
	}

	public final void applyModelProcessor(DataTuple tuple, ClusModelProcessor proc) throws IOException {
		for (int i = 0; i < getModelSize(); i++) {
			ClusRule rule = getRule(i);
			if (rule.covers(tuple)) proc.modelUpdate(tuple, rule);
		}
	}

	public void setError(ClusErrorList error, int subset) {
		m_HasRuleErrors = true;
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = getRule(i);
			if (error != null) rule.setError(error.getErrorClone(), subset);
			else rule.setError(null, subset);
		}
	}

	public boolean hasRuleErrors() {
		return m_HasRuleErrors;
	}

	public int getID() {
		return 0;
	}

	public void numberRules() {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = getRule(i);
			rule.setID(i+1);
		}
	}

	public ClusModel prune(int prunetype) {
		return this;
	}

	public void retrieveStatistics(ArrayList list) {
	}
	
	/**
	 * Returns the form of this rule set that can be used
	 * for weight optimization 
	 * @param outLogFile File stream for outputs.
	 * @param data Data the optimization is based on.
	 * @return Parameters for optimization. Include true values and predictions for each of the data instances.
	 */
	public OptProbl.OptParam giveFormForWeightOptimization(PrintWriter outLogFile, RowData data){

		DecimalFormat mf = new DecimalFormat("###.000");
		ClusSchema schema = data.getSchema();

		// We need to transform the default rule to ordinary rule. We want to optimize it also.
		addDefaultRuleToRuleSet();

		// Generate optimization input
		ClusStatistic tar_stat = m_StatManager.getStatistic(ClusAttrType.ATTR_USE_TARGET);
		int nb_target = tar_stat.getNbAttributes();
		int nb_rules = getModelSize();
		int nb_rows = data.getNbRows();
		boolean isClassification = false;
		if (m_TargetStat instanceof ClassificationStat) {
			isClassification = true;
		}

		// ***************** TRUE VALUES
		
		ClusAttrType[] trueValuesTemp = new ClusAttrType[nb_target];
		if (isClassification) {
			//NominalAttrType[] 
			trueValuesTemp = (ClusAttrType[])schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		} else { // regression
			//NumericAttrType[]
			trueValuesTemp = (ClusAttrType[])schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		}		
		
		/** 
		 * True values for each target and instance
		 */
		double[][] trueValues = new double[nb_rows][nb_target];
		// Index over the instances of data
		for (int iRows = 0; iRows < nb_rows; iRows++) {
			DataTuple tuple = data.getTuple(iRows);

			// Take the true values from the data target by target
			for (int kTargets = 0; kTargets < nb_target; kTargets++)
			{
				if (isClassification){
					trueValues[iRows][kTargets] = ((NominalAttrType)trueValuesTemp[kTargets]).getNominal(tuple);
				} else {
					trueValues[iRows][kTargets] = ((NumericAttrType)trueValuesTemp[kTargets]).getNumeric(tuple);
				}
			}
		}

//		// ***************** DEFAULT PREDICTION
//		// Used if no other rule cover the instance
//		/**
//		 * Default predictions for each target
//		 */
//		double[] defaultPred = new double[nb_target];
//		if (isClassification){
//			int[] tempDefaults = ((ClassificationStat)m_TargetStat).getNominalPred();
//
//			// Casting from int[] to double[]
//			for (int kTargets = 0; kTargets < nb_target; kTargets++)
//			{
//				defaultPred[kTargets] = (double)tempDefaults[kTargets];
//			}
//		} else {
//			defaultPred = ((RegressionStat)m_TargetStat).getNumericPred();
//		}
		

		// ************ PREDICTIONS

		// Number of nominal values for each target. For regression, no number of nominal values needed, i.e. 1
		int nb_values[] = new int[nb_target]; 

		for (int iTarget=0; iTarget < nb_target;iTarget++) {
			if (isClassification) {
				nb_values[iTarget] = ((ClassificationStat)m_TargetStat).getAttribute(0).getNbValues();		
			} else { // regression
				nb_values[iTarget] = 1; // Nominal values not needed
			}
		}

		// [rule][instance][target][class_value]
		// For regression, class_value = 0 always.
		//double[][][][] rule_pred = new double[nb_rules][nb_rows][nb_target][nb_values];
		double[][][][] rule_pred = new double[nb_rules][nb_rows][nb_target][];

		// Index over the instances of data. This loop is first because getting tuples may be slower.
		for (int iRows = 0; iRows < nb_rows; iRows++) {
			DataTuple tuple = data.getTuple(iRows);
			for (int jRules = 0; jRules < nb_rules; jRules++) {
				ClusRule rule = getRule(jRules);

				// Initialize rule predictions for this rule and instance combination
				for (int iTarget=0; iTarget < nb_target;iTarget++) {
					rule_pred[jRules][iRows][iTarget] = new double[nb_values[iTarget]];
				}

				if (rule.covers(tuple)) {

					// Returns the prediction for the data
					if (isClassification) {					
						rule_pred[jRules][iRows] = 
							((ClassificationStat)rule.predictWeighted(tuple)).normalizedCopy().getClassCounts();
					} else {
						//Only one nominal value is used for regression.
						double[] targets = ((RegressionStat)rule.predictWeighted(tuple)).normalizedCopy()
											.getNumericPred();
						for (int kTargets = 0; kTargets < nb_target; kTargets++) {
							rule_pred[jRules][iRows][kTargets][0] = targets[kTargets];
						}
					}
				} else { // Rule does not cover the instance. Mark as NaN
					for (int kTargets = 0; kTargets < nb_target; kTargets++)
					{   for (int lValues = 0; lValues < nb_values[kTargets]; lValues++) {
							rule_pred[jRules][iRows][kTargets][lValues] = Double.NaN; 
					    }
					}
				}

				// The output does not necessarily have to be given
				if (outLogFile != null)
				{
					// Print predictions to the file.
					outLogFile.print("[");
					for (int kTargets = 0; kTargets < nb_target; kTargets++)
					{
						if (isClassification) outLogFile.print("{");
						for (int lValues = 0; lValues < nb_values[kTargets]; lValues++) {
							outLogFile.print(mf.format(rule_pred[jRules][iRows][kTargets][lValues]));
							if (lValues < nb_values[kTargets]-1)	outLogFile.print("; ");
						}
	
						if (isClassification) outLogFile.print("}");
						if (kTargets < nb_target-1) outLogFile.print("; ");
					}
					outLogFile.print("]"); 
				}
			} // For rules


			if (outLogFile != null) {
				// Print the real outputs
				//wrt_pred.print(" :: {" + mf.format(true_val[iRows]) + "}\n");
				outLogFile.print(" :: [");
				for (int kTargets = 0; kTargets < nb_target; kTargets++) {
					outLogFile.print(mf.format(trueValues[iRows][kTargets]));
					if (kTargets < nb_target-1)	outLogFile.print("; ");
				}
				outLogFile.print("]\n"); 
			}
		}// For instances of data		

		if (outLogFile != null) {
			outLogFile.flush();
		}

//		OptProbl.OptParam param = new OptProbl.OptParam(rule_pred, defaultPred, trueValues); 
		OptProbl.OptParam param = new OptProbl.OptParam(rule_pred, trueValues);
		return param;
	}

	/** 
	 * Add rules in the given set to this rule set.
	 * Uniqueness is not checked because it is based on the test only.
	 * Thus different predictions for rules do not make rules different. 
	 * @param newRules Rules to be added.
	 * @return How many added rules were unique when only descriptions are considered.
	 */
	public int addRuleSet(ClusRuleSet newRules) {
		int numberOfUnique = 0;
		for (int iRule = 0; iRule < newRules.getModelSize(); iRule++)
		{
			if (unique(newRules.getRule(iRule))) {
				numberOfUnique++;
			}
			
			// Add a rule from addRules to this rule set
			add(newRules.getRule(iRule));
		}
		return numberOfUnique;
	}

	/** For optimization, we need to make the default rule a real rule. We need a weight for it etc. 
	 * This rule is the rule that covers all the examples (no test statement) */
	public void addDefaultRuleToRuleSet() {
		ClusRule defaultRuleForEnsembles = new ClusRule(m_StatManager);
		defaultRuleForEnsembles.m_TargetStat = m_TargetStat;
		m_Rules.add(0, defaultRuleForEnsembles); // Adds the default rule to the first position.
		m_TargetStat = null; // To make sure this is not used anymore
		m_allCoveringRuleExists = true;
	}
}
