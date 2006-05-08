/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import jeans.util.*;
import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;
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
	protected ClusErrorParent[] m_Errors;
	/* Accuracy score of the rule on the training set */
	protected double m_TrainAccuracy;
	/* Optimized weight of the rule */
	protected double m_OptWeight;
	
	public ClusRule(ClusStatManager statManager) {
		m_StatManager = statManager;
		m_TrainAccuracy = -1;
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
		setTrainAccuracy();
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
	 * Removes the examples that have been covered by enough rules, i.e.,
	 * have weights below the threshold.
	 * @param data
	 * @return data
	 */
	public RowData removeCoveredEnough(RowData data) {
		double MAX_TIMES_COVERED = 5; // TODO: Should be a parameter
		double threshold;
		if ((getSettings().getCoveringMethod() == Settings.COVERING_METHOD_WEIGHTED_ERROR) ||
			  (getSettings().getCoveringMethod() == Settings.COVERING_METHOD_RULE_SET)){
			threshold = 0.01; // TODO: Should be a parameter
		} else {
			threshold = 1 / (MAX_TIMES_COVERED + 1 + 1);
		}
		// if (!getSettings().isCompHeurRuleDist()) {
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
	 * Reweighs all the examples covered by this rule. Reweighting
	 * method can be additive (w=1/(1+i)) or multiplicative (w=gamma^i).
	 * See Lavrac et al. 2004: Subgroup discovery with CN2-SD.
	 * 
	 * TODO: reweigh only positive examples, what to do for regression???
	 * 
	 * @param data all examples
	 * @return data with reweighted examples
	 * @throws ClusException 
	 */
	public RowData reweighCovered(RowData data) throws ClusException {
		int method = getSettings().getCoveringMethod();
		double gamma = getSettings().getCoveringWeight();
		double newweight;
		RowData result = new RowData(data.getSchema(), data.getNbRows());
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			double oldweight = tuple.getWeight();
			if (oldweight > 0) {
				if (method == Settings.COVERING_METHOD_WEIGHTED_ADDITIVE) {
					double olditer = (oldweight != 1) ? ((1-oldweight)/oldweight) : 0;
					newweight = 1/(olditer+1+1);
				} else if (method == Settings.COVERING_METHOD_WEIGHTED_MULTIPLICATIVE) {
					newweight = oldweight*gamma;
				} else if (method == Settings.COVERING_METHOD_WEIGHTED_ERROR ||
						       method == Settings.COVERING_METHOD_RULE_SET ||
						       method == Settings.COVERING_METHOD_BEAM_RULE_SET ||
					         method == Settings.COVERING_METHOD_BEAM_RULE_DEF_SET ||
					         method == Settings.COVERING_METHOD_RANDOM_RULE_SET) {
					if (m_TargetStat instanceof ClassificationStat) {  // Classification
						int[] predictions = predictWeighted(tuple).getNominalPred();
						if (predictions.length > 1) {
							// DONE: weighted by a proportion of incorrectly classified target attributes
							// TODO: weighted by a distance to a prototype of examples covered by this rule
							double coeff = predictions.length;
							for (int j = 0; j < predictions.length; j++) {
								int true_value = tuple.getIntVal(j);
								if (predictions[j] == true_value) {
									coeff--;
								}
							}
							coeff = coeff != 0.0 ? coeff / predictions.length : 0.0;
							newweight = oldweight * coeff * gamma;
						} else {
							int prediction = predictions[0];
							int true_value = tuple.getClassification(); 
							if (prediction == true_value) {
								newweight = oldweight * gamma;
							} else {
								newweight = oldweight;
							}
						}
					} else if (m_TargetStat instanceof RegressionStat) {  // Regression
						double[] predictions = predictWeighted(tuple).getNumericPred();
						if (predictions.length > 1) {
							double[] true_values = new double[predictions.length];
							ClusStatistic stat = m_StatManager.getGlobalStat();
							int[] target_idx = new int[predictions.length];
							double[] variance = new double[predictions.length];
							double[] coef = new double[predictions.length];
							for (int j = 0; j < true_values.length; j++) {  
								true_values[j] = tuple.getDoubleVal(j);
								NumericAttrType[] num_targets = m_StatManager.getSchema().
								getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
								target_idx[j] = num_targets[0].getArrayIndex();
								variance[j] = ((CombStat)stat).getRegressionStat().getVariance(target_idx[j]);
								// TODO: below (2x) are two possibilities for parameter gamma. Which is better?
								// coef[j] = Math.abs(predictions[j] - true_values[j]) / (gamma * Math.sqrt(variance[j]));
								coef[j] = Math.abs(predictions[j] - true_values[j]) / Math.sqrt(variance[j]);
							}
							double mean_coef = 0;
							for (int j = 0; j < true_values.length; j++) {
								mean_coef += coef[j];
							}
							mean_coef /= coef.length;
							if (mean_coef > 1) { // Limit max weight to 1
								mean_coef = 1; 
							}
							// newweight = oldweight * mean_coef;
							newweight = oldweight * mean_coef * gamma;
						} else {
							double prediction = predictions[0];
							double true_value = tuple.getDoubleVal(0);
							NumericAttrType[] num_targets = m_StatManager.getSchema().
							getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
							int target_idx = num_targets[0].getArrayIndex();
							ClusStatistic stat = m_StatManager.getGlobalStat();
							double variance = ((CombStat)stat).getRegressionStat().getVariance(target_idx);
							// double coef = Math.abs(prediction - true_value) / (gamma * Math.sqrt(variance));
							double coef = Math.abs(prediction - true_value) / Math.sqrt(variance);
							if (coef > 1) { // Limit max weight to 1
								coef = 1; 
							}
							// newweight = oldweight * coef;
							newweight = oldweight * coef * gamma;
						}
					} else {
						throw new ClusException("Error weighted covering not yet supported for mixed classification/regression!");
					}
				} else {
					throw new ClusException("Unsupported covering method!");
				}
				if (covers(tuple)) {
					result.setTuple(tuple.changeWeight(newweight), i);
				} else {
					result.setTuple(tuple, i);
				}
			} else if (oldweight < 0){
				throw new ClusException("Negative example weights not supported!");
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
	
	public void attachModel(Hashtable table) throws ClusException {
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
		if (getSettings().computeCompactness() && (m_CombStat[ClusModel.TRAIN] != null)) {
			if (getSettings().getRulePredictionMethod() == Settings.RULE_PREDICTION_METHOD_OPTIMIZED) {
				wrt.println("\n   Rule weight        : " + fr.format(getOptWeight()));
			}
			wrt.println("   Compactness (train): " + m_CombStat[ClusModel.TRAIN].getCompactnessString());
			wrt.println("   Coverage    (train): " + fr.format(m_Coverage[ClusModel.TRAIN]));
			wrt.println("   Cover*Comp  (train): " + fr.format((m_CombStat[ClusModel.TRAIN].compactnessCalc()*m_Coverage[ClusModel.TRAIN])));
			if (m_CombStat[ClusModel.TEST] != null) {
				wrt.println("   Compactness (test):  " + m_CombStat[ClusModel.TEST].getCompactnessString());
				wrt.println("   Coverage    (test):  " + fr.format(m_Coverage[ClusModel.TEST]));
				wrt.println("   Cover*Comp  (test):  " + fr.format((m_CombStat[ClusModel.TEST].compactnessCalc()*m_Coverage[ClusModel.TEST])));
			}
		}
		if (hasErrors()) {
			// Enable with setting PrintRuleWiseErrors = Yes
			ClusErrorParent train_err = getError(ClusModel.TRAIN);
			if (train_err != null) {
				wrt.println();
				wrt.println("Training error");
				train_err.showError(wrt);    			
			}
			ClusErrorParent test_err = getError(ClusModel.TEST);
			if (test_err != null) {
				wrt.println();
				wrt.println("Testing error");
				test_err.showError(wrt);    			
			}    		
		}
	}
	
	public void printModelToPythonScript(PrintWriter wrt) {
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
	
	public double getTrainAccuracy() {
		return m_TrainAccuracy;
	}	
	
	/** Calculates the accuracy score - average of accuracies
	 *  across all target attributes (kind of).
	 *
	 */
	public void setTrainAccuracy() {
		if (m_TargetStat instanceof ClassificationStat) {
			double sum_acc = 0;
			int nb_tar = m_TargetStat.getNbNominalAttributes();
			for (int i = 0; i < nb_tar; i++) {
				int maj_class = ((ClassificationStat)m_TargetStat).getNominalPred()[i];
				double acc = ((ClassificationStat)m_TargetStat).getCount(i,maj_class) / m_TargetStat.m_SumWeight;
				sum_acc += acc;
			}
			m_TrainAccuracy = sum_acc / nb_tar;
		} else if (m_TargetStat instanceof RegressionStat) {
			// TODO: I'm not really sure about this
			double sum_acc = 0;
			int nb_tar = m_TargetStat.getNbNumericAttributes();
			for (int i = 0; i < nb_tar; i++) {
				double acc = ((RegressionStat)m_TargetStat).getStandardDeviation(i) / m_TargetStat.m_SumWeight;
				sum_acc += acc;
			}
			m_TrainAccuracy = 1 / (sum_acc / nb_tar); // ?!
		} else {
			m_TrainAccuracy = -1;
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
	 * Computes the compactness of data tuples covered by this rule.
	 * @param mode 0 for train set, 1 for test set
	 */
	public void computeCompactness(int mode) {
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
	public void setError(ClusErrorParent error, int subset) {
		if (m_Errors == null) m_Errors = new ClusErrorParent[2];
		m_Errors[subset] = error;
	}
	
	public ClusErrorParent getError(int subset) {
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
