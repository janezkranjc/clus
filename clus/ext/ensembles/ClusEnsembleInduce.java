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

import jeans.io.ini.INIFileNominalOrIntOrVector;
import jeans.resource.ResourceInfo;

import clus.Clus;
import clus.algo.*;
import clus.algo.tdidt.*;
import clus.main.*;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.model.modelio.ClusModelCollectionIO;
import clus.data.rows.DataTuple;
import clus.data.rows.TupleIterator;
import clus.data.type.*;
import clus.error.ClusErrorList;
import clus.ext.hierarchical.WHTDStatistic;
import clus.selection.BaggingSelection;
import clus.statistic.ClusStatistic;
import clus.util.ClusException;
import clus.util.ClusRandom;

public class ClusEnsembleInduce extends ClusInductionAlgorithm {
	Clus m_BagClus;
	static ClusAttrType[] m_RandomSubspaces;
	ClusForest m_OForest;//Forest with the original models
	ClusForest m_DForest;//Forest with stumps (default models)

	//memory optimization
	static boolean m_OptMode;
	static int[] m_HashCodeTuple;
	static double[][] m_AvgPredictions;
	
	//output ensemble at different values
	int[] m_OutEnsembleAt;//sorted values (ascending)!
	static int m_NbMaxBags;
	
	public ClusEnsembleInduce(ClusSchema schema, Settings sett, Clus clus) throws ClusException, IOException {
		super(schema, sett);
		m_BagClus = clus;
		//optimize if train/test and HMC
		m_OptMode = (!Settings.IS_XVAL && m_BagClus.hasTestSet() && (getStatManager().getMode() == ClusStatManager.MODE_HIERARCHICAL));
//		m_OptMode = false;
		m_OutEnsembleAt = getIntermediateEnsembles(sett.getNbBaggingSets());
		m_NbMaxBags = m_OutEnsembleAt[m_OutEnsembleAt.length-1];		
	}
	
	public static boolean isOptimized(){
		return m_OptMode;
	}
	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		
		switch (cr.getStatManager().getSettings().getEnsembleMethod()){
		case 0: {//Bagging
			induceBagging(cr);
			break;
			}
		case 1: {//RForest
			induceBagging(cr);
			break;
			}
		case 2: {//RSubspaces
			induceSubspaces(cr);
			break;
			}
		case 3: {//Bagging Subspaces
			induceBaggingSubspaces(cr);
			break;
			}
		}
		
		postProcessForest(cr);
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException,
			IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void postProcessForest(ClusRun cr) throws ClusException{
		
		if (!m_OptMode){
			ClusModelInfo def_info = cr.addModelInfo(ClusModel.DEFAULT);
			def_info.setModel(m_DForest);
			def_info.setName("Default");
		}
		
		ClusModelInfo orig_info = cr.addModelInfo(ClusModel.ORIGINAL);
		orig_info.setModel(m_OForest);
		orig_info.setName("Original");
	
	//Application of Thresholds for HMC
	if (cr.getStatManager().getMode() == ClusStatManager.MODE_HIERARCHICAL){
			double[] thresholds = cr.getStatManager().getSettings().getClassificationThresholds().getDoubleVector();
			//setting the printing preferences in the HMC mode
			m_OForest.setPrintModels(Settings.isPrintEnsembleModels());
			m_DForest.setPrintModels(Settings.isPrintEnsembleModels());
			if (thresholds != null){
				for (int i = 0; i < thresholds.length; i++){
					ClusModelInfo pruned_info = cr.addModelInfo(ClusModel.PRUNED + i);
					ClusForest new_forest = m_OForest.cloneForestWithThreshold(thresholds[i]);
					new_forest.setPrintModels(Settings.isPrintEnsembleModels());
					pruned_info.setModel(new_forest);
					pruned_info.setName("T("+thresholds[i]+")");
				}
			}
		}	
	}

	public void induceSubspaces(ClusRun cr) throws ClusException, IOException {
		m_OForest = new ClusForest(getStatManager());
		m_DForest = new ClusForest(getStatManager());
		
		long start_time = ResourceInfo.getTime();
		TupleIterator train_iterator = cr.getTrainIter();
		TupleIterator test_iterator = cr.getTestSet().getIterator();
		
		if (m_OptMode){
//			initializeTupleHashCodes(train_iterator, test_iterator);
			initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows()+cr.getTestSet().getNbRows());
			initPredictions(m_OForest);
		}
		System.out.println("Memory And Time Optimization = "+m_OptMode);
		
