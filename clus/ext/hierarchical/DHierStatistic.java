package clus.ext.hierarchical;

import java.util.*;
import jeans.util.array.*;

import jeans.list.*;

import clus.main.Settings;
import clus.statistic.*;
import clus.data.rows.*;
import clus.util.*;

public class DHierStatistic extends HierStatistic {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected transient BitList m_Bits = new BitList();
	protected transient boolean m_Modified = true;
	protected transient double m_Value;

	public DHierStatistic(ClassHierarchy hier) {
		super(hier);
	}
	
	public ClusStatistic cloneStat() {		
		DHierStatistic stat = new DHierStatistic(m_Hier);
		stat.cloneFrom(this);		
		return stat;
	}
	
	public ClusStatistic cloneSimple() {
		DHierStatistic stat = new DHierStatistic(m_Hier);
		stat.cloneFrom(this);
		return stat;
	}	
	
	public void cloneFrom(DHierStatistic other) {
		int nb = other.m_Bits.size();
		if (nb > 0) {
			System.out.println("Cloned nonzero size bitvector stat");
			setSDataSize(nb);
		}
	}
	
	public void optimizePreCalc(RowData data) {
		if (!m_Modified) return;				
		calcMean();
		int nb = m_Bits.size();		
		HierSSPCalc calc = new HierSSPCalc(m_Hier.getKMatrix(), m_Hier.getType());
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				calc.addTuple(data.getTuple(i));
			}
		}
		m_Value = calc.getValue(m_MeanTuple);
		m_Modified = false;
	}	
	
	public double getSS(RowData data) {
		optimizePreCalc(data);
		return m_Value;
	}
	
	public double getDiffSS(DHierStatistic pos, RowData data) {
		int nb = m_Bits.size();
		BitList posbits = pos.m_Bits;		
		HierSSPCalc calc = new HierSSPCalc(m_Hier.getKMatrix(), m_Hier.getType());
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i) && (!posbits.getBit(i))) {
				calc.addTuple(data.getTuple(i));				
			}
		}		
		double[] diffcounts = MDoubleArray.clone(m_Counts);
		MDoubleArray.subtractFromThis(diffcounts, pos.m_Counts);
		ClassesTuple pred = m_Hier.getBestTuple(diffcounts, m_SumWeight - pos.m_SumWeight);
		return calc.getValue(pred);		
	}
					
	public void setSDataSize(int nbex) {
		m_Bits.resize(nbex);
		m_Modified = true;
	}	
		
	public void updateWeighted(DataTuple tuple, int idx) {
		double weight = tuple.getWeight();
		m_SumWeight += weight;		
		ClassesAttrType type = m_Hier.getType();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(type.getArrayIndex());
		int nb = tp.size();
		for (int i = 0; i < nb; i++) {
			int index = tp.getPosition(i);
			m_Counts[index] += weight;
		}
		m_Bits.setBit(idx);
		m_Modified = true;
	}

	public void printTree() {
		m_Hier.print(ClusFormat.OUT_WRITER, m_Counts);
		ClusFormat.OUT_WRITER.flush();		
	}
				
	public String getString(StatisticPrintInfo info) {
		return m_MeanTuple.toString();
	}
	
	
	
	public String getArrayOfStatistic() {
		return "["+m_MeanTuple.toString()+"]";
	}

	public void reset() {
		m_SumWeight = 0.0;
		Arrays.fill(m_Counts, 0.0);
		m_Bits.reset();
		m_Modified = true;		
	}

	public void copy(ClusStatistic other) {
		DHierStatistic or = (DHierStatistic)other;	
		m_SumWeight = or.m_SumWeight;
		System.arraycopy(or.m_Counts, 0, m_Counts, 0, m_Counts.length);
		m_Bits.copy(or.m_Bits);
		m_Modified = or.m_Modified;			
		m_Value = or.m_Value;		
	}
	
	public void add(ClusStatistic other) {
		DHierStatistic or = (DHierStatistic)other;		
		m_SumWeight += or.m_SumWeight;
		MDoubleArray.add(m_Counts, or.m_Counts);
		m_Bits.add(or.m_Bits);
		m_Modified = true;
	}
	
	public void addScaled(double scale, ClusStatistic other) {
		System.err.println("addScaled not implemented");
	}	
	
	public void subtractFromThis(ClusStatistic other) {
		DHierStatistic or = (DHierStatistic)other;
		m_SumWeight -= or.m_SumWeight;
		MDoubleArray.subtractFromThis(m_Counts, or.m_Counts);
		m_Bits.subtractFromThis(or.m_Bits);
		m_Modified = true;		
	}
	
	public void subtractFromOther(ClusStatistic other) {
		DHierStatistic or = (DHierStatistic)other;
		m_SumWeight = or.m_SumWeight - m_SumWeight;
		MDoubleArray.subtractFromOther(m_Counts, or.m_Counts);
		m_Bits.subtractFromOther(or.m_Bits);
		m_Modified = true;		
	}	
}
