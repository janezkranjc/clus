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
 * @author Timo Aho modified for multi target use 10.11.2008.
 */
public class DeProbl {

	/** Number of weights/variables to optimize */
	private int m_NumVar;				
	/** Min value of each variable */
	private ArrayList m_VarMin;			
	/** Max value of each variable */
	private ArrayList m_VarMax;			
	
	/** 
	 * Rule predictions [instance][rule index][target index][class_value] is for nominal attributes
	 * or [instance][rule index][target index][0] for regression.
	 * The [target index] is always [0] for single target use.
	 * The class value dimension is dynamic for each target index.  
	 */
	private double[][][][] m_RulePred;
	//private ArrayList<Double>[][][] m_RulePred;
//	private double[] m_TrueVal;			/** True target values for the data points */

	/** True target values for the data points. [instance][target index] */
	private double[][] m_TrueVal;			
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

	
	/**
	 * Constructor for problem to be solved with differential evolution. Both classification and regression.
	 * TODO How to solve a problem with both regression and classification targets?
	 * @param stat_mgr Statistics
	 * @param rule_pred Four dimensional array for the predictions. [instance][rule][target index][class_value]
	 *                  For regression the class_value = 0 always. 
	 * @param true_val True values for instances. [instance][target index]
	 * @param isClassification Is it classification or regression?
	 */
	public DeProbl(ClusStatManager stat_mgr, double[][][][] rule_pred, double[][] true_val,
			boolean isClassification) {
		m_NumVar = (rule_pred[0]).length;
		m_VarMin = new ArrayList(m_NumVar);
		m_VarMax = new ArrayList(m_NumVar);
		m_RulePred = rule_pred;
		m_TrueVal = true_val;
		m_StatMgr = stat_mgr;
		m_ClssTask = isClassification;
		for (int i = 0; i < m_NumVar; i++) {
			m_VarMin.add(new Double(0));
			m_VarMax.add(new Double(1));
		}
	}

//	/**
//	 * Constructor for regression problem to be solved with differential evolution.
//	 * @param stat_mgr Statistics
//	 * @param rule_pred Three dimensional array for the predictions. [instance][rule][target index]
//	 * @param true_val True values for instances. [instance][target index]
//	 */
//	public DeProbl(ClusStatManager stat_mgr, double[][][] rule_pred, double[][] true_val) {
//		m_NumVar = (rule_pred[0]).length;
//		m_VarMin = new ArrayList(m_NumVar);
//		m_VarMax = new ArrayList(m_NumVar);
//		//m_RulePred = new double[1][][][];
//      //Should add the last dimension, i.e. m_RulePred = new double[][][][1] in for loop. 
//		m_RulePred[0] = rule_pred;
//		m_TrueVal = true_val;
//		m_StatMgr = stat_mgr;
//		m_ClssTask = false;
//		for (int i = 0; i < m_NumVar; i++) {
//			m_VarMin.add(new Double(0));
//			m_VarMax.add(new Double(1));
//		}
//	}

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

//	public double calcFitness(ArrayList genes, int typeLossFunction, int typeRegularization) {
// int LOSS_RRMSE = //Relative root mean squared error for regression, Zenko 2007, p. 27
// int LOSS_CLASS_ERROR = //Multi targeted 0/1 classification error, Zenko 2007, p. 26
// int LOSS_HUBER = // Huber 1962 Loss function. Robust for outliers. See Friedman&Popescu 2005, p. 7.
// int LOSS_SQUARED = // Squared error loss
// int REGUL_RIDGE = // Ridge regularization, i.e. sum of l_2 penalty.
// int REGUL_LASSO = // Lasso regularization, i.e. sum of l_1 penalty.
// int REGUL_GENERAL = // TODO : Give the regularization function as function object
// int LOSS_GENERAL = // TODO: Give the loss function as function object  
	/**
	 * Fitness function.
	 * The classification prediction is voting without weights.
	 * @param genes The current generation (population).
	 * @return fitness score
	 */
	public double calcFitness(ArrayList<Double> genes) {
		// Only the target attributes are returned?
		ClusStatistic tar_stat = m_StatMgr.getStatistic(ClusAttrType.ATTR_USE_TARGET);

		// TODO mixture of nominal and numberic attributes
		// TODO: For multi target case some of the targets may be nominal, some numerical
		// Thus we check the targets one by one.


		int nb_rows = m_TrueVal.length; // Number of instances
		int nb_covered = 0; // Number of rule covered instances


		int nb_targets = tar_stat.getNbTargetAttributes();
		
		/** Number of values for each target.  For regression classes are not needed, thus value is 1.*/
		int[] nb_values = new int[nb_targets];
		

		for (int iTarget = 0; iTarget < nb_targets; iTarget++){
			if (m_ClssTask) {
				// Number of different values for the first attribute (assuming single target?). TODO
				nb_values[iTarget] = ((ClassificationStat)tar_stat).getAttribute(iTarget).getNbValues(); 
			} else {// regression
				nb_values[iTarget] = 1; // No classes are needed
			}
		}

		/** Prediction of the gene for the instances */
		double pred[][]=new double[nb_rows][nb_targets];

		// An index over the instances
		for (int iInstance = 0; iInstance < nb_rows; iInstance++) {

			// Sum of class weights from different individuals for the prediction
			// For regression nb_values = 1 always.
			double[][] pred_sum = new double[nb_targets][];

			for (int iTarget=0; iTarget < nb_targets;iTarget++) {
				if (m_ClssTask) {
					pred[iInstance][iTarget]= -1; // Initialize for invalid? class
				} else 
				{
					pred[iInstance][iTarget]= 0; // For regression initialize to zero
				}
				pred_sum[iTarget] = new double[nb_values[iTarget]];
			}

			boolean covered = false; // Is the instance covered

			// An index over the weights for the rules (variables to optimize) 
			for (int iRule = 0; iRule < m_NumVar; iRule++) {

				// An index over the targets of an instance (for multi targeted environments)
				for (int iTarget = 0; iTarget < nb_targets; iTarget++)
				{
					// An index over the possible values of nominal attribute. 1 for regression
					for (int iClass = 0; iClass < nb_values[iTarget]; iClass++) {
						if (m_RulePred[iInstance][iRule][iTarget][iClass] != Double.NaN) {
							covered = true;
							// For each nominal value and target, add variable
							// <current optimized parameter value>*<strenght of nominal value> OR
							// <current optimized parameter value>*<regression prediction for rule>
							// I.e. this is the real prediction function - weighted sum over the rules
							pred_sum[iTarget][iClass] += ((Double)genes.get(iRule)).doubleValue()
				            					* m_RulePred[iInstance][iRule][iTarget][iClass];			
//							pred_sum[iTarget].set(iClass,
//							pred_sum[iTarget].get(iClass) + genes.get(iRule)*
//							m_RulePred[iInstance][iRule][iTarget].get(iClass));


						}
					}
				}
			}
			
			// The prediction	
			if (m_ClssTask)
			{
				pred[iInstance] = predictClass(pred_sum);
			} else {
				// For regression, the prediction is the number we got
				pred[iInstance] = predictRegression(pred_sum);
			}
			
			if (covered) {
				nb_covered++; // One more instance covered
			}
		} // for over instances

	

		double loss = 0;
		// Loss function Loss(prediction, true value)
		// For classification the default is 0-1 loss
		if (m_ClssTask) {
			loss = loss01(m_TrueVal, pred);
			loss = (loss*nb_rows)/nb_covered; //We want to care for covered instances only.
		} else {	// For regression
			//loss = lossSquared(m_TrueVal, pred);
			loss = lossRRMSE(m_TrueVal, pred);
		}
		
		
		// Regularization for getting the weights as small as possible
		// Default is lasso regularity
		double reg_penalty = regulLasso(genes); 

		//fitness = (1 - (acc / nb_covered*nb_targets)) + getSettings().getOptRegPar() *  reg_penalty;
		// TODO: regularization penalty should include dispersion, coverage?

		
		return loss+getSettings().getOptRegPar()* reg_penalty;
		
	}
	
	

	
	/**
	 * Squared distance loss function for data set.
	 * TODO Should add the weights for the target attributes
	 * @param trueValue The true values of targets. [Instance][Target]
	 * @param prediction The prediction. [Instance][Target]
	 * @return Loss for the data.
	 */
	protected double lossSquared(double[][] trueValue, double[][] prediction){
		
		double loss = 0;
		int numberOfInstances = prediction.length;
		int numberOfTargets = prediction[0].length;
		
		for (int jTarget = 0; jTarget < numberOfTargets; jTarget++)
		{
			double attributeLoss = 0; // Loss for one attribute.
			for(int iInstance = 0; iInstance < numberOfInstances; iInstance++)
			{
			
				attributeLoss += Math.pow(trueValue[iInstance][jTarget]-prediction[iInstance][jTarget],2);
			}
			
			loss += 1/numberOfTargets*attributeLoss; // TODO: the weights for attributes are now equal
		}
		
		return loss/numberOfInstances; // Average loss over instances
	}
	
