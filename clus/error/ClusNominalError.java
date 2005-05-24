package clus.error;

import clus.main.*;
import clus.data.rows.*;
import clus.statistic.*;

public abstract class ClusNominalError extends ClusError {

	protected int[] m_Default;

	public ClusNominalError(ClusErrorParent par) {
		super(par, par.getNbNominal());
	}
	
	public ClusNominalError(ClusErrorParent par, int nb_nominal) {
		super(par, nb_nominal);
	}	
	
	public void setDefault(int[] value) {
		m_Default = value;
	}
	
	public void setDefault(ClusStatistic pred) {
		m_Default = ((ClassificationStat)pred).m_MajorityClasses;
	}	
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		addExample(tuple.m_Ints, pred.getNominalPred());
	}	
	
	public void addExample(ClusData data, int idx, ClusStatistic pred) {
		addExample(data.getNominal(idx), pred.getNominalPred());
	}
	
	public abstract void addExample(int[] real, int[] predicted);
}
