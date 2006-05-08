package clus.main;

import clus.util.*;
import clus.data.rows.*;
import clus.error.*;
import clus.selection.*;
import clus.statistic.*;

public abstract class ClusData {

	protected int m_NbRows;
	protected int m_NbNonZeroRows;
	
	public final int getNbRows() {
		return m_NbRows;
	}	
	
	public final void setNbRows(int nb) {
		m_NbRows = nb;
	}
	
	public final int getNbNonZeroRows() {
		return m_NbNonZeroRows;
	}	
	
	public final void setNbNonZeroRows(int nb) {
		m_NbNonZeroRows = nb;
	}
	
	public ClusData selectFrom(ClusSelection sel) {
		return null;
	}	
	
	public abstract ClusData cloneData();
	
	public abstract ClusData select(ClusSelection sel);
	
	public abstract void insert(ClusData other, ClusSelection sel);
	
	public abstract ClusView createNormalView(ClusSchema schema) throws ClusException;
	
	public abstract void resize(int nbrows);
	
	public abstract void attach(ClusNode node);
	
	public abstract void calcTotalStat(ClusStatistic stat);
	
	public abstract void calcError(ClusNode node, ClusErrorParent par);
	
	public abstract double[] getNumeric(int idx);
	
	public abstract int[] getNominal(int idx);
	
	public abstract void preprocess(int pass, DataPreprocs pps) throws ClusException;
	
	public void calcTotalStats(ClusStatistic[] stats) {
		for (int i = 0; i < stats.length; i++) {
			calcTotalStat(stats[i]);
		}
	}
}
