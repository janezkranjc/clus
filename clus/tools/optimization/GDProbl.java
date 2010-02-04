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

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.*;

import clus.main.*;
import clus.tools.optimization.OptProbl.OptParam;
import clus.util.ClusFormat;

// Created 28.11.2008 from previous DeProbl class

/**
 * Class representing a gradient descent optimization problem.
 * This gives tool functions for the actual optimization algorithm.
 * @author Timo Aho
 */
public class GDProbl extends OptProbl {

	/** Do we print debugging information. */
	static protected boolean m_printGDDebugInformation = false;

	/**
	 * Covariances between weights. Are computed only if needed.
	 * [iFirstWeight][iSecondWeight][iTarget]
	 * OBSOLETE, only averages of covariances are used any more!
	 */
	//protected double[][][] m_covariances;  OBSOLETE, only averages of covariances are used any more!
	protected double[][] m_covariances;

	/**
	 *  Is covariance computed for this dimension?
	 *  To reduce computational costs, covariances for certain dimension are computed only if needed
	 *  Weight is always zero if the corresponding coefficient is zero.
	 *  This array SHOULD NOT be used for indication of nonzero weights.
	 *  The covariance may be computed but the weight nonzero if (and only if)
	 *  we are running the algorithm multiple times on the same predictions etc.
	 */
	protected boolean[] m_isCovComputed;

	/**
	 *  Is the weight nonzero for this run. If we are running optimization only once
	 *  this is same as m_isCovComputed.
	 */
	protected boolean[] m_isWeightNonZero;



	/** For covariance computing, we need means (expected values) of predictions
	 * [predictor][target]
	 * OBSOLETE. For predictions means are not computed because, e.g. for all covering rule the its effect would
	 * go to zero (by prediction - mean)*/
	//protected double[][] m_meanPredictors;

	/** For covariance computing, we need means (expected values) of true values
	 * [target] OBSOLETE*/
	protected double[] m_meanTrueValues;

	/**
	 * Number of rules with nonzero weights. I.e. how many trues in m_isWeightNonZero.
	 * The 'default rule' - the first rule that is always a constant is not counted!
	 */
	protected int m_nbOfNonZeroRules;

	/** Computed negative gradients for each of the dimensions
	 * [iWeight][iTarget]*/
//	protected double[][] m_gradients;
	protected double[] m_gradients;

	/** Includes the weights that are banned.
	 * One value for each of the weights, if value > nbOfIterations, weight is banned.
	 */
	protected int[] m_bannedWeights;

	/** Affective gradients ON THIS ITERATION.
	 * These are computed again after every alteration of gradients
	 * and are valid only until the next modification.
	 * In here we combine different targets to single gradient value.
	 */
	protected double[] m_affectiveGradientsForIter;

	/** Step size for gradient descent */
	protected double m_stepSize;

	/** Separate test set for early stopping */
	protected OptParam m_dataEarlyStop;

	/** New problem for computing fitness function with the early stop data */
	protected OptProbl m_earlyStopProbl;


