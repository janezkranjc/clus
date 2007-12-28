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

public class ClusForest implements ClusModel, Serializable{

	private static final long serialVersionUID = 1L;

	ArrayList m_Forest;
	ClusStatistic m_Stat;
	static ClusAttrType[] m_RandomSubspaces;
	boolean m_PrintModels;
	
	public ClusForest(){
		m_Forest = new ArrayList();
//		m_PrintModels = false;
	}
	
	public ClusForest(ClusStatManager statmgr){
		m_Forest = new ArrayList();
		initForest(statmgr);
	}
	
	public void initForest(ClusStatManager statmgr){
		if (statmgr.getMode() == ClusStatManager.MODE_CLASSIFY){
			m_Stat = new ClassificationStat(statmgr.getSchema().getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET));
		}else if (statmgr.getMode() == ClusStatManager.MODE_REGRESSION){
			m_Stat = new RegressionStat(statmgr.getSchema().getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET));
		}else if (statmgr.getMode() == ClusStatManager.MODE_HIERARCHICAL){
			m_Stat = new WHTDStatistic(statmgr.getHier(),statmgr.getCompatibility());
		}else{
			System.err.println(getClass().getName() + "initForest(): Error initializing the statistic");
		}
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

	public void attachModel(Hashtable table) throws ClusException {
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
		ClusModel model;
		ArrayList votes = new ArrayList();
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			votes.add(model.predictWeighted(tuple));
			if (tuple.getWeight() != 1.0) System.out.println("Tuple "+tuple.getIndex()+" = "+tuple.getWeight());
		}
		m_Stat.vote(votes);
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
		}else{
			wrt.write("Forest with "+getNbModels()+" models\n");
		}
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
		}else{
			wrt.write("Forest with "+getNbModels()+" models\n");
		}
	}

	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			model.printModelAndExamples(wrt, info, examples);
		}
	}

	public void printModelToPythonScript(PrintWriter wrt) {
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			model.printModelToPythonScript(wrt);
		}
	}

	public void printModelToQuery(PrintWriter wrt, ClusRun cr, int starttree, int startitem) {
		// TODO Auto-generated method stub
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			model.printModelToQuery(wrt, cr, starttree, startitem);
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

	public ClusModel getModel(int idx){
		return (ClusModel)m_Forest.get(idx);
	}
	
	public int getNbModels(){
		//for now same as getModelSize();
		return m_Forest.size();
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
			System.err.println(getClass().getName()+" thrsholdToModel(): Error while applying threshold "+threshold+" to model "+model_nb);
			e.printStackTrace();
		}
	}
	
	public ArrayList getModels(){
		return m_Forest;
	}
	
	public void setModels(ArrayList models){
		m_Forest = models;
	}
	
	public ClusForest cloneForestSimple(){
		ClusForest clone = new ClusForest();
		clone.setModels(m_Forest);
		clone.setStat(m_Stat);
		return clone;
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
}
