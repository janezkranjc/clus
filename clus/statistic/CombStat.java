/**
 * Class that combines statistics for nominal and numeric attributes.
 */
package clus.statistic;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.io.*;

import org.apache.commons.math.MathException;

import clus.data.attweights.*;
import clus.data.rows.DataTuple;
import clus.data.type.*;
import clus.main.ClusSchema;
import clus.main.ClusStatManager;
import clus.main.Settings;
import clus.util.ClusFormat;

public class CombStat extends ClusStatistic {

  public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
  
  public static int IN_HEURISTIC = 0;
  public static int IN_OUTPUT = 1;
  
  private int m_NbNumAtts;
  private int m_NbNomAtts;
  private NominalAttrType[] m_NomAtts;
  private NumericAttrType[] m_NumAtts;
  protected RegressionStat m_RegStat;
  protected ClassificationStat m_ClassStat;
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
  
  public void updateWeighted(DataTuple tuple, int idx) { // idx?
    m_RegStat.updateWeighted(tuple, tuple.getWeight());
    m_ClassStat.updateWeighted(tuple, tuple.getWeight());
    m_SumWeight += tuple.getWeight();
  }

  public void calcMean() {
    m_RegStat.calcMean();
    m_ClassStat.calcMean();
  }
  
  /** Returns the compactness of all attributes. Used when outputing the compactness.
   * @return combined compactness
   */
  public double compactnessCalc() {
    return compactness(IN_OUTPUT);
  }

  /** Dispersion based heuristic - additive version
   *  Smaller values are better!
   * @return heuristic value
   */
  public double dispersionAdtHeur() {
    double comp = 1.0 + compactness(IN_HEURISTIC);  // 1.0 - offset just in case?
    // Coverage part
  	double train_sum_w = m_StatManager.getTrainSetStat().getTotalWeight();
    double cov_par = getSettings().getHeurCoveragePar();
    comp += (1.0 - cov_par*m_SumWeight/train_sum_w);
    // Prototype distance part
    if (getSettings().isHeurPrototypeDistPar()) {
    	double proto_par = getSettings().getHeurPrototypeDistPar();
    	double proto_val = prototypeDifference((CombStat)m_StatManager.getTrainSetStat());
    	comp += (1.0 - proto_par*m_SumWeight/train_sum_w*proto_val);
    }
    // Significance testing part - TODO: Complete or remove altogether
    if (Settings.IS_RULE_SIG_TESTING) {
    	int sign_diff;
    	int thresh = getSettings().getRuleNbSigAtt();
    	if (thresh > 0) {
    		sign_diff = signDifferent();
    		if (sign_diff < thresh) {
    			comp += 1000; // Some big number ???
    		}
    	} else if (thresh < 0) { // Testing just one target attribute - TODO: change!
    		if (!targetSignDifferent()) {
    			comp += 1000; // Some big number ???
    		}
    	}
    }
    return comp;
  }

  /** Weighted relative dispersion based heuristic - additive version
   *  Smaller values are better!
   * @return heuristic value
   */
  public double wRDispersionAdtHeur() {
    double comp = compactness(IN_HEURISTIC);
    double def_comp = ((CombStat)m_StatManager.getTrainSetStat()).compactness(IN_HEURISTIC);
    comp = comp - def_comp;  // This should be < 0
    // Coverage part
  	double train_sum_w = m_StatManager.getTrainSetStat().getTotalWeight();
    double cov_par = getSettings().getHeurCoveragePar();
    comp += (1.0 - cov_par*m_SumWeight/train_sum_w);
    // Prototype distance part
    if (getSettings().isHeurPrototypeDistPar()) {
    	double proto_par = getSettings().getHeurPrototypeDistPar();
    	double proto_val = prototypeDifference((CombStat)m_StatManager.getTrainSetStat());
    	comp += (1.0 - proto_par*m_SumWeight/train_sum_w*proto_val);
    }
    // Significance testing part - TODO: Complete or remove altogether
    if (Settings.IS_RULE_SIG_TESTING) {
    	int sign_diff;
    	int thresh = getSettings().getRuleNbSigAtt();
    	if (thresh > 0) {
    		sign_diff = signDifferent();
    		if (sign_diff < thresh) {
    			comp += 1000; // Some big number ???
    		}
    	} else if (thresh < 0) { // Testing just one target attribute - TODO: change!
    		if (!targetSignDifferent()) {
    			comp += 1000; // Some big number ???
    		}
    	}
    }
    return comp;
  }

