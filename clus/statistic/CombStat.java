/**
 * Class that combines statistics for nominal and numeric attributes.
 */
package clus.statistic;

import java.text.NumberFormat;
import java.io.*;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.type.*;
import clus.main.ClusStatManager;
import clus.main.Settings;
import clus.util.ClusFormat;

public class CombStat extends ClusStatistic {

  public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
  
  private static int IN_HEURISTIC = 0;
  private static int IN_OUTPUT = 1;
  
  private int m_NbNumAtts;
  private int m_NbNomAtts;
  private NominalAttrType[] m_NomAtts;
  private NumericAttrType[] m_NumAtts;
  private RegressionStat m_RegStat;
  private ClassificationStat m_ClassStat;
  private ClusStatManager m_StatManager;
  
  /**
   * Constructor for this class.
   * @param statManager ClusStatManager (to get access to attribute weights
   *        for normalization, nominal and numeric attributes, ...)
   */
  public CombStat(ClusStatManager statManager, NumericAttrType[] num, NominalAttrType[] nom) {    
    m_StatManager = statManager;
    m_NbNumAtts = num.length;
    m_NumAtts = num;
    m_NbNomAtts = nom.length;
    m_NomAtts = nom;
    m_RegStat = new RegressionStat(num);
    m_ClassStat = new ClassificationStat(nom);
  }

  protected CombStat() {
  }
  
  public ClusStatistic cloneStat() {
  	return new CombStat(m_StatManager, m_NumAtts, m_NomAtts);
  }
  
  public RegressionStat getRegressionStat() {
    return m_RegStat;
  }
  
  public void updateWeighted(DataTuple tuple, double weight) {
    m_RegStat.updateWeighted(tuple, weight);
    m_ClassStat.updateWeighted(tuple, weight);
    m_SumWeight += weight;  	
  }
  
  /**
   * Adds the tuple to the statistics
   * @param tuple
   */
  public void updateWeighted(DataTuple tuple, int idx) {
    m_RegStat.updateWeighted(tuple, tuple.getWeight());
    m_ClassStat.updateWeighted(tuple, tuple.getWeight());
    m_SumWeight += tuple.getWeight();
  }

  /**
   * Calculates means of both statistics.
   */
  public void calcMean() {
    m_RegStat.calcMean();
    m_ClassStat.calcMean();
  }
  
  /** Returns the compactness of all attributes. Used when outputing the compactness.
   * 
   * @return combined compactness
   */
  public double compactnessCalc() {
    double num_weight;
    double nom_weight;
    if (getSettings().getCompactnessWeights().hasArrayIndexNames()) {
      // Weights given for target, non-target, numeric and nominal
      num_weight = getSettings().getCompactnessWeights().getDouble(Settings.NUMERIC_WEIGHT);
      nom_weight = getSettings().getCompactnessWeights().getDouble(Settings.NOMINAL_WEIGHT);
    } else {
      num_weight = 1.0; // Or something like that?
      nom_weight = 1.0;
    }
    double sum = num_weight + nom_weight;
    num_weight = num_weight / sum; 
    nom_weight = nom_weight / sum;
    double proportion_num = num_weight * m_NbNumAtts / (m_NbNumAtts + m_NbNomAtts);
    double proportion_nom = nom_weight * m_NbNomAtts / (m_NbNumAtts + m_NbNomAtts);
    return proportion_nom * compactnessNom(IN_OUTPUT) + proportion_num * compactnessNum(IN_OUTPUT);
  }

  /** Returns the compactness of all attributes. Used when outputing the compactness.
   * 
   * @return combined compactness
   * TODO: Change!
   */
  public double compactnessNumCalc() {
    double num_weight;
    double nom_weight;
    if (getSettings().getCompactnessWeights().hasArrayIndexNames()) {
      // Weights given for target, non-target, numeric and nominal
      num_weight = getSettings().getCompactnessWeights().getDouble(Settings.NUMERIC_WEIGHT);
      nom_weight = getSettings().getCompactnessWeights().getDouble(Settings.NOMINAL_WEIGHT);
    } else {
      num_weight = 1.0; // Or something like that?
      nom_weight = 1.0;
    }
    double sum = num_weight + nom_weight;
    num_weight = num_weight / sum; 
    nom_weight = nom_weight / sum;
    double proportion_num = num_weight * m_NbNumAtts / (m_NbNumAtts + m_NbNomAtts);
    // double proportion_nom = nom_weight * m_NbNomAtts / (m_NbNumAtts + m_NbNomAtts);
    return proportion_num * compactnessNum(IN_OUTPUT);
  }

