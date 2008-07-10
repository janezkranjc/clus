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

package clus.ext.beamsearch;


import java.util.ArrayList;

import clus.data.rows.*;
import clus.main.*;
import clus.model.ClusModel;
import clus.statistic.*;


public class ClusBeamModelDistance{

	RowData m_Data;
	int m_NbRows; //number of rows of the dataset
	static int m_NbTarget; //number of target attributes
	boolean isNumeric = false;
	boolean isNominal = false;
	boolean isStatInitialized = false;
	boolean isBeamUpdated = false; 
	
	public ClusBeamModelDistance(ClusRun run, ClusBeam beam){
		m_Data = (RowData)run.getTrainingSet();
		if (m_Data == null){
			System.err.println(getClass().getName()+": ClusBeamTreeDistance(): Error while reading the train data");
			System.exit(1);
		}
		m_NbRows = m_Data.getNbRows();
		setStatType(run.getStatManager());
		fillBeamWithPredictions(beam);
	}
	
	public void setStatType(ClusStatManager mngr){
		if (mngr.getMode() == ClusStatManager.MODE_CLASSIFY)
			isNominal = true;
		else if (mngr.getMode() == ClusStatManager.MODE_REGRESSION)
			isNumeric = true;
		else {
			System.err.println(getClass().getName()+": initializeStat(): Unsupported Target Variables");
			System.exit(1);			
		}
		isStatInitialized = true;
	}
	
	public void fillBeamWithPredictions(ClusBeam beam){
		ArrayList arr = beam.toArray();
		ClusBeamModel model;
		for (int k = 0; k < arr.size(); k++){
			model = (ClusBeamModel)arr.get(k);
			model.setModelPredictions(getPredictions(model.getModel()));
		}
	}
	
	//this method should be optimized
	public ArrayList getPredictions(ClusModel model){
		ClusStatistic stat;
		DataTuple tuple;
		ArrayList predictions = new ArrayList();
		double[] predictattr;// = new double[m_NbRows];
		for (int k = 0; k < m_NbTarget; k++){
			predictattr = new double[m_NbRows];
			for (int i = 0; i < (m_NbRows); i++){
				tuple = m_Data.getTuple(i);
				stat = model.predictWeighted(tuple);
				if (isNumeric)	predictattr[i] = stat.getNumericPred()[k];
				else if (isNominal)	predictattr[i] = stat.getNominalPred()[k];
			}
			predictions.add(predictattr);
		}
		return predictions;
	}
	
	public static ArrayList getPredictionsDataSet(ClusModel model, RowData train, boolean isNum){
		ClusStatistic stat;
		DataTuple tuple;
		ArrayList predictions = new ArrayList();
		double[] predictattr;// = new double[m_NbRows];
		int nb_target = train.getSchema().getNbNumericTargetAttributes() + train.getSchema().getNbNominalTargetAttributes();
		for (int k = 0; k < nb_target; k++){
			predictattr = new double[train.getNbRows()];
			for (int i = 0; i < (train.getNbRows()); i++){
				tuple = train.getTuple(i);
				stat = model.predictWeighted(tuple);
				if (isNum)	predictattr[i] = stat.getNumericPred()[k];
				else predictattr[i] = stat.getNominalPred()[k];
			}
			predictions.add(predictattr);
		}
		return predictions;
	}
	