	/**
	 * Constructor for problem to be solved with gradient descent. Currently only for regression.
	 * @param stat_mgr Statistics
	 * @param dataInformation The true values and predictions for the instances. These are used by OptimProbl.
	 *                        The optimization procedure is based on this data information
	 * @param isClassification Is it classification or regression?
	 */
	public GDProbl(ClusStatManager stat_mgr, OptParam optInfo) {
		super(stat_mgr, optInfo);


		//m_meanPredictors = new double[getNumVar()][getNbOfTargets()];
		// We do not want to use average in covariance computing
		m_meanTrueValues = new double[getNbOfTargets()];

//		if (getSettings().isOptNormalization()) { // Normalize data inside optimization
//			normalizeData(optInfo);	
//		} else
		if  (!getSettings().isOptNormalization() &&
				getSettings().getNormalizeData() == Settings.NORMALIZE_DATA_NONE) {
			m_meanTrueValues = computeMeans(); // TODO ?Compute means for targets over the whole training set
		}

		// If early stopping criteria is chosen, reserve part of the training set for early stop testing.
		if (getSettings().getOptGDEarlyStopAmount() > 0) {

			int nbDataTest = (int)Math.ceil(getNbOfInstances() * getSettings().getOptGDEarlyStopAmount());

			// For random sample
			Random randGen = new Random(0);


			// Create the early stopping data variables.
			m_dataEarlyStop = new OptParam(optInfo.m_rulePredictions.length,
					optInfo.m_baseFuncPredictions.length,
					nbDataTest,
					getNbOfTargets());
			OptParam rest = new OptParam(optInfo.m_rulePredictions.length,
					optInfo.m_baseFuncPredictions.length,
					getNbOfInstances()-nbDataTest,
					getNbOfTargets());

			// Copy the prediction and true value references (no cloning)

			/** All the test set instances are added here so no duplication occurs */
			boolean[] selectedInstances = new boolean[getNbOfInstances()];

			for (int iTestSetInstance = 0; iTestSetInstance < nbDataTest; iTestSetInstance++){

				// Take a random index
				// random number between [0,still available dataset[
				int newIndex = randGen.nextInt(getNbOfInstances()-iTestSetInstance);

				/** Instance index in the data set to be added for test set */
				int iNewTestInstance = 0;
				// Search for the real index when duplicates are not taken into account
				// Thus skip the instance in indexOfUnUsedInstance if it is already taken
				for (int indexOfUnUsedInstance = 0; indexOfUnUsedInstance < newIndex; iNewTestInstance++) {
					if (!selectedInstances[iNewTestInstance])
						
						indexOfUnUsedInstance++;
				}
				// Still if we ended up to selected index, skip all the selected
				while (selectedInstances[iNewTestInstance])
					iNewTestInstance++;

				// Here we should have in iNewTestInstance the 'newIndex'th unused instance
				selectedInstances[iNewTestInstance] = true;

				m_dataEarlyStop.m_trueValues[iTestSetInstance] = optInfo.m_trueValues[iNewTestInstance];
				// To be safe, put original reference to null
				optInfo.m_trueValues[iNewTestInstance] = null;

				// Add the new instance for all the rules
				for (int iNonRule = 0; iNonRule < optInfo.m_baseFuncPredictions.length;iNonRule++){
					m_dataEarlyStop.m_baseFuncPredictions[iNonRule][iTestSetInstance] = optInfo.m_baseFuncPredictions[iNonRule][iNewTestInstance];
					// 	To be safe, put original reference to null
					optInfo.m_baseFuncPredictions[iNonRule][iNewTestInstance] = null;
				}
				
				for (int iRule = 0; iRule < optInfo.m_rulePredictions.length;iRule++){
					m_dataEarlyStop.m_rulePredictions[iRule].m_cover[iTestSetInstance] = optInfo.m_rulePredictions[iRule].m_cover[iNewTestInstance];
				}

			}
			// Add rest of the instances to training set
//			int sum = 0;
//			for (int i =0;i<selectedInstances.length;i++)
//				if (selectedInstances[i]) sum++;

			/** Index for the rest array */
			int iInstanceRestIndex = 0;
			int nbOfInstances = getNbOfInstances();
			for (int iInstance = 0; iInstance < nbOfInstances; iInstance++){

				if (!selectedInstances[iInstance]) {
					// Not used as test instance - add it
					rest.m_trueValues[iInstanceRestIndex] =	optInfo.m_trueValues[iInstance];

					for (int iRule = 0; iRule < optInfo.m_baseFuncPredictions.length;iRule++){
						rest.m_baseFuncPredictions[iRule][iInstanceRestIndex] =	optInfo.m_baseFuncPredictions[iRule][iInstance];
					}
					
					for (int iRule = 0; iRule < optInfo.m_rulePredictions.length;iRule++){
						rest.m_rulePredictions[iRule].m_cover[iInstanceRestIndex] = optInfo.m_rulePredictions[iRule].m_cover[iInstance];
					}
					
					iInstanceRestIndex++;
				}
			}
			
			for (int iRule = 0; iRule < optInfo.m_rulePredictions.length; iRule++){			
				rest.m_rulePredictions[iRule].m_prediction = optInfo.m_rulePredictions[iRule].m_prediction;
				m_dataEarlyStop.m_rulePredictions[iRule].m_prediction = optInfo.m_rulePredictions[iRule].m_prediction;
			}

			if (iInstanceRestIndex != rest.m_trueValues.length) {
				System.err.println("GDProbl error. Wrong amount of early stop data added");
				System.exit(1);
			}

			changeData(rest);  // Change data for super class

			m_earlyStopProbl = new OptProbl(stat_mgr, m_dataEarlyStop);
			// Give the same std devs for this smaller part of data.
			m_earlyStopProbl.modifyDataStatistics(getDataStdDevs());

			// We are using Fitness function of  the problem. Let us put the reg penalty to 0 because we do not
			// want to use it
			getSettings().setOptRegPar(0);
			getSettings().setOptNbZeroesPar(0);

		}


		int nbWeights = getNumVar();
//		int nbTargets = getNbOfTargets();

		//m_covariances = new double[nbWeights][nbWeights][nbTargets];
		m_covariances = new double[nbWeights][nbWeights];
		for (int i = 0; i < nbWeights; i++) {
			for (int j = 0; j < nbWeights; j++) {
//				for (int k = 0; k < nbTargets; k++)
//					m_covariances[i][j][k] = Double.NaN;
				m_covariances[i][j] = Double.NaN;
			}
		}
		m_isCovComputed = new boolean[nbWeights]; // Initial value is false

		// This is called from GDAlg
		//initGDForNewRunWithSamePredictions();
	}

//	/** Normalizes the predictions and true values by (x-avg)/(2*std dev).*/
//	private void normalizeData(OptParam optInfo) {
//		int nbOfTargets = getNbOfTargets();
//		int nbOfInstances = getNbOfInstances();
//		int nbOfRules = optInfo.m_rulePredictions.length;
//		int nbOfOtherPred = optInfo.m_baseFuncPredictions.length;
//		
//		for (int iTarget = 0; iTarget < nbOfTargets; iTarget++){
//			for (int iInstance = 0; iInstance < nbOfInstances; iInstance++){			
//				// True values
//				if (isValidValue(optInfo.m_trueValues[iInstance][iTarget])){
//					if (!getSettings().isOptDefaultShiftPred() && false) { // TODO is this useful at all?
//						optInfo.m_trueValues[iInstance][iTarget] -=
//							getDataMean(iTarget);
//					}
//						
//					optInfo.m_trueValues[iInstance][iTarget] /=
//						2*getDataStdDev(iTarget);
//				}
//			
//				// Other predictions
//				for (int iPred = 0; iPred < nbOfOtherPred; iPred++){
//					// Predictions
//					if (!getSettings().isOptDefaultShiftPred()&& false) { // TODO is this useful at all?
//						optInfo.m_baseFuncPredictions[iInstance][iPred][iTarget][0] -=
//							getDataMean(iTarget);
//					}
//					optInfo.m_baseFuncPredictions[iInstance][iPred][iTarget][0] /=
//						2*getDataStdDev(iTarget);
//				}
//			}
//			
//			// Rule redictions	
//			for (int iRule = 0; iRule < nbOfRules; iRule++){
//
//				if (!getSettings().isOptDefaultShiftPred()&& false) { // TODO is this useful at all?
//					optInfo.m_rulePredictions[iRule].m_prediction[iTarget][0]-=
//						getDataMean(iTarget);
//				}
//					
//				optInfo.m_rulePredictions[iRule].m_prediction[iTarget][0] /=
//						2*getDataStdDev(iTarget);
//			}
//
//		}
//	}


