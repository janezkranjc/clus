/*
 * Created on 2006.3.29
 */
package clus.tools.optimization.de;

import java.util.*;

import clus.main.*;
import clus.data.type.*;
import clus.statistic.*;

/**
 * Class representing the optimization problem.
 * 
 * @author Tea Tusar
 */
public class DeProbl {
	
	private int m_NumVar;							// Number of variables to optimize
	private ArrayList m_VarMin;				// Min value of each variable
	private ArrayList m_VarMax;				// Max value of each variable
	private double[][][] m_RulePred;	// Rule predictions [instance][rule][class_value] or [0][instance][rule]
	private double[] m_TrueVal;				// True values
	private ClusStatManager m_StatMgr;
	private boolean m_ClssTask; 
	
	/**
	 * Testing constructor
	 * @param num_var
	 */
	public DeProbl(int num_var) {
		m_NumVar = num_var;
		m_VarMin = new ArrayList(m_NumVar);
		m_VarMax = new ArrayList(m_NumVar);
		m_RulePred = null;
		m_TrueVal = null;
		for (int i = 0; i < m_NumVar; i++) {
			m_VarMin.add(new Double(0));
			m_VarMax.add(new Double(1));
		}
	}
	
	public DeProbl(ClusStatManager stat_mgr, double[][][] rule_pred, double[] true_val) {
		m_NumVar = (rule_pred[0]).length;
		m_VarMin = new ArrayList(m_NumVar);
		m_VarMax = new ArrayList(m_NumVar);
		m_RulePred = rule_pred;
		m_TrueVal = true_val;
		m_StatMgr = stat_mgr;
		m_ClssTask = true;
		for (int i = 0; i < m_NumVar; i++) {
			m_VarMin.add(new Double(0));
			m_VarMax.add(new Double(1));
		}
	}
	
	public DeProbl(ClusStatManager stat_mgr, double[][] rule_pred, double[] true_val) {
		m_NumVar = (rule_pred[0]).length;
		m_VarMin = new ArrayList(m_NumVar);
		m_VarMax = new ArrayList(m_NumVar);
		m_RulePred = new double[1][][];
		m_RulePred[0] = rule_pred;
		m_TrueVal = true_val;
		m_StatMgr = stat_mgr;
		m_ClssTask = false;
		for (int i = 0; i < m_NumVar; i++) {
			m_VarMin.add(new Double(0));
			m_VarMax.add(new Double(1));
		}
	}
	
	/**
	 * Generates a random solution.
	 */
	public ArrayList getRandVector(Random rand)	{
		ArrayList result = new ArrayList(m_NumVar);
		for (int i = 0; i < m_NumVar; i++) {			
			result.add(new Double(
			  ((Double)m_VarMin.get(i)).doubleValue() + 
			  (((Double)m_VarMax.get(i)).doubleValue() - 
			   ((Double)m_VarMin.get(i)).doubleValue()) * 
			  (double)rand.nextDouble()));
		}                                                         
		return result;
	}

	public ArrayList getRoundVector(ArrayList genes) {
		ArrayList result = new ArrayList(m_NumVar);
		for (int i = 0; i < m_NumVar; i++) {
			if (((Double)genes.get(i)).doubleValue() >= 
				((Double)m_VarMax.get(i)).doubleValue())
				result.add(m_VarMax.get(i));
			else if (((Double)genes.get(i)).doubleValue() <= 
					 ((Double)m_VarMin.get(i)).doubleValue())
				result.add(m_VarMin.get(i));
			else 
				result.add(genes.get(i));
		}		
		return result;
	}

	/**
	 * Fitness function
	 * @param genes
	 * @return fitness score
	 */
	public double calcFitness(ArrayList genes) {
		double fitness = 0;
		ClusStatistic tar_stat = m_StatMgr.getStatistic(ClusAttrType.ATTR_USE_TARGET);
		int nb_values = ((ClassificationStat)tar_stat).getAttribute(0).getNbValues(); // TODO: more than 1 attribute
		int nb_rows = m_TrueVal.length;
		int nb_covered = 0;
		double[] par = new double[m_NumVar];
		double reg_penalty = 0;
		for (int j = 0; j < m_NumVar; j++) {
			par[j] = ((Double)(genes.get(j))).doubleValue();
			reg_penalty += par[j];
		}
		if (m_ClssTask) {
			double acc = 0;
			for (int i = 0; i < nb_rows; i++) {
				int pred = -1;
				double max = 0;
				double[] pred_sum = new double[nb_values];
				boolean covered = false;
				for (int j = 0; j < m_NumVar; j++) {
					for (int k = 0; k < nb_values; k++) {
						if (m_RulePred[i][j][k] != Double.NaN) {
							covered = true;
							pred_sum[k] += par[j] * m_RulePred[i][j][k];
						}
					}
				}
				for (int k = 0; k < nb_values; k++) {
					if (pred_sum[k] > max) {
						pred = k;
						max = pred_sum[k];
					}
				}
				if ((int)m_TrueVal[i] == pred) {
					acc++;
				}
				if (covered) {
					nb_covered++;
				}
			}
			fitness = (1 - (acc / nb_covered)) + getSettings().getOptRegPar() *  reg_penalty;
			// TODO: regularization penalty should include compactness, coverage?
		} else { // regression
			System.err.println("Fitness function for regression not implemented yet!");
		}
		return fitness;
	}

	/*
	public double calcFitness(ArrayList genes) {
		double fitness = 0;
		for (int i = 0; i < m_NumVar; i++) {	
		fitness += Math.abs(0.1 - ((Double)genes.get(i)).doubleValue()); 
  	}
		return fitness;
	}	 */  

	
	public int getNumVar() {
		return m_NumVar;
	}

	public void setNumVar(int numVar) {
		m_NumVar = numVar;
	} 

	public Settings getSettings() {
		return m_StatMgr.getSettings();
	}
	
}