		for (int i = 0; i < m_NbMaxBags; i++) {
			System.out.println("Bag: "+(i+1));
			ClusRun crSingle = new ClusRun(cr.getTrainingSet(), cr.getSummary());
			ClusEnsembleInduce.selectRandomSubspaces(cr.getStatManager().getSchema().getDescriptiveAttributes(), cr.getStatManager().getSettings().getNbRandomAttrSelected());
			DepthFirstInduce ind = new DepthFirstInduce(this);
			ind.initialize();
			crSingle.getStatManager().initClusteringWeights();
			ClusModel model = ind.induceSingleUnpruned(crSingle);
			
//			ClusModelInfo model_info = crSingle.addModelInfo(ClusModels.ORIGINAL);
//			model_info.setModel(model);	
//			model_info.setName("Original");
//			m_OForest.addModelToForest(crSingle.getModel(ClusModels.ORIGINAL));
			
			if (m_OptMode){
				//for i == 0 [i.e. the first run] it will initialize the predictions
				if (i == 0) initModelPredictionForTuples(model, train_iterator, test_iterator);
				else addModelPredictionForTuples(model, train_iterator, test_iterator, i+1);
			}
			else{
				ClusModelInfo model_info = crSingle.addModelInfo(ClusModel.ORIGINAL);
				model_info.setModel(model);	
				model_info.setName("Original");
				m_OForest.addModelToForest(crSingle.getModel(ClusModel.ORIGINAL));
				
				ClusModel defmod = ClusDecisionTree.induceDefault(crSingle);		
				ClusModelInfo def_info = crSingle.addModelInfo(ClusModel.DEFAULT);
				def_info.setModel(defmod);
				def_info.setName("Default");
				m_DForest.addModelToForest(crSingle.getModel(ClusModel.DEFAULT));
			}
			
			//Valid only when test set is supplied
			if (m_OptMode && ((i+1) != m_NbMaxBags) && checkToOutEnsemble(i+1)){
				long done_time = ResourceInfo.getTime();
				crSingle.setInductionTime(done_time-start_time);
				postProcessForest(crSingle);
				crSingle.setTestSet(cr.getTestIter());
				crSingle.setTrainSet(cr.getTrainIter());
				outputBetweenForest(crSingle, m_BagClus, "_"+(i+1)+"_");
			}
		}
	}
	
	public void induceBagging(ClusRun cr) throws ClusException, IOException {
		int nbrows = cr.getTrainingSet().getNbRows();
		m_OForest = new ClusForest(getStatManager());
		m_DForest = new ClusForest(getStatManager());

		long start_time = ResourceInfo.getTime();
		TupleIterator train_iterator = cr.getTrainIter();
		TupleIterator test_iterator = cr.getTestSet().getIterator();
		
		if (m_OptMode){
//			initializeTupleHashCodes(train_iterator, test_iterator);
			initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows()+cr.getTestSet().getNbRows());
			initPredictions(m_OForest);
		}
		System.out.println("Memory And Time Optimization = "+m_OptMode);
		
		for (int i = 0; i < m_NbMaxBags; i++) {
			System.out.println("Bag: "+(i+1));
			BaggingSelection msel = new BaggingSelection(nbrows);
			ClusRun crSingle = m_BagClus.partitionDataBasic(cr.getTrainingSet(),msel,cr.getSummary(),i + 1);
			DepthFirstInduce ind = new DepthFirstInduce(this);
			ind.initialize();
			crSingle.getStatManager().initClusteringWeights();
			ClusModel model = ind.induceSingleUnpruned(crSingle);
//			ClusModelInfo model_info = crSingle.addModelInfo(ClusModels.ORIGINAL);
//			model_info.setModel(model);	
//			model_info.setName("Original");
//			m_OForest.addModelToForest(crSingle.getModel(ClusModels.ORIGINAL));
			
			if (m_OptMode){
				//for i == 0 [i.e. the first run] it will initialize the predictions
				if (i == 0) initModelPredictionForTuples(model, train_iterator, test_iterator);
				else addModelPredictionForTuples(model, train_iterator, test_iterator, i+1);
			}
			else{
				ClusModelInfo model_info = crSingle.addModelInfo(ClusModel.ORIGINAL);
				model_info.setModel(model);	
				model_info.setName("Original");
				m_OForest.addModelToForest(crSingle.getModel(ClusModel.ORIGINAL));
				
				ClusModel defmod = ClusDecisionTree.induceDefault(crSingle);		
				ClusModelInfo def_info = crSingle.addModelInfo(ClusModel.DEFAULT);
				def_info.setModel(defmod);
				def_info.setName("Default");
				m_DForest.addModelToForest(crSingle.getModel(ClusModel.DEFAULT));
			}
			
			//Valid only when test set is supplied
			if (m_OptMode && ((i+1) != m_NbMaxBags) && checkToOutEnsemble(i+1)){
				long done_time = ResourceInfo.getTime();
				crSingle.setInductionTime(done_time-start_time);
				postProcessForest(crSingle);
				crSingle.setTestSet(cr.getTestIter());
				crSingle.setTrainSet(cr.getTrainIter());
				outputBetweenForest(crSingle, m_BagClus, "_"+(i+1)+"_");
			}
		}
	}
	
	
	public void induceBaggingSubspaces(ClusRun cr) throws ClusException, IOException {
		int nbrows = cr.getTrainingSet().getNbRows();
		m_OForest = new ClusForest(getStatManager());
		m_DForest = new ClusForest(getStatManager());
		
		long start_time = ResourceInfo.getTime();
		TupleIterator train_iterator = cr.getTrainIter();
		TupleIterator test_iterator = cr.getTestSet().getIterator();
		
		if (m_OptMode){
//			initializeTupleHashCodes(train_iterator, test_iterator);
			initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows()+cr.getTestSet().getNbRows());
			initPredictions(m_OForest);
		}
		System.out.println("Memory And Time Optimization = "+m_OptMode);
		
		for (int i = 0; i < m_NbMaxBags; i++) {
			System.out.println("Bag: "+(i+1));
			BaggingSelection msel = new BaggingSelection(nbrows);
			ClusRun crSingle = m_BagClus.partitionDataBasic(cr.getTrainingSet(),msel,cr.getSummary(),i + 1);
			ClusEnsembleInduce.selectRandomSubspaces(cr.getStatManager().getSchema().getDescriptiveAttributes(), cr.getStatManager().getSettings().getNbRandomAttrSelected());
			DepthFirstInduce ind = new DepthFirstInduce(this);
			ind.initialize();
			crSingle.getStatManager().initClusteringWeights();
			ind.initializeHeuristic();
			ClusModel model = ind.induceSingleUnpruned(crSingle);
//			ClusModelInfo model_info = crSingle.addModelInfo(ClusModels.ORIGINAL);
//			model_info.setModel(model);	
//			model_info.setName("Original");
//			m_OForest.addModelToForest(crSingle.getModel(ClusModels.ORIGINAL));
			
			if (m_OptMode){
				//for i == 0 [i.e. the first run] it will initialize the predictions
				if (i == 0) initModelPredictionForTuples(model, train_iterator, test_iterator);
				else addModelPredictionForTuples(model, train_iterator, test_iterator, i+1);
			}
			else{
				ClusModelInfo model_info = crSingle.addModelInfo(ClusModel.ORIGINAL);
				model_info.setModel(model);	
				model_info.setName("Original");
				m_OForest.addModelToForest(crSingle.getModel(ClusModel.ORIGINAL));
				
				ClusModel defmod = ClusDecisionTree.induceDefault(crSingle);		
				ClusModelInfo def_info = crSingle.addModelInfo(ClusModel.DEFAULT);
				def_info.setModel(defmod);
				def_info.setName("Default");
				m_DForest.addModelToForest(crSingle.getModel(ClusModel.DEFAULT));
			}
			
			//Valid only when test set is supplied
			if (m_OptMode && ((i+1) != m_NbMaxBags) && checkToOutEnsemble(i+1)){
				long done_time = ResourceInfo.getTime();
				crSingle.setInductionTime(done_time-start_time);
				postProcessForest(crSingle);
				crSingle.setTestSet(cr.getTestIter());
				crSingle.setTrainSet(cr.getTrainIter());
				outputBetweenForest(crSingle, m_BagClus, "_"+(i+1)+"_");
			}
		}
	}
	
	public ClusForest getOriginalForest(){
		if (m_OForest != null)return m_OForest;
		else {
			System.err.println(getClass().getName()+" getForest(): Original Forest is Not created Yet");
			return null;
		}
	}

	public ClusForest getDefaultForest(){
		if (m_DForest != null)return m_DForest;
		else {
			System.err.println(getClass().getName()+" getForest(): Default Forest is Not created Yet");
			return null;
		}
	}
	
	public void initializeTupleHashCodes(TupleIterator train, TupleIterator test, int nb_tuples) throws IOException, ClusException{
		m_HashCodeTuple = new int[nb_tuples];
		int count = 0;
		train.init();
		DataTuple train_tuple = train.readTuple();
		while (train_tuple != null){
			m_HashCodeTuple[count] = train_tuple.hashCode();
			count++;
			train_tuple = train.readTuple();
		}
		test.init();
		DataTuple test_tuple = test.readTuple();
		while (test_tuple != null){
			m_HashCodeTuple[count] = test_tuple.hashCode();
			count++;
			test_tuple = test.readTuple();
		}
	}
	
	public void initializeTupleHashCodes(TupleIterator train, TupleIterator test) throws IOException, ClusException{
		ArrayList<Integer> tuple_hash = new ArrayList<Integer>();
		train.init();
		DataTuple train_tuple = train.readTuple();
		while (train_tuple != null){
			tuple_hash.add(train_tuple.hashCode());
			train_tuple = train.readTuple();
		}
		train.init();
		test.init();//restart the iterator
		DataTuple test_tuple = test.readTuple();
		while (test_tuple != null){
			tuple_hash.add(test_tuple.hashCode());
			test_tuple = test.readTuple();
		}
		test.init();//restart the iterator
		int nb_tuples = tuple_hash.size();
		m_HashCodeTuple = new int[nb_tuples];
		for (int k = 0; k < nb_tuples; k++)
			m_HashCodeTuple[k] = tuple_hash.get(k);
	}
	
	public void initPredictions(ClusForest forest){
		ClusEnsembleInduce.m_AvgPredictions = new double[ClusEnsembleInduce.m_HashCodeTuple.length][forest.getStat().getNbAttributes()];	
	}
	
	public void initModelPredictionForTuples(ClusModel model, TupleIterator train, TupleIterator test) throws IOException, ClusException{		
		train.init();
		DataTuple train_tuple = train.readTuple();
		while (train_tuple != null){
			int position = locateTuple(train_tuple);
			WHTDStatistic stat = (WHTDStatistic) model.predictWeighted(train_tuple);
			ClusEnsembleInduce.m_AvgPredictions[position] = stat.getNumericPred();
			train_tuple = train.readTuple();
		}
		train.init();
		test.init();
		DataTuple test_tuple = test.readTuple();
		while (test_tuple != null){
			int position = locateTuple(test_tuple);
			WHTDStatistic stat = (WHTDStatistic) model.predictWeighted(test_tuple);
			ClusEnsembleInduce.m_AvgPredictions[position] = stat.getNumericPred();
			test_tuple = test.readTuple();
		}
		test.init();
	}	
	
	public void addModelPredictionForTuples(ClusModel model, TupleIterator train, TupleIterator test, int nb_models) throws IOException, ClusException{
		train.init();
		DataTuple train_tuple = train.readTuple();
		while (train_tuple != null){
			int position = locateTuple(train_tuple);
			WHTDStatistic stat = (WHTDStatistic) model.predictWeighted(train_tuple);
			ClusEnsembleInduce.m_AvgPredictions[position] = incrementPredictions(ClusEnsembleInduce.m_AvgPredictions[position], stat.getNumericPred(), nb_models);
			train_tuple = train.readTuple();
		}
		train.init();
		test.init();
		DataTuple test_tuple = test.readTuple();
		while (test_tuple != null){
			int position = locateTuple(test_tuple);
			WHTDStatistic stat = (WHTDStatistic) model.predictWeighted(test_tuple);
			ClusEnsembleInduce.m_AvgPredictions[position] = incrementPredictions(ClusEnsembleInduce.m_AvgPredictions[position], stat.getNumericPred(), nb_models);
			test_tuple = test.readTuple();
		}
		test.init();
		
	}
	
	public static int locateTuple(DataTuple tuple){
		int position = -1;
		boolean found  = false;
		int i = 0;
		//search for the tuple
		while (!found && i < ClusEnsembleInduce.m_HashCodeTuple.length){
			if (ClusEnsembleInduce.m_HashCodeTuple[i] == tuple.hashCode()) {
				position = i;
				found = true;
			}
			i++;
		}
		return position;
	}
	
	public double[] incrementPredictions(double[] avg_predictions, double[] predictions, int nb_models){
		//the current averages are stored in the avg_predictions
		int plength = avg_predictions.length;
		double[] result = new double[plength];
		for (int i = 0; i < plength; i++)
			result[i] = avg_predictions[i]+(predictions[i]-avg_predictions[i])/nb_models;
		return result;
	}
	
	public int[] getIntermediateEnsembles(INIFileNominalOrIntOrVector values){
		int[] result = new int[values.getVectorLength()];
		for (int i = 0; i < result.length; i++)
			result[i] = values.getInt(i);
		int swap;
		//sorting the intermediate ensembles
		for (int j = 0; j < result.length-1; j++)
			for (int k = j+1; k < result.length; k++)
				if (result[j] > result[k]){
					swap = result[j];
					result[j] = result[k];
					result[k] = swap;
				}
		return result;
	}	

	//Checks whether we reached a limit
	public boolean checkToOutEnsemble(int idx){
		for (int i = 0; i < m_OutEnsembleAt.length; i++)
			if  (m_OutEnsembleAt[i] == idx) return true;
		return false;	
	}
	
	public void outputBetweenForest(ClusRun cr, Clus cl, String addname)	throws IOException, ClusException {
		Settings sett = cr.getStatManager().getSettings();
		ClusSchema schema = cr.getStatManager().getSchema();
		ClusOutput output = new ClusOutput(sett.getAppName() + addname +".out", schema, sett);
		ClusSummary summary = cr.getSummary();

		ClusStatistic tr_stat = cr.getStatManager().createStatistic(ClusAttrType.ATTR_USE_ALL);
		cr.getTrainingSet().calcTotalStat(tr_stat);
		cr.getStatManager().setTrainSetStat(tr_stat);				

		cl.calcError(cr, null); // Calc error
		if (summary != null) {
			for (int i = ClusModel.ORIGINAL; i < cr.getNbModels(); i++) {
				ClusModelInfo summ_info = cr.getModelInfo(i);
				ClusErrorList test_err = summ_info.getTestError();
				summ_info.setTestError(test_err);
			}
		}
	
		cl.calcExtraTrainingSetErrors(cr);
		output.writeHeader();
		output.writeOutput(cr, true, true);
		output.close();
		cl.getClassifier().saveInformation(sett.getAppName());
		ClusModelCollectionIO io = new ClusModelCollectionIO();
		cl.saveModels(cr, io);
		io.save(cl.getSettings().getFileAbsolute(cr.getStatManager().getSettings().getAppName() + addname+".model"));	
		cr.deleteDataAndModels();
	}
	
	public static int getMaxNbBags(){
		return ClusEnsembleInduce.m_NbMaxBags;
	}
	
	public static void selectRandomSubspaces(ClusAttrType[] attrs, int select){
		int origsize = attrs.length;
		int[] samples = new int [origsize];
		int rnd;
		boolean randomize = true;
		int i = 0;
		while (randomize) {
			rnd = ClusRandom.nextInt(ClusRandom.RANDOM_SELECTION, origsize);
			if (samples[rnd] == 0) {
				samples[rnd]++;
				i++;
			}
			if ( i == select)
				randomize = false;
		}
		ClusAttrType[] result = new ClusAttrType[select];
		int res = 0;
		for (int k = 0; k < origsize; k++){
			if (samples[k] !=0){
				result[res] = attrs[k];
				res++;
			}
		}
//		System.out.println(java.util.Arrays.toString(samples));
		setRandomSubspaces(result);
	}
	
	public static ClusAttrType[] getRandomSubspaces(){
		return m_RandomSubspaces;
	}
	
	public static void setRandomSubspaces(ClusAttrType[] attrs){
		m_RandomSubspaces = attrs;
	}
	
	public static ClusAttrType[] selectAttributesForRandomForest(ClusAttrType[] attrs, int select){
		int origsize = attrs.length;
		int[] samples = new int [origsize];
		int rnd;
		boolean randomize = true;
		int i = 0;
		while (randomize) {
			rnd = ClusRandom.nextInt(ClusRandom.RANDOM_SELECTION, origsize);
			if (samples[rnd] == 0) {
				samples[rnd]++;
				i++;
			}
			if ( i == select)
				randomize = false;
		}
		ClusAttrType[] result = new ClusAttrType[select];
		int res = 0;
		for (int k = 0; k < origsize; k++){
			if (samples[k] !=0){
				result[res] = attrs[k];
				res++;
			}
		}
//		System.out.println(java.util.Arrays.toString(samples));
		return result;
	}
	
}