	/**Dragi
	 * For nominal target attributes
	 * @param pred1 - predictions for model 1
	 * @param pred2 - predictions for model 2
	 * @return square root from mean distance between the predictions
	 */
	public static double getDistanceNominal(ArrayList pred1, ArrayList pred2){
		double result = 0.0;
		double resAttr;
		double[] pred1val;
		double[] pred2val;
		for (int k = 0; k < pred1.size(); k++){
			resAttr = 0.0;
			pred1val = (double[])pred1.get(k);
			pred2val = (double[])pred2.get(k);
			for (int i=0; i < pred1val.length; i++)
				if (pred1val[i] != pred2val[i]) 
					resAttr++;
			result += Math.sqrt(resAttr / pred1val.length);
		}
		return result / pred1.size();
	}
	
	
	/**Dragi
	 * For numeric target attributes
	 * This method works only if the values of the target attribute are non-negative.
	 * @param pred1 - predictions for model 1
	 * @param pred2 - predictions for model 2
	 * @return normalized square root from mean squared distance between the predictions
	 */
	public static double getDistanceNumeric(ArrayList pred1, ArrayList pred2){
		double result = 0.0;
		double resAttr;// = 0.0;
		double max;// = Double.NEGATIVE_INFINITY;
		double min;// = Double.POSITIVE_INFINITY;
		double[] pred1val;
		double[] pred2val;
		for (int k = 0; k < pred1.size(); k++){
			max = Double.NEGATIVE_INFINITY;
			min = Double.POSITIVE_INFINITY;
			pred1val = (double[])pred1.get(k);
			pred2val = (double[])pred2.get(k);
			resAttr = 0.0;
			for (int i=0; i < pred1val.length; i++){
				if (pred1val[i] > max) max = pred1val[i];
				if (pred2val[i] > max) max = pred2val[i];
				if (pred1val[i] < min) min = pred1val[i];
				if (pred2val[i] < min) min = pred2val[i];
				resAttr +=Math.pow((pred1val[i]-pred2val[i]), 2);	
			}
			if (max == min) return 0.0; // this is extreme case when all predictions are equal
			result += Math.sqrt(resAttr/pred1val.length)/(max-min);
		}
		return result / pred1.size();
	}
	
	public void calculatePredictionDistances(ClusBeam beam,ClusBeamModel candidate){
		ArrayList arr = beam.toArray();
		ClusBeamModel beamModel1, beamModel2;
		ArrayList predModel1, predModel2, predCandidate;
		predCandidate = candidate.getModelPredictions();
		double dist; // the average distance of each model of the beam to the other beam members + the candidate model
		double candidateDist = 0.0; //the average distance of the candidate model to all beam members
		for (int i = 0; i < arr.size(); i++){
			beamModel1 = (ClusBeamModel)arr.get(i);
			predModel1 = beamModel1.getModelPredictions();
			dist = 0.0;
			for (int j = 0; j < arr.size(); j++){
				beamModel2 = (ClusBeamModel) arr.get(j);
				predModel2 = beamModel2.getModelPredictions();
				if (isNumeric) 
					dist += getDistanceNumeric(predModel1, predModel2);
				else if (isNominal)
					dist += getDistanceNominal(predModel1, predModel2);
			}
			
			if (isNumeric) {
				dist += getDistanceNumeric(predModel1, predCandidate);
				candidateDist += getDistanceNumeric(predModel1, predCandidate);
			}
			else if (isNominal){	
				dist += getDistanceNominal(predModel1, predCandidate);
				candidateDist += getDistanceNominal(predModel1, predCandidate);
			}
//			dist = 1-(dist / beam.getCrWidth());
			beamModel1.setDistanceToBeam(dist);
		}
//		candidateDist = 1 - (candidateDist / beam.getCrWidth());
//		similarity = 1 - average(distance)
		candidate.setDistanceToBeam(candidateDist);
	}
	
