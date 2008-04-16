package clus.error;

import java.util.ArrayList;

import clus.data.rows.DataTuple;
import clus.statistic.ClusStatistic;
import clus.statistic.CombStat;

public class ClusSumError extends ClusError {

	protected ArrayList m_Errors = new ArrayList();
	
	public ClusSumError(ClusErrorList par) {
		super(par);
	}

	public void reset() {
		for (int i = 0; i < m_Errors.size(); i++) {
			ClusError err = (ClusError)m_Errors.get(i);
			err.reset();
		}
	}
	
	public void add(ClusError other) {
		ClusSumError others = (ClusSumError)other; 
		for (int i = 0; i < m_Errors.size(); i++) {
			ClusError err = (ClusError)m_Errors.get(i);
			err.add(others.getComponent(i));
		}
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		// this can be made more general
		CombStat stat = (CombStat)pred;
		getComponent(0).addExample(tuple, stat.getRegressionStat());
		getComponent(1).addExample(tuple, stat.getClassificationStat());
	}
	
	public void addExample(DataTuple real, DataTuple pred) {
		for (int i = 0; i < m_Errors.size(); i++) {
			ClusError err = (ClusError)m_Errors.get(i);
			err.addExample(real, pred);
		}
	}
	
	public void addInvalid(DataTuple tuple) {
		for (int i = 0; i < m_Errors.size(); i++) {
			ClusError err = (ClusError)m_Errors.get(i);
			err.addInvalid(tuple);
		}
	}
	
	public ClusError getErrorClone(ClusErrorList par) {
		ClusSumError result = new ClusSumError(par);
		for (int i = 0; i < m_Errors.size(); i++) {
			ClusError err = (ClusError)m_Errors.get(i);
			result.addComponent(err.getErrorClone(par));
		}
		return result;
	}

	public String getName() {
		StringBuffer name = new StringBuffer();
		for (int i = 0; i < m_Errors.size(); i++) {
			ClusError err = (ClusError)m_Errors.get(i);
			if (i != 0) name.append(", ");
			name.append(err.getName());
		}
		return name.toString();
	}

	public void addComponent(ClusError err) {
		m_Errors.add(err);		
	}
	
	public ClusError getComponent(int i) {
		return (ClusError)m_Errors.get(i);
	}
}