  /** Dispersion based heuristic - multiplicative version
   *  Smaller values are better!
   * @return heuristic value
   */
  public double dispersionMltHeur() {
    // double comp = 1.0 + compactness(IN_HEURISTIC);
    double comp = compactness(IN_HEURISTIC);
double dis1 = comp;
    // Coverage part
  	double train_sum_w = m_StatManager.getTrainSetStat().getTotalWeight();
    double cov_par = getSettings().getHeurCoveragePar();
    comp *= (1.0 + cov_par*train_sum_w/m_SumWeight);
    // comp *= cov_par*m_SumWeight/train_sum_w;
double dis2 = comp;
    // Prototype distance part
    if (getSettings().isHeurPrototypeDistPar()) {
    	double proto_par = getSettings().getHeurPrototypeDistPar();
    	double proto_val = prototypeDifference((CombStat)m_StatManager.getTrainSetStat());
    	comp *= (1.0 + proto_par*m_SumWeight/train_sum_w*proto_val);
    }
    // Significance testing part - TODO: Complete or remove altogether
    if (Settings.IS_RULE_SIG_TESTING) {
    	int sign_diff;
    	int thresh = getSettings().getRuleNbSigAtt();
    	if (thresh > 0) {
    		sign_diff = signDifferent();
    		if (sign_diff < thresh) {
    			comp *= 1000; // Some big number ??? - What if comp < 0???
    		}
    	} else if (thresh < 0) { // Testing just one target attribute - TODO: change!
    		if (!targetSignDifferent()) {
    			comp *= 1000; // Some big number ???
    		}
    	}
    }
// System.err.println("Disp: " + dis1 + " FDisp: " + dis2);
    return comp;
  }

  /** Weighted relative dispersion based heuristic - multiplicative version
   *  Smaller values are better!
   * @return heuristic value
   */
  public double wRDispersionMltHeur() {
  	// Original
/*  	double train_sum_w = m_StatManager.getTrainSetStat().getTotalWeight();
    double comp = compactness(IN_HEURISTIC);
    double def_comp = ((CombStat)m_StatManager.getTrainSetStat()).compactness(IN_HEURISTIC);
    return -m_SumWeight/train_sum_w*(def_comp-comp);
*/    double comp = compactness(IN_HEURISTIC);
double dis1 = comp;
    double def_comp = ((CombStat)m_StatManager.getTrainSetStat()).compactness(IN_HEURISTIC);
    comp = comp - def_comp;  // This should be < 0
    double dis2 = comp;
    // Coverage part
  	double train_sum_w = m_StatManager.getTrainSetStat().getTotalWeight();
    double cov_par = getSettings().getHeurCoveragePar();
    // comp *= (1.0 + cov_par*train_sum_w/m_SumWeight); // How about this???
    // comp *= cov_par*train_sum_w/m_SumWeight;
    comp *= cov_par*m_SumWeight/train_sum_w;
double dis3 = comp;
    // Prototype distance part
    if (getSettings().isHeurPrototypeDistPar()) {
    	double proto_par = getSettings().getHeurPrototypeDistPar();
    	double proto_val = prototypeDifference((CombStat)m_StatManager.getTrainSetStat());
    	comp *= (1.0 + proto_par*m_SumWeight/train_sum_w*proto_val);
    }
    // Significance testing part - TODO: Complete or remove altogether
    if (Settings.IS_RULE_SIG_TESTING) {
    	int sign_diff;
    	int thresh = getSettings().getRuleNbSigAtt();
    	if (thresh > 0) {
    		sign_diff = signDifferent();
    		if (sign_diff < thresh) {
    			comp *= 1000; // Some big number ??? - What if comp < 0???
    		}
    	} else if (thresh < 0) { // Testing just one target attribute - TODO: change!
    		if (!targetSignDifferent()) {
    			comp *= 1000; // Some big number ???
    		}
    	}
    }
// System.err.println("Disp: " + dis1 + " DDisp: " + def_comp + " RDisp: " + dis2 + " FDisp: " + dis3);
    return comp;
  }

