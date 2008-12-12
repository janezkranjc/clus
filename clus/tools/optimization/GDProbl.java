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

// Created 28.11.2008 from previous DeProbl class

/**
 * Class representing a gradient descent optimization problem. 
 * This gives tool functions for the actual optimization algorithm.
 * @author Timo Aho
 */
public class GDProbl extends OptProbl {


	/**
	 * Covariances between weights. Are computed only if needed.
	 */
	protected double[][] m_covariances; 
	
	/**
	 *  Is covariance computed for this dimension?
	 *  To reduce computational costs, covariances for certain dimension are computed only if needed
	 *  Weight is always zero if the corresponding coefficient is zero.
	 */
	protected boolean[] m_isCovComputed;
	
	/**
	 * Number of nonzero rules (for which covariance is computed)
	 */
	protected int m_nbOfNonZeroRules;
	
	/** Computed negative gradients for each of the dimensions */
	protected double[] m_gradients;
	
	/** Step size for gradient descent */
	protected double m_stepSize;
	
	/** Separate test set for early stopping */
	protected OptParam m_dataEarlyStop;
	
	/** New problem for computing fitness function with the early stop data */
	protected OptProbl m_earlyStopProbl;
	
	/**
	 * Constructor for problem to be solved with differential evolution. Both classification and regression.
	 * @param stat_mgr Statistics
	 * @param dataInformation The true values and predictions for the instances. These are used by OptimProbl.
	 *                        The optimization procedure is based on this data information
	 * @param isClassification Is it classification or regression?
	 */
	public GDProbl(ClusStatManager stat_mgr, OptParam optInfo) {
		super(stat_mgr, optInfo);
		// If early stopping criteria is chosen, reserve part of the training set for early stop testing.
		if (getSettings().getOptGDEarlyStopAmount() > 0) {
			double amountData = getSettings().getOptGDEarlyStopAmount();
			m_dataEarlyStop = new OptParam(optInfo.m_predictions.length,
					(int)Math.ceil(amountData*optInfo.m_predictions[0].length), 
					optInfo.m_predictions[0][0].length);
			OptParam rest = new OptParam(optInfo.m_predictions.length,
					(int)Math.floor((1-amountData)*optInfo.m_predictions[0].length), 
					optInfo.m_predictions[0][0].length);
			for (int iRule = 0; iRule < optInfo.m_predictions.length;iRule++){
				for (int iInstance = 0; iInstance < optInfo.m_predictions[0].length; iInstance++){

//					if (iInstance < (int)Math.ceil(amountData*optInfo.m_predictions[0].length)) {
//						m_dataEarlyStop.m_predictions[iRule][iInstance] = 
//							optInfo.m_predictions[iRule][iInstance];
//						if (iRule == 0) // Only once for each rule
//							m_dataEarlyStop.m_trueValues[iInstance] =
//								optInfo.m_trueValues[iInstance];
//					} else {
//						rest.m_predictions[iRule][iInstance] = 
//							optInfo.m_predictions[iRule][iInstance];
//						if (iRule == 0) // Only once for each rule
//							rest.m_trueValues[iInstance] =
//								optInfo.m_trueValues[iInstance];							
//					}
//	
//					
					
					for (int iTarget = 0; iTarget < optInfo.m_predictions[0][0].length; iTarget++){

						if (iInstance < (int)Math.ceil(amountData*optInfo.m_predictions[0].length)) {
							m_dataEarlyStop.m_predictions[iRule][iInstance][iTarget][0] = 
								optInfo.m_predictions[iRule][iInstance][iTarget][0];
							if (iRule == 0) // Only once for each rule
								m_dataEarlyStop.m_trueValues[iInstance][iTarget] =
									optInfo.m_trueValues[iInstance][iTarget];
						} else {
							int changeInstance = iInstance - (int)Math.ceil(amountData*optInfo.m_predictions[0].length);
							rest.m_predictions[iRule][changeInstance][iTarget][0] = 
								optInfo.m_predictions[iRule][changeInstance][iTarget][0];
							if (iRule == 0) // Only once for each rule
								rest.m_trueValues[changeInstance][iTarget] =
									optInfo.m_trueValues[changeInstance][iTarget];							
						}
					}
				}
			}
					
					
		
//			for (int iTarget = 0; iTarget < optInfo.m_predictions[0][0].length; iTarget++){
//				m_dataEarlyStop.m_defaultPrediction[iTarget] = 
//					optInfo.m_defaultPrediction[iTarget];
//				rest.m_defaultPrediction[iTarget] = 
//					optInfo.m_defaultPrediction[iTarget];
//			}

			changeData(rest);  // Change data for super class
			m_earlyStopProbl = new OptProbl(stat_mgr, m_dataEarlyStop);
			// We are using Fitness function of  the problem. Let us put the reg penalty to 0 because we do not
			// want to use it
			getSettings().setOptRegPar(0);
			getSettings().setOptNbZeroesPar(0);
			
			m_oldFitnesses = new ArrayList<Double>();
			

		}

		int nbWeights = getNumVar();
		m_covariances = new double[nbWeights][nbWeights];
		m_stepSize = getSettings().getOptGDStepSize();

		for (int i = 0; i < nbWeights; i++) {
			for (int j = 0; j < nbWeights; j++) {
				//				if (i == j) 				// This is for Noose, is it also for this?
				//					m_covariances[i][j] = 1/(m_GDProbl.getNbOfInstances()*m_GDProbl.getNbOfTargets());
				//				else
				m_covariances[i][j] = Double.NaN;

			}   }

		m_gradients = new double[nbWeights];  // Initial value is zero
		m_isCovComputed = new boolean[nbWeights]; // Initial value is false
		m_nbOfNonZeroRules = 0;
	}


