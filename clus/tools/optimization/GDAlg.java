/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2008                                                    *
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
 * Created on 27.11.2008
 */
package clus.tools.optimization;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import clus.main.ClusStatManager;
import clus.util.ClusFormat;


/**
 * Class for gradient descent optimization. 
 * @author Timo Aho
 *
 */
public class GDAlg extends OptAlg{
	
	/** Includes the information about settings of optimization. Has all the helping functions and variables. */
	private GDProbl m_GDProbl;
	
	/** Current weights */
	protected ArrayList<Double> m_weights;
	
	/** After how many steps check if early stopping is ok already */
	protected int m_earlyStopStep;
		
	/**
	 * Constructor for classification and regression optimization.
	 * @param stat_mgr Statistics
	 * @param dataInformation The true values and predictions for the instances. These are used by OptimProbl.
	 *                        The optimization procedure is based on this data information
	 */
	public GDAlg(ClusStatManager stat_mgr, OptProbl.OptParam dataInformation) {
		super(stat_mgr, dataInformation);
		m_GDProbl = new GDProbl(stat_mgr, dataInformation);
		m_weights = m_GDProbl.getZeroVector();
		
		// Oscillation detection
		m_prevChange = null;
		m_iPrevDimension = null;
		m_iNewDimension = null;
		m_newChange = null;
		m_earlyStopStep = 100;
		
		
	}


	/**
	 *  Optimize the weights for the given data with gradient descent algorithm.
	 *  This is the algorithm by Friedman, Popescu 2004
	 * @throws Exception 
	 */
	public ArrayList<Double> optimize() { 

		System.out.print("\nGradient descent: Optimizing rule weights (" + getSettings().getOptGDMaxIter() + ") ");
		
		PrintWriter wrt_log = null;
		
		try {
			wrt_log = new PrintWriter(new OutputStreamWriter
					(new FileOutputStream("gradDesc.log")));
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Log file could not be opened. Logging omitted.");
		}

		if (m_GDProbl.isClassifTask()){
			try {
				throw new Exception("Classification not yeat implemented for gradient descent. Skipping the optimization.");
			} catch(Exception s) {
				s.printStackTrace();
			}
			return null;
		}
		// Compute initial gradients
		m_GDProbl.initGradients(m_weights);
			
		int nbOfIterations = 0;
		while(nbOfIterations < getSettings().getOptGDMaxIter()) {
			if (nbOfIterations % 100 == 0) System.out.print(".");
			if (nbOfIterations % m_earlyStopStep == 0 && getSettings().getOptGDEarlyStopAmount() > 0 &&
					m_GDProbl.isEarlyStop(m_weights))
			{
				wrt_log.println("Early stopping detected.");
				System.err.println("\nEarly stopping detected after " + nbOfIterations + " iterations.\n");
				break;
			}
			
			// Print 
			OutputLog(nbOfIterations, wrt_log);
			//3)
			// Search for maximum gradients
			int[] maxGradients = m_GDProbl.getMaxGradients();
			
			boolean oscillation = false;
			
			// For detecting oscillation			
			storeGradientsForOscillation(maxGradients);

			//b) // Take the gradient step on the peak dimension

			// First compute the change values and detect possible oscillation
			
			//Change of the weight according to this gradient.
			double[] valueChange = new double[maxGradients.length];
			
			for (int iiGradient = 0; iiGradient < maxGradients.length; iiGradient++) {	
				//c) We are changing the value of the weight. Let's compute the covariance if it is not yet done
				m_GDProbl.computeCovariancesIfNeeded(maxGradients[iiGradient]);

				//Change of the weight according to this gradient.
				valueChange[iiGradient] = m_GDProbl.howMuchWeightChanges(maxGradients[iiGradient]);
				
				// For detecting oscillation. If oscillation is already true, do not change it to false.
				oscillation = (detectOscillation(iiGradient, valueChange[iiGradient]) || oscillation);		
			}
			
			// If oscillation was detected we do not take real steps.
			// we reduce the step size until the new step is smaller than the first one.
			// It should be smaller because otherwise we are going even further from the optimal point.
			if (oscillation) {
				
				// This is needed if we are changing more than one dimension at time.
				// The step size is made for the biggest oscillation and thus some of the other oscillating ones
				// never come back to sensible values.
				reversePreviousStep(); // Reverse the previous step. We also skip this step.
				
				reduceStepSizeDueOscillation();
				continue; // Start next iteration
			} else {
				// Store the current data for use in the next iterations.
				storeTheOscillationData();
			}
			
			// After it is sure no oscillation is detected, make the changes
			for (int iiGradient = 0; iiGradient < maxGradients.length; iiGradient++) {	
				m_weights.set(maxGradients[iiGradient], m_weights.get(maxGradients[iiGradient]).doubleValue()
						+ valueChange[iiGradient]);
			}
		


			// d) compute the gradients again for the weights that changed
			// This must not affect the computation during this iteration
			m_GDProbl.modifyGradients(maxGradients);

			nbOfIterations++;
		} // While
		
		System.out.println(" done!");
		
		wrt_log.println("The result of optimization");
		OutputLog(nbOfIterations, wrt_log);
		wrt_log.close();

		return m_weights;
	}

	
	/** Maximum step size reduction found on this iteration */
	private double m_minStepSizeReduction = 1;
	
