package clus.statistic;

import clus.data.rows.DataTuple;

public interface ClusDistance {

	public double calcDistance(DataTuple t1, DataTuple t2);

	public String getDistanceName();

}