  /** Returns the compactness of all attributes. Used when outputing the compactness.
   * 
   * @return combined compactness
   * TODO: Change!
   */
  public double compactnessNomCalc() {
    double num_weight;
    double nom_weight;
    if (getSettings().getCompactnessWeights().hasArrayIndexNames()) {
      // Weights given for target, non-target, numeric and nominal
      num_weight = getSettings().getCompactnessWeights().getDouble(Settings.NUMERIC_WEIGHT);
      nom_weight = getSettings().getCompactnessWeights().getDouble(Settings.NOMINAL_WEIGHT);
    } else {
      num_weight = 1.0; // Or something like that?
      nom_weight = 1.0;
    }
    double sum = num_weight + nom_weight;
    num_weight = num_weight / sum; 
    nom_weight = nom_weight / sum;
    // double proportion_num = num_weight * m_NbNumAtts / (m_NbNumAtts + m_NbNomAtts);
    double proportion_nom = nom_weight * m_NbNomAtts / (m_NbNumAtts + m_NbNomAtts);
    return proportion_nom * compactnessNom(IN_OUTPUT);
  }
  
  /** Returns the compactness of all attributes. Used in compactness based heuristics.
   * 
   * @return combined compactness
   */
  public double compactnessHeur() {
    double num_weight;
    double nom_weight;
    if (getSettings().getClusteringWeights().hasArrayIndexNames()) {
      // Weights given for target, non-target, numeric and nominal
      num_weight = getSettings().getClusteringWeights().getDouble(Settings.NUMERIC_WEIGHT);
      nom_weight = getSettings().getClusteringWeights().getDouble(Settings.NOMINAL_WEIGHT);
    } else {
      num_weight = 1.0; // Or something like that?
      nom_weight = 1.0;
    }
    double sum = num_weight + nom_weight;
    num_weight = num_weight / sum; 
    nom_weight = nom_weight / sum;
    double proportion_num = num_weight * m_NbNumAtts / (m_NbNumAtts + m_NbNomAtts);
    double proportion_nom = nom_weight * m_NbNomAtts / (m_NbNumAtts + m_NbNomAtts);
    return proportion_nom * compactnessNom(IN_HEURISTIC) + proportion_num * compactnessNum(IN_HEURISTIC);
  }
  
  /** Returns the compactness of numeric attributes.
   * 
   * @return compactness of numeric attributes
   */
  public double compactnessNum(int use) {
    return meanVariance(use);
  }
  
  /** Returns the compactness of nominal attributes.
   * 
   * @return compactness of nominal attributes
   */
  public double compactnessNom(int use) {
    // return meanEntropy();
    return meanDistNom(use);
  }
  
  /**
   * Returns the mean variance of all numeric attributes.
   * @return the mean variance
   */
  public double meanVariance(int use) {
    double svar = 0;
    double weight = 0;
    double sumweight = 0;
    for (int i = 0; i < m_NbNumAtts; i++) {
      if (use == IN_HEURISTIC) {
        weight = m_StatManager.getClusteringWeights().getWeight(m_RegStat.getAttribute(i));
      } else { // use == IN_OUTPUT
        weight = m_StatManager.getCompactnessWeights().getWeight(m_RegStat.getAttribute(i));
      }
      sumweight += weight;
      svar += m_RegStat.getVariance(i) * weight;
    }
    return sumweight == 0 ? 0.0 : svar / sumweight;
  }

  /**
   * Returns the mean distance of all nominal attributes
   * from the prototypes.
   * @return the mean distance
   */
  public double meanDistNom(int use) {
    double sumdist = 0;
    double weight = 0;
    double sumweight = 0;
    for (int i = 0; i < m_NbNomAtts; i++) {
      if (use == IN_HEURISTIC) {
        weight = m_StatManager.getClusteringWeights().getWeight(m_ClassStat.getAttribute(i));
      } else { // use == IN_OUTPUT
        weight = m_StatManager.getCompactnessWeights().getWeight(m_ClassStat.getAttribute(i));
      }
      sumweight += weight;
      sumdist += meanDistNomOne(i) * weight;
    }
    return sumweight == 0 ? 0.0 : sumdist / sumweight;
  }

  /**
   * Returns the mean distance of values of a nominal attribute
   * from the prototype.
   * @param attr the attribute
   * @return the mean distance
   */
  public double meanDistNomOne(int attr) {
    // m_ClassStat.m_ClassCounts[nomAttIdx][valueIdx]
    double[] counts = m_ClassStat.m_ClassCounts[attr];
    double[] prototype = new double[counts.length];
    double sum = 0;
    double dist = 0;
    int nbval = counts.length;
    // Calculate the prototype
    for (int i = 0; i < nbval; i++) {
      sum += counts[i];
    }
    for (int i = 0; i < nbval; i++) {
      prototype[i] = counts[i] / sum;
    }
    // Calculate the distance
    for (int i = 0; i < nbval; i++) {
      dist += (1 - prototype[i]) * counts[i];
    }
    return dist != 0.0 ? dist / sum : 0.0;
  }

