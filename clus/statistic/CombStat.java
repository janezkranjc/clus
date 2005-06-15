/**
 * Class that combines statistics for nominal and numeric attributes.
 */
package clus.statistic;

import java.text.NumberFormat;

import clus.data.rows.DataTuple;
import clus.data.type.NominalAttrType;
import clus.main.ClusStatManager;
import clus.main.Settings;
import clus.util.ClusFormat;

public class CombStat extends ClusStatistic {

  public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
  
  private int m_NbNumAtts;
  private int m_NbNomAtts;
  /* Weights for combining compactness of numeric and nominal attrs */
  private double m_WeightNum = 1.0;
  private double m_WeightNom = 1.0;
  private NominalAttrType[] m_NomAtts;
  private RegressionStat m_RegStat;
  private ClassificationStat m_ClassStat;
  private ClusStatManager m_StatManager;
  
  /**
   * Constructor for this class.
   * @param statManager ClusStatManager (to get access to attribute weights
   *        for normalization, nominal and numeric attributes, ...)
   */
  public CombStat(ClusStatManager statManager) {
    
    m_StatManager = statManager;
    m_NbNumAtts = statManager.getSchema().getNbNumeric(); // Without targets!
    m_RegStat = new RegressionStat(m_NbNumAtts);
    m_NbNomAtts = m_StatManager.getSchema().getNbNom(); // Without targets!
    m_NomAtts = new NominalAttrType[m_NbNomAtts];
    for (int i = 0; i < m_NbNomAtts; i++) {
      m_NomAtts[i] = (NominalAttrType)m_StatManager.getSchema().getNominalAttrs()[i];
    }
    // m_NomAtts = (NominalAttrType[])m_StatManager.getSchema().getNominalAttrs();
    m_ClassStat = new ClassificationStat(m_NomAtts);
    }

  protected CombStat() {
  }
  
  public ClusStatistic cloneStat() {
    CombStat result = new CombStat();
    result.m_StatManager = m_StatManager;
    result.m_NbNumAtts = m_NbNumAtts;
    result.m_RegStat = (RegressionStat)m_RegStat.cloneStat();
    result.m_NbNomAtts = m_NbNomAtts;
    result.m_NomAtts = m_NomAtts;
    result.m_ClassStat = (ClassificationStat)m_ClassStat.cloneStat();
    
    
    return result;
  }
  
  public RegressionStat getRegressionStat() {
    return m_RegStat;
  }

  /**
   * Adds the tuple to the statistics
   * @param tuple
   */
  public void updateWeighted(DataTuple tuple, int idx) {
    m_RegStat.updateWeighted(tuple, tuple.getWeight());
    m_ClassStat.updateWeighted(tuple, tuple.getWeight());
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
   * Returns the normalized variance of a numeric attribute in array
   * @param attr intex of the attribute
   * @return the variance
   */
  public double variance(int attr) {
    return m_RegStat.getVariance(attr) *
           m_StatManager.getGlobalWeights().m_NumWeights[attr];
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

  public void reset() {
    // TODO Auto-generated method stub
    
  }

  public void copy(ClusStatistic other) {
    // TODO Auto-generated method stub
    
  }

  public void addPrediction(ClusStatistic other, double weight) {
    // TODO Auto-generated method stub
    
  }

  public void add(ClusStatistic other) {
    // TODO Auto-generated method stub
    
  }

  public void subtractFromThis(ClusStatistic other) {
    // TODO Auto-generated method stub
    
  }

  public void subtractFromOther(ClusStatistic other) {
    // TODO Auto-generated method stub
    
  }

  public int getNbNominalAttributes() {
    return m_ClassStat.getNbNominalAttributes();
  }

  public int getNbNumericAttributes() {
    return m_RegStat.getNbNumericAttributes();
  }
}
