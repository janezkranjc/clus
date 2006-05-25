package clus.ext.timeseries;

import java.text.NumberFormat;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.ext.sspd.SSPDMatrix;
import clus.main.Settings;
import clus.statistic.BitVectorStat;
import clus.statistic.ClusStatistic;
import clus.statistic.StatisticPrintInfo;
import clus.util.ClusFormat;

public abstract class TimeSeriesStat extends BitVectorStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	/*
	 * Aco:
	 * m_RepresentativeTS is the time series representing the claster
	*/
	public double m_SumWeightTS=0;
	public TimeSeries m_RepresentativeTS=new TimeSeries("[]");
	
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
	
	//public abstract void updateWeighted(DataTuple tuple, int idx);
	
	public abstract double calcDistance(TimeSeries t1, TimeSeries t2);
	
	public abstract String getString(StatisticPrintInfo info);
	
	public String getString(){
		return getString(StatisticPrintInfo.getInstance());
	}
	
}