	/** Estimate of expected value of covariance for given prediction.
	 * The covariance of this prediction with its true value is returned.
	 * We just take the average over the target covariances because that is what we are
	 * minimizing in the loss functions also.
	 */
	protected double getCovForPrediction(int iPred) {

		double[] covs = new double[getNbOfTargets()];
		double sumOfCovs = 0;
		
		for (int iTarget = 0; iTarget < getNbOfTargets(); iTarget++) {
			for (int iInstance = 0; iInstance < getNbOfInstances(); iInstance++) {
				double trueVal = getTrueValue(iInstance,iTarget);
				if (isValidValue(trueVal)) // Not a valid true value, rare but happens
					covs[iTarget] += trueVal*predictWithRule(iPred, iInstance)[iTarget];
			}

			covs[iTarget] /= getNbOfInstances();
			sumOfCovs += covs[iTarget];
		}
		
		return sumOfCovs/getNbOfTargets();
	}
	
	private boolean isValidValue(double pred) {
		return !Double.isInfinite(pred) && !Double.isNaN(pred);
	}


	/**
	 * Return the right stored covariance value.
	 */
	// Only one corner is computed (the other is similar)
	protected double getWeightCov(int iFirst, int iSecond) {
		if (Double.isNaN(m_covariances[Math.min(iFirst, iSecond)][Math.max(iFirst, iSecond)]))
			throw new Error("Asked covariance not yet computed. Something wrong in the covariances in GDProbl.");
		return m_covariances[Math.min(iFirst, iSecond)][Math.max(iFirst, iSecond)];
	}
	
	/** Compute the covariances for this dimension. Because for covariance
	 * cov(a,b) = cov(b,a) compute only one of them.
	 * @par dimension The dimension for which the covariances are computed
	 */
	protected void computeWeightCov(int dimension) {
		// Because of symmetry cov(dimension, b) is already computed if for some earlier phase b was dimension
		// Thus if covariances for b are computed, this does not have to be computed anymore.
		
		// The weights with index lower than this
		for (int iMin = 0; iMin < dimension; iMin++) {
			if (!m_isCovComputed[iMin]) { // Is this covariance computed already
//				if (!Double.isNaN(m_covariances[iMin][dimension]))
//					System.err.println("WARNING: Covariances are recalculated, waste of computation!");
				m_covariances[iMin][dimension] = computeCovFor2Preds(iMin, dimension);
			}
		}

		m_covariances[dimension][dimension] = computeCovFor2Preds(dimension, dimension);
		for (int iMax = dimension+1; iMax < getNumVar(); iMax++) {
			if (!m_isCovComputed[iMax]) {
//				if (!Double.isNaN(m_covariances[iMax][dimension]))
//					System.err.println("WARNING: Covariances are recalculated, waste of computation!");
				m_covariances[dimension][iMax] = computeCovFor2Preds(dimension, iMax);
			}
		}		
	}
	
