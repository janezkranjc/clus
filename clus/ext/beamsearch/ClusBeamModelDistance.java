package clus.ext.beamsearch;

import java.util.ArrayList;

import clus.data.rows.*;
import clus.data.type.*;
import clus.main.*;
import clus.statistic.*;

public class ClusBeamModelDistance{

	RowData m_Data;
	int m_NbRows; //number of rows of the dataset
	static int m_NbTarget; //number of target attributes
	static boolean isNumeric = false;
	static boolean isNominal = false;
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
			if (num.length != 0) {
				isNumeric = true;
				m_NbTarget = num.length;
			}
			 else if (nom.length != 0){
				 isNominal = true;
				 m_NbTarget = nom.length;
			 }
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
	
	public static ArrayList getPredictionsDataSet(ClusModel model, RowData train){
		ClusStatistic stat;
		DataTuple tuple;
		ArrayList predictions = new ArrayList();
		double[] predictattr;// = new double[m_NbRows];
		for (int k = 0; k < m_NbTarget; k++){
			predictattr = new double[train.getNbRows()];
			for (int i = 0; i < (train.getNbRows()); i++){
				tuple = train.getTuple(i);
				stat = model.predictWeighted(tuple);
				if (isNumeric)	predictattr[i] = stat.getNumericPred()[k];
				else if (isNominal)	predictattr[i] = stat.getNominalPred()[k];
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
			if (max == min) return 0.0; // this is extreme case when all predictions are equal to 0
			result += Math.sqrt(resAttr/pred1val.length)/(max-min);
		}
		return result / pred1.size();
	}
	
	public void calculatePredictionDistances(ClusBeam beam,ClusBeamModel candidate){
		ArrayList arr = beam.toArray();
		ClusBeamModel beamModel1, beamModel2;
//		double[] predModel1, predModel2, predCandidate;
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
			dist = 1-(dist / beam.getCrWidth());
//			System.out.println("dist = "+dist);
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
//		double[] predModel1, predModel2;
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
	}
	
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
	 * @return Beam Similarity
	 */
	public static double calcBeamSimilarity(ArrayList beam, RowData data, boolean isNum){
		ArrayList predictions = new ArrayList();
		ClusBeamModel model;
		double result = 0.0;
		double dist;
		for (int i = 0; i < beam.size(); i++){
			model = (ClusBeamModel)beam.get(i);
			predictions.add(getPredictionsDataSet(model.getModel(), data));
		}
		for (int m = 0; m < predictions.size(); m++){
			dist = 0.0;
			for (int n = 0; n < predictions.size(); n++){
				if (isNum)dist += getDistanceNumeric((ArrayList)predictions.get(m), (ArrayList)predictions.get(n));
				else dist += getDistanceNominal((ArrayList)predictions.get(m), (ArrayList)predictions.get(n));
			}
			dist = 1 - (dist / beam.size());
			result += dist;
		}
		return result / beam.size();
	}
}
