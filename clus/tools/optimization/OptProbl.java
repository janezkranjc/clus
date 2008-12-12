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
package clus.tools.optimization;

import java.util.*;

import clus.main.*;
import clus.data.type.*;
import clus.statistic.*;

// Created 28.11.2008 from previous DeProbl class

/**
 * Class representing a optimization problem. 
 * Can be used e.g. in Differential evolution and Gradient descent optimization
 * Includes loss functions and regularization
 * @author Timo Aho
 * @author Tea Tusar
 */
public class OptProbl {

	/** A regression prediction for initialization.
	 * It is negative so that this class is not selected in classification.
	 * Because default rule is used, this value should never go to loss function.
	 */
	static protected final double INVALID_PREDICTION = Double.NEGATIVE_INFINITY; 
		
	/**
	 * Parameters for optimization algorithm. 
	 * Predictions and true values for all the instances of data.
	 * The default prediction is used, if no other rule covers the instance.
	 * See m_RulePred and m_TrueVal for what the indices mean.
	 */
	static public class OptParam {
		public OptParam(double[][][][] predictions,
//				double[] defaultPrediction,
				double[][] trueValues ){
			m_predictions = predictions;
//			m_defaultPrediction = defaultPrediction;
			m_trueValues = trueValues;
		}
		/** An empty class */ 
		public OptParam(int nbRule, int nbInst, int nbTarg){
			m_predictions = new double[nbRule][nbInst][nbTarg][1];
//			m_defaultPrediction = new double[nbTarg];
			m_trueValues = new double[nbInst][nbTarg];
		}
		public double[][][][] m_predictions;
//		double[] m_defaultPrediction;
		public double[][] m_trueValues;
	}
	
	/** Number of weights/variables to optimize */
	private int m_NumVar;				
		
	/** 
	 * Rule predictions [rule index][instance][target index][class_value] is for nominal attributes
	 * or [rule index][instance][target index][0] for regression.
	 * The [target index] is always [0] for single target use.
	 * The class value dimension is dynamic for each target index.  
	 */
	private double[][][][] m_RulePred;
	
	/**
	 * Default prediction if no rule covers the instance.
	 */
//	private double[] m_defaultPred;
	
	/** True target values for the data points. [instance][target index] */
	private double[][] m_TrueVal;			
	private ClusStatManager m_StatMgr;
	private boolean m_ClssTask;

	/**
	 * Constructor for problem to be solved with differential evolution. Both classification and regression.
	 * @param stat_mgr Statistics
	 * @param dataInformation The true values and predictions for the instances. These are used by OptimProbl.
	 *                        The optimization procedure is based on this data information
	 * @param isClassification Is it classification or regression?
	 */
	public OptProbl(ClusStatManager stat_mgr, OptParam optInfo) {
		m_NumVar = (optInfo.m_predictions).length;
		m_RulePred = optInfo.m_predictions;
		m_TrueVal = optInfo.m_trueValues;
//		m_defaultPred = optInfo.m_defaultPrediction;
		m_StatMgr = stat_mgr;
		
//		try {
			if (m_StatMgr.getMode() != ClusStatManager.MODE_REGRESSION &&  
					m_StatMgr.getMode() != ClusStatManager.MODE_CLASSIFY)
			{
System.err.println("Weight optimization: Mixed types of targets (reg/clas) not implemented. Assuming regression.\n ");
//				"The targets are of different kind, i.e. they are not all for regression or for classifying.\n" +
//				"Mixed targets are not yet implemented. The targets are considered as regression.\n" +
//				"This error message may be due the clustering variables also.\n" +
//				"The optimization may not work in this case also.\n");
			}
		//} catch (Exception e){
//			e.printStackTrace();
//		}
			
		m_ClssTask = (m_StatMgr.getMode() == ClusStatManager.MODE_CLASSIFY);
	}