	/**
	 * Compute covariance between two predictions (base learner predictions).
	 * This is a help function for computeWeightCov
	 * @param iFirst Base learner index
	 * @param iSecond Base learner index.
	 * @return
	 */
	private double computeCovFor2Preds(int iFirstRule, int iSecondRule) {
		double[] covs = new double[getNbOfTargets()];
		double sumOfCovs = 0;
		
		for (int iTarget = 0; iTarget < getNbOfTargets(); iTarget++) {
			for (int iInstance = 0; iInstance < getNbOfInstances(); iInstance++) {
				covs[iTarget] += predictWithRule(iFirstRule,iInstance)[iTarget] * 
				                 predictWithRule(iSecondRule, iInstance)[iTarget];
			}
			covs[iTarget] /= getNbOfInstances();
			sumOfCovs += covs[iTarget];
		}
		
		return sumOfCovs/getNbOfTargets();
	}
	
	
	/**
	 * Generates a zero vector. 
	 */
	protected ArrayList<Double> getZeroVector()
	{
		ArrayList<Double> result = new ArrayList<Double>(getNumVar());
		for (int i = 0; i < getNumVar(); i++) {
			result.add(new Double(0.0));
		}
		return result;
	}
	
	
	
	/** Returns the real prediction when this rule is used. If the rule does
	 * not give prediction for some target, default rule is used.
	 * ONLY FOR REGRESSION! Classification not implemented.
	 */
	protected double[] predictWithRule(int iRule, int iInstance) {
		double[] prediction = new double[getNbOfTargets()];
		
		for (int iTarget= 0; iTarget < getNbOfTargets(); iTarget++) {
			if (!isValidValue(getPredictions(iRule,iInstance,iTarget)))
				// If the instance is not covered, zero is the prediction. TODO: change for classification
				prediction[iTarget] = 0; // getDefaultPrediction(iTarget);
			else
				prediction[iTarget] = getPredictions(iRule,iInstance,iTarget);
		}
		return prediction;
	}

	/** Compute the gradients for weights */
	public void initGradients(ArrayList<Double> m_weights) {
		// Compute all the gradients for current weights.
		for (int iWeight = 0; iWeight < m_weights.size(); iWeight++ ) {
			m_gradients[iWeight] = getGradient(iWeight, m_weights);
		}
		
	}


	/** Compute gradient for the given weight dimension */
	protected double getGradient(int iWeightDim, ArrayList<Double> weights) {
		
		double gradient = 0;
		switch (getSettings().getOptGDLossFunction()) { 	
		case Settings.DE_LOSS_FUNCTIONS_01ERROR:
			//gradient = loss01(trueValue, prediction);
			//break;
		case Settings.DE_LOSS_FUNCTIONS_HUBER:	
			//gradient = lossHuber(trueValue, prediction);
//			break;
			try {
				throw new Exception("0/1 or Huber loss function not yet implemented for Gradient descent.\n" +
						"Using squared loss.\n");
			} catch(Exception s) {
				s.printStackTrace();
			} //TODO Huber and alpha computing
			//Default case
		case Settings.DE_LOSS_FUNCTIONS_SQUARED:
		default:
			gradient = gradientSquared(iWeightDim, weights);
			break;
		}	
		
		return gradient;
	}
	
	/**
	 * Squared loss gradient. p. 18 in Friedman & Popescu, 2004
	 * @param iGradWeightDim Weight dimension for which the gradient is computed. 
	 * @return
	 */
	private double gradientSquared(int iGradWeightDim, ArrayList<Double> weights) {
		double gradient = 0;

		gradient =  getCovForPrediction(iGradWeightDim);
		
		for (int iWeight = 0; iWeight < getNumVar(); iWeight++){
			if (weights.get(iWeight).doubleValue() != 0) // Covariance not computed.
				gradient -= weights.get(iWeight).doubleValue()* getWeightCov(iWeight,iGradWeightDim);				
		}
		
		return gradient;
	}
	

