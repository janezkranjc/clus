package clus.error.multiscore;

import java.io.*;

import clus.main.*;
import clus.data.rows.*;
import clus.error.*;
import clus.statistic.*;

public class MultiScoreError extends ClusError {

	ContingencyTable m_Table;
	MultiScore m_Score;
	int[] m_CValues;

	public MultiScoreError(ClusErrorParent par, MultiScore score) {
		super(par, score.getNbTarget());
		m_Score = score;
		m_Table = new ContingencyTable(m_Parent, score.getTarSchema());
		m_CValues = new int[score.getNbTarget()];
	}

	public void reset() {
	}

	// Calculate my own default :-)	
	public void setDefault(ClusStatistic pred) {
	}	
	
	public double getModelError() {
		return 0.0;
	}
	
	public void preprocess(ClusData data) {
		int dim = m_Score.getNbTarget();
		int nb = data.getNbRows();
		ClassificationStat stat = new ClassificationStat(dim);
		for (int i = 0; i < nb; i++) {
			m_Score.multiScore(data.getNumeric(i), m_CValues);
			stat.update(i, m_CValues);
		}
		stat.calcMean();
		m_Table.setDefault(stat);
	}
	
	public void addExample(ClusData data, int i, ClusStatistic pred) {
		m_Score.multiScore(data.getNumeric(i), m_CValues);
		m_Table.addExample(m_CValues, pred.getNominalPred());
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		m_Score.multiScore(tuple.m_Doubles, m_CValues);
		m_Table.addExample(m_CValues, pred.getNominalPred());
	}	
		
	public void add(ClusError other) {
		MultiScoreError oe = (MultiScoreError)other;
		m_Table.add(oe.m_Table);
	}	
	
	public void showModelError(PrintWriter out, int detail) {
		m_Table.showModelError(out, detail);
	}
	
	public String getName() {
		return "Multi-score Error";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new MultiScoreError(par, m_Score);
	}
}