  /** Returns the compactness over clustering or all attributes (nominal and numeric).
   *  @return compactness score
   */
  public double compactness(int use) {
    // return compactnessNom(use) + compactnessNum(use);
  	return compactnessNom(use) + meanVariance(use);
  }

  /** Returns the compactness of numeric attributes.
   * 
   * @return compactness of numeric attributes
   */
  public double compactnessNum(int use) {
  	// TODO: Try using mean abs distance instead of variance ...
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
    double sumvar = 0;
    double weight;
    // Normalization with the purpose of geting most of the single variances within the 
    // [0,1] interval. This weight is in stdev units,
    // default value = 4 = (-2sigma,2sigma) should cover 95% of examples 
    double norm = getSettings().getNumCompNormWeight();
    for (int i = 0; i < m_NbNumAtts; i++) {
      if (use == IN_HEURISTIC) {
        weight = m_StatManager.getClusteringWeights().getWeight(m_RegStat.getAttribute(i));
      } else { // use == IN_OUTPUT
        weight = m_StatManager.getCompactnessWeights().getWeight(m_RegStat.getAttribute(i));
      }
      sumvar += m_RegStat.getVariance(i) * weight / (norm*norm);
    }
    return sumvar;
  }

  /**
   * Returns the weighted sum of mean distances for all nominal attributes.
   * @return sum of mean distances
   */
  public double meanDistNom(int use) {
    double sumdist = 0;
    double weight = 0;
    for (int i = 0; i < m_NbNomAtts; i++) {
      if (use == IN_HEURISTIC) {
        weight = m_StatManager.getClusteringWeights().getWeight(m_ClassStat.getAttribute(i));
      } else { // use == IN_OUTPUT
        weight = m_StatManager.getCompactnessWeights().getWeight(m_ClassStat.getAttribute(i));
      }
      sumdist += meanDistNomOne(i) * weight;
    }
    return sumdist;
  }

