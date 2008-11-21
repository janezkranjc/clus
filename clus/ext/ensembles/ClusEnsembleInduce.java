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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import jeans.io.ini.INIFileNominalOrIntOrVector;
import jeans.resource.ResourceInfo;

import clus.Clus;
import clus.algo.*;
import clus.algo.split.FindBestTest;
import clus.algo.tdidt.*;
import clus.main.*;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.model.modelio.ClusModelCollectionIO;
import clus.model.processor.ModelProcessorCollection;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.rows.TupleIterator;
import clus.data.type.*;
import clus.error.Accuracy;
import clus.error.ClusErrorList;
import clus.error.MSError;
import clus.ext.hierarchical.WHTDStatistic;
import clus.selection.*;
import clus.statistic.ClassificationStat;
import clus.statistic.ClusStatistic;
import clus.statistic.RegressionStat;
import clus.util.ClusException;
import clus.util.ClusRandom;

public class ClusEnsembleInduce extends ClusInductionAlgorithm {
	Clus m_BagClus;
	static ClusAttrType[] m_RandomSubspaces;
	ClusForest m_OForest;//Forest with the original models
	ClusForest m_DForest;//Forest with stumps (default models)
	static int m_Mode;
	
	//memory optimization
	static boolean m_OptMode;
	static int[] m_HashCodeTuple;
	static double[][] m_AvgPredictions;

	//output ensemble at different values
	int[] m_OutEnsembleAt;//sorted values (ascending)!
	static int m_NbMaxBags;

	//OOB estimate for Bagging and Random Forests
	static boolean m_OOBEstimate;
	static HashMap m_OOBPredictions;
	static HashMap m_OOBUsage;
	static boolean m_OOBCalculation;

	//FeatureRanking
	HashMap m_AllAttributes;//key is the AttributeName, and the value is array with the order in the file and the rank
	boolean m_FeatRank;
	
	public ClusEnsembleInduce(ClusSchema schema, Settings sett, Clus clus) throws ClusException, IOException {
		super(schema, sett);
		initialize(schema, sett, clus);
	}
	
	public ClusEnsembleInduce(ClusInductionAlgorithm other, Clus clus) throws ClusException, IOException {
		super(other);
		initialize(getSchema(), getSettings(), clus);
	}

	public void initialize(ClusSchema schema, Settings sett, Clus clus) throws ClusException, IOException {
		m_BagClus = clus;
		m_Mode = getStatManager().getMode();
		//optimize if not XVAL and HMC
		m_OptMode = (Settings.shouldOptimizeEnsemble() && !Settings.IS_XVAL && (m_Mode == ClusStatManager.MODE_HIERARCHICAL));
//		m_OptMode = false;
		m_OutEnsembleAt = sett.getNbBaggingSets().getIntVectorSorted();
		m_NbMaxBags = m_OutEnsembleAt[m_OutEnsembleAt.length-1];
//		m_OOBEstimate = Settings.shouldEstimateOOB() && (m_Mode == ClusStatManager.MODE_HIERARCHICAL);
		m_OOBEstimate = Settings.shouldEstimateOOB();
		m_OOBCalculation = false;
		
		m_FeatRank = sett.shouldPerformRanking() && !Settings.IS_XVAL;
		if (m_FeatRank && !m_OOBEstimate){
			System.err.println();
			System.err.println(this.getClass().getName()+": For Feature Ranking OOB estimate of error should be performed");
			System.err.println();
			m_FeatRank = false;
		}
		if (m_FeatRank)	initializeAttributes(schema.getDescriptiveAttributes());

	}
	public void initializeAttributes(ClusAttrType[] descriptive){
		m_AllAttributes = new HashMap();
		int num = -1;
		int nom = -1;
//		System.out.println("NB = "+descriptive.length);
		for (int i = 0; i < descriptive.length; i++) {
			ClusAttrType type = descriptive[i];
			if (!type.isDisabled()) {
				double[] info = new double[3];
				if (type.getTypeIndex() == 0){
					nom ++;
					info[0] = 0; //type
					info[1] = nom; //order in nominal attributes
				}
				if (type.getTypeIndex() == 1){
					num ++;
					info[0] = 1; //type
					info[1] = num; //order in numeric attributes
				}
				info[2] = 0; //current rank
//					System.out.print(type.getName()+": "+info[1]+"\t");
				m_AllAttributes.put(type.getName(),info);
			}
		}		
	}

