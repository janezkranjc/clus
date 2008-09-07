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

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.*;
import clus.data.type.ClusAttrType;
import clus.data.type.NumericAttrType;
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
	static double[] m_NormCoefficients;//The squared distances are normalized only for regression. Here we keep stored 1/var of the targets...
	
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
		else if (mngr.getMode() == ClusStatManager.MODE_REGRESSION){
			isNumeric = true;
			initNormCoefficients(mngr);
		}
		else {
			System.err.println(getClass().getName()+": initializeStat(): Unsupported Target Variables");
			System.exit(1);			
		}
		m_NbTarget = mngr.getSchema().getNbNumericTargetAttributes()+mngr.getSchema().getNbNominalTargetAttributes();
	}

	public void initNormCoefficients(ClusStatManager mngr){//this is used only for regression
		NumericAttrType[] attrs = mngr.getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		ClusAttributeWeights caw = mngr.getNormalizationWeights();
		m_NormCoefficients = new double[attrs.length];
		for (int i = 0; i < attrs.length; i++)
			m_NormCoefficients[i] = caw.getWeight(attrs[i]);
	}
	
	public void fillBeamWithPredictions(ClusBeam beam){
		ArrayList arr = beam.toArray();
		ClusBeamModel model;
		for (int k = 0; k < arr.size(); k++){
			model = (ClusBeamModel)arr.get(k);
			model.setModelPredictions(getPredictions(model.getModel()));
		}
	}

	public ArrayList getPredictions(ClusModel model){
		ArrayList predictions = new ArrayList();
//		double[] predictattr;// = new double[m_NbRows];
		for (int i = 0; i < m_NbRows; i++){
			DataTuple tuple = m_Data.getTuple(i);
			ClusStatistic stat = model.predictWeighted(tuple);
			if (isNumeric){
				double[] tuple_predictions = stat.getNumericPred();
				predictions.add(tuple_predictions);
			}else {
				double[][] tuple_predictions = ((ClassificationStat)stat).getProbabilityPrediction();
				predictions.add(tuple_predictions);
			}
		}
		return predictions;
	}	
	
	public static ArrayList getPredictionsDataSet(ClusModel model, RowData train, boolean isNum){
		ArrayList predictions = new ArrayList();
		for (int i = 0; i < train.getNbRows(); i++){
			DataTuple tuple = train.getTuple(i);
			ClusStatistic stat = model.predictWeighted(tuple);
			if (isNum){
				double[] tuple_predictions = stat.getNumericPred();
				predictions.add(tuple_predictions);
			}else {
				double[][] tuple_predictions = ((ClassificationStat)stat).getProbabilityPrediction();
				predictions.add(tuple_predictions);
			}
		}
		return predictions;
	}

	/**Dragi
	 * For nominal target attributes
	 * @param pred1 - predictions for model 1
	 * @param pred2 - predictions for model 2
	 * @return averaged squared distances between the predictions
	 */
	public static double getDistanceNominal(ArrayList predictions1, ArrayList predictions2){
		double result = 0.0;
		double[] per_target = new double[m_NbTarget];
		for (int k = 0; k < predictions1.size(); k++){//for all tuples
			double[][] pred1val = (double[][])predictions1.get(k);
			double[][] pred2val = (double[][])predictions2.get(k);
			for (int i=0; i < pred1val.length; i++) //for each target
				per_target[i] += getSquaredDistance(pred1val[i], pred2val[i]);
//				result_attr += getSquaredDistance(pred1val[i], pred2val[i]);
//			result += result_attr / pred1val.length; //average per target
		}
		for (int j = 0; j < per_target.length; j++)
			result += per_target[j];
		result = result / per_target.length; //average per target
		return result / predictions1.size();//average per tuple
	}

	/**Dragi
	 * For numeric target attributes
	 * This method works only if the values of the target attribute are non-negative.
	 * @param pred1 - predictions for model 1
	 * @param pred2 - predictions for model 2
	 * @return normalized square root from mean squared distance between the predictions
	 */
	public static double getDistanceNumeric(ArrayList predictions1, ArrayList predictions2){
		double result = 0.0;
		double[] per_target = new double[m_NbTarget];
		for (int k = 0; k < predictions1.size(); k++){//for all tuples
			double[] pred1val = (double[])predictions1.get(k);
			double[] pred2val = (double[])predictions2.get(k);
			for (int i=0; i < pred1val.length; i++){ //for all targets
				per_target[i] += (pred1val[i]-pred2val[i])*(pred1val[i]-pred2val[i]);
			}
		}
		for (int j = 0; j < per_target.length; j++)
			result += per_target[j] * m_NormCoefficients[j];
		result = result / per_target.length; //average per target
		return result / predictions1.size();//average per tuple
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

//	public void printDebugInfo(ClusBeam beam){
//		ArrayList arr = beam.toArray();
//		ClusBeamModel model;
//		for (int i = 0; i < arr.size(); i++){
//			model = (ClusBeamModel) arr.get(i);
//			System.out.print(model.getDistanceToBeam()+"\t");
//		}
//		System.out.println();
//	}

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
	
	public final static double getSquaredDistance(double[] a, double[] b){
		double result = 0.0;
		for (int i = 0; i < a.length; i++)
			result += (a[i] - b[i]) * (a[i] - b[i]);
		return result;
	}
}