  /**
   * Returns the mean entropy of all nominal attributes.
   * @return the mean nominal
   */
  public double meanEntropy() {
    double sent = 0;
    for (int i = 0; i < m_NbNomAtts; i++) {
      sent += entropy(i, m_NomAtts[i].getNbValues());
    }
    return sent / m_NbNomAtts;
  }

  /**
   * Returns the entropy of attribute attr.
   * @param attr the attribute
   * @param total the number of possible values of this attribute
   * @return the entropy
   */
  public double entropy(int attr, double total) {
    return m_ClassStat.entropy(attr, total);
  }

  /**
   * 
   * @return String representation of the combined statistics
   */
  public String getCompactnessString() {
    StringBuffer buf = new StringBuffer();    
    NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
    buf.append("[");
    buf.append(fr.format(compactnessCalc()));
    buf.append(" : ");
    buf.append(fr.format(compactnessNum(IN_OUTPUT)));
    buf.append(" , ");
    buf.append(fr.format(compactnessNom(IN_OUTPUT)));
    buf.append("]");    
    return buf.toString();
  }

  public String getString(StatisticPrintInfo info) {
    StringBuffer buf = new StringBuffer();    
    buf.append("[");
    buf.append(m_ClassStat.getString(info));
    buf.append(" | ");
    buf.append(m_RegStat.getString(info));
    buf.append("]");    
    return buf.toString();
  }

	public String getArrayOfStatistic(){
	    return null;
	}

  public void reset() {
    m_RegStat.reset();
    m_ClassStat.reset();
    m_SumWeight = 0.0;
  }

  public void copy(ClusStatistic other) {
    CombStat or = (CombStat)other;
    m_SumWeight = or.m_SumWeight;
    m_StatManager = or.m_StatManager;
    m_NbNumAtts = or.m_NbNumAtts;
    m_NumAtts = or.m_NumAtts;
    m_NbNomAtts = or.m_NbNomAtts;
    m_NomAtts = or.m_NomAtts;
    m_RegStat.copy(or.m_RegStat);
    m_ClassStat.copy(or.m_ClassStat);
  }

  // TODO: Is this ok???
  public void addPrediction(ClusStatistic other, double weight) {
    CombStat or = (CombStat)other;
    m_RegStat.addPrediction(or.m_RegStat, weight);
    m_ClassStat.addPrediction(or.m_ClassStat, weight);
  }

  // TODO: Is this ok???
  public void add(ClusStatistic other) {
    CombStat or = (CombStat)other;
    m_RegStat.add(or.m_RegStat);
    m_ClassStat.add(or.m_ClassStat);
    m_SumWeight += or.m_SumWeight;
  }

  // TODO: Is this ok???
  public void subtractFromThis(ClusStatistic other) {
    CombStat or = (CombStat)other;
    m_RegStat.subtractFromThis(or.m_RegStat);
    m_ClassStat.subtractFromThis(or.m_ClassStat);
    m_SumWeight -= or.m_SumWeight;
  }

  // TODO: Is this ok???
  public void subtractFromOther(ClusStatistic other) {
    CombStat or = (CombStat)other;
    m_RegStat.subtractFromOther(or.m_RegStat);
    m_ClassStat.subtractFromOther(or.m_ClassStat);
    m_SumWeight = or.m_SumWeight - m_SumWeight;
  }

  public int getNbNominalAttributes() {
    return m_ClassStat.getNbNominalAttributes();
  }

  public int getNbNumericAttributes() {
    return m_RegStat.getNbNumericAttributes();
  }

  public double[] getNumericPred() {
    return m_RegStat.getNumericPred();
  }
  
  public int[] getNominalPred() {
    return m_ClassStat.getNominalPred();
  } 

  public Settings getSettings() {
    return m_StatManager.getSettings();
  }

  public double getError() {
    return getError(null);
  }
  
  // TODO: This error asessement should be changed, I guess.
  public double getError(ClusAttributeWeights scale) {
    switch (m_StatManager.getMode()) {
    case ClusStatManager.MODE_CLASSIFY:
      return m_ClassStat.getError(scale);
    case ClusStatManager.MODE_REGRESSION:
      return m_RegStat.getError(scale);
    case ClusStatManager.MODE_CLASIFFY_AND_REGRESSION:
      return m_RegStat.getError(scale) + m_ClassStat.getError(scale);
    }
    System.err.println(getClass().getName()+": getError(): Invalid mode!");
    return Double.POSITIVE_INFINITY;
  }

  public void printDistribution(PrintWriter wrt) throws IOException {
  	m_ClassStat.printDistribution(wrt);
  	m_RegStat.printDistribution(wrt);
  }
}
