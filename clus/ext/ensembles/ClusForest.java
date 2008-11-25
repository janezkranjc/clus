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


import jeans.util.*;

import clus.main.*;
import clus.model.ClusModel;
import clus.algo.tdidt.ClusNode;
import clus.data.rows.*;
import clus.data.type.*;
import clus.ext.hierarchical.HierClassTresholdPruner;
import clus.ext.hierarchical.WHTDStatistic;
import clus.statistic.*;
import clus.util.*;

import java.io.*;
import java.util.*;

/**
 * Ensemble of decision trees.
 *
 */
public class ClusForest implements ClusModel, Serializable{

	private static final long serialVersionUID = 1L;

	/** List of decision trees */
	ArrayList m_Forest;
	ClusStatistic m_Stat;
	boolean m_PrintModels;
	String m_AttributeList;
	String m_AppName;

	public ClusForest(){
		m_Forest = new ArrayList();
//		m_PrintModels = false;
	}

	public ClusForest(ClusStatManager statmgr){
		m_Forest = new ArrayList();
		if (statmgr.getMode() == ClusStatManager.MODE_CLASSIFY){
			m_Stat = new ClassificationStat(statmgr.getSchema().getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET));
		}else if (statmgr.getMode() == ClusStatManager.MODE_REGRESSION){
			m_Stat = new RegressionStat(statmgr.getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET));
		}else if (statmgr.getMode() == ClusStatManager.MODE_HIERARCHICAL){
			m_Stat = new WHTDStatistic(statmgr.getHier(),statmgr.getCompatibility());
		}else{
			System.err.println(getClass().getName() + "initForest(): Error initializing the statistic");
		}
		m_AppName = statmgr.getSettings().getFileAbsolute(statmgr.getSettings().getAppName());
		m_AttributeList = "";
		ClusAttrType[] cat = ClusSchema.vectorToAttrArray(statmgr.getSchema().collectAttributes(ClusAttrType.ATTR_USE_DESCRIPTIVE, ClusAttrType.THIS_TYPE));
		for (int ii=0;ii<cat.length-1;ii++) m_AttributeList = m_AttributeList.concat(cat[ii].getName()+", ");
		m_AttributeList = m_AttributeList.concat(cat[cat.length-1].getName());
	}

	public void addModelToForest(ClusModel model){
		m_Forest.add(model);
	}

	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			model.applyModelProcessors(tuple, mproc);
		}
	}

	public void attachModel(HashMap table) throws ClusException {
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			model.attachModel(table);
		}
	}

	public int getID() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getModelInfo() {
		String result = "FOREST with " +getNbModels()+" models\n";
		for (int i = 0; i<getNbModels(); i++)
			result +="\t Model "+(i+1)+": "+getModel(i).getModelInfo()+"\n";
		return result;
	}

	public int getModelSize() {
		return m_Forest.size();		//Maybe something else ?!
	}
	
	public ClusStatistic predictWeighted(DataTuple tuple) {

		if (ClusEnsembleInduce.m_OOBCalculation)
			return predictWeightedOOB(tuple);

		if (! ClusEnsembleInduce.m_OptMode) return predictWeightedStandard(tuple);
		else return predictWeightedOpt(tuple);

/*		ClusModel model;
		ArrayList votes = new ArrayList();
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			votes.add(model.predictWeighted(tuple));
			if (tuple.getWeight() != 1.0) System.out.println("Tuple "+tuple.getIndex()+" = "+tuple.getWeight());
		}
		m_Stat.vote(votes);
		return m_Stat;*/
	}

	public ClusStatistic predictWeightedStandard(DataTuple tuple) {
		ClusModel model;
		ArrayList votes = new ArrayList();
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			votes.add(model.predictWeighted(tuple));
//			if (tuple.getWeight() != 1.0) System.out.println("Tuple "+tuple.getIndex()+" = "+tuple.getWeight());
		}
		m_Stat.vote(votes);
		return m_Stat;
	}

	public ClusStatistic predictWeightedOOB(DataTuple tuple) {
		
		if (ClusEnsembleInduce.m_Mode == ClusStatManager.MODE_HIERARCHICAL || ClusEnsembleInduce.m_Mode == ClusStatManager.MODE_REGRESSION)
			return predictWeightedOOBRegressionHMC(tuple);
		if (ClusEnsembleInduce.m_Mode == ClusStatManager.MODE_CLASSIFY)
			return predictWeightedOOBClassification(tuple);
		
		System.err.println(this.getClass().getName()+".predictWeightedOOB(DataTuple) - Error in Setting the Mode");
		return null;
	}

	public ClusStatistic predictWeightedOOBRegressionHMC(DataTuple tuple) {
		double[] predictions = null;
		if (ClusEnsembleInduce.m_OOBPredictions.containsKey(tuple.hashCode()))
			predictions = (double[])ClusEnsembleInduce.m_OOBPredictions.get(tuple.hashCode());
		else{
			System.err.println(this.getClass().getName()+".predictWeightedOOBRegressionHMC(DataTuple) - Missing Prediction For Tuple");
			System.err.println("Tuple Hash = "+tuple.hashCode());
		}
		m_Stat.reset();
		
		if (ClusEnsembleInduce.m_Mode == ClusStatManager.MODE_HIERARCHICAL || ClusEnsembleInduce.m_Mode == ClusStatManager.MODE_REGRESSION){
			((RegressionStat)m_Stat).m_Means = new double[predictions.length];
			for (int j = 0; j < predictions.length; j++){
				((RegressionStat)m_Stat).m_Means[j] = predictions[j];
			}	
			if (ClusEnsembleInduce.m_Mode == ClusStatManager.MODE_HIERARCHICAL) m_Stat = (WHTDStatistic)m_Stat;
			m_Stat.computePrediction();
		}
		
		return m_Stat;
	}

	public ClusStatistic predictWeightedOOBClassification(DataTuple tuple) {
		double[][] predictions = null;
		if (ClusEnsembleInduce.m_OOBPredictions.containsKey(tuple.hashCode()))
			predictions = (double[][])ClusEnsembleInduce.m_OOBPredictions.get(tuple.hashCode());
		else{
			System.err.println(this.getClass().getName()+".predictWeightedOOBClassification(DataTuple) - Missing Prediction For Tuple");
			System.err.println("Tuple Hash = "+tuple.hashCode());
		}
		m_Stat.reset();
		
		((ClassificationStat)m_Stat).m_ClassCounts = new double[predictions.length][];
			for (int m = 0; m < predictions.length; m++){
				((ClassificationStat)m_Stat).m_ClassCounts[m] = new double[predictions[m].length];
				for (int n = 0; n < predictions[m].length; n++){
					((ClassificationStat)m_Stat).m_ClassCounts[m][n] = predictions[m][n];
				}
			}
			m_Stat.computePrediction();
		return m_Stat;
	}

	public ClusStatistic predictWeightedOpt(DataTuple tuple) {
		int position = ClusEnsembleInduce.locateTuple(tuple);
		int predlength = ClusEnsembleInduce.m_AvgPredictions[position].length;
		m_Stat.reset();
		((WHTDStatistic)m_Stat).m_Means = new double[predlength];
		for (int j = 0; j < predlength; j++){
			((WHTDStatistic)m_Stat).m_Means[j] = ClusEnsembleInduce.m_AvgPredictions[position][j];
		}
		m_Stat.computePrediction();
		return m_Stat;
	}

	public void printModel(PrintWriter wrt) {
		// This could be better organized
		if (Settings.isPrintEnsembleModels()){
			ClusModel model;
			for (int i = 0; i < m_Forest.size(); i++){
				model = (ClusModel)m_Forest.get(i);
				if (m_PrintModels) thresholdToModel(i, getThreshold());//This will be enabled only in HMLC mode
				wrt.write("Model "+(i+1)+": \n");
				wrt.write("\n");
				model.printModel(wrt);
				wrt.write("\n");
			}
		}else	wrt.write("Forest with "+getNbModels()+" models\n");

	}

	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		// This could be better organized

		if (Settings.isPrintEnsembleModels()){
			ClusModel model;
			for (int i = 0; i < m_Forest.size(); i++){
				model = (ClusModel)m_Forest.get(i);
				if (m_PrintModels) thresholdToModel(i, getThreshold());//This will be enabled only in HMLC mode
				wrt.write("Model "+(i+1)+": \n");
				wrt.write("\n");
				model.printModel(wrt);
				wrt.write("\n");
			}
		}else	wrt.write("Forest with "+getNbModels()+" models\n");

	}

	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			model.printModelAndExamples(wrt, info, examples);
		}
	}

	public void printModelToPythonScript(PrintWriter wrt) {
		printForestToPython();
	}

	public void printForestToPython(){
		//create a separate .py file
		try{
			File pyscript = new File(m_AppName+"_models.py");
			PrintWriter wrtr = new PrintWriter(new FileOutputStream(pyscript));
			wrtr.println("# Python code of the trees in the ensemble");
			wrtr.println();
			for (int i = 0; i < m_Forest.size(); i++){
				ClusModel model = (ClusModel) m_Forest.get(i);
				wrtr.println("#Model "+(i+1));
				wrtr.println("def clus_tree_"+(i+1)+"( "+m_AttributeList+" ):");
				model.printModelToPythonScript(wrtr);
				wrtr.println();
			}
			wrtr.flush();
			wrtr.close();
			System.out.println("Model to Python Code written to: "+pyscript.getName());
		}catch (IOException e) {
			System.err.println(this.getClass().getName()+".printForestToPython(): Error while writing models to python script");
			e.printStackTrace();
		}
	}

	public void printModelToQuery(PrintWriter wrt, ClusRun cr, int starttree, int startitem, boolean ex) {
		// TODO Auto-generated method stub
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			model.printModelToQuery(wrt, cr, starttree, startitem,ex);
		}
	}

	public ClusModel prune(int prunetype) {
		// TODO Auto-generated method stub
		return null;
	}

	public void retrieveStatistics(ArrayList list) {
		// TODO Auto-generated method stub
	}

	public void printForest(){
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			System.out.println("***************************");
			model = (ClusModel)m_Forest.get(i);
			((ClusNode)model).printTree();
			System.out.println("***************************");
		}
	}
	
	/**
	 * @param idx The index of the decision tree. Thus this is NOT the type enumeration introduced in ClusModel.
	 * @return
	 */
	public ClusModel getModel(int idx){
		return (ClusModel)m_Forest.get(idx);
	}

	/**
	 * @return Number of decision trees in the ensemble.
	 */
	public int getNbModels(){
		//for now same as getModelSize();
		return m_Forest.size();
	}

	public ClusStatistic getStat(){
		return m_Stat;
	}

	public void setStat(ClusStatistic stat){
		m_Stat = stat;
	}

	public void thresholdToModel(int model_nb, double threshold){
		try {
			HierClassTresholdPruner pruner = new HierClassTresholdPruner(null);
			pruner.pruneRecursive((ClusNode)getModel(model_nb), threshold);
		} catch (ClusException e) {
			System.err.println(getClass().getName()+" thresholdToModel(): Error while applying threshold "+threshold+" to model "+model_nb);
			e.printStackTrace();
		}
	}

	/**
	 * Return the list of decision trees = the ensemble.
	 */
	public ArrayList getModels(){
		return m_Forest;
	}

	public void setModels(ArrayList models){
		m_Forest = models;
	}

	//this is only for Hierarchical ML Classification
	public ClusForest cloneForestWithThreshold(double threshold){
		ClusForest clone = new ClusForest();
		clone.setModels(getModels());
		WHTDStatistic stat = (WHTDStatistic)getStat().cloneStat();
		stat.copyAll(getStat());
		stat.setThreshold(threshold);
		clone.setStat(stat);
		return clone;
	}

	public void setPrintModels(boolean print){
		m_PrintModels = print;
	}

	public boolean isPrintModels(){
		return m_PrintModels;
	}

	public double getThreshold(){
		return ((WHTDStatistic)getStat()).getThreshold();
	}

	public void removeModels(){
		m_Forest.clear();
	}
}
