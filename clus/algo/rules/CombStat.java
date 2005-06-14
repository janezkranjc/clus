/**
 * Class that combines statistics for nominal and numeric attributes.
 */
package clus.algo.rules;

import java.io.Serializable;
import java.text.NumberFormat;

import clus.data.rows.DataTuple;
import clus.data.type.NominalAttrType;
import clus.statistic.ClassificationStat;
import clus.statistic.RegressionStat;
import clus.util.ClusFormat;

public class CombStat implements Serializable {

  // public static int DESCRIPTIVE = 0;
  // public static int CLUSTERING = 1;
  // public static int TARGET = 2;

  private int m_NbNumAtts;
  private int m_NbNomAtts;
  private double m_WeightNum = 1.0;
  private double m_WeightNom = 1.0;
  private NominalAttrType[] m_NomAtts;
  private RegressionStat m_RegStat;
  private ClassificationStat m_ClassStat;
  
  /**
   * Constructor for this class.
   * @param nbNumAtts number of numeric attributes to calculate
   *                  statistics for
   * @param nomAtts nominal attributes to calculate statistics for
   */
  public CombStat(int nbNumAtts, NominalAttrType[] nomAtts) {
    
    m_NbNumAtts = nbNumAtts;
    m_RegStat = new RegressionStat(nbNumAtts);
    m_NbNomAtts = nomAtts.length;
    m_NomAtts = nomAtts;
    m_ClassStat = new ClassificationStat(nomAtts);
    }

  /**
   * Adds the tuple to the statistics
   * @param tuple
   */
  public void updateWeighted(DataTuple tuple) {
    m_RegStat.updateWeighted(tuple, tuple.getWeight());
    m_ClassStat.updateWeighted(tuple, tuple.getWeight());
  }

  /**
   * Calculates means of both statistics.
   */
  public void calcMeans() {
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
    return meanEntropy();
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
    return svar / m_NbNumAtts;
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
   * Returns the variance of i-th numeric attribute in array
   * @param i
   * @return
   */
  public double variance(int i) {
    return m_RegStat.getVariance(i);
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
  public String getString() {
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

}
