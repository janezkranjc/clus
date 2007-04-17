package clus.main;


import jeans.util.*;

import clus.data.rows.*;
import clus.data.type.*;
import clus.statistic.*;
import clus.util.*;

import java.io.*;
import java.util.*;

public class ClusForest implements ClusModel, Serializable{

	private static final long serialVersionUID = 1L;

	ArrayList m_Forest;
	ClusStatistic m_FStat;
	
	public ClusForest(ClusSchema schema){
		m_Forest = new ArrayList();
		NumericAttrType[] num = schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		NominalAttrType[] nom = schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
		if (nom.length != 0) {
			m_FStat = new ClassificationStat(nom);
		} else if (num.length != 0) {
			m_FStat = new RegressionStat(num);
		} else {
			System.err.println(getClass().getName() + "initForest(): Error itializing the statistic");
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
		}
		m_FStat.vote(votes);
		return m_FStat;
	}

	public void printModel(PrintWriter wrt) {
		// This should be better organized
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			wrt.write("Model "+(i+1)+": \n");
			wrt.write("\n");
			model.printModel(wrt);
			wrt.write("\n");
		}
	}

	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		// This should be better organized
		ClusModel model;
		for (int i = 0; i < m_Forest.size(); i++){
			model = (ClusModel)m_Forest.get(i);
			wrt.write("Model "+(i+1)+": \n");
			wrt.write("\n");
			model.printModel(wrt, info);
			wrt.write("\n");
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
}
