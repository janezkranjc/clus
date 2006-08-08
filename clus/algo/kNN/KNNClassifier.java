package clus.algo.kNN;

import java.io.IOException;

import jeans.util.cmdline.CMDLineArgs;

import clus.main.*;
import clus.util.ClusException;
import clus.*;
import clus.algo.induce.ClusInduce;
import clus.algo.tdidt.*;
import clus.data.rows.RowData;
import clus.data.rows.DataTuple;

import clus.data.type.NominalAttrType;
import clus.data.type.ClusAttrType;
import clus.data.type.NumericAttrType;

/*import clus.algo.induce.*;
import clus.error.multiscore.*;*/

/**
 * This class represents a Nearest Neighbour classifier for the Clus program.
 */
public class KNNClassifier extends ClusClassifier {

	public KNNClassifier(Clus clus) {
		super(clus);
	}

	/**
	 * Prints some info about this classifier to standard output.
	 */
	public void printInfo(){
		System.out.println("Nearest Neighbour Classifier");
		System.out.println("# Neighbours : "+Settings.kNN_k.getValue());
		String s = Settings.kNN_distWeighted.getValue()?"yes":"no";
		System.out.println("Distance Weighted ? "+ s);
		System.out.println("Vector Distance Measure used: "+Settings.kNN_vectDist.getValue());
		s = Settings.kNN_normalized.getValue()?"yes":"no";
		System.out.println("Attribute Normalizing used: "+s);
		s = Settings.kNN_attrWeighted.getValue()?"yes":"no";
		System.out.println("Separate Attribute Weights used: "+s);
	}

	public ClusModel induceSingleUnpruned(ClusRun cr) {
		return null;
	}
	
	/**
	 * Normally should induce the given data, but
	 * there is nothing to induce in a nearest neightbour classifier.
	 */
	public ClusModel induceSingle(ClusRun cr){

		System.out.println("Calculating Statistical Measures...");
		// Calculate important measures of the trainingdata.
		RowData trainData = (RowData) cr.getTrainingSet();
		KNNStatistics stats = new KNNStatistics(trainData);
		System.out.println("Done.");

		//See if normalization and attribute weights are wanted
		double[] weights;
		if(Settings.kNN_attrWeighted.getValue() && Settings.kNN_normalized.getValue()){
			weights = calcWeights(stats,trainData);
		}
		else {
			weights = noWeights(trainData);
		}
		//Find out which distance to use
		ClusAttrType[] attrs = trainData.getSchema().getDescriptiveAttributes();
		VectorDistance vd;
		String d = Settings.kNN_vectDist.getValue();
		if (d.equals("Manhattan")){
			vd = new ManhattanDistance(attrs,weights);
		}
		else{
			vd = new EuclidianDistance(attrs,weights);
		}

		KNNModel model = new KNNModel(cr,vd);
		return model;
	}

	/**
	 * Normally should induce the given data, but
	 * there is nothing to induce in a nearest neightbour classifier.
	 */
	public void induce(ClusRun cr){

		System.out.println("Calculating Statistical Measures...");
		// Calculate important measures of the trainingdata.
		RowData trainData = (RowData) cr.getTrainingSet();
		KNNStatistics stats = new KNNStatistics(trainData);
		System.out.println("Done.");

		//See if normalization and attribute weights are wanted
		double[] weights;
		if(Settings.kNN_attrWeighted.getValue() && Settings.kNN_normalized.getValue()){
			weights = calcWeights(stats,trainData);
		}
		else {
			weights = noWeights(trainData);
		}


		//Find out which distance to use
		ClusAttrType[] attrs = trainData.getSchema().getDescriptiveAttributes();
		VectorDistance vd;
		String d = Settings.kNN_vectDist.getValue();
		if (d.equals("Manhattan")){
			vd = new ManhattanDistance(attrs,weights);
		}
		else{
			vd = new EuclidianDistance(attrs,weights);
		}


		KNNModel model = new KNNModel(cr,vd);



		cr.getModelInfo(ClusModels.ORIGINAL).setModel(model);
		ClusModel defmodel = ClusDecisionTree.induceDefault(cr);
		cr.getModelInfo(ClusModels.DEFAULT).setModel(defmodel);
	}

	public void initializeSummary(ClusSummary summ) {
		NominalBasicDistance nomDist = new NominalBasicDistance();
		NumericalBasicDistance numDist = new NumericalBasicDistance();
		ClusSchema schema = m_Clus.getSchema();
		ClusAttrType[] attrs = schema.getDescriptiveAttributes();
		for (int i = 0; i< attrs.length;i++){
			if (attrs[i].getTypeIndex() == NominalAttrType.THIS_TYPE) {
				attrs[i].setBasicDistance(nomDist);
			} else {
				attrs[i].setBasicDistance(numDist);
			}
		}

	}

	//used for calculating the attribute weights used in distance calculations
	private double[] calcWeights(KNNStatistics stats,RowData data){
		ClusAttrType[] attrs = data.getSchema().getDescriptiveAttributes();
		double[] weights = new double[attrs.length];
		int nbr = data.getNbRows();
		DataTuple curTup,curProto;
		int curTargetVal;
		double w_upd;

		NominalBasicDistance nomDist = new NominalBasicDistance();
		NumericalBasicDistance numDist = new NumericalBasicDistance();

		for (int i = 0; i <nbr;i++){
			curTup = data.getTuple(i);
			curTargetVal = curTup.getClassification();
			curProto = stats.getPrototype(curTargetVal);
			for (int j = 0; j < attrs.length; j++){

				if (attrs[j].getTypeIndex() == NominalAttrType.THIS_TYPE){
					w_upd = nomDist.getDistance(attrs[j],curTup,curProto);
					weights[j] = weights[j] + w_upd;
				}
				else if (attrs[j].getTypeIndex() == NumericAttrType.THIS_TYPE){
					w_upd = numDist.getDistance(attrs[j],curTup,curProto);
					weights[j] = weights[j] + w_upd;
				}
			}
		}
		System.out.println("Weights : ");
		for (int j = 0; j < attrs.length; j++){
			weights[j] = 1 - weights[j]/nbr;
			System.out.print(weights[j]+",");
		}
		System.out.println();
		return weights;
	}
	// Just creates a weight vector with all weights 1.0
	private double[] noWeights(RowData data){
		ClusAttrType[] attrs = data.getSchema().getDescriptiveAttributes();
		double[] weights = new double[attrs.length];
		for (int j = 0; j < weights.length; j++){
			weights[j]=1.0;
		}
		return weights;
	}
	
	public void pruneAll(ClusRun cr) throws ClusException, IOException {
	}
	 
	public ClusModel pruneSingle(ClusModel model, ClusRun cr) throws ClusException, IOException {
		return model;
	}
	
	public ClusInduce createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {
		return null;
	}
	
}
