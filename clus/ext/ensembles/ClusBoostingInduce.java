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

package clus.ext.ensembles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import jeans.util.array.MDoubleArray;

import clus.algo.*;
import clus.algo.tdidt.*;
import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.*;
import clus.main.*;
import clus.model.*;
import clus.statistic.ClusStatistic;
import clus.util.*;

// Method based on:
// "Improving Regressors using Boosting Techniques" by Harris Drucker

public class ClusBoostingInduce extends ClusInductionAlgorithm {

	Random m_Random = new Random(0);
	
	public ClusBoostingInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		super(schema, sett);
	}
	
	public double[] computeNormalizedLoss(RowData trainData, ClusNode tree) {
		ClusAttributeWeights weights = getStatManager().getClusteringWeights();
		double[] L = new double[trainData.getNbRows()];
		for (int i = 0; i < trainData.getNbRows(); i++) {
			DataTuple tuple = trainData.getTuple(i);
			ClusStatistic prediction = tree.predictWeighted(tuple);
			L[i] = prediction.getSquaredDistance(tuple, weights);			
		}
		double D = MDoubleArray.max(L);
		MDoubleArray.dotscalar(L, 1.0/D);
		return L;
	}
	
	public double computeAverageLoss(RowData trainData, double[] L) {
		double avg = 0.0;
		double tot_w = trainData.getSumWeights();
		for (int i = 0; i < trainData.getNbRows(); i++) {
			DataTuple tuple = trainData.getTuple(i);
			avg += L[i] * tuple.getWeight() / tot_w;
		}
		return avg;
	}
	
	public void updateWeights(RowData trainData, double[] L, double beta) {
		for (int i = 0; i < trainData.getNbRows(); i++) {
			DataTuple tuple = trainData.getTuple(i);
			tuple.setWeight(tuple.getWeight() * Math.pow(beta, 1 - L[i]));			
		}
	}
		
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException,	IOException {
		ClusForest result = new ClusForest(getStatManager());		
		RowData trainData = ((RowData)cr.getTrainingSet()).shallowCloneData();		
		DepthFirstInduce tdidt = new DepthFirstInduce(this);
		for (int i = 0; i < 50; i++) {
			System.out.println();
			System.out.println("Tree: "+i);
			RowData train = trainData.sampleWeighted(m_Random);		
			ClusNode tree = tdidt.induceSingleUnpruned(train);		
			double[] L = computeNormalizedLoss(trainData, tree);
			double Lbar = computeAverageLoss(trainData, L);
			double beta = Lbar / (1-Lbar);
			System.out.println("Average loss: "+Lbar+" beta: "+beta);		
			updateWeights(trainData, L, beta);
			result.addModelToForest(tree);
		}		
		return result;
	}
}
