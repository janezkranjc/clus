/*
 * Created on May 18, 2005
 */
package clus.statistic;

import java.io.*;
import java.util.*;

import clus.main.*;

public class TargetWeightProducer implements Serializable {

	public double[] m_NumWeights;
  public double[] m_NomWeights;
  protected ClusStatManager m_Manager;

	public TargetWeightProducer(ClusStatManager mgr) {
		m_Manager = mgr;
	}
	
	public Settings getSettings() {
		return m_Manager.getSettings();
	}
	
 public void setTotalStat(ClusStatistic stat) {
		// This version creates uniform weights of 1.0
		int nbnum = stat.getNbNumericAttributes();
		double[] numweights = new double[nbnum];
	  Arrays.fill(numweights, 1);
		setNumWeights(numweights);
    int nbnom = stat.getNbNominalAttributes();
    double[] nomweights = new double[nbnom];
    Arrays.fill(nomweights, 1);
    setNomWeights(nomweights);
 }
	
	public double[] getNumWeights() {
		return m_NumWeights;
	}
	
	public void setNumWeights(double[] weights) {
		m_NumWeights = weights;
	}
  
  public double[] getNomWeights() {
    return m_NomWeights;
  }
  
  public void setNomWeights(double[] weights) {
    m_NomWeights = weights;
  }
	
	public String getName() {
		return "Uniform weights";
	}
}
