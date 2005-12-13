package clus.ext.hierarchical;

import jeans.util.array.*;

import clus.main.Settings;
import clus.statistic.*;
import clus.data.rows.*;

public class WAHNDStatistic extends BitHierStatistic {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public WAHNDStatistic(ClassHierarchy hier) {
		super(hier);
	}
	
	public ClusStatistic cloneStat() {		
		return new WAHNDStatistic(m_Hier);
	}
	
	public ClusStatistic cloneSimple() {
		return new WAHNDStatistic(m_Hier);
	}	
	
	public void optimizePreCalc(RowData data) {
		if (!m_Modified) return;
		int nb = m_Bits.size();
		// Calculate mean
		double result = 0.0;		
		double[] mean = new double[m_Counts.length];
		ClassesAttrType type = m_Hier.getType();		
		int sidx = type.getArrayIndex();
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				System.arraycopy(m_Counts, 0, mean, 0, mean.length);
				MDoubleArray.dotscalar(mean, 1.0/m_SumWeight);				 
				DataTuple tuple = data.getTuple(i);
				ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
				for (int j = 0; j < tp.size(); j++) {
					ClassesValue val = tp.elementAt(j);
					int idx = val.getIndex();
					mean[idx] -= val.getAbundance();
				}
				double temp = 0.0;
				for(int k=0; k < mean.length; k ++) {
					temp += m_Hier.getWeight(k)*Math.abs(mean[k]);
				}
				result+=temp*temp*tuple.getWeight();
			}
		}
		m_Value = result;
		m_Modified = false;
	}	
	
	public double getSS(RowData data) {
		optimizePreCalc(data);
		return m_Value;
	}
}