	/** Reduce the step size. Take the biggest needed reduction and do that */
	private void reduceStepSizeDueOscillation() {
//		System.err.print("Dropping the step size.\n");
		 
		// We are lowering the step size just enough to prevent further oscillation
		m_GDProbl.dropStepSize(m_minStepSizeReduction);
		m_minStepSizeReduction = 1;	
	}
	





	/** Information about the previous change for oscillation warning. The change value */
	private double[] m_prevChange;
	private double[] m_newChange;
	
	
	/** Information about the previous change for oscillation warning. The changed dimension.
	 * Works only if one dimension at time changed. */ //TODO
	private int[] m_iPrevDimension;
	private int[] m_iNewDimension;
	
	/** A iteration has been done and oscillation not detected - store the previous changes */ 
	private void storeTheOscillationData() {
		m_prevChange = m_newChange.clone();
		m_iPrevDimension = m_iNewDimension.clone();
	}
	
	
	/** Reverse previous (not this) step if oscillation found. Thus the state should be like it was before.
	 *  This has to be called before storeTheOscillationData.
	 * */
	private void reversePreviousStep() {
		// First reverse the weights
		for (int iiGradient = 0; iiGradient < m_iPrevDimension.length; iiGradient++) {	
			m_weights.set(m_iPrevDimension[iiGradient], m_weights.get(m_iPrevDimension[iiGradient]).doubleValue()
					- m_prevChange[iiGradient]);
		}
	
		// Compute the gradients again. This should be done so often that this costs very much. Thus we
		// Can compute them again from scratch.
		m_GDProbl.initGradients(m_weights);
		
		// Empty the oscillation control parameters (previous step was never done)
		m_prevChange = null;
		m_iPrevDimension = null;
	}

	
	/** Stores the used gradients for oscillation detection later */
	private void storeGradientsForOscillation(int[] maxGradients) {
		m_iNewDimension = maxGradients.clone();
		m_newChange = new double[maxGradients.length];
	}
	
	/** Oscillation is detected if same dimension is changed such that
	 * 	the direction is different and the new step is bigger than previous.
	 *  @param iiNewChange The index of MaxGradients/m_iNewDimension we are going on.
	 *  @param valueChange How much the weight is going to be changed
	 * @return If oscillation was detected.*/
	private boolean detectOscillation(int iiNewChange, double valueChange) {
		boolean detectOscillation = false;
		
		// Store the value for the change, dimensions have already been stored
		m_newChange[iiNewChange] = valueChange;
	
		// Go through the previous changes and check if some of them is for same dimension
		for (int iiPrevChange = 0; m_prevChange != null && iiPrevChange < m_prevChange.length; iiPrevChange++) {

			// Is the modified dimension same as before 
			if (m_iPrevDimension[iiPrevChange] == m_iNewDimension[iiNewChange])
			{
				//Does the change tell us about oscillation

				if (valueChange*m_prevChange[iiPrevChange] < 0 &&
						Math.abs(valueChange) > Math.abs(m_prevChange[iiPrevChange])) {
//					System.err.print("\nLikely oscillation detected in GDAlg.\n The dimension " 
//							+ m_iPrevDimension[iiPrevChange] 
//							+ " was changed consequently with change values: " + m_prevChange[iiPrevChange]
//                          + " and " + valueChange + ".\n");

					// How much we need step size reduction 
					double needReduction = Math.abs(m_prevChange[iiPrevChange])/Math.abs(valueChange);
					
					// If the reduction is greater than any of the previous on this iteration, do it.
					if (needReduction < m_minStepSizeReduction)
						m_minStepSizeReduction = needReduction;
					
					detectOscillation = true;
					//System.exit(1);
				} 
				
				break; // The dimension was found. do not check the latter ones.
			}
		}
		
		return detectOscillation;
	}


	/** Print the current weights output file. */
	public void OutputLog(int iterNro, PrintWriter wrt) {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		wrt.print("Iteration " + iterNro + ": ");
		for (int i = 0; i < m_weights.size(); i++) {
			wrt.print(fr.format((double)m_weights.get(i))+"\t");
		}
		wrt.print("\n");
	}

}
