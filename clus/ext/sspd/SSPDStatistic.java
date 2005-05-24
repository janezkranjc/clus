package clus.ext.sspd;

import jeans.list.*;

import clus.statistic.*;
import clus.data.rows.*;

// This is used in combination with SSPDHeuristic
// Pairwise distances are taken from matrix

public class SSPDStatistic extends BitVectorStat {

	protected SSPDMatrix m_Matrix;
	protected double m_Value;

	public SSPDStatistic(SSPDMatrix mtrx) {
		m_Matrix = mtrx;
	}

	public ClusStatistic cloneStat() {
		SSPDStatistic stat = new SSPDStatistic(m_Matrix);
		stat.cloneFrom(this);
		return stat;
	}
	
	public void copy(ClusStatistic other) {
		SSPDStatistic or = (SSPDStatistic)other;
		super.copy(or);
		m_Value = or.m_Value;
	}	
	
	public void optimizePreCalc(RowData data) {
		if (!m_Modified) return;
		m_Value = 0.0;		
		int nb = m_Bits.size();
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				DataTuple a = data.getTuple(i);
				double a_weight = a.getWeight();			
				int a_idx = a.m_Ints[0];
				for (int j = 0; j <= i; j++) {
					if (m_Bits.getBit(j)) {
						DataTuple b = data.getTuple(j);
						m_Value += a_weight*b.getWeight()*m_Matrix.get(a_idx, b.m_Ints[0]);
					}	
				}
			}
		}
		m_Modified = false;
	}	
	
	public double getSS(RowData data) {
		optimizePreCalc(data);
		return m_Value;
	}
	
	public double getDiffSS(SSPDStatistic pos, RowData data) {
		double value = 0.0;
		int nb = m_Bits.size();			
		BitList posbits = pos.m_Bits;
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i) && (!posbits.getBit(i))) {
				DataTuple a = data.getTuple(i);
				double a_weight = a.getWeight();			
				int a_idx = a.m_Ints[0];
				for (int j = 0; j <= i; j++) {
					if (m_Bits.getBit(j) && (!posbits.getBit(j))) {
						DataTuple b = data.getTuple(j);
						value += a_weight*b.getWeight()*m_Matrix.get(a_idx, b.m_Ints[0]);
					}
				}
			}
		}
		return value;
	}
}
