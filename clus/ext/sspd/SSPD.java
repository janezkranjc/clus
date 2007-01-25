
package clus.ext.sspd;

import clus.data.rows.*;

public class SSPD {

	public static double computeSSPDVariance(SSPDDistance dist, RowData data) {
		double sum = 0.0;
		double sumWiDiag = 0.0;
		double sumWiTria = 0.0;
		int nb = data.getNbRows();
		for (int j = 0; j < nb; j++) {
			DataTuple t1 = data.getTuple(j);
			double w1 = t1.getWeight();
			for (int i = 0; i < j; i++) {
				DataTuple t2 = data.getTuple(i);
				double wi = w1 * t2.getWeight();
				double d = dist.calcDistance(t1, t2);
				sum += wi * d * d;
				sumWiTria += wi;				
			}			
			sumWiDiag += w1 * w1;
		}
		return sum / (2 * sumWiTria + sumWiDiag);
	}

}