	/**
	 * Fitness function.
	 * The classification prediction is voting without weights.
	 * @param genes The current generation (population).
	 * @param dataEarlyStop 
	 * @return fitness score
	 */
	public double calcFitness(ArrayList<Double> genes) {
		// Only the target attributes are returned?
		ClusStatistic tar_stat = getTargetStat();

		// TODO mixture of nominal and numeric attributes. For multi target case some of the targets may be nominal, some numerical
		int nb_rows = getNbOfInstances(); // Number of instances
		int nb_covered = 0; // Number of rule covered instances


		int nb_targets = tar_stat.getNbAttributes();
		
		/** Number of values for each target.  For regression classes are not needed, thus value is 1.*/
		int[] nb_values = new int[nb_targets];
		

		for (int iTarget = 0; iTarget < nb_targets; iTarget++){
			if (isClassifTask()) {
				// Number of different values for the attribute
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
				pred_sum[iTarget] = new double[nb_values[iTarget]];
				
				if (isClassifTask()) {
					//pred[iInstance][iTarget]= -1; // Initialize for invalid? class
					pred[iInstance][iTarget]= INVALID_PREDICTION; // Initialize for invalid? class
					for (int iValue = 0; iValue < nb_values[iTarget]; iValue++) {
						pred_sum[iTarget][iValue] = INVALID_PREDICTION;
					}
				} else 
				{
					// For regression initialize to zero.
					pred[iInstance][iTarget]= 0;
					pred_sum[iTarget][0] = 0;
				}
			}

			boolean covered = false; // Is the instance covered

			// An index over the weights for the rules (variables to optimize) 
			for (int iRule = 0; iRule < getNumVar(); iRule++) {

				// An index over the targets of an instance (for multi targeted environments)
				for (int iTarget = 0; iTarget < nb_targets; iTarget++)
				{
					// An index over the possible values of nominal attribute. 1 for regression
					for (int iClass = 0; iClass < nb_values[iTarget]; iClass++) {
					//	if (m_RulePred[iInstance][iRule][iTarget][iClass] != Double.N NaN) {
						if (!Double.isNaN(getPredictions(iRule,iInstance,iTarget,iClass)) ) {
							covered = true;
							
							// To create lots of loss, undefined predictions have special value
							// Initially pred_sum = INVALID_PREDICTION. For the first time nonzero
							// prediction comes, we put this to zero.
							if (pred_sum[iTarget][iClass] == INVALID_PREDICTION &&
								((Double)genes.get(iRule)).doubleValue() != 0) {
								pred_sum[iTarget][iClass] = 0;
							}
							// For each nominal value and target, add variable
							// <current optimized parameter value>*<strenght of nominal value> OR
							// <current optimized parameter value>*<regression prediction for rule>
							// I.e. this is the real prediction function - weighted sum over the rules
							pred_sum[iTarget][iClass] += ((Double)genes.get(iRule)).doubleValue()
				            					* getPredictions(iRule,iInstance,iTarget,iClass);			
						}
					}
				}
			}
			
			// The prediction	
			if (isClassifTask())
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
		if (isClassifTask()) {
			// Only one sensible loss type available for classification and it is not the default.
			if (getSettings().getOptDELossFunction() != Settings.DE_LOSS_FUNCTIONS_01ERROR)
			{
				try{
					throw new Exception("DE optimization task is for classification, but the chosen loss " +
					"is mainly for regression. Use OptDELossFunction = 01Error to correct this.");
				} catch (Exception e){
					e.printStackTrace();
				}
				loss = loss(pred);
			} else {
				//We want to care for covered instances only.
				loss = (loss(pred)*nb_rows)/nb_covered;
			}
		} else {	// For regression
			loss = loss(pred);
		}
		
		
		// Regularization for getting the weights as small as possible
		double reg_penalty = regularization(genes); 

		
		// Second Regularization (especially for DE): how many zeroes
		int nbOfZeroes = returnNbOfZeroes(genes);
		
		//fitness = (1 - (acc / nb_covered*nb_targets)) + getSettings().getOptRegPar() *  reg_penalty;
		// TODO: regularization penalty should include dispersion, coverage?

		
		return loss+getSettings().getOptRegPar()* reg_penalty + getSettings().getOptNbZeroesPar()* nbOfZeroes;
		
	}
	
	/** Number of zeroes for regularization purposes */
	private int returnNbOfZeroes(ArrayList<Double> genes) {
		
		int nbOfZeroes = 0;

		for (int j = 0; j < genes.size(); j++) {
			if (genes.get(j).doubleValue() == 0.0)
				nbOfZeroes++;
		}

		
		return nbOfZeroes;
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
			
//			if (predictionSums[iTarget][0] == INVALID_PREDICTION) {
//				// No prediction is given (none of the nonzero weight rules cover the instance)
//				// Use the default rule instead
//				prediction[iTarget] = getDefaultPrediction(iTarget);
//				
//			} else {
				// For regression there is only one class and the prediction is the given value.
				prediction[iTarget] = predictionSums[iTarget][0];
//			}
			
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
			double max = 0; // Maximum value so far
			int iMaxClass = 0; // The class with maximum value so far.  
			
			// Which one of the classes has the highest voting, i.e. maximum value
			for (int iClass = 0; iClass < predictionSums[iTarget].length; iClass++) {	
				if (predictionSums[iTarget][iClass] > max) 
				{
					//prediction[iTarget] = (double)iClass;
					iMaxClass = iClass;
					max = predictionSums[iTarget][iClass];
				}
			}
				
//			if (predictionSums[iTarget][iMaxClass] == INVALID_PREDICTION) {
//				// We should come here only if for all the classes have INVALID_PREDICTION
//				// (This is because maximum value is -infinity = INVALID_PREDICTION)
//				// Thus no prediction is given (none of the nonzero weight rules cover the instance)
//				// Use the default rule instead
//				prediction[iTarget] = getDefaultPrediction(iTarget);
//			} else {
			
			// The default rule is included in the rule set. At least it always covers all the examples.
			prediction[iTarget] = (double)iMaxClass;
//			}
		}	
		return prediction;
	}
	
	
	
	
	