	public void calculatePredictionDistancesOpt(ClusBeam beam,ClusBeamModel candidate){
		ArrayList arr = beam.toArray();
		int size = arr.size();
		ClusBeamModel beamModel1, beamModel2;
		ArrayList predModel1, predModel2, predCandidate;
		predCandidate = candidate.getModelPredictions();
		double candidateDist = 0.0; //the average distance of the candidate model to all beam members
		double[] tempDist = new double[size];
		double temp = 0.0;
		for (int i = 0; i < (size-1); i++){
			beamModel1 = (ClusBeamModel)arr.get(i);
			predModel1 = beamModel1.getModelPredictions();
			for (int j = i+1; j < size; j++){
				beamModel2 = (ClusBeamModel) arr.get(j);
				predModel2 = beamModel2.getModelPredictions();
				if (isNumeric) {
					temp = getDistanceNumeric(predModel1, predModel2);
				}
				else if (isNominal){
					temp = getDistanceNominal(predModel1, predModel2);
				}
				tempDist[i] += temp;
				tempDist[j] += temp;
			}
			if (isNumeric) {
				temp = getDistanceNumeric(predModel1, predCandidate);
			}
			else if (isNominal){	
				temp = getDistanceNominal(predModel1, predCandidate);
			}
			tempDist [i] += temp; 
			candidateDist += temp;
			beamModel1.setDistanceToBeam(tempDist[i]);
		}
		if (isNumeric) {
			temp = getDistanceNumeric(((ClusBeamModel)arr.get(size-1)).getModelPredictions(), predCandidate);
		}
		else if (isNominal){	
			temp = getDistanceNominal(((ClusBeamModel)arr.get(size-1)).getModelPredictions(), predCandidate);
		}
		tempDist [size-1] += temp;
		candidateDist += temp;
		((ClusBeamModel)arr.get(size-1)).setDistanceToBeam(tempDist[size-1]);
		candidate.setDistanceToBeam(candidateDist);
	}
	
	public void addDistToCandOpt(ClusBeam beam, ClusBeamModel candidate){
		ArrayList arr = beam.toArray();
		int size = arr.size();
		ClusBeamModel model;
		ArrayList candidatepredictions = candidate.getModelPredictions();
		double dist = 0.0;
		double candidatedist = 0.0;
		double distance;
		for (int i = 0; i < size; i++){
			model = (ClusBeamModel)arr.get(i);
			if (isNumeric){
				dist = getDistanceNumeric(model.getModelPredictions(), candidatepredictions);
			}
			else if (isNominal){
				dist = getDistanceNominal(model.getModelPredictions(), candidatepredictions);
			}
			candidatedist += dist;
			distance = model.getDistanceToBeam();
			distance += dist;
			model.setDistanceToBeam(distance);
		}
		candidate.setDistanceToBeam(candidatedist);
		
	}
	
	public void deductFromBeamOpt(ClusBeam beam, ClusBeamModel candidate, int position){
		ArrayList arr = beam.toArray();
		int size = arr.size();
		ArrayList candidatepredictions = candidate.getModelPredictions();
		ClusBeamModel model;
		double dist = 0.0;
		double distance;
		if (position == size){
			//the candidate does not enter the beam
			for (int i = 0; i < size; i++){
				model = (ClusBeamModel)arr.get(i);
				if (isNumeric){
					dist = getDistanceNumeric(model.getModelPredictions(), candidatepredictions); 	
				}
				else if (isNominal){
					dist = getDistanceNominal(model.getModelPredictions(), candidatepredictions);
				}
				distance = model.getDistanceToBeam();
				distance -= dist;
				model.setDistanceToBeam(distance);
			}
		}else{
			ClusBeamModel exitmodel = (ClusBeamModel)arr.get(position);
			ArrayList exitpredictions = exitmodel.getModelPredictions();
			for (int j = 0; j < size; j++){
				if (j != position){
					model = (ClusBeamModel)arr.get(j);
					if (isNumeric){
						dist = getDistanceNumeric(model.getModelPredictions(), exitpredictions);
					}else if (isNominal){
						dist = getDistanceNominal(model.getModelPredictions(),exitpredictions);
					}
					distance = model.getDistanceToBeam();
					distance -= dist;
					model.setDistanceToBeam(distance);
				}
			}
			if (isNumeric){
				dist = getDistanceNumeric(candidatepredictions, exitpredictions);
			}
			else if (isNominal){
				dist = getDistanceNominal(candidatepredictions, exitpredictions);
			}
			distance = candidate.getDistanceToBeam();
			distance -= dist;
			candidate.setDistanceToBeam(distance);
		}
	}
	
	
	
/*	*//**Dragi
	 * Updates the distances of each model to the other members of the beam
	 * and calculates the beam similarity
	 * @param beam
	 *//*
	public void updateDistancesWithinBeam(ClusBeam beam){
		ArrayList arr = beam.toArray();
		ClusBeamModel beamModel1, beamModel2;
		ArrayList predModel1, predModel2;
		double dist;// the average distance of each model of the beam to the other beam members
		double bsim = 0.0;//the average beam similarity
		for (int i = 0; i < arr.size(); i++){
			beamModel1 = (ClusBeamModel)arr.get(i);
			predModel1 = beamModel1.getModelPredictions();
			dist = 0.0;
			for (int j = 0; j < arr.size(); j++){
				beamModel2 = (ClusBeamModel) arr.get(j);
				predModel2 = beamModel2.getModelPredictions();
				if (isNumeric) 
					dist += getDistanceNumeric(predModel1, predModel2);
				else	
					dist += getDistanceNominal(predModel1, predModel2);
			}
			dist = 1-(dist / beam.getCrWidth());
//			similarity = 1 - average(distance)
			bsim += dist;
			beamModel1.setSimilarityWithBeam(dist);
		}
		beam.setBeamSimilarity(bsim/beam.m_CrWidth);
	}*/
	
