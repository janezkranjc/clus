package clus.ext.timeseries;

import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.RowData;
import clus.main.Settings;
import clus.statistic.ClusStatistic;

public class QDMTimeSeriesStat extends TimeSeriesStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public double calcDistance(TimeSeries t1, TimeSeries t2) {
		// Ljupco's measure if the time series are the same length
		// my proposal if they are not is cyclic, to be defined with Ljupco
		int m = Math.max(t1.length(), t2.length());
		int n = Math.min(t1.length(), t2.length());
		double[] vt1 = t1.getValues();
		double[] vt2 = t2.getValues();
		int distance = 0;
		for (int i = 1; i < m; i++) {
			for (int j = i + 1; j < m; j++) {
				distance += Math.abs(Math.signum(vt1[j] - vt1[i]) - Math.signum(vt2[j % n] - vt2[i % n]));
			}
		}
		return distance / (m * (m-1));
	}

	public double getSS(ClusAttributeWeights scale, RowData data) {
		// TODO Auto-generated method stub
		return super.getSS(scale, data);
	}

	public void optimizePreCalc(RowData data) {
		// TODO Auto-generated method stub
		super.optimizePreCalc(data);
	}

	public ClusStatistic cloneStat() {
		QDMTimeSeriesStat stat = new QDMTimeSeriesStat();
		stat.cloneFrom(this);
		return stat;
	}

	public void copy(ClusStatistic other) {
		QDMTimeSeriesStat or = (QDMTimeSeriesStat) other;
		super.copy(or);
		m_Value = or.m_Value;
	}

	public double getError(ClusAttributeWeights scale) {
		// TODO Auto-generated method stub
		return Double.POSITIVE_INFINITY;
	}

}