	/** Initialize GD optimization for new run with same predictions and true values
	 * This can be used if some parameters change. Thus we e.g. do not compute covariances
	 * again
	 */
	public void initGDForNewRunWithSamePredictions() {
		int nbWeights = getNumVar();
//		int nbTargets = getNbOfTargets();

		if (getSettings().getOptGDEarlyStopAmount() > 0) {
			//			m_oldFitnesses = new ArrayList<Double>();
			m_minFitness = Double.POSITIVE_INFINITY;
			m_minFitWeights = new ArrayList<Double>(getNumVar());

			for (int iWeight = 0; iWeight < getNumVar(); iWeight++)
			{
				m_minFitWeights.add(new Double(0)); // Initialize
			}

		}

		m_isWeightNonZero = new boolean[nbWeights];

		if (getSettings().getOptGDMTGradientCombine() == Settings.OPT_GD_MT_GRADIENT_MAX_LOSS_VALUE) {
			m_bannedWeights = new int[nbWeights]; // Are used only for MaxLoss
		} else {
			m_bannedWeights = null;
		}
		m_gradients = new double[nbWeights];  // Initial value is zero

		m_nbOfNonZeroRules = 0;
		m_affectiveGradientsForIter = new double[nbWeights];
		m_stepSize = getSettings().getOptGDStepSize();
	}

// TODO Moved to OptProbl
//	/** Compute means (e.g. for covariance computation) for true values
//	 * For predictions means are not computed because, e.g. for all covering rule the its effect would
//	 * go to zero (by prediction - mean)
//	 * If the data is normalized, mean of true value is 0 and mean is not needed to compute*/
//	//protected void computeMeans(double[] predMeans, double[] trueValMeans) {
//	protected double[] computeMeans() {
//		int nbOfTargets = getNbOfTargets();
//		int nbOfValidValues = 0;
//		double[] means = new double[nbOfTargets];
//		
//		for (int iTarget = 0; iTarget < nbOfTargets; iTarget++){
//			means[iTarget] = 0;
//			for (int iInstance = 0; iInstance < getNbOfInstances(); iInstance++){
//				if (isValidValue(getTrueValue(iInstance,iTarget))){
//					means[iTarget] +=
//						getTrueValue(iInstance,iTarget);
//					nbOfValidValues++;
//				}
//			}
//			means[iTarget] /= nbOfValidValues;
//		}
//		return means;
//	}

	/**
	 * Generates a zero vector.
	 */
	protected ArrayList<Double> getInitialWeightVector()
	{
		ArrayList<Double> result = new ArrayList<Double>(getNumVar());
		for (int i = 0; i < getNumVar(); i++) {
			result.add(new Double(0.0));
		}

		// The following is commented because causes overfitting - too big weight values in the beginning
//		if (getSettings().isOptOmitRulePredictions()) {
//			// Start first weight (default value) with better value - mean value
//			double[] means = computeMeans();
//			
//			double sumOfMeans = 0;
//			
//			// Take average over how to get the average with the first rule
//			for (int i = 0; i < getNbOfTargets(); ++i) {
//				sumOfMeans += means[i]/getPredictionsWhenCovered(0,0,i);			
//			}
//			sumOfMeans /= getNbOfTargets();
//			
//			// Scale the weight to better value
//			result.set(0, sumOfMeans);		
//		}

		return result;
	}
	
	
	/** Estimate of expected value of covariance for given prediction.
	 * The covariance of this prediction with its true value is returned.
	 */
//	 * Return the covariance for each of the targets.
//	protected double[] getCovForPrediction(int iPred) {
	protected double getCovForPrediction(int iPred) {
		double[] covs = new double[getNbOfTargets()];
		int nbOfTargets = getNbOfTargets();
		for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
 			for (int iInstance = 0; iInstance < getNbOfInstances(); iInstance++) {
				double trueVal = getTrueValue(iInstance,iTarget);
				if (isValidValue(trueVal)) // Not a valid true value, rare but happens. Can happen for linear terms.
					//covs[iTarget] += trueVal*predictWithRule(iPred, iInstance,iTarget);
					covs[iTarget] +=
						(trueVal-m_meanTrueValues[iTarget])*
						predictWithRule(iPred, iInstance,iTarget);
			}

			covs[iTarget] /= getNbOfInstances();
			if (getSettings().isOptNormalization()) {
				covs[iTarget] /= 2*getDataStdDev(iTarget);
			}
		}