	public static boolean isOptimized(){
		return m_OptMode;
	}

	public static boolean isCalcOOB(){
		return m_OOBCalculation;
	}

	public void induceAll(ClusRun cr) throws ClusException, IOException {
		System.out.println("Memory And Time Optimization = " + m_OptMode);
		System.out.println("Out-Of-Bag Estimate of the error = " + m_OOBEstimate);
		System.out.println("\tPerform Feature Ranking = " + m_FeatRank);
		switch (cr.getStatManager().getSettings().getEnsembleMethod()){
		case 0: {	//Bagging
			induceBagging(cr);
			break;
			}
		case 1: {	//RForest
			induceBagging(cr);
			break;
			}
		case 2: {	//RSubspaces
			induceSubspaces(cr);
			break;
			}
		case 3: {	//Bagging Subspaces
			induceBaggingSubspaces(cr);
			break;
			}
		}
		if (m_FeatRank) printRanking(cr.getStatManager().getSettings().getFileAbsolute(cr.getStatManager().getSettings().getAppName()));
		postProcessForest(cr);
		
//		This section is for calculation of the similarity in the ensemble
//		ClusBeamSimilarityOutput bsimout = new ClusBeamSimilarityOutput(cr.getStatManager().getSettings());
//		bsimout.appendToFile(m_OForest.getModels(), cr);
	}

	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException,
			IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void postProcessForest(ClusRun cr) throws ClusException{

		ClusModelInfo orig_info = cr.addModelInfo(ClusModel.ORIGINAL);
		orig_info.setModel(m_OForest);
		orig_info.setName("Original");

		if (!m_OptMode){
			ClusModelInfo def_info = cr.addModelInfo(ClusModel.DEFAULT);
			def_info.setModel(m_DForest);
			def_info.setName("Default");
		}//		avoid printing the M1 attributes in the header of the oob predictions in the .oob.pred file
		else cr.getModelInfo(ClusModel.DEFAULT).setName("Default");

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

	public void postProcessForestForOOBEstimate(ClusRun cr, OOBSelection oob_total, RowData all_data, Clus cl, String addname) throws ClusException, IOException{
			Settings sett = cr.getStatManager().getSettings();
			ClusSchema schema = all_data.getSchema();
			ClusOutput output = new ClusOutput(sett.getAppName() + addname +".oob", schema, sett);
			ClusSummary summary = cr.getSummary();
			m_OOBCalculation = true;

			//this is the section for writing the predictions from the OOB estimate
			//should new option in .s file be introduced???
//			ClusStatistic target = getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
//			PredictionWriter wrt = new PredictionWriter(sett.getAppName() + addname	+ ".oob.pred", sett, target);
//			wrt.globalInitialize(schema);
//			ClusModelInfo allmi = cr.getAllModelsMI();
//			allmi.addModelProcessor(ClusModelInfo.TRAIN_ERR, wrt);
//			cr.copyAllModelsMIs();
//			wrt.initializeAll(schema);
			calcOOBError(oob_total,all_data, ClusModelInfo.TRAIN_ERR, cr);
//			summary.addSummary(cr);

			if (summary != null) {
				for (int i = ClusModel.ORIGINAL; i < cr.getNbModels(); i++) {
					ClusModelInfo summ_info = cr.getModelInfo(i);
					ClusErrorList train_err = summ_info.getTrainingError();
					summ_info.setTrainError(train_err);
				}
			}
			cl.calcExtraTrainingSetErrors(cr);
			output.writeHeader();
			output.writeOutput(cr, true, true);
			output.close();
//			wrt.close();
			m_OOBCalculation = false;
		}

	public void printRanking(String fname) throws IOException{
		Set attributes = m_AllAttributes.keySet();
		Iterator iter = attributes.iterator();
		TreeMap sorted = new TreeMap();
		while (iter.hasNext()){
			String attr = (String)iter.next();
			double score = ((double[])m_AllAttributes.get(attr))[2]/m_NbMaxBags;
			ArrayList attrs = new ArrayList();
			if (sorted.containsKey(score))
				attrs = (ArrayList)sorted.get(score);
			attrs.add(attr);
			sorted.put(score, attrs);
		}
		File franking = new File(fname+".fimp");
		FileWriter wrtr = new FileWriter(franking);
		wrtr.write("Ranking via Random Forests\n");
		wrtr.write("--------------------------\n");
		while (!sorted.isEmpty()){
//			wrtr.write(sorted.get(sorted.lastKey()) + "\t" + sorted.lastKey()+"\n");
			wrtr.write(writeRow((ArrayList)sorted.get(sorted.lastKey()),(Double)sorted.lastKey()));
			sorted.remove(sorted.lastKey());
		}
		wrtr.flush();
		wrtr.close();
		System.out.println("Feature Ranking via Random Forests written in " + franking.getName());
	}
	
	public String writeRow(ArrayList attributes, double value){
		String output = "";
		for (int i = 0; i < attributes.size(); i++){
			String attr = (String)attributes.get(i);
			attr = attr.replaceAll("\\[", "");
			attr = attr.replaceAll("\\]", "");
			output += attr +"\t"+value+"\n";
		}
		return output;
	}
	
	public void induceSubspaces(ClusRun cr) throws ClusException, IOException {
		m_OForest = new ClusForest(getStatManager());
		m_DForest = new ClusForest(getStatManager());
		long summ_time = 0; // = ResourceInfo.getTime();
		TupleIterator train_iterator = null; // = train set iterator
		TupleIterator test_iterator = null; // = test set iterator

		if (m_OptMode){
			train_iterator = cr.getTrainIter();
			if (m_BagClus.hasTestSet()){
				test_iterator = cr.getTestSet().getIterator();
				initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows()+cr.getTestSet().getNbRows());
			}else	initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows());//train only
			initPredictions(m_OForest);
		}
		for (int i = 0; i < m_NbMaxBags; i++) {
			long one_bag_time = ResourceInfo.getTime();
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

			summ_time += ResourceInfo.getTime() - one_bag_time;

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
				crSingle.setInductionTime(summ_time);
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
		long summ_time = 0;
		TupleIterator train_iterator = null; // = train set iterator
		TupleIterator test_iterator = null; // = test set iterator
		OOBSelection oob_total = null; // = total OOB selection
		OOBSelection oob_sel = null; // = current OOB selection

		if (m_OptMode){
			train_iterator = cr.getTrainIter();
			if (m_BagClus.hasTestSet()){
				test_iterator = cr.getTestSet().getIterator();
				initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows()+cr.getTestSet().getNbRows());
			}
			else	initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows());//train only
			initPredictions(m_OForest);
		}
		for (int i = 0; i < m_NbMaxBags; i++) {
			long one_bag_time = ResourceInfo.getTime();
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

			summ_time += ResourceInfo.getTime() - one_bag_time;
			
			if (m_OOBEstimate){		//OOB estimate is on
				oob_sel = new OOBSelection(msel);	
				if (i == 0){ //initialization
					oob_total = new OOBSelection(msel);
					m_OOBPredictions = new HashMap();
					m_OOBUsage = new HashMap();
				}else oob_total.addToThis(oob_sel);
				updateOOBTuples(oob_sel, (RowData)cr.getTrainingSet(), model);
			}
			if (m_FeatRank){
				ArrayList attests = new ArrayList();
				fillWithAttributesInTree((ClusNode)model, attests);
				RowData tdata = (RowData)((RowData)cr.getTrainingSet()).deepCloneData();
				double oob_err = calcAverageError((RowData)tdata.selectFrom(oob_sel), model);
//				System.out.println("-------Model-------");
//				((ClusNode)model).printTree();
//				System.out.println("-------------------");
//				System.out.println("oob_err = " + oob_err);
				for (int z = 0; z < attests.size(); z++){//for the attributes that appear in the tree
					String current_attribute = (String)attests.get(z);
					double [] info = ((double[])m_AllAttributes.get(current_attribute));
					double type = info[0];
					double position = info[1];
					RowData permuted = createRandomizedOOBdata(oob_sel, (RowData)tdata.selectFrom(oob_sel), (int)type, (int)position);
					if (getStatManager().getMode() == ClusStatManager.MODE_REGRESSION){//increase in error rate (oob_err -> MSError)
						info[2] += (calcAverageError((RowData)permuted, model) - oob_err)/oob_err;
					}else if (getStatManager().getMode() == ClusStatManager.MODE_CLASSIFY){//decrease in accuracy (oob_err -> Accuracy)
						info[2] += (oob_err - calcAverageError((RowData)permuted, model))/oob_err;
					}
					m_AllAttributes.put(current_attribute, info);
				}
			}
			
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
				m_DForest.addModelToForest(defmod);
			}

			//Valid only when test set is supplied
			if (m_OptMode && ((i+1) != m_NbMaxBags) && checkToOutEnsemble(i+1)){
				crSingle.setInductionTime(summ_time);
				postProcessForest(crSingle);
				crSingle.setTestSet(cr.getTestIter());
				crSingle.setTrainSet(cr.getTrainIter());
				outputBetweenForest(crSingle, m_BagClus, "_"+(i+1)+"_");
//				crSingle.deleteDataAndModels();
			}
			
			if (m_OOBEstimate && checkToOutEnsemble(i+1)){
				crSingle.setInductionTime(summ_time);
				postProcessForest(crSingle);
				postProcessForestForOOBEstimate(crSingle, oob_total, (RowData)cr.getTrainingSet(), m_BagClus, "_"+(i+1)+"_");
			}
			crSingle.deleteDataAndModels();
		}
	}


	public void induceBaggingSubspaces(ClusRun cr) throws ClusException, IOException {
		int nbrows = cr.getTrainingSet().getNbRows();
		m_OForest = new ClusForest(getStatManager());
		m_DForest = new ClusForest(getStatManager());
		long summ_time = 0; // = ResourceInfo.getTime();
		TupleIterator train_iterator = null; // = train set iterator
		TupleIterator test_iterator = null; // = test set iterator
		OOBSelection oob_total = null; // = total OOB selection
		OOBSelection oob_sel = null; // = current OOB selection

		if (m_OptMode){
			train_iterator = cr.getTrainIter();
			if (m_BagClus.hasTestSet()){
				test_iterator = cr.getTestSet().getIterator();
				initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows()+cr.getTestSet().getNbRows());
			}else	initializeTupleHashCodes(train_iterator, test_iterator, cr.getTrainingSet().getNbRows());//train only
			initPredictions(m_OForest);
		}
		for (int i = 0; i < m_NbMaxBags; i++) {
			long one_bag_time = ResourceInfo.getTime();
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

			summ_time += ResourceInfo.getTime() - one_bag_time;

			if (m_OOBEstimate){		//OOB estimate is on
				oob_sel = new OOBSelection(msel);
				if (i == 0){ //initialization
					oob_total = new OOBSelection(msel);
					m_OOBPredictions = new HashMap();
					m_OOBUsage = new HashMap();
				}else oob_total.addToThis(oob_sel);
				updateOOBTuples(oob_sel, (RowData)cr.getTrainingSet(), model);
			}

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
				crSingle.setInductionTime(summ_time);
				postProcessForest(crSingle);
				crSingle.setTestSet(cr.getTestIter());
				crSingle.setTrainSet(cr.getTrainIter());
				outputBetweenForest(crSingle, m_BagClus, "_"+(i+1)+"_");
//				crSingle.deleteDataAndModels();
			}
			if (m_OOBEstimate && checkToOutEnsemble(i+1)){
				crSingle.setInductionTime(summ_time);
				postProcessForest(crSingle);
				postProcessForestForOOBEstimate(crSingle, oob_total, (RowData)cr.getTrainingSet(), m_BagClus, "_"+(i+1)+"_");
			}
			crSingle.deleteDataAndModels();
		}
	}

	/**
	 * 
	 * @param selection
	 * @param data
	 * @param type    -> 0 nominal, 1 numeric
	 * @param position -> at which position
	 * @return
	 */
	public RowData createRandomizedOOBdata(OOBSelection selection, RowData data, int type, int position){
		RowData result = data;
		Random rndm = new Random(data.getNbRows());
		for (int i = 0; i < result.getNbRows(); i++){
//			int rnd = i + ClusRandom.nextInt(ClusRandom.RANDOM_ALGO_INTERNAL, result.getNbRows()- i);
			int rnd = i + rndm.nextInt(result.getNbRows()- i);
			DataTuple first = result.getTuple(i);
			DataTuple second = result.getTuple(rnd);
			if (type == 0){//nominal
				int swap = first.getIntVal(position);
				first.setIntVal(second.getIntVal(position), position);
				second.setIntVal(swap, position);				
			}else if (type == 1){//numeric
				double swap = first.getDoubleVal(position);
				first.setDoubleVal(second.getDoubleVal(position), position);
				second.setDoubleVal(swap, position);
			}else System.err.println("Error at the Random Permutations");
		}
		return result;
	}
	
	public void fillWithAttributesInTree(ClusNode node, ArrayList attributes){
		for (int i = 0; i < node.getNbChildren(); i++){
			String att = node.getTest().getType().getName();
			if (!attributes.contains(att)){
//				System.out.println("Appending..."+att);
				attributes.add(att);
			}
			fillWithAttributesInTree((ClusNode)node.getChild(i), attributes);
		}
	}
	
	public double calcAverageError(RowData data, ClusModel model) throws ClusException{
		ClusSchema schema = data.getSchema();
		/* create error measure */
		ClusErrorList error = new ClusErrorList();
		NumericAttrType[] num = schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom = schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (nom.length != 0) {
			error.addError(new Accuracy(error, nom));
		} else if (num.length != 0) {
			error.addError(new MSError(error, num));
		} else System.err.println("Supported only nominal or numeric targets!");
		/* attach model to given schema */		
		schema.attachModel(model);
		/* iterate over tuples and compute error */
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			ClusStatistic pred = model.predictWeighted(tuple);
			error.addExample(tuple, pred);
		}
		/* return the average error */
		double err = error.getFirstError().getModelError();
		return err;
	}
	
	public void updateOOBTuples(OOBSelection oob_sel, RowData train_data, ClusModel model) throws IOException, ClusException{
		for (int i = 0; i < train_data.getNbRows(); i++){
			if (oob_sel.isSelected(i)){
				DataTuple tuple = train_data.getTuple(i);
				if (existsOOBtuple(tuple)) updateOOBTuple(tuple, model);
				else addOOBTuple(tuple, model);
			}
		}
	}

	public boolean existsOOBtuple(DataTuple tuple){
		if (m_OOBUsage.containsKey(tuple.hashCode()) && m_OOBPredictions.containsKey(tuple.hashCode()))
			return true;
		if (!m_OOBUsage.containsKey(tuple.hashCode()) && m_OOBPredictions.containsKey(tuple.hashCode()))
			System.err.println(this.getClass().getName()+":existsOOBtuple(DataTuple) OOB tuples mismatch-> Usage = False, Predictions = True");
		if (m_OOBUsage.containsKey(tuple.hashCode()) && !m_OOBPredictions.containsKey(tuple.hashCode()))
			System.err.println(this.getClass().getName()+":existsOOBtuple(DataTuple) OOB tuples mismatch-> Usage = True, Predictions = False");
		return false;
	}

	public void addOOBTuple(DataTuple tuple, ClusModel model){
		m_OOBUsage.put(tuple.hashCode(), 1);
		
		if (m_Mode == ClusStatManager.MODE_HIERARCHICAL){
			//for HMC we store the averages
			WHTDStatistic stat = (WHTDStatistic)model.predictWeighted(tuple);
			m_OOBPredictions.put(tuple.hashCode(),stat.getNumericPred());
		}
		
		if (m_Mode == ClusStatManager.MODE_REGRESSION){
			//for Regression we store the averages
			RegressionStat stat = (RegressionStat)model.predictWeighted(tuple);
			m_OOBPredictions.put(tuple.hashCode(),stat.getNumericPred());
		}
		
		if (m_Mode == ClusStatManager.MODE_CLASSIFY){
			//this should have a [][].for each attribute we store: Majority: the winning class, for Probability distribution, the class distribution
			ClassificationStat stat = (ClassificationStat)model.predictWeighted(tuple);
			switch (Settings.m_ClassificationVoteType.getValue()){//default is Majority Vote
				case 0: m_OOBPredictions.put(tuple.hashCode(), transformToMajority(stat.m_ClassCounts));break;
				case 1: m_OOBPredictions.put(tuple.hashCode(), transformToProbabilityDistribution(stat.m_ClassCounts));break;
				default: m_OOBPredictions.put(tuple.hashCode(), transformToMajority(stat.m_ClassCounts));
			}		
		}
	}

	//transform the class counts to majority vote (the one with max votes gets 1)
	public double[][] transformToMajority(double[][] m_Counts){
		int[] maxPerTarget = new int[m_Counts.length];
		for (int i = 0; i < m_Counts.length; i++){
			maxPerTarget[i] = -1;
			double m_max = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < m_Counts[i].length;j++){
				if (m_Counts[i][j]>m_max){
					maxPerTarget[i] = j;
					m_max = m_Counts[i][j];
				}
			}
		}	
		double[][] result = new double[m_Counts.length][];
		for (int m = 0; m < m_Counts.length; m++){
			result[m] = new double[m_Counts[m].length];
			result[m][maxPerTarget[m]] ++; //the positions of max class will be 1
		}
		return result;
	}
		
	//transform the class counts to probability distributions
	public double[][] transformToProbabilityDistribution(double[][] m_Counts){
		double[] sumPerTarget = new double[m_Counts.length];
		for (int i = 0; i < m_Counts.length; i++)
			for (int j = 0; j < m_Counts[i].length;j++)
				sumPerTarget[i] += m_Counts[i][j];
		double[][] result = new double[m_Counts.length][];
		
		for (int m = 0; m < m_Counts.length; m++){
			result[m] = new double[m_Counts[m].length];
			for (int n = 0; n < m_Counts[m].length; n++){
				result[m][n] = m_Counts[m][n]/sumPerTarget[m];
			}
		}
		return result;
	}
	
	public void updateOOBTuple(DataTuple tuple, ClusModel model){
		Integer used = (Integer)m_OOBUsage.get(tuple.hashCode());
		used = used.intValue()+1;
		m_OOBUsage.put(tuple.hashCode(), used);
	
		if (m_Mode == ClusStatManager.MODE_HIERARCHICAL){
			//the HMC and Regression have the same voting scheme: average
			WHTDStatistic stat = (WHTDStatistic)model.predictWeighted(tuple);
			double[] predictions = stat.getNumericPred();
			double[] avg_predictions = (double[])m_OOBPredictions.get(tuple.hashCode());
			avg_predictions = incrementPredictions(avg_predictions, predictions, used.doubleValue());
			m_OOBPredictions.put(tuple.hashCode(), avg_predictions);
		}
		
		if (m_Mode == ClusStatManager.MODE_REGRESSION){
			//the HMC and Regression have the same voting scheme: average
			RegressionStat stat = (RegressionStat)model.predictWeighted(tuple);
			double[] predictions = stat.getNumericPred();
			double[] avg_predictions = (double[])m_OOBPredictions.get(tuple.hashCode());
			avg_predictions = incrementPredictions(avg_predictions, predictions, used.doubleValue());
			m_OOBPredictions.put(tuple.hashCode(), avg_predictions);
		}
		
		if (m_Mode == ClusStatManager.MODE_CLASSIFY){
			//implement just addition!!!! and then 
			
			ClassificationStat stat =(ClassificationStat) model.predictWeighted(tuple);
			double[][] predictions = stat.m_ClassCounts.clone();
			switch (Settings.m_ClassificationVoteType.getValue()){//default is Majority Vote
				case 0: predictions = transformToMajority(predictions);break;
				case 1: predictions = transformToProbabilityDistribution(predictions);break;
				default: predictions = transformToMajority(predictions);
			}	
			double[][] sum_predictions = (double[][])m_OOBPredictions.get(tuple.hashCode());
			sum_predictions = incrementPredictions(sum_predictions, predictions);
			m_OOBPredictions.put(tuple.hashCode(), sum_predictions);
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
		if (test != null){
			test.init();
			DataTuple test_tuple = test.readTuple();
			while (test_tuple != null){
				m_HashCodeTuple[count] = test_tuple.hashCode();
				count++;
				test_tuple = test.readTuple();
			}
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
		if (test != null){
			test.init();//restart the iterator
			DataTuple test_tuple = test.readTuple();
			while (test_tuple != null){
				tuple_hash.add(test_tuple.hashCode());
				test_tuple = test.readTuple();
			}
			test.init();//restart the iterator
		}
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
		if (test != null){
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
		if (test != null){
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

	public double[] incrementPredictions(double[] avg_predictions, double[] predictions, double nb_models){
		//the current averages are stored in the avg_predictions
		int plength = avg_predictions.length;
		double[] result = new double[plength];
		for (int i = 0; i < plength; i++)
			result[i] = avg_predictions[i]+(predictions[i]-avg_predictions[i])/nb_models;
		return result;
	}

	public double[][] incrementPredictions(double[][] sum_predictions, double[][] predictions){
		//the current sums are stored in sum_predictions
		double[][] result = new double[sum_predictions.length][];
		for (int i = 0; i < sum_predictions.length; i++){
			result[i] = new double[sum_predictions[i].length];
			for (int j = 0; j < sum_predictions[i].length; j++){
				result[i][j] = sum_predictions[i][j] + predictions[i][j];
			}
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
		getStatManager().computeTrainSetStat((RowData)cr.getTrainingSet());
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
//		cr.deleteDataAndModels();
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

/*	public static ClusAttrType[] selectAttributesForRandomForest(ClusAttrType[] attrs, int select){
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
	}*/

	public final void calcOOBError(OOBSelection oob_tot, RowData all_data, int type, ClusRun cr) throws IOException, ClusException {
		ClusSchema mschema = all_data.getSchema();
//		if (iter.shouldAttach()) attachModels(mschema, cr);
		cr.initModelProcessors(type, mschema);
		ModelProcessorCollection allcoll = cr.getAllModelsMI().getAddModelProcessors(type);
		DataTuple tuple;// = iter.readTuple();

		for (int t = 0; t < all_data.getNbRows(); t++){
			if (oob_tot.isSelected(t)){
				tuple = all_data.getTuple(t);
				allcoll.exampleUpdate(tuple);
				for (int i = 0; i < cr.getNbModels(); i++) {
					ClusModelInfo mi = cr.getModelInfo(i);
					ClusModel model = mi.getModel();
					if (model != null) {
						ClusStatistic pred = model.predictWeighted(tuple);
						ClusErrorList err = mi.getError(type);
						if (err != null) err.addExample(tuple, pred);
						ModelProcessorCollection coll = mi.getModelProcessors(type);
						if (coll != null) {
							if (coll.needsModelUpdate()) {
								model.applyModelProcessors(tuple, coll);
								coll.modelDone();
							}
							coll.exampleUpdate(tuple, pred);
						}
					}
				}
				allcoll.exampleDone();
			}
		}
		cr.termModelProcessors(type);
	}

}
