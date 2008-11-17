package clus.error;

import java.util.*;

import jeans.util.compound.DoubleBoolean;

public class ROCAndPRCurve {

	protected int m_NbPos, m_NbNeg;
	protected ArrayList m_Values = new ArrayList();
	protected ArrayList m_ROC = new ArrayList();
	protected ArrayList m_PR = new ArrayList();	
	protected double m_AreaROC;
	
	public void addExample(boolean actual, double predicted) {
		DoubleBoolean value = new DoubleBoolean(predicted, actual);
		m_Values.add(value);
		if (actual) m_NbPos++;
		else m_NbNeg++;
	}
	
	public void computeCurves() {
		enumerateThresholds();
		m_AreaROC = computeArea(m_ROC);
	}
	
	public void enumerateThresholds() {
		Collections.sort(m_Values);
		boolean first = true;
		int TP_cnt = 0, FP_cnt = 0;
		double prev = Double.NaN;
		addOutput(0, 0);
		for (int i = 0; i < m_Values.size(); i++) {
			DoubleBoolean val = (DoubleBoolean)m_Values.get(i);
			if (val.getDouble() != prev && !first) {
				addOutput(TP_cnt, FP_cnt);
			}
			if (val.getBoolean()) {
				TP_cnt++;
			} else {
				FP_cnt++;
			}
			prev = val.getDouble();
			first = false;
		}
		addOutput(TP_cnt, FP_cnt);
	}
	
	public double computeArea(ArrayList curve) {
		double area = 0.0;
		double[] prev = (double[])curve.get(0);
		for (int i = 1; i < curve.size(); i++) {
			double[] pt = (double[])curve.get(i);
			area += 0.5*(pt[1]+prev[1])/(prev[0]-pt[0]);
			prev = pt;
		}
		return area;
	}
	
	public void addOutput(int TP, int FP) {
		addOutputROC(TP, FP);
		addOutputPR(TP, FP);
	}

	public void addOutputROC(int TP, int FP) {
		double[] point = new double[2];
		point[0] = (double)FP / m_NbNeg;
		point[1] = (double)TP / m_NbPos;
		m_ROC.add(point);
	}

	public void addOutputPR(int TP, int FP) {
		int P = TP + FP;
		if (P != 0) {
			double Prec = (double)TP / P;	
			double Reca = (double)TP / m_NbPos;

		}
	}	
}