	// int REGUL_GENERAL = // Give the regularization function as function object
	// int LOSS_GENERAL = // Give the loss function as function object  
	
	/**
	 * Loss function for data set. Chooses the right loss function
	 * based on the settings file.
	 * @param trueValue The true values of targets. [Instance][Target]
	 * @param prediction The prediction. [Instance][Target]
	 * @return Loss for the data.
	 */
	protected double loss(double[][] prediction){
		
		double loss = 0;
		switch (getSettings().getOptDELossFunction()) {

		case Settings.DE_LOSS_FUNCTIONS_01ERROR:
			loss = loss01(getTrueValues(), prediction);
			break;
		case Settings.DE_LOSS_FUNCTIONS_RRMSE:
			loss = lossRRMSE(getTrueValues(), prediction);
			break;
		case Settings.DE_LOSS_FUNCTIONS_HUBER:
			loss = lossHuber(getTrueValues(), prediction);
			break;
			//Default case
		case Settings.DE_LOSS_FUNCTIONS_SQUARED:
		default:
			loss = lossSquared(getTrueValues(), prediction);
			break;
		}	
		
		return loss;
	}
	
	/**
	 * Squared distance loss function for data set.
	 * Is ok, because the data is normalized.
	 * @param trueValue The true values of targets. [Instance][Target]
	 * @param prediction The prediction. [Instance][Target]
	 * @return Loss for the data.
	 */
	private double lossSquared(double[][] trueValue, double[][] prediction){
		
		double loss = 0;
		int numberOfInstances = prediction.length;
		int numberOfTargets = prediction[0].length;
		
		for (int jTarget = 0; jTarget < numberOfTargets; jTarget++)
		{
			double attributeLoss = 0; // Loss for one attribute.
			for(int iInstance = 0; iInstance < numberOfInstances; iInstance++)
			{
				// Missing values? if (!Double.isNaN(trueValue[iInstance][jTarget])) {
				attributeLoss += Math.pow(trueValue[iInstance][jTarget]-prediction[iInstance][jTarget],2);
			}
			
			loss += ((double)1)/numberOfTargets*attributeLoss; // TODO: the weights for attributes are now equal
		}
		
		return loss/numberOfInstances; // Average loss over instances
	}
	