	public boolean getIsBeamUpdated(){
		return isBeamUpdated;
	}
	
	public void setIsBeamUpdated(boolean update){
		isBeamUpdated = update;
	}
	
	/**Dragi
	 * Calculates BeamSimilarity for a given Data Set
	 * 
	 * @param beam
	 * @param data
	 * @param isNum
	 * @return
	 */
	public static double calcBeamSimilarity(ArrayList beam, RowData data, boolean isNum) {
		ArrayList predictions = new ArrayList();
//		ClusBeamModel model;
		double result = 0.0;
		double dist;
		for (int i = 0; i < beam.size(); i++){
			try{
				ClusBeamModel model = (ClusBeamModel)beam.get(i);
				predictions.add(getPredictionsDataSet(model.getModel(), data, isNum));
			}catch(ClassCastException e){
				ClusModel model = (ClusModel)beam.get(i);
				predictions.add(getPredictionsDataSet(model, data, isNum));
			}
		}
//		System.out.println("\tHeur\t\t\tSimilarity");
		for (int m = 0; m < predictions.size(); m++){
			dist = 0.0;
			for (int n = 0; n < predictions.size(); n++){
				if (isNum)dist += getDistanceNumeric((ArrayList)predictions.get(m), (ArrayList)predictions.get(n));
				else dist += getDistanceNominal((ArrayList)predictions.get(m), (ArrayList)predictions.get(n));
			}
			dist = 1 - (dist / beam.size());
//			System.out.println("Model "+m+": "+(-((ClusBeamModel)beam.get(m)).getValue())+"\t"+dist);
			result += dist;
		}
		return result / beam.size();
	}
	
	public void printDebugInfo(ClusBeam beam){
		ArrayList arr = beam.toArray();
		ClusBeamModel model;
		for (int i = 0; i < arr.size(); i++){
			model = (ClusBeamModel) arr.get(i);
			System.out.print(model.getDistanceToBeam()+"\t");
		}
		System.out.println();
	}
	
	/**Dragi
	 * Calculates the distance between a given model and the syntactic constraint
	 * @param model
	 * @param constraint
	 * @return Similarity between the candidate and the constraint
	 */
	public double getDistToConstraint(ClusBeamModel model, ClusBeamSyntacticConstraint constraint){
		if (isNumeric)
			return (1 - getDistanceNumeric(model.getModelPredictions(), constraint.getConstraintPredictions()));
		else
			return (1- getDistanceNominal(model.getModelPredictions(), constraint.getConstraintPredictions()));
	}
}