	/** Recompute the gradients new iteration.
	 * This is lot of faster than computing everything from the scratch.
	 * @param changedWeightIndex The index of weights that have changed. Only these affect the change in the new gradient.
	 * 						  Friedman&Popescu p.18 
	 */
	protected void modifyGradients(int[] changedWeightIndex) {

		switch (getSettings().getOptGDLossFunction()) { 	
		case Settings.DE_LOSS_FUNCTIONS_01ERROR:
		case Settings.DE_LOSS_FUNCTIONS_HUBER:
			//TODO Huber and alpha computing
			//Default case
		case Settings.DE_LOSS_FUNCTIONS_SQUARED:
		default:
			modifyGradientSquared(changedWeightIndex);
			break;
		}	
	}
	

	/** Recomputation of gradients for least squares loss function */
	public void modifyGradientSquared(int[] changedWeightIndex) {
				
		// New gradients are computed with the old gradients.
		// Only the changed gradients are stored here
		double[] oldGradsOfChanged = new double[changedWeightIndex.length];
		
		for (int iCopy = 0; iCopy < changedWeightIndex.length; iCopy++) {
			oldGradsOfChanged[iCopy] = m_gradients[changedWeightIndex[iCopy]];
		}
		
		// Index over the gradient we are changing (ALL GRADIENTS)
		for (int iTarget = 0; iTarget < m_gradients.length; iTarget++) {
			// Index over the other gradients that are affecting (THE WEIGHTS THAT ALTERED)
			for (int iiAffecting = 0; iiAffecting < changedWeightIndex.length; iiAffecting++) {

				//The stepsize * old gradients is equal to the change!
				m_gradients[iTarget] -= m_stepSize* oldGradsOfChanged[iiAffecting]*
										getWeightCov(changedWeightIndex[iiAffecting],iTarget);					
			}
		}
	}

	
//	public void recomputeGradients(int iTargetWeight, int[] changedWeights) {
//		for (int iIndex = 0; iIndex < changedWeights.length; iIndex++) {
//			//Gradients are changed more if they are related (correlated) to the peak dimension
//			m_gradients[iTargetWeight] -= m_stepSize* m_gradients[changedWeights[iIndex]]*
//				getWeightCov(changedWeights[iIndex],iTargetWeight);					
//		}
//
	
	/** Return the gradients with maximum absolute value. For the weights we want to change */
	public int[] getMaxGradients() {
		/** Maximum number of nonzero elements. Can we add more? */
		int maxElements = getSettings().getOptGDMaxNbWeights();
		
		/** Sorted array of indexes */
		int[] iSorted;
		
		if (maxElements > 0 && m_nbOfNonZeroRules >= maxElements) {
			// If maximum number of nonzero elements is reached, 
			// search for the biggest one among the nonzero weights

			iSorted = IndexMergeSorter.sortSubArray(m_gradients, m_isCovComputed, m_nbOfNonZeroRules, true); 
			/**
			 * We need to store the maxGradient because we do not know if 0 or any other weight
			 * is nonzero (if it is allowed to use)
			 */
//			double maxGradient = 0;
//			for (int iWeight = 0; iWeight < getNumVar(); iWeight++)
//			{
//				if (m_isCovComputed[iWeight]){ // weight is nonzero
//					if (maxGradient < Math.abs(m_gradients[iWeight])) {
//						iMaxGrad = iWeight;
//						maxGradient = Math.abs(m_gradients[iWeight]);
//					}
//				}
//			}
		} else { 
			// All the weights are used
			iSorted = IndexMergeSorter.sort(m_gradients, true);
			
//			for (int iWeight = 0; iWeight < getNumVar(); iWeight++) {
//				if (Math.abs(m_gradients[iMaxGrad]) < Math.abs(m_gradients[iWeight])) 
//					iMaxGrad = iWeight;
//			}
		}
		
		// iSorted is sorted in descending order
		int iiLastIndex = iSorted.length-1; 
		// The least allowed item.
		double minAllowed = Math.abs(getSettings().getOptGDGradTreshold() * m_gradients[iSorted[iiLastIndex]]);
		
		int iiLastAllowed = 0;
		// binarySearch does not work because the array is not sorted.
		for (int iiSearch = iiLastIndex; iiSearch >=0 ; iiSearch--) {
			// TODO: Just check if the sorting works
			if (iiSearch > 0 && (Math.abs(m_gradients[iSorted[iiSearch]]) < Math.abs(m_gradients[iSorted[iiSearch-1]])
					|| iSorted[iiSearch] == iSorted[iiSearch-1])) {
				System.err.println("ERROR: IndexMergeSorter does not work.");
			}
				
			if (Math.abs(m_gradients[iSorted[iiSearch]]) < minAllowed) {
				iiLastAllowed = iiSearch +1; // Last allowed item. We are going the reverse direction.
				break;
			}
		}
		
		int[] maxGradients = new int[iiLastIndex-iiLastAllowed+1];
		
		// Copy and reverse at the same time
		for (int iCopy = 0; iCopy < maxGradients.length; iCopy++) {
			maxGradients[iCopy] = iSorted[iiLastIndex-iCopy];
		}
//		maxGradients[0] = iMaxGrad;
		return maxGradients;
	}