	/**
	 * Relative root mean squared error RRMSE loss function for data set.
	 * RRMSE is sum of squared errors divided by the variance.
	 * Relativeness is not useful, because the data is normalized.
	 * @param trueValue The true values of targets. [Instance][Target]
	 * @param prediction The prediction. [Instance][Target]
	 * @return Loss for the data.
	 */
	//Suggested by Zenko 2007, p. 27
	private double lossRRMSE(double[][] trueValue, double[][] prediction){
		
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
			loss += ((double)1)/numberOfTargets*Math.sqrt(attributeLoss/attribVariance); 
		}
		
		return loss/numberOfInstances; // Average loss over instances
	}
	
	/**
	 * Huber 1962 loss function for data set.
	 * This is mainly the squared distance error. However for great distances it is smoothed.
	 * Thus it is robust to outliers.
	 * @param trueValue The true values of targets. [Instance][Target]
	 * @param prediction The prediction. [Instance][Target]
	 * @return Loss for the data.
	 */
	//Suggested by Friedman&Popescu 2007, p. 7
	private double lossHuber(double[][] trueValue, double[][] prediction){
		
		double loss = 0;
		int numberOfInstances = prediction.length;
		
		// If no instances given, jump out
		if (numberOfInstances == 0)
			return 0;
		
		int numberOfTargets = prediction[0].length;

		/** For Huber 1962 loss function we need the the delta values for each target.
		 * Delta value depends on the alpha quantiles of the data.
		 */
		double	deltas[] = computeHuberDeltas(trueValue, prediction);
		
		for (int jTarget = 0; jTarget < numberOfTargets; jTarget++)
		{
			double attributeLoss = 0; // Loss for one attribute.
			for(int iInstance = 0; iInstance < numberOfInstances; iInstance++)
			{
				if (Math.abs(trueValue[iInstance][jTarget]-prediction[iInstance][jTarget]) < deltas[jTarget]) {
					attributeLoss += Math.pow(trueValue[iInstance][jTarget]-prediction[iInstance][jTarget],2);
				} else { // Smoothed for distant objects
					attributeLoss += deltas[jTarget]*(Math.abs(trueValue[iInstance][jTarget]-prediction[iInstance][jTarget])
											-deltas[jTarget]/2);
				}
			}

			loss += ((double)1)/numberOfTargets*attributeLoss; // TODO: the weights for attributes are now equal
		}	

		return loss/numberOfInstances; // Average loss over instances

	}
	
	
	/**
	 * For Huber 1962 loss function we need the the delta values for each target.
	 * Delta value depends on the alpha quantiles of the data
	 * 
	 */
	private double[] computeHuberDeltas(double[][] trueValues,
			double[][] predictions) {
		
		int numberOfInstances = trueValues.length;
		int numberOfTargets = trueValues[0].length;
		
		// Alpha quantile for how much of data is considered potential outliers
		double alpha = getSettings().getOptDEHuberAlpha();
		double deltas[] = new double[numberOfTargets];
		
		double targetDistances[] = new double[numberOfInstances]; 
		
		for (int jTarget = 0; jTarget < numberOfTargets; jTarget++){
			for(int iInstance = 0; iInstance < numberOfInstances; iInstance++)
			{
				targetDistances[iInstance] = Math.abs(trueValues[iInstance][jTarget]
				                                    - predictions[iInstance][jTarget]);
			}
			// Sort in ascending order
			Arrays.sort(targetDistances); 
 
			// Find the value for which not more than alpha amount of doubles are less
			deltas[jTarget]= targetDistances[(int)Math.floor(numberOfInstances*alpha)];
			
		}
		return deltas;
	}
	
	/**
	 * 0-1 distance loss function for data set.
	 * Usually used only in classification.
	 * @param trueValue The true values of targets. [Instance][Target]
	 * @param prediction The prediction. [Instance][Target]
	 * @return Loss for the data.
	 */
	// TODO Squared error ramp loss could also be used
	//Suggested by Zenko 2007, p. 26
	private double loss01(double[][] trueValue, double[][] prediction)
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
	 *  The power for the differences can be changed in settings file.
	 *  The default is lasso, power = 1. This keeps the weights zero if possible.
	 *  @param genes The genes, i.e. the weights.
	 *  @return Penalty for the weights.
	 */
	protected double regularization(ArrayList<Double> genes) {
		double reg_penalty = 0;
		
		for (int j = 0; j < genes.size(); j++) {
			// Lasso penalty, i.e. sum of absolute values of weights
			reg_penalty += Math.pow(Math.abs( ((Double)(genes.get(j))).doubleValue() ),
									getSettings().getOptDERegulPower());
		}
		
		return reg_penalty;
	}
	
	/** Number of variables to be optimized */
	public int getNumVar() {
		return m_NumVar;
	}

	protected Settings getSettings() {
		return m_StatMgr.getSettings();
	}

	protected ClusStatistic getTargetStat() {
		return m_StatMgr.getStatistic(ClusAttrType.ATTR_USE_TARGET);
	}
	
	/** Value of rule prediction. Can be used also for nominal attributes.
	 * Note that this does not use the include of default rule!
	 * So this is not the real prediction but can be NaN.
	 */
	protected double getPredictions( int iRule, int iInstance, int iTarget, int iClass) {
		return m_RulePred[iRule][iInstance][iTarget][iClass];
	}
	
	/** Returns the prediction of the rule in regression use.
	 * Note that this does not use the include of default rule!
	 * So this is not the real prediction but can be NaN.
	 */
	protected double getPredictions(int iRule, int iInstance, int iTarget) {
		return m_RulePred[iRule][iInstance][iTarget][0];
	}
	
	/** Returns all targets
	 * 
	 */
