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
  
  private int m_NbNumAtts;
  private int m_NbNomAtts;
  /* Weights for combining compactness of numeric and nominal attrs */
  private double m_WeightNum;
  private double m_WeightNom;
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
    m_WeightNum = (double)m_NbNumAtts / (m_NbNumAtts + m_NbNomAtts);
    m_WeightNom = (double)m_NbNomAtts / (m_NbNumAtts + m_NbNomAtts);
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
  
  /** Returns the compactness of all attributes.
   * 
   * @return combined compactness
   */
  public double compactness() {
    return m_WeightNom * compactnessNom() + m_WeightNum * compactnessNum();
  }
  
  /** Returns the compactness of numeric attributes.
   * 
   * @return compactness of numeric attributes
   */
  public double compactnessNum() {
    return meanVariance();
  }
  
  /** Returns the compactness of nominal attributes.
   * 
   * @return compactness of nominal attributes
   */
  public double compactnessNom() {
    // return meanEntropy();
    return meanDistNom();
  }
  
  /**
   * Returns the mean variance of all numeric attributes.
   * @return the mean variance
   */
  public double meanVariance() {
    double svar = 0;
    for (int i = 0; i < m_NbNumAtts; i++) {
      svar += variance(i);
    }
    return m_NbNumAtts == 0 ? 0.0 : svar / m_NbNumAtts;
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
   * Returns the normalized variance of a numeric attribute in array
   * @param attr intex of the attribute
   * @return the variance
   */
  public double variance(int attr) {
    return m_RegStat.getVariance(attr) *
           m_StatManager.getCompactnessWeights().getWeight(m_RegStat.getAttribute(attr));
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
   * Returns the mean distance of all nominal attributes
   * from the prototypes.
   * @return the mean distance
   */
  public double meanDistNom() {
    double dist = 0;
    for (int i = 0; i < m_NbNomAtts; i++) {
      dist += meanDistNom(i);
    }
    return dist / m_NbNomAtts;
  }

  /**
   * Returns the mean distance of values of a nominal attribute
   * from the prototype.
   * @param attr the attribute
   * @return the mean distance
   */
  public double meanDistNom(int attr) {
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
   * @return Returns the m_WeightNom.
   */
  public double getWeightNom() {
    return m_WeightNom;
  }

  /**
   * @param weightNom The m_WeightNom to set.
   */
  public void setWeightNom(double weightNom) {
    m_WeightNom = weightNom;
  }

  /**
   * @return Returns the m_WeightNum.
   */
  public double getWeightNum() {
    return m_WeightNum;
  }

  /**
   * @param weightNum The m_WeightNum to set.
   */
  public void setWeightNum(double weightNum) {
    m_WeightNum = weightNum;
  }

  /**
   * 
   * @return String representation of the combined statistics
   */
  public String getCompactnessString() {
    StringBuffer buf = new StringBuffer();    
    NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
    buf.append("[");
    buf.append(fr.format(compactness()));
    buf.append(" : ");
    buf.append(fr.format(compactnessNum()));
    buf.append(" , ");
    buf.append(fr.format(compactnessNom()));
    buf.append("]");    
    return buf.toString();
  }

  public String getString() {
    StringBuffer buf = new StringBuffer();    
    buf.append("[");
    buf.append(m_ClassStat.getString());
    buf.append(m_RegStat.getString());
    buf.append("]");    
    return buf.toString();
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
    m_RegStat.copy(or.m_RegStat);     // TODO: Is this ok???
    m_ClassStat.copy(or.m_ClassStat); // TODO: Is this ok???
    m_WeightNum = or.m_WeightNum;
    m_WeightNom = or.m_WeightNom;
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
    case ClusStatManager.MODE_CLASIFFYANDREGRESSION:
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
