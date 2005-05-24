package clus.algo.kNN;

import clus.main.*;
import clus.data.rows.*;
import clus.statistic.ClusStatistic;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import jeans.io.ObjectSaveStream;
import jeans.util.MyArray;

public class KNNModel implements ClusModel {

	public ClusStatManager m_SMgr;

	private RowData $trainingData;

	private VectorDistance $vectDist; //used for calculating distances

	public KNNModel(ClusRun clRun,VectorDistance vd){
		$trainingData = (RowData) clRun.getTrainingSet();
		m_SMgr = clRun.getStatManager();

		$vectDist = vd;

	}

	public String getModelInfo() {
		return "KNNModel";
	}	
	
	/**
	 * This method predicts the value for the given tuple and returns
	 * some statistic.
	 */
	public ClusStatistic predictWeighted(DataTuple tuple){
		ClusStatistic stat = m_SMgr.createStatistic();
		//find out how much neighbours necessary (via settings)
		int amountNBS = Settings.kNN_k.getValue();
		//find out if Distance-Weighted kNN used
		boolean distWeighted = Settings.kNN_distWeighted.getValue();

		//make a priorityqueue of size amountNBS to find nearest neighbours
		PriorityQueue q = new PriorityQueue(amountNBS);

		int nbr = $trainingData.getNbRows();
		DataTuple curTup;
		double dist;
		//find the nearest neighbours
		for (int i = 0; i <nbr;i++){
			curTup = $trainingData.getTuple(i);
			dist = calcDistance(tuple,curTup);
			q.addElement(curTup,dist);
		}
		//add the kNN's to the statistic

		//weights all the same for now
		double weight = 1.0;
		for (int i=0;i<amountNBS;i++){
			//Change weights when distance-weighted kNN is wanted
			if (distWeighted) weight = 1.0 / Math.pow(q.getValue(i),2);
			stat.updateWeighted((DataTuple)q.getElement(i),weight);
		}
		stat.calcMean();
		return stat;
	}

	// Calculates distance between 2 tuples
	private double calcDistance(DataTuple t1,DataTuple t2){
		return $vectDist.getDistance(t1,t2);
	}


	//?
	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException{
	}

	//?
	public int getModelSize(){return 0;}

	public void printModel(PrintWriter wrt) {
	}
	
	public void saveModel(ObjectSaveStream strm)  throws IOException {
	}
	
	public void attachModel(Hashtable table) {
		System.err.println(getClass().getName()+"attachModel() not implemented");
	}
	
/*
	public final ClusStatistic getTotalStat() {
		return m_TotStat;
	}

	public final double getTotWeight() {
		return m_TotStat.m_SumWeight;
	}

	public final void setTotalStat(ClusStatistic stat) {
		m_TotStat = stat;
	}

	public final void initTotalStat(ClusStatManager smgr) {
		m_TotStat = smgr.createStatistic();
	}*/
}