	/**
	 * Relative root mean squared error RRMSE loss function for data set.
	 * RRMSE is sum of squared errors divided by the variance.
	 * TODO Should add the weights for the target attributes
	 * @param trueValue The true values of targets. [Instance][Target]
	 * @param prediction The prediction. [Instance][Target]
	 * @return Loss for the data.
	 */
	//Suggested by Zenko 2007, p. 27
	protected double lossRRMSE(double[][] trueValue, double[][] prediction){
		
		double loss = 0;
		int numberOfInstances = prediction.length;
		int numberOfTargets = prediction[0].length;
		
		for (int jTarget = 0; jTarget < numberOfTargets; jTarget++)
		{
			double attributeLoss = 0;
			double attribVariance = 0; // Variance of one attribute for true values
			double attribMean = 0; // Mean for an attribute true values.
			
			// Compute mean of true values
			for(int iInstance = 0; iInstance < numberOfInstances; iInstance++)
			{
				attribMean += trueValue[iInstance][jTarget];
			}
			attribMean = attribMean/numberOfInstances;
			
			for(int iInstance = 0; iInstance < numberOfInstances; iInstance++)
			{
				attributeLoss += Math.pow(prediction[iInstance][jTarget]-trueValue[iInstance][jTarget],2);
				attribVariance += Math.pow(attribMean-trueValue[iInstance][jTarget],2);
			}

			// TODO: the weights for attributes are now equal
			loss += 1/numberOfTargets*Math.sqrt(attributeLoss/attribVariance); 
		}
		
		return loss/numberOfInstances; // Average loss over instances
	}
	