		double avgCov = 0;
		for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
			avgCov += covs[iTarget]/nbOfTargets;
		}
		return avgCov;
		//return covs;
	}

	/**
	 * Return the right stored covariance
	 */
	// Only one corner is computed (the other is similar)
//	protected double[] getWeightCov(int iFirst, int iSecond) {
	protected double getWeightCov(int iFirst, int iSecond) {
		int min = Math.min(iFirst, iSecond);
		int max = Math.max(iFirst, iSecond);
		//if (Double.isNaN(m_covariances[min][max][0]))
		if (Double.isNaN(m_covariances[min][max]))
			throw new Error("Asked covariance not yet computed. Something wrong in the covariances in GDProbl.");
		return m_covariances[min][max];
	}

	/** Compute the covariances for this dimension. This means that
	 * we compute all the pairs where this takes part.
	 * Because for covariance
	 * cov(a,b) = cov(b,a) compute only one of them.
	 * @par dimension The dimension for which the covariances are computed
	 */
	protected void computeWeightCov(int dimension) {
		// Because of symmetry cov(dimension, b) is already computed if for some earlier phase b was dimension
		// Thus if covariances for b are computed, this does not have to be computed anymore.

		// The weights with index lower than this
		for (int iMin = 0; iMin < dimension; iMin++) {
			// If the covariances for the other part are already computed, this covariance is already
			// computed also. Thus skip this covariance.
			if (!m_isCovComputed[iMin]) {
//				if (!Double.isNaN(m_covariances[iMin][dimension][0]))
//					System.err.println("WARNING: Covariances are recalculated, waste of computation!");
				m_covariances[iMin][dimension] = computeCovFor2Preds(iMin, dimension);
			}
		}

		m_covariances[dimension][dimension] = computeCovFor2Preds(dimension, dimension);
		for (int iMax = dimension+1; iMax < getNumVar(); iMax++) {
			if (!m_isCovComputed[iMax]) {
//				if (!Double.isNaN(m_covariances[iMax][dimension][0]))
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
//	private double[] computeCovFor2Preds(int iFirstRule, int iSecondRule) {
	private double computeCovFor2Preds(int iFirstRule, int iSecondRule) {
		int nbOfTargets = getNbOfTargets();
		int nbOfInstances = getNbOfInstances();

		double[] covs = new double[nbOfTargets];

		for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
			for (int iInstance = 0; iInstance < nbOfInstances; iInstance++) {
				covs[iTarget] += predictWithRule(iFirstRule,iInstance,iTarget) *
				                 predictWithRule(iSecondRule,iInstance,iTarget);

				// We optimize this function because this is the one that takes most of the time (70%).
				// We should use "predictWithRule" method, but this is slightly faster
//				double firstPred = getPredictions(iFirstRule,iInstance,iTarget);
//				double secondPred = getPredictions(iSecondRule,iInstance,iTarget);
//
//				// Is valid prediction or is not covered?
//				firstPred = Double.isNaN(firstPred) ? 0 : firstPred;
//				secondPred = Double.isNaN(secondPred) ? 0 : secondPred;
//
//				covs[iTarget] += firstPred * secondPred;
			}
			covs[iTarget] /= getNbOfInstances();
			
			if (getSettings().isOptNormalization()) {
				covs[iTarget] /= 2*getDataStdDev(iTarget);
			}
		}
		
		double avgCov = 0;
		
		for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
			avgCov += covs[iTarget]/nbOfTargets;
		}
		return avgCov;
//		return covs;
	}



	/** Returns the real prediction when this rule is used. If the rule does
	 * not give prediction for some target, default rule is used.
	 * ONLY FOR REGRESSION! Classification not implemented.
	 */
	final protected double predictWithRule(int iRule, int iInstance, int iTarget) {
		return 	isCovered(iRule,iInstance) ? getPredictionsWhenCovered(iRule, iInstance, iTarget) : 0;
	}

	/** Compute the gradients for weights */
	public void initGradients(ArrayList<Double> weights) {
		// Compute all the gradients for current weights.
		for (int iWeight = 0; iWeight < weights.size(); iWeight++ ) {
			m_gradients[iWeight] = getGradient(iWeight, weights);
		}

		initCombineGradientValues(weights);
	}


	/** Compute gradient for the given weight dimension */
	protected double getGradient(int iWeightDim, ArrayList<Double> weights) {

		double gradient = 0;
		switch (getSettings().getOptGDLossFunction()) {
		case Settings.OPT_LOSS_FUNCTIONS_01ERROR:
			//gradient = loss01(trueValue, prediction);
			//break;
		case Settings.OPT_LOSS_FUNCTIONS_HUBER:
		case Settings.OPT_LOSS_FUNCTIONS_RRMSE:
			//gradient = lossHuber(trueValue, prediction);
//			break;
			try {
				throw new Exception("0/1 or Huber loss function not yet implemented for Gradient descent.\n" +
						"Using squared loss.\n");
			} catch(Exception s) {
				s.printStackTrace();
			} //TODO Huber and alpha computing
			//Default case
		case Settings.OPT_LOSS_FUNCTIONS_SQUARED:
		default:
			gradient = gradientSquared(iWeightDim, weights);
			break;
		}

		return gradient;
	}

	/**
	 * Squared loss gradient. p. 18 in Friedman & Popescu, 2004
	 * @param iGradWeightDim Weight dimension for which the gradient is computed.
	 * @return Gradient average
	 */
//	private double[] gradientSquared(int iGradWeightDim, ArrayList<Double> weights) {
	private double gradientSquared(int iGradWeightDim, ArrayList<Double> weights) {

		//double[] gradient = new double[getNbOfTargets()] ;
		double gradient = 0;
//		int nbOfTargets = getNbOfTargets();

		gradient = getCovForPrediction(iGradWeightDim);

		for (int iWeight = 0; iWeight < getNumVar(); iWeight++){
			//if (m_isWeightNonZero[weights.get(iWeight).doubleValue() != 0) {
			if (m_isWeightNonZero[iWeight]) {
				gradient -= weights.get(iWeight).doubleValue()
                					* getWeightCov(iWeight,iGradWeightDim);

//				double[] covariance = getWeightCov(iWeight,iGradWeightDim);
//				for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
//					gradient[iTarget] -= weights.get(iWeight).doubleValue()
//					                     * covariance[iTarget];
//				}
			}
		}

		return gradient;
	}


	/** Recompute the gradients new iteration.
	 * This is lot of faster than computing everything from the scratch.
	 * @param changedWeightIndex The index of weights that have changed. Only these affect the change in the new gradient.
	 * 						  Friedman&Popescu p.18
	 */
	protected void modifyGradients(int[] changedWeightIndex, ArrayList<Double> weights) {

		switch (getSettings().getOptGDLossFunction()) {
		case Settings.OPT_LOSS_FUNCTIONS_01ERROR:
		case Settings.OPT_LOSS_FUNCTIONS_HUBER:
		case Settings.OPT_LOSS_FUNCTIONS_RRMSE:
			//TODO Huber and alpha computing
			//Default case
		case Settings.OPT_LOSS_FUNCTIONS_SQUARED:
		default:
			modifyGradientSquared(changedWeightIndex);
			break;
		}
		modifyCombineGradientValues(weights, changedWeightIndex); // Combine new gradient values
	}


	/** Recomputation of gradients for least squares loss function */
	public void modifyGradientSquared(int[] changedWeightIndex) {
		//int nbOfTargets = getNbOfTargets();
		// New gradients are computed with the old gradients.
		// Only the changed gradients are stored here
		// However since we use affective gradients which are not YET changed,
		// we can use directly them.
//		double[] oldGradsOfChanged = new double[changedWeightIndex.length];
//
//		for (int iCopy = 0; iCopy < changedWeightIndex.length; iCopy++) {
//			for (int iTarget = 0; iTarget < nbOfTargets; iTarget++)
//				oldGradsOfChanged[iCopy][iTarget] = m_gradients[changedWeightIndex[iCopy]][iTarget];
//		}

		// Index over the gradient we are changing (ALL GRADIENTS)
		for (int iWeightChange = 0; iWeightChange < m_gradients.length; iWeightChange++) {
			// Index over the other gradients that are affecting (THE WEIGHTS THAT ALTERED)
			for (int iiAffecting = 0; iiAffecting < changedWeightIndex.length; iiAffecting++) {
				double cov = getWeightCov(changedWeightIndex[iiAffecting],iWeightChange);
//				for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
					//The stepsize * old gradients is equal to the change!
					// However we have to use the AFFECTIVE gradients (the gradients that were used last iteration)
					m_gradients[iWeightChange] -= m_stepSize*
						m_affectiveGradientsForIter[changedWeightIndex[iiAffecting]]*
										cov;

//				}
			}
		}
	}


	/** After changing the gradient values, we have to combine them to single
	 * gradient value that is used for step taking etc. This is done in this function.
	 * The gradient values are stored in m_affectiveGradientsForIter and are
	 * valid until next gradient change
	 */
	protected void initCombineGradientValues(ArrayList<Double> weights) {
		// Compute affective gradients for this iteration. They are based on
		// new gradient values. They are easiest to compute from scratch
		// because it does not cost much and the fast updating method is different for
		// different settings

		// What is the gradient that affects
		//int nbOfTargets = getNbOfTargets();
		switch (getSettings().getOptGDMTGradientCombine()) {
		case Settings.OPT_GD_MT_GRADIENT_AVG:
		{ // Average over the gradients.

			for (int iGradient = 0; iGradient < m_affectiveGradientsForIter.length; iGradient++) {
//				m_affectiveGradientsForIter[iGradient] = 0;
				m_affectiveGradientsForIter[iGradient] = m_gradients[iGradient];

//				for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
//					m_affectiveGradientsForIter[iGradient] +=
//						m_gradients[iGradient][iTarget];
//				}
//				m_affectiveGradientsForIter[iGradient] /= nbOfTargets;
			}
			break;
		}
		case Settings.OPT_GD_MT_GRADIENT_MAX_GRADIENT:
		{// Take the maximum gradient. So go according to the biggest slope.

			
			// The efficient version was not really efficient. Also max loss is worse than avg.
			System.err.println("Error: Multi-target max gradient not implemented.");
			System.exit(1);
			
//			for (int iGradient = 0; iGradient < m_affectiveGradientsForIter.length; iGradient++) {
//				double maxGradient = 0;
//				for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
//					if (Math.abs(maxGradient) < Math.abs(m_gradients[iGradient][iTarget]))
//						maxGradient = m_gradients[iGradient][iTarget];
//				}
//				m_affectiveGradientsForIter[iGradient] = maxGradient;
//			}
//			break;
		}

		case Settings.OPT_GD_MT_GRADIENT_MAX_LOSS_VALUE:
		{
			// The efficient version was not really efficient. Also max loss is worse than avg.
			System.err.println("Error: Multi-target max loss not implemented.");
			System.exit(1);
			
//			
//			// Assume the loss function is the max of all the target loss functions
//			// this creates convex loss function. Thus we select the gradient that
//			// is for the maximal target loss function.
//			int iMaxTargetLossFunctionValue = 0;
//			double[] lossFunctionValues = new double[nbOfTargets];
//
//			for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
//				// Compute loss function for training set
//				lossFunctionValues[iTarget] = calcFitnessForTarget(weights, iTarget);
//				// Greatest loss = greatest fitness function return value
//				if (lossFunctionValues[iTarget]
//				                       > lossFunctionValues[iMaxTargetLossFunctionValue]) {
//					iMaxTargetLossFunctionValue = iTarget;
//				}
//			}
//
//			for (int iGradient = 0; iGradient < m_affectiveGradientsForIter.length; iGradient++) {
//				m_affectiveGradientsForIter[iGradient] = m_gradients[iGradient][iMaxTargetLossFunctionValue];
//			}
//			break;
		}
		case Settings.OPT_GD_MT_GRADIENT_MAX_LOSS_VALUE_FAST:
		{
			// The efficient version was not really efficient. Also max loss is worse than avg.
			System.err.println("Error: Multi-target max loss, efficient computation not implemented.");
			System.exit(1);
		}

		}

	}


	/** This does mostly the same as initCombineGradientValues:
	 * Computes the combined gradient values from all the different targets.
	 * However, this is meant after modifying the gradients and is faster.
	 */
	protected void modifyCombineGradientValues(ArrayList<Double> weights, int[] changedWeightIndex) {
		// Compute affective gradients for this iteration. They are based on
		// new gradient values. They are easiest to compute from scratch
		// because it does not cost much and the fast updating method is different for
		// different settings

		// What is the gradient that affects
		//int nbOfTargets = getNbOfTargets();
		switch (getSettings().getOptGDMTGradientCombine()) {
		case Settings.OPT_GD_MT_GRADIENT_AVG:
		case Settings.OPT_GD_MT_GRADIENT_MAX_GRADIENT:
		{
			initCombineGradientValues(weights);
			break;
		}

//		case Settings.OPT_GD_MT_GRADIENT_MAX_LOSS_VALUE:
//		{
//			// Assume the loss function is the max of all the target loss functions
//			// this creates convex loss function. Thus we select the gradient that
//			// is for the maximal target loss function.
//			int iMaxTargetLossFunctionValue = 0;
//			double[] lossFunctionValues = new double[nbOfTargets];
//
//			for (int iTarget = 0; iTarget < nbOfTargets; iTarget++) {
//				// Compute loss function for training set
//				lossFunctionValues[iTarget] = calcFitnessForTarget(weights, iTarget);
//				// Greatest loss = greatest fitness function return value
//				if (lossFunctionValues[iTarget]
//				                       > lossFunctionValues[iMaxTargetLossFunctionValue]) {
//					iMaxTargetLossFunctionValue = iTarget;
//				}
//			}
//
//			for (int iGradient = 0; iGradient < m_affectiveGradientsForIter.length; iGradient++) {
//				m_affectiveGradientsForIter[iGradient] = m_gradients[iGradient][iMaxTargetLossFunctionValue];
//			}
//			break;
//		}
		}

	}

	/** Return the gradients with maximum absolute value. For the weights we want to change
	 * The function is assuming (if max allowed rule nb is set) that index 0 of gradients includes
	 * the "default rule" which is not counted.
	 * @param nbOfIterations Used for detecting if weight is banned (not usually used)*/
	public int[] getMaxGradients(int nbOfIterations) {
		/** Maximum number of nonzero elements. Can we add more? */
		int maxElements = getSettings().getOptGDMaxNbWeights();
		boolean maxNbOfWeightReached = false;
		if (maxElements > 0 && m_nbOfNonZeroRules >= maxElements) {
			// If maximum number of nonzero elements is reached,
			// search for the biggest one among the nonzero weights

			maxNbOfWeightReached = true;
		}

		double maxGrad = 0; // Maximum gradient
		for (int iGrad = 0; iGrad < m_affectiveGradientsForIter.length; iGrad++) {
			if (m_bannedWeights != null && m_bannedWeights[iGrad] > nbOfIterations) {
				// The weight is banned
				continue;
			}
			// We choose the gradient if max nb of weights is reached only if it is
			// already nonzero
			if (Math.abs(m_affectiveGradientsForIter[iGrad]) > maxGrad
					&& (!maxNbOfWeightReached || m_isWeightNonZero[iGrad] || iGrad == 0))
				maxGrad = Math.abs(m_affectiveGradientsForIter[iGrad]);
		}

		ArrayList<Integer> iMaxGradients = new ArrayList<Integer>();

		// The least allowed item.
		double minAllowed = getSettings().getOptGDGradTreshold() * maxGrad;

		// Copy all the items that are greater to a returned array
		for (int iCopy = 0; iCopy < m_affectiveGradientsForIter.length; iCopy++) {
			if (m_bannedWeights != null && m_bannedWeights[iCopy] > nbOfIterations) {
				// The weight is banned
				continue;
			}
			if (Math.abs(m_affectiveGradientsForIter[iCopy]) >= minAllowed
					&& (!maxNbOfWeightReached || m_isWeightNonZero[iCopy] || iCopy == 0)) {
				iMaxGradients.add(iCopy);
				// If the treshold is 1, we only want to change one dimension at time
				if (getSettings().getOptGDGradTreshold() == 1.0)
					break;
			}
		}

		// If we have maximum amount of rules and treshold value is not 1, we may be
		// returning too many gradients and thus having too many rules. Thus select
		// only the ones needed.
		// However if maximum number of nonzero weights is already reached, we can't
		// take too much of these.
		if (maxElements > 0 && !maxNbOfWeightReached && getSettings().getOptGDGradTreshold() < 1.0) {

			// Gradients that are already nonzero. (count also default rule, because it is
			// not counted in maxgradients)
			int nbOfOldGrads = 0;

			// Count how many old ones we have in the current gradients
			for (int iGrad = 0; iGrad < iMaxGradients.size(); iGrad++) {
				if (m_isWeightNonZero[iMaxGradients.get(iGrad)] || iMaxGradients.get(iGrad) == 0) {
					nbOfOldGrads++;
				}
			}

			// if number of new rules in gradients is greater than maximum number of
			// allowed elements, we have to get rid of some of them.
			int nbOfAllowedNewGradients = maxElements - m_nbOfNonZeroRules;
			if (nbOfAllowedNewGradients < iMaxGradients.size()-nbOfOldGrads) {

				//ArrayList is slow for inserts in the middle.
				//LinkedList should be fast for iterating thru and inserting.
				LinkedList<Integer> iAllowedNewMaxGradients = new LinkedList<Integer>();

				for (int iGrad = 0; iGrad < iMaxGradients.size(); iGrad++) {

					// If the gradient is new.
					if (!m_isWeightNonZero[iMaxGradients.get(iGrad)] && iMaxGradients.get(iGrad) != 0) {

						// Insertion sort to iAllowed by the greatness of gradient
						ListIterator<Integer> iAllowed  = iAllowedNewMaxGradients.listIterator();
						while (iAllowed.hasNext()) {
							// If we already went too far
							if (Math.abs(m_affectiveGradientsForIter[iAllowed.next()])
							    <  Math.abs(m_affectiveGradientsForIter[iMaxGradients.get(iGrad)])) {
								iAllowed.previous();
								break;
							}
						}

						// We should now be on the insertion place
						iAllowed.add(iMaxGradients.get(iGrad));
						iMaxGradients.remove(iGrad);
						iGrad--;

						// To keep the list smaller
						if (iAllowedNewMaxGradients.size() > nbOfAllowedNewGradients) {
							iAllowedNewMaxGradients.removeLast();
						}
					}
				}

				// now we have in sorted order gradients in iAllowedNewMaxGradients. Let us add
				// only so much that is allowed
				ListIterator<Integer> iList  = iAllowedNewMaxGradients.listIterator();

				for (int addedElements = 0;
					addedElements < nbOfAllowedNewGradients; addedElements++ ) {
					iMaxGradients.add(iList.next());
				}

			}


		}

		// Efficient enough
		int[] iMaxGradientsArray = new int[iMaxGradients.size()];
		for (int iCopy = 0; iCopy < iMaxGradients.size(); iCopy++) {
			iMaxGradientsArray[iCopy]=iMaxGradients.get(iCopy);
		}
		return iMaxGradientsArray;
	}

	/**
	 * Compute the change of target weight because of the gradient
	 * @param iTargetWeight Weight index we want to change.
	 */
	public double howMuchWeightChanges(int iTargetWeight) {
//		return m_stepSize* m_gradients[iTargetWeight];
		return m_stepSize* m_affectiveGradientsForIter[iTargetWeight];
	}

	/** Compute the needed covariances for the weight. Only called
	 * if we are going to change the weight. */
	public void computeCovariancesIfNeeded(int iWeight) {
		if (!m_isCovComputed[iWeight]){
			computeWeightCov(iWeight);
			m_isCovComputed[iWeight] = true; // Mark the covariance computed
		}
		// For multiple runs for same predictions, may not be same as before
		if (!m_isWeightNonZero[iWeight]) {
			m_isWeightNonZero[iWeight] = true;

			// Do not count first default rule as a nonzero weight.
			if (iWeight !=0) {
				m_nbOfNonZeroRules++;
			}
		}
	}
	/** In case of oscillation, make the step size shorter
	 * We should be changing the step size just enough not to prevent further oscillation */
	public void dropStepSize(double amount) {
		if (amount >=1)
			System.err.println("Something wrong with dropStepSize. Argument >= 1.");

		//m_stepSize *= 0.1;
		m_stepSize *= amount; // We make the new step size a little smaller than is limit (because of rounding mistakes)
	}

	/** List of old fitnesses for plateau detection (andy for debugging) */
	//protected ArrayList<Double> m_oldFitnesses;
	protected double m_minFitness;
	/** Weights when the Fitness was minimum */
	protected ArrayList<Double> m_minFitWeights;


	/** Returns best fitness so far. */
	public double getBestFitness() {
		return m_minFitness;
	}

	/** Early stopping is needed if the error rate is too much bigger than the smallest error rate
	 * we have had. */

	public boolean isEarlyStop(ArrayList<Double> weights) {
		double newFitness = m_earlyStopProbl.calcFitness(weights);

		if (newFitness < m_minFitness) {
			m_minFitness = newFitness;
			// Copy the weights
			for (int iWeight = 0; iWeight < weights.size(); iWeight++)
			{
				m_minFitWeights.set(iWeight, weights.get(iWeight).doubleValue());
			}
		}


		boolean stop = false;

		if (newFitness > getSettings().getOptGDEarlyStopTreshold()*m_minFitness) {
			stop = true;
			if (m_printGDDebugInformation)
				System.err.println("\nGD: Independent test set error increase detected - overfitting.\n");
		}

		return stop;
	}

	/** Restore the weight with minimum fitness. */
	public void restoreBestWeight(ArrayList<Double> targetWeights) {
		for (int iWeight = 0; iWeight < targetWeights.size(); iWeight++)
		{
			targetWeights.set(iWeight, m_minFitWeights.get(iWeight).doubleValue());
		}
	}

	//static ArrayList<Integer> turha = new ArrayList<Integer>();
	/** Return a maximum tree depth based on RulefFit (Friedman, Popescu. 2005) function used.
	 * Function is Pr(nbOfLeaves) = exp(-nbOfLeaves/(avgDepth/2))/(avgDepth-2).
	 * @param unifRand Random uniform number on which the number is based on.
	 * @param avgDepth Average depth (L in RuleFit).
	 * @return Random depth
	 */
	public static int randDepthWighExponentialDistribution(double unifRand, int avgDepth) {
		// Friedman is computing the number of terminal leaves with exponential distribution
		// To get the limit depth for this we need to compute
		// depth =  ceil(log(2+floor(-(L-2)/(lg(L-2))lg(unifRand)))) +1
		// Here L is the average and 2+floor(-(L-2)/(lg(L-2))lg(unifRand)) is the number of terminal leaves
		// However in Clus depth seems to be so that only root is on depth 0, and depth 1 has two leaves. Thus
		// we leave the last +1 out and compute ceil(log(2+floor(-(L-2)/(lg(L-2))lg(unifRand))))
		// Also we do not take the floor for computing amount of terminal nodes because this reduces the average
		// depth too much. Without it the average depth for value 3 is in reality 2.7
		int maxDepths = 0;

		if (unifRand == 0.0) {
			// This means that the limit depth should be infinite. Can't take logarithm of this
			maxDepths = -1;
		} else {
			int avgNbLeaves = (int)(Math.pow(2,avgDepth)); // The root depth is 0, thus not avgDepth-1.
			//int terminalNodes = (int) (2+Math.floor((double)(2-avgNbLeaves)/Math.log(avgNbLeaves-2)*Math.log(unifRand)));
			double terminalNodes = 2+(double)(2-avgNbLeaves)/Math.log(avgNbLeaves-2)*Math.log(unifRand);
			maxDepths = (int) Math.ceil(Math.log(terminalNodes)/Math.log(2.0)); // Binary logarithm. Root
		}
		//turha.add(new Integer(maxDepths));
		return maxDepths;
	}	
	
	/** Print gradients to output file. */
	public void printGradientsToFile(int iterNro, PrintWriter wrt) {
		if (!m_printGDDebugInformation)
			return;

		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		wrt.print("Iteration " + iterNro +":");
		for (int i = 0; i < m_affectiveGradientsForIter.length; i++) {
			wrt.print(fr.format((double)m_affectiveGradientsForIter[i])+"\t");
		}
		wrt.print("\n");
	}
		
}
