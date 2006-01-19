package clus.algo.kNN;

import clus.main.*;
import clus.*;
import clus.algo.tdidt.*;

import clus.data.type.NominalAttrType;
import clus.data.type.NumericAttrType;
import clus.data.type.ClusAttrType;
import clus.data.rows.*;
import jeans.util.MyArray;
import clus.util.ClusException;
import clus.error.ClusErrorParent;
import clus.pruning.BottomUpPruningVSB;


/*import clus.algo.induce.*;
import clus.error.multiscore.*;*/

import java.io.IOException;

/**
 * This class represents a classifier for the Clus program that
 * combines kNN and Decision Trees
 */
public class KNNTreeClassifier extends ClusClassifier {

	public KNNTreeClassifier(Clus clus) {
		super(clus);
	}

	/**
	 * Prints some info about this classifier to standard output.
	 */
	public void printInfo(){
		System.out.println("Nearest Neighbour Decision Tree Classifier");
		System.out.println("# Neighbours : "+Settings.kNNT_k.getValue());
		String s = Settings.kNNT_distWeighted.getValue()?"yes":"no";
		System.out.println("Distance Weighted ? "+s);
		System.out.println("Vector Distance Measure used: "+Settings.kNNT_vectDist.getValue());
		s = Settings.kNN_normalized.getValue()?"yes":"no";
		System.out.println("Attribute Normalizing used: "+s);
		s = Settings.kNN_attrWeighted.getValue()?"yes":"no";
		System.out.println("Separate Attribute Weights used: "+s);
	}
	/**
	 * Normally should induce the given data, but
	 * there is nothing to induce in a nearest neightbour classifier.
	 */
	public ClusModel induceSingle(ClusRun cr) throws ClusException {
		// First make normal decision tree
		ClusNode orig = getInduce().induce(cr, m_Clus.getScore());

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


		// Next transform decision tree into a kNN-tree
		KNNTree tree = KNNTree.makeTree(cr,orig,vd);
		storeDataInTree((RowData) cr.getTrainingSet(),tree);

		return tree;
	}

	/**
	 * Normally should induce the given data, but
	 * there is nothing to induce in a nearest neightbour classifier.
	 */
	public void induce(ClusRun cr) throws ClusException {

		// First make normal decision tree
		ClusNode orig = getInduce().induce(cr, m_Clus.getScore());

		// We store the original decision tree
		// for easy comparison of results.
		cr.getModelInfo(ClusModels.ORIGINAL).setModel(orig);


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



		// Next transform decision tree into a kNN-tree
		KNNTree tree = KNNTree.makeTree(cr,orig,vd);

		//Store the data into the correct leafs of the tree
		storeDataInTree(trainData,tree);

		// Store this new tree
		cr.getModelInfo(ClusModels.KNN_ORIGINAL).setModel(tree);

		//try to prune the tree
		//(of course) first see if package clus.pruning is wanted
		double vsb = m_Clus.getSettings().getPruneProportion();
		if (vsb > 0.0){
			// next make a clone of the tree.
			KNNTree pruned = (KNNTree) tree.cloneTree();
			//Prepare a pruner for the tree
			ClusErrorParent error_parent = cr.getStatManager().createEvalError();
			RowData pruneset = (RowData) cr.getPruneSet();
			BottomUpPruningVSB pruner = new BottomUpPruningVSB(error_parent,pruneset);
			//prune the cloned tree
			pruner.prune(pruned);
			//Store the pruned tree
			cr.getModelInfo(ClusModels.KNN_PRUNED).setModel(pruned);
		}

		// Also store a default prediction tree for comparison
		ClusModel defmodel = ClusDecisionTree.induceDefault(cr);
		cr.getModelInfo(ClusModels.DEFAULT).setModel(defmodel);

	}

	public void initializeSummary(ClusSummary summ) {
		NominalBasicDistance nomDist = new NominalBasicDistance();
		NumericalBasicDistance numDist = new NumericalBasicDistance();

		ClusModels.DEFAULT = summ.addModel("Default");
		ClusModels.ORIGINAL = summ.addModel("Decision Tree");
		ClusModels.KNN_ORIGINAL = summ.addModel("KNNTree");
		//see if package clus.pruning wanted
		double vsb = m_Clus.getSettings().getPruneProportion();
		if (vsb > 0.0){
			ClusModels.KNN_PRUNED = summ.addModel("Pruned KNNTree");
		}

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

	// Stores all training examples in the correct place in the tree.
	private void storeDataInTree(RowData data, KNNTree tree){
		try{
			StoreExampleProcessor p = new StoreExampleProcessor();
			MyArray a = new MyArray();
			a.addElement(p);

			TupleIterator iter = data.getIterator();
			DataTuple tuple = iter.readTuple();
			while (tuple != null) {
				tree.applyModelProcessors(tuple, a);
				//a.modelDone();
				tuple = iter.readTuple();
			}
			iter.close();
		} catch (IOException ioe){
			System.err.println("IOException occurred:"+ioe.getMessage());
		} catch (ClusException ce){
			System.err.println("ClusException occurred:"+ce.getMessage());
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
		for (int j = 0; j < attrs.length; j++){
			weights[j] = 1 - weights[j]/nbr;
		}

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

}
