package clus.ext.beamsearch;

import java.util.ArrayList;

import clus.data.rows.*;
import clus.data.type.*;
import clus.main.*;
import clus.statistic.*;

public class ClusBeamModelDistance{

	RowData m_Data;
	int m_NbRows; //number of rows of the dataset
	boolean isNumeric = false;
	boolean isNominal = false;
	boolean isStatInitialized = false;
	boolean isBeamUpdated = false; 
	
	public ClusBeamModelDistance(ClusRun run, ClusBeam beam){
		m_Data = (RowData)run.getTrainingSet();
		if (m_Data == null){
			System.err.println(getClass().getName()+": ClusBeamTreeDistance(): Error while reading the Data");
			System.exit(1);
		}
		m_NbRows = m_Data.getNbRows();
		setStatType(run);
		fillBeamWithPredictions(beam);
	}
	
	public void setStatType(ClusRun run){
			NumericAttrType[] num = run.getStatManager().getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
			NominalAttrType[] nom = run.getStatManager().getSchema().getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
			if (num.length != 0) isNumeric = true;
			 else if (nom.length != 0)isNominal = true;

			if (isNumeric && isNominal){
				System.err.println(getClass().getName()+": initializeStat(): Combined Heuristic not yet implemented");
				System.exit(1);
			}
			if (!isNumeric && !isNominal){
				System.err.println(getClass().getName()+": initializeStat(): Unsupported Target Variable");
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
	
	public double[] getPredictions(ClusModel model){
		ClusStatistic stat;
		DataTuple tuple;
		double[] predictions = new double[m_NbRows];
		for (int i = 0; i < (m_NbRows); i++){
			tuple = m_Data.getTuple(i);
			stat = model.predictWeighted(tuple);
			if (isNumeric)	predictions[i] = stat.getNumericPred()[0];
			else if (isNominal)	predictions[i] = stat.getNominalPred()[0];
		}
		return predictions;
	}
	
	/**Dragi
	 * For nominal target attributes
	 * @param pred1 - predictions for model 1
	 * @param pred2 - predictions for model 2
	 * @return quare root from mean distance between the predictions
	 */
	public double getDistanceNominal(double[] pred1, double[] pred2){
		double result = 0.0;
		if (pred1.length != pred2.length){
			System.err.println(getClass().getName()+": getDistanceNominal(): Error in the size of predictions");
			System.exit(1);
		}
		for (int i=0; i < pred1.length; i++)
			if (pred1[i] != pred2[i]) 
				result++;
		return Math.sqrt(result / m_NbRows);
	}
	
	
	/**Dragi
	 * For numeric target attributes
	 * This method works only if the values of the target attribute are non-negative.
	 * @param pred1 - predictions for model 1
	 * @param pred2 - predictions for model 2
	 * @return normalized square root from mean squared distance between the predictions
	 */
	public double getDistanceNumeric(double[] pred1, double[] pred2){
		double result = 0.0;
		if (pred1.length != pred2.length){
			System.err.println(getClass().getName()+": getDistanceNumeric(): Error in the size of predictions");
			System.exit(1);
		}
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;
		for (int i=0; i < pred1.length; i++){
			if (pred1[i] > max) max = pred1[i];
			if (pred2[i] > max) max = pred2[i];
			if (pred1[i] < min) min = pred1[i];
			if (pred2[i] < min) min = pred2[i];
//			max = Math.max(pred1[i], max);
//			max = Math.max(pred2[i], max);
//			min = Math.min(pred1[i], min);
//			min = Math.min(pred2[i], min);
			result +=Math.pow((pred1[i]-pred2[i]), 2);	
		}
		if (max == min) return 0.0; // this is extreme case when all predictions are equal to 0
		return Math.sqrt(result/m_NbRows)/(max-min);
	}
	
	public void calculatePredictionDistances(ClusBeam beam,ClusBeamModel candidate){
		ArrayList arr = beam.toArray();
		ClusBeamModel beamModel1, beamModel2;
		double[] predModel1, predModel2, predCandidate;
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
			dist = 1-(dist / beam.getCrWidth());
			beamModel1.setSimilarityWithBeam(dist);
		}
		candidateDist = 1 - (candidateDist / beam.getCrWidth());
//		similarity = 1 - average(distance)
		candidate.setSimilarityWithBeam(candidateDist);
	}
	
	/**Dragi
	 * Updates the distances of each model to the other members of the beam
	 * and calculates the beam similarity
	 * @param beam
	 */
	public void updateDistancesWithinBeam(ClusBeam beam){
		ArrayList arr = beam.toArray();
		ClusBeamModel beamModel1, beamModel2;
		double[] predModel1, predModel2;
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
	}
	
	public boolean getIsBeamUpdated(){
		return isBeamUpdated;
	}
	
	public void setIsBeamUpdated(boolean update){
		isBeamUpdated = update;
	}
}
