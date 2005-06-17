package clus.ext.hierarchical;

import java.util.*;
import jeans.util.array.*;

import jeans.list.*;

import clus.statistic.*;
import clus.data.rows.*;
import clus.util.*;

public class BitHierStatistic extends HierStatistic {

	protected transient BitList m_Bits = new BitList();
	protected transient boolean m_Modified = true;
	protected transient double m_Value;

	public BitHierStatistic(ClassHierarchy hier) {
		super(hier);
	}
	
	public ClusStatistic cloneStat() {		
		return new BitHierStatistic(m_Hier);
	}
	
	public ClusStatistic cloneSimple() {
		return new BitHierStatistic(m_Hier);
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
			m_Counts[index] += weight * tp.getValue(i);
		}
		m_Bits.setBit(idx);
		m_Modified = true;
	}

	public void printTree() {
		m_Hier.print(ClusFormat.OUT_WRITER, m_Counts);
		ClusFormat.OUT_WRITER.flush();		
	}
				
	public String getString() {
	    return String.valueOf(m_SumWeight);
//	    return m_MeanTuple.toString();
	}
	
	public void reset() {
		m_SumWeight = 0.0;
		Arrays.fill(m_Counts, 0.0);
		m_Bits.reset();
		m_Modified = true;		
	}

	public void copy(ClusStatistic other) {
		BitHierStatistic or = (BitHierStatistic)other;	
		m_SumWeight = or.m_SumWeight;
		System.arraycopy(or.m_Counts, 0, m_Counts, 0, m_Counts.length);
		m_Bits.copy(or.m_Bits);
		m_Modified = or.m_Modified;			
		m_Value = or.m_Value;		
	}
	
	public void add(ClusStatistic other) {
		BitHierStatistic or = (BitHierStatistic)other;		
		m_SumWeight += or.m_SumWeight;
		MDoubleArray.add(m_Counts, or.m_Counts);
		m_Bits.add(or.m_Bits);
		m_Modified = true;
	}
	
	public void addScaled(double scale, ClusStatistic other) {
		System.err.println("addScaled not implemented");
	}	
	
	public void subtractFromThis(ClusStatistic other) {
		BitHierStatistic or = (BitHierStatistic)other;
		m_SumWeight -= or.m_SumWeight;
		MDoubleArray.subtractFromThis(m_Counts, or.m_Counts);
		m_Bits.subtractFromThis(or.m_Bits);
		m_Modified = true;		
	}
	
	public void subtractFromOther(ClusStatistic other) {
		BitHierStatistic or = (BitHierStatistic)other;
		m_SumWeight = or.m_SumWeight - m_SumWeight;
		MDoubleArray.subtractFromOther(m_Counts, or.m_Counts);
		m_Bits.subtractFromOther(or.m_Bits);
		m_Modified = true;		
	}	
	
	public String getDebugString() {
		return String.valueOf(m_SumWeight) + " " + m_Bits.toVector();
	}
}