//	protected double[][] getPredictions(int iRule, int iInstance) {
//		return m_RulePred[iRule][iInstance];
//	}
	
	/** Returns all predictions
	 * 
	 */
//	protected double[][][] getPredictions(int iRule) {
//		return m_RulePred[iRule];
//	}
	
	
//	protected double getDefaultPrediction(int iTarget) {
//		return m_defaultPred[iTarget];
//	}
	
	protected double getTrueValue(int iInstance, int iTarget)  {
		return m_TrueVal[iInstance][iTarget];			
	}
	
	/**
	 * Returns all the targets
	 * @param iInstance
	 * @return
	 */
//	protected double[] getTrueValues(int iInstance)  {
//		return m_TrueVal[iInstance];			
//	} TODO remove


	/**
	 * The loss function should be separate in the sense
	 * that it does not use member functions.
	 * @return
	 */
	private double[][] getTrueValues() {
		return m_TrueVal;			
	}
	
	protected boolean isClassifTask() {
		return m_ClssTask;
	}
	
	protected int getNbOfInstances() {
		return m_TrueVal.length;
	}
	
	protected int getNbOfTargets() {
		return getTargetStat().getNbAttributes();
	}

	/** Change the data used for learning. This is used if part of the data
	 * is used for e.g. testing. */
	protected void changeData(OptParam newData) {
		m_RulePred = newData.m_predictions;
//		m_defaultPred = newData.m_defaultPrediction;
		m_TrueVal = newData.m_trueValues;			 		
	}	
}
