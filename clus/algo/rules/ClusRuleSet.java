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
import clus.data.rows.*;
import clus.data.type.*;
import clus.error.ClusErrorList;
import clus.main.*;
import clus.statistic.*;
import clus.util.*;

public class ClusRuleSet implements ClusModel, Serializable {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected ClusStatistic m_TargetStat;
	protected ArrayList m_Rules = new ArrayList();
	/* Array of tuples covered by the default rule. */
	protected ArrayList m_DefaultData = new ArrayList();
	protected ClusStatManager m_StatManager;
	protected boolean m_HasRuleErrors;
	
	/**
	 * Constructor for this class.
	 * @param statmanager
	 */
	public ClusRuleSet(ClusStatManager statmanager) {
		m_StatManager = statmanager;
	}

	/**
	 * Clones the rule set so that it points to the same rules.
	 * @return cloned rule set
	 */
	public ClusRuleSet cloneRuleSet() {
		ClusRuleSet new_ruleset = new ClusRuleSet(m_StatManager);
		for (int i = 0; i < getModelSize(); i++) {
			new_ruleset.add(getRule(i));
		}		
		return new_ruleset;
	}
	
	public void add(ClusRule rule) {
		if ((getSettings().getCoveringMethod() == Settings.COVERING_METHOD_WEIGHTED_ADDITIVE) ||
				(getSettings().getCoveringMethod() == Settings.COVERING_METHOD_WEIGHTED_MULTIPLICATIVE) ||
				(getSettings().getCoveringMethod() == Settings.COVERING_METHOD_WEIGHTED_ERROR)) {
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
	 * @param rule to test
	 * @return
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
		// int cover_method = getSettings().getCoveringMethod();
		int prediction_method = getSettings().getRulePredictionMethod();
		// if (cover_method == Settings.COVERING_METHOD_STANDARD) {
		if (prediction_method == Settings.RULE_PREDICTION_METHOD_DECISION_LIST) {
			for (int i = 0; i < getModelSize(); i++) {
				ClusRule rule = getRule(i);
				if (rule.covers(tuple)) {
					return rule.getTargetStat();
				}
			}
			return m_TargetStat;
			// } else if (cover_method == Settings.COVERING_METHOD_UNION) {
		} else if (prediction_method == Settings.RULE_PREDICTION_METHOD_UNION) {
			// In multi-label classification: predicted set of classes is
			// union of predictions by individual rules 
			boolean covered = false;
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
			return covered ? stat : m_TargetStat;
		} else {  // Unordered rules, i.e., weighted covering methods
			boolean covered = false;
			ClusStatistic stat = m_TargetStat.cloneSimple();
			if (prediction_method == Settings.RULE_PREDICTION_METHOD_COVERAGE_WEIGHTED) {
				for (int i = 0; i < getModelSize(); i++) {
					ClusRule rule = getRule(i);
					if (rule.covers(tuple)) {
						ClusStatistic rulestat = rule.predictWeighted(tuple);
						stat.addPrediction(rulestat, 1); // Is this ok for regression also?
						covered = true;         
					}
				}
			} else if (prediction_method == Settings.RULE_PREDICTION_METHOD_COV_ACC_WEIGHTED) {
				double weight = 1;
				double weight_sum = 0;
				for (int i = 0; i < getModelSize(); i++) {
					ClusRule rule = getRule(i);
					if (rule.covers(tuple)) {
						weight = rule.getTrainAccuracy();
						weight_sum += weight;
					}
				}
				for (int i = 0; i < getModelSize(); i++) {
					ClusRule rule = getRule(i);
					if (rule.covers(tuple)) {
						ClusStatistic rulestat = rule.predictWeighted(tuple);
						weight = rule.getTrainAccuracy();
						stat.addPrediction(rulestat, weight/weight_sum);
						covered = true;         
					}
				}
			} else if (prediction_method == Settings.RULE_PREDICTION_METHOD_OPTIMIZED) {
				for (int i = 0; i < getModelSize(); i++) {
					ClusRule rule = getRule(i);
					if (rule.covers(tuple)) {
						ClusStatistic rulestat = rule.predictWeighted(tuple);
						double weight = rule.getOptWeight();
						// TODO: for regression
						// if (rule.getTargetStat() instanceof ClassificationStat) {
							// TODO: This is probably not ok
						// 	rulestat.resetToSimple(weight);
						// }
						stat.addPrediction(rulestat, weight);
						// stat.addPrediction(rulestat, 1);
						covered = true;         
					}
				}
			}
			stat.computePrediction();
			if (covered) {
				return stat;  
			} else {
				return m_TargetStat;
			}
		}
	}
	
	public void removeEmptyRules() {
		for (int i = getModelSize()-1; i >= 0; i--) {
			if (getRule(i).isEmpty()) {
				m_Rules.remove(i);
			}
		}
	}	

	public void removeLowWeightRules() {
		double threshold = getSettings().getOptRuleWeightThreshold();
		int nb_rules = getModelSize();
		for (int i = nb_rules-1; i >= 0; i--) {
			if (getRule(i).getOptWeight() < threshold) {
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
	
	public void attachModel(Hashtable table) throws ClusException {
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
		boolean headers = getSettings().computeCompactness() || hasRuleErrors();
		// [train/test][comb/num/nom]
		double[][] avg_compactness = new double[2][3];
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
				if (getSettings().computeCompactness()) {
					avg_compactness[0][0] += rule.m_CombStat[0].compactnessCalc();
					avg_compactness[0][1] += rule.m_CombStat[0].compactnessNum(1);
					avg_compactness[0][2] += rule.m_CombStat[0].compactnessNom(1);
					avg_coverage[0] += rule.m_Coverage[0];
					avg_prod[0][0] += rule.m_CombStat[0].compactnessCalc()*rule.m_Coverage[0];
					avg_prod[0][1] += rule.m_CombStat[0].compactnessNum(1)*rule.m_Coverage[0];
					avg_prod[0][2] += rule.m_CombStat[0].compactnessNom(1)*rule.m_Coverage[0];
					if (rule.m_CombStat[1] != null) {
						avg_compactness[1][0] += rule.m_CombStat[1].compactnessCalc();
						avg_compactness[1][1] += rule.m_CombStat[1].compactnessNum(1);
						avg_compactness[1][2] += rule.m_CombStat[1].compactnessNom(1);
						avg_coverage[1] += rule.m_Coverage[1];
						avg_prod[1][0] += rule.m_CombStat[1].compactnessCalc()*rule.m_Coverage[1];
						avg_prod[1][1] += rule.m_CombStat[1].compactnessNum(1)*rule.m_Coverage[1];
						avg_prod[1][2] += rule.m_CombStat[1].compactnessNom(1)*rule.m_Coverage[1];
					}
				}
			}
			rule.printModel(wrt, info);
			wrt.println();
		}
		if (m_TargetStat != null && m_TargetStat.isValidPrediction()) {
			if (headers) {
				wrt.println("Default rule:");
				wrt.println("=============");
			}
			wrt.println("Default = "+(m_TargetStat == null ? "N/A" : m_TargetStat.getString()));
		}
		if (headers && getSettings().computeCompactness()) {
			wrt.println("\n\nRule set compactness:");
			wrt.println("=====================");
			avg_compactness[0][0] = avg_compactness[0][0] == 0 ? 0 : avg_compactness[0][0]/m_Rules.size();
			avg_compactness[0][1] = avg_compactness[0][1] == 0 ? 0 : avg_compactness[0][1]/m_Rules.size();
			avg_compactness[0][2] = avg_compactness[0][2] == 0 ? 0 : avg_compactness[0][2]/m_Rules.size();
			avg_coverage[0] = avg_coverage[0] == 0 ? 0 : avg_coverage[0]/m_Rules.size();
			avg_prod[0][0] = avg_prod[0][0] == 0 ? 0 : avg_prod[0][0]/m_Rules.size();
			avg_prod[0][1] = avg_prod[0][1] == 0 ? 0 : avg_prod[0][1]/m_Rules.size();
			avg_prod[0][2] = avg_prod[0][2] == 0 ? 0 : avg_prod[0][2]/m_Rules.size();
			avg_compactness[1][0] = avg_compactness[1][0] == 0 ? 0 : avg_compactness[1][0]/m_Rules.size();
			avg_compactness[1][1] = avg_compactness[1][1] == 0 ? 0 : avg_compactness[1][1]/m_Rules.size();
			avg_compactness[1][2] = avg_compactness[1][2] == 0 ? 0 : avg_compactness[1][2]/m_Rules.size();
			avg_coverage[1] = avg_coverage[1] == 0 ? 0 : avg_coverage[1]/m_Rules.size();
			avg_prod[1][0] = avg_prod[1][0] == 0 ? 0 : avg_prod[1][0]/m_Rules.size();
			avg_prod[1][1] = avg_prod[1][1] == 0 ? 0 : avg_prod[1][1]/m_Rules.size();
			avg_prod[1][2] = avg_prod[1][2] == 0 ? 0 : avg_prod[1][2]/m_Rules.size();
			wrt.println("   Avg_Compactness (train): " + fr.format(avg_compactness[0][0]) + " = " + fr.format(avg_compactness[0][1]) + " + " + fr.format(avg_compactness[0][2]));
			wrt.println("   Avg_Coverage    (train): " + fr.format(avg_coverage[0]));
			wrt.println("   Avg_Cover*Comp  (train): " + fr.format(avg_prod[0][0]) + " = " + fr.format(avg_prod[0][1]) + " + " + fr.format(avg_prod[0][2]));
			wrt.println("   Avg_Compactness (test):  " + fr.format(avg_compactness[1][0]) + " = " + fr.format(avg_compactness[1][1]) + " + " + fr.format(avg_compactness[1][2]));
			wrt.println("   Avg_Coverage    (test):  " + fr.format(avg_coverage[1]));
			wrt.println("   Avg_Cover*Comp  (test):  " + fr.format(avg_prod[1][0]) + " = " + fr.format(avg_prod[1][1]) + " + " + fr.format(avg_prod[1][2]));
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
		printModelAndExamples(wrt, info, examples.getSchema());
		removeDataFromRules();		
	}	
	
	public void printModelToPythonScript(PrintWriter wrt) {
	}
	
	public void printModelToQuery(PrintWriter wrt, ClusRun cr, int starttree, int startitem) {
	}
	public int getModelSize() {
		return m_Rules.size();
	}
	
	public Settings getSettings() {
		return m_StatManager.getSettings();
	}
	
	public ClusRule getRule(int i) {
		return (ClusRule)m_Rules.get(i);
	}
	
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
	 * Computes the compactness of data tuples covered by each rule.
	 * @param mode 0 for train set, 1 for test set
	 */
	public void computeCompactness(int mode) {
		for (int i = 0; i < m_Rules.size(); i++) {
			ClusRule rule = (ClusRule)m_Rules.get(i);
			rule.computeCompactness(mode);
		}
	}
	
	/** 
	 * Computes the error score of the rule set.
	 * @param data data to compute error score on
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
				for (int j = 0; j < nb_tar; j++) {
					// Target attributes are first in m_Ints[]
					true_value = tuple.getIntVal(j);
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
		// Average variance over all target attributes
		} else if (tar_stat instanceof RegressionStat) {
			double result = 0;
			int nb_rows = data.getNbRows();
			int nb_tar = tar_stat.getNbNumericAttributes();
			ClusStatistic stat = tar_stat.cloneStat();
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				ClusStatistic ruleset_stat = predictWeighted(tuple);
				stat.addPrediction(ruleset_stat, 1);
			}
			for (int j = 0; j < nb_tar; j++) {
				// TODO: replace!
				result += ((RegressionStat)stat).getVariance(j);
				//result += (RegressionStat)stat.getRMSE(scale);
			}
			result /= nb_tar; 
			return result;
		} else {
			return -1;
		}
	}
	
	/** 
	 * Adds the tuple to the rule which covers it.
	 * @param tuple the data tuple
	 * @return true if tuple is covered by any rule in this RuleSet,
	 *         false otherwise
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
		return covered;
	}
	
	/**
	 * Adds the data tuples to rules which cover them. Noncovered
	 * tuples are added to the m_DefaultData array. 
	 * @param data the data 
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
}
