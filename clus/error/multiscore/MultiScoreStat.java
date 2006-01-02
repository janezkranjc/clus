package clus.error.multiscore;

import java.text.*;

import clus.main.Settings;
import clus.statistic.*;
import clus.util.*;
import clus.data.cols.*;
import clus.data.rows.*;

public class MultiScoreStat extends ClusStatistic {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected int m_NbTarget;
	protected int[] m_Score;
	protected double[] m_MeanValues;

	public MultiScoreStat(ClusStatistic stat, MultiScore score) {
		m_MeanValues = stat.getNumericPred();
		m_NbTarget = m_MeanValues.length;
		m_Score = score.multiScore(m_MeanValues);
	}
	
	public String getString(StatisticPrintInfo info) {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		StringBuffer buf = new StringBuffer();				
		buf.append("[");		
		for (int i = 0; i < m_NbTarget; i++) {
			if (i != 0) buf.append(",");
			buf.append(1-m_Score[i]);
		}
		buf.append("] : [");				
		for (int i = 0; i < m_NbTarget; i++) {
			if (i != 0) buf.append(",");
//			buf.append(fr.format(m_Target.transform(m_MeanValues[i], i)));
			buf.append(fr.format(m_MeanValues[i]));
		}
		buf.append("]");		
		return buf.toString();

	}

	public double[] getNumericPred() {
		return m_MeanValues;
	}

	public int[] getNominalPred() {
		return m_Score;
	}
	
	public boolean samePrediction(ClusStatistic other) {
		MultiScoreStat or = (MultiScoreStat)other;
		for (int i = 0; i < m_NbTarget; i++) 
			if (m_Score[i] != or.m_Score[i]) return false;
		return true;
	}

	public ClusStatistic cloneStat() {
		return null;
	}
	
	public void update(ColTarget target, int idx) {
	}
	
	public void updateWeighted(DataTuple tuple, int idx) {
	}	

	public void calcMean() {
	}
	
	public void reset() {
	}

	public void copy(ClusStatistic other) {
	}
	
	public void addPrediction(ClusStatistic other, double weight) {
	}

	public void add(ClusStatistic other) {
	}
	
	public void addScaled(double scale, ClusStatistic other) {
	}		
	
	public void subtractFromThis(ClusStatistic other) {
	}
	
	public void subtractFromOther(ClusStatistic other) {
	}	
}