	/**
	 * Compute the change of target weight because of the gradient
	 * @param iTargetWeight Weight index we want to change. 
	 */
	public double howMuchWeightChanges(int iTargetWeight) {
		return m_stepSize* m_gradients[iTargetWeight];
	}

	/** Compute the needed covariances for the weight */
	public void computeCovariancesIfNeeded(int iWeight) {
		if (!m_isCovComputed[iWeight]){
			computeWeightCov(iWeight);
			m_isCovComputed[iWeight] = true; // Mark the covariance computed
			m_nbOfNonZeroRules++;
		}	
	}
	/** In case of oscillation, make the step size shorter
	 * We should be changing the step size just enough not to prevent further oscillation */
	public void dropStepSize(double amount) {
		if (amount >=1)
			System.err.println("Something wrong with dropStepSize. Argument >= 1.");
		
		//m_stepSize *= 0.1;
		m_stepSize *= (amount*0.99); // We make the new step size a little smaller than is limit (because of rounding mistakes)
	}

	/** List of old fitnesses */
	protected ArrayList<Double> m_oldFitnesses;
	
	/** Early stopping is needed if the error rate is too much bigger than the smallest error rate
	 * we have had. */
	
	public boolean isEarlyStop(ArrayList<Double> weights) {
		double newFitness = m_earlyStopProbl.calcFitness(weights);
		
		m_oldFitnesses.add(new Double(newFitness));
//		if (m_oldFitnesses.size() == 100) {
//			boolean b = false;
//			b=true;
//			if (b){}	
//		}
		boolean stop = false;
		
		int lastIndex = m_oldFitnesses.size()-1;
		// For some data sets this still seems to go forever. If the change is too small, stop anyway
		// PLATEAU DETECTION
		int nbOfLastFew = 5;
		if (lastIndex >= nbOfLastFew) {

			double max = m_oldFitnesses.get(lastIndex-nbOfLastFew+1);
			double min = m_oldFitnesses.get(lastIndex-nbOfLastFew+1);
			
			//If the difference between maximum and minimum is big enough
			for (int iFitness = lastIndex-nbOfLastFew+2; 
			     iFitness < m_oldFitnesses.size(); iFitness++)
			{
				if (m_oldFitnesses.get(iFitness) > max) {
					max = m_oldFitnesses.get(iFitness);
				} else if (m_oldFitnesses.get(iFitness) < min) {
					min = m_oldFitnesses.get(iFitness);
				}
				
			}
//			Math.abs(m_oldFitnesses.get(lastIndex).doubleValue() - newFitness) < 0.001*newFitness &&
//		    Math.abs(m_oldFitnesses.get(lastIndex-1).doubleValue() - newFitness) < 0.001*newFitness){

			// If the difference is too small, we are most likely on plateau and should stop.
			if (max-min < 0.001*m_stepSize*Math.abs(min)) {
				stop = true;
				System.err.println("\nGD: Plateau detected.\n");
			}
		}
		
		for (int iFitness = 0; iFitness < m_oldFitnesses.size() && !stop; iFitness++) {
			if (newFitness > getSettings().getOptGDEarlyStopTreshold()*
					m_oldFitnesses.get(iFitness).doubleValue()) {
				stop = true;
				System.err.println("\nGD: Independent test set error increase detected - overfitting.\n");
			}	
		}


		return stop;
	}
}