  /**
   * Returns the mean distance of values of a nominal attribute
   * from the prototype.
   * @param attr_idx the attribute
   * @return the mean distance
   */
  public double meanDistNomOne(int attr_idx) {
    // Syntax: m_ClassStat.m_ClassCounts[nomAttIdx][valueIdx]
    double[] counts = m_ClassStat.m_ClassCounts[attr_idx];
    double[] prototype = new double[counts.length];
    double sum = 0;
    double dist = 0;
    int nbval = counts.length;
    // Prototype
    for (int i = 0; i < nbval; i++) {
      sum += counts[i];
    }
    for (int i = 0; i < nbval; i++) {
      prototype[i] = sum != 0 ? counts[i] / sum : 0;
    }
    // Sum of distances 
    for (int i = 0; i < nbval; i++) {
      dist += (1 - prototype[i]) * counts[i];
    }
    // Scale and normalize to [0,1]
    dist = dist * nbval / (nbval - 1);
    dist = dist != 0.0 ? dist / sum : 0.0;
    return dist;
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
   * Calculates the difference 
   * @return difference
   */
  public double prototypeDifference(CombStat stat) {
    double sumdiff = 0;
    double weight;
    // Numeric atts: abs difference
    for (int i = 0; i < m_NbNumAtts; i++) {
      weight = m_StatManager.getClusteringWeights().
               getWeight(m_RegStat.getAttribute(i));
    	sumdiff += Math.abs(prototypeNum(i) - stat.prototypeNum(i)) * weight;
    	// System.err.println("sumdiff: " + Math.abs(prototypeNum(i) - stat.prototypeNum(i)) * weight);
    }
    // Nominal atts: Mantattan distance
    for (int i = 0; i < m_NbNomAtts; i++) {
      weight = m_StatManager.getClusteringWeights().
               getWeight(m_ClassStat.getAttribute(i));
      double sum = 0;
      double[] proto1 = prototypeNom(i);
      double[] proto2 = stat.prototypeNom(i);
    	for (int j = 0; j < proto1.length; j++) {
    		sum += Math.abs(proto1[j] - proto2[j]);
    	}
    	sumdiff += sum * weight;
    	// System.err.println("sumdiff: " + (sum * weight));
    }
    // System.err.println("sumdiff-total: " + sumdiff);
    return sumdiff != 0 ? sumdiff : 0.0;
  }

  /**
   * Returns the prototype of a nominal attribute.
   * @param attr_idx the attribute
   * @return the prototype
   */
  public double[] prototypeNom(int attr_idx) {
    // Syntax: m_ClassStat.m_ClassCounts[nomAttIdx][valueIdx]
    double[] counts = m_ClassStat.m_ClassCounts[attr_idx];
    double[] prototype = new double[counts.length];
    double sum = 0;
    int nbval = counts.length;
    for (int i = 0; i < nbval; i++) {
      sum += counts[i];
    }
    for (int i = 0; i < nbval; i++) {
      prototype[i] = sum != 0 ? counts[i]/sum : 0;
    }
    return prototype;
  }

  /**
   * Returns the prototype of a numeric attribute.
   * @param attr_idx the attribute
   * @return the prototype
   */
  public double prototypeNum(int attr_idx) {
    return m_RegStat.getMean(attr_idx);
  }

  /**
   * 
   * @return number of attributes with significantly different
   * distributions
   */
  public int signDifferent() {
    int sign_diff = 0;
    // Nominal attributes
    for (int i = 0; i < m_ClassStat.getNbAttributes(); i++) {
      if (SignDifferentNom(i)) {
        sign_diff++;
      }
    }
    // Numeric attributes
     for (int i = 0; i < m_RegStat.getNbAttributes(); i++) {
      try {
        if (SignDifferentNum(i)) {
          sign_diff++;
        }
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (MathException e) {
        e.printStackTrace();
      }
    }
    System.out.println("Nb.sig.atts: " + sign_diff);
    return sign_diff;
  }

  /**
   * Checks weather values of a target attribute are significantly different
   * @return
   */
  public boolean targetSignDifferent() {
    boolean res = false;
    int att = -1;
    String att_name;
    String att_name2;
    ClusStatistic targetStat = m_StatManager.getStatistic(ClusAttrType.ATTR_USE_TARGET);
    if (targetStat instanceof ClassificationStat) {
      for (int i = 0; i < targetStat.getNbNominalAttributes(); i++) {
        att_name = ((ClassificationStat)targetStat).getAttribute(i).getName();
        for (int j = 0; j < m_ClassStat.getNbNominalAttributes(); j++) {
          att_name2 = m_ClassStat.getAttribute(j).getName();
          if (att_name.equals(att_name2)) {
            att = j;
            break;
          }
        }
        if (SignDifferentNom(att)) {
          res = true;
          break; // TODO: If one target att significant, the whole rule significant!?
        }
      }
      // System.out.println("Target sign. testing: " + res);
      return res;
    } else if (targetStat instanceof RegressionStat) {
      for (int i = 0; i < targetStat.getNbNumericAttributes(); i++) {
        att_name = ((RegressionStat)targetStat).getAttribute(i).getName();
        for (int j = 0; j < m_RegStat.getNbNumericAttributes(); j++) {
          att_name2 = m_RegStat.getAttribute(j).getName();
          if (att_name.equals(att_name2)) {
            att = j;
            break;
          }
        }
        try {
          if (SignDifferentNum(att)) {
            res = true;
            break; // TODO: If one target att significant, the whole rule significant!?
          }
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (MathException e) {
          e.printStackTrace();
        }
      }
      return res;
    } else {
      // TODO: Classification and regression
      return true;
    }
  }

  /**
   * Significance testing for a nominal attribute
   * @param att attribute index
   * @return true if this distribution significantly different from global distribution
   * @throws IllegalArgumentException
   * @throws MathException
   */
  private boolean SignDifferentNom(int att) {
    /* double global_n = ((CombStat)m_StatManager.getGlobalStat()).getTotalWeight();
    double local_n = getTotalWeight();
    double ratio = local_n / global_n;
    double global_counts[] = new double[m_ClassStat.getClassCounts(att).length];  
    long local_counts[] = new long[global_counts.length];
    for (int i = 0; i < local_counts.length; i++) {
      local_counts[i] = (long)(m_ClassStat.getClassCounts(att)[i]);
      global_counts[i] = ((CombStat)m_StatManager.getGlobalStat()).m_ClassStat.getClassCounts(att)[i] * ratio;
    }
    ChiSquareTestImpl testStatistic = new ChiSquareTestImpl();
    // alpha = siginficance level, confidence = 1-alpha
    double alpha = getSettings().getRuleSignificanceLevel();
    System.err.println("Attr.nom.: " + att + ", p-valueX: " + testStatistic.chiSquareTest(global_counts, local_counts));
    System.err.println("Attr.nom.: " + att + ", p-valueG: " + m_ClassStat.getGTestPValue(att, m_StatManager));
    System.err.println("Attr.nom.: " + att + ", Gvalue/thresh: " + m_ClassStat.getGTest(att, m_StatManager) + " / " + m_StatManager.getChiSquareInvProb(global_counts.length-1));
    boolean result = testStatistic.chiSquareTest(global_counts, local_counts, alpha);
    System.err.println("Attr.nom.: " + att + ", result: " + result);
    return result; */   
    return m_ClassStat.getGTest(att, m_StatManager);
  }

  /**
   * Significance testing for a numeric attribute
   * @param att attribute index
   * @return true if this distribution significantly different from global distribution
   * @throws IllegalArgumentException
   * @throws MathException
   */
  private boolean SignDifferentNum(int att) throws IllegalArgumentException, MathException {
    // alpha = siginficance level, confidence = 1-alpha
    double alpha = getSettings().getRuleSignificanceLevel();
    double p_value = m_RegStat.getTTestPValue(att, m_StatManager);
    return (p_value < alpha);
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
  
	public void addPredictWriterSchema(String prefix, ClusSchema schema) {
		m_ClassStat.addPredictWriterSchema(prefix, schema);
		m_RegStat.addPredictWriterSchema(prefix, schema);		
	}
	
	public String getPredictWriterString() {
		StringBuffer buf = new StringBuffer();
		buf.append(m_ClassStat.getPredictWriterString());
		if (buf.length() != 0) buf.append(",");
		buf.append(m_RegStat.getPredictWriterString());
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
  
  public String getPredictedClassName(int idx) {
		return "";
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

  // TODO: Is this 'null' ok?
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
    case ClusStatManager.MODE_CLASSIFY_AND_REGRESSION:
      return m_RegStat.getError(scale) + m_ClassStat.getError(scale);
    }
    System.err.println(getClass().getName()+": getError(): Invalid mode!");
    return Double.POSITIVE_INFINITY;
  }

  public void printDistribution(PrintWriter wrt) throws IOException {
  	m_ClassStat.printDistribution(wrt);
  	m_RegStat.printDistribution(wrt);
  }
  
  public void vote(ArrayList votes) {
	  System.err.println(getClass().getName() + "vote (): Not implemented");
  }
}
