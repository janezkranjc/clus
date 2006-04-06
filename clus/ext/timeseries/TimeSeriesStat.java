package clus.ext.timeseries;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.ext.sspd.SSPDMatrix;
import clus.main.Settings;
import clus.statistic.BitVectorStat;

public abstract class TimeSeriesStat extends BitVectorStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected SSPDMatrix m_Matrix;
	protected double m_Value;

	public double getSS(ClusAttributeWeights scale, RowData data) {
		optimizePreCalc(data);
		return m_Value;
	}

	public void optimizePreCalc(RowData data) {
		if (!m_Modified) return;
		m_Value = 0.0;		
		int nb = m_Bits.size();
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				DataTuple a = data.getTuple(i);
				TimeSeries t1 = (TimeSeries)a.getObjVal(0);
				double a_weight = a.getWeight();			
				int a_idx = a.m_Ints[0];
				for (int j = 0; j <= i; j++) {
					if (m_Bits.getBit(j)) {
						DataTuple b = data.getTuple(j);
						TimeSeries t2 = (TimeSeries)b.getObjVal(0);
						m_Value += a_weight*b.getWeight()*calcDistance(t1,t2);
					}	
				}
			}
		}
		m_Modified = false;
	}	
	
	public abstract double calcDistance(TimeSeries t1, TimeSeries t2);
	
	
}