	/**
	 * 0-1 distance loss function for data set.
	 * Usually used only in classification.
	 * TODO Should add the weights for the target attributes.
	 * @param trueValue The true values of targets. [Instance][Target]
	 * @param prediction The prediction. [Instance][Target]
	 * @return Loss for the data.
	 */
	//Suggested by Zenko 2007, p. 26
	protected double loss01(double[][] trueValue, double[][] prediction)
	{
		int accuracy = 0;
		int numberOfInstances = prediction.length;
		int numberOfTargets = prediction[0].length;
		
		for (int jTarget = 0; jTarget < numberOfTargets; jTarget++){
			for(int iInstance = 0; iInstance < numberOfInstances; iInstance++)
			{

				if (trueValue[iInstance][jTarget] == prediction[iInstance][jTarget])
				{
					accuracy++;
				}	
			}
			//TODO add the weights for attributes
		}
		
		// For all the target attributes, the weight is now 1/numberOfTargets
		return 1 - ((double)accuracy) / (numberOfInstances*numberOfTargets);
	}

	
	/**
	 *  Regularization penalty for the optimization function.
	 *  This reduces the size of genes so that they do not grow too much.
	 *  In this case it is lasso penalty (sum of l1 norms of weights).
	 *  @param genes The genes, i.e. the weights.
	 *  @return Lasso penalty for the weights.
	 */
	private double regulLasso(ArrayList genes) {
		double reg_penalty = 0;
		
		for (int j = 0; j < genes.size(); j++) {
			// Lasso penalty, i.e. sum of absolute values of weights
			reg_penalty += Math.abs(((Double)(genes.get(j))).doubleValue());

			// Changed by Timo: This was originally without absolute values! Why? I assume
			// the weights are always positive.
			//par[j] = ((Double)(genes.get(j))).doubleValue();
			//reg_penalty += par[j];
		}
		
		return reg_penalty;
	}


	/**
	 * Regression prediction.
	 * @param predictionSums The weighted sum of rules for all targets. 
	 * 						 The second dimension not used i.e. [Target][1].
	 * @param nbOfTargets Number of targets
	 * @param nbOfValues Number of class values
	 * @return Array of prediction [target]
	 */
	private double[] predictRegression(double[][] predictionSums) 
	{
		int nbOfTargets = predictionSums.length;
		double[] prediction = new double[nbOfTargets];
		for (int iTarget = 0; iTarget < nbOfTargets; iTarget++)
		{
			// For regression there is only one class and the prediction is the given value.
			prediction[iTarget] = predictionSums[iTarget][0];
		}
		
		return prediction;
	}


	/**
	 * Classification prediction.
	 * @param predictionSums The weighted sum of rules for different classes. [Target][Class value]
	 * @param nbOfTargets Number of targets
	 * @param nbOfValues Number of class values
	 * @return Array of prediction [target]
	 */
	protected double[] predictClass(double[][] predictionSums) {
		int nbOfTargets = predictionSums.length;
		double[] prediction = new double[nbOfTargets];
				
		for (int iTarget = 0; iTarget < nbOfTargets; iTarget++)
		{
			double max = 0; // Search for maximum
			
			// Which one of the classes has the highest voting, i.e. maximum value
			for (int iClass = 0; iClass < predictionSums[iTarget].length; iClass++) {
			
				if (predictionSums[iTarget][iClass] > max) 
				{
					prediction[iTarget] = (double)iClass;   
					max = predictionSums[iTarget][iClass];
				}
			}
		}	
		return prediction;
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
