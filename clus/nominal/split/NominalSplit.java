package clus.nominal.split;

import clus.main.*;
import clus.data.type.*;
import clus.model.test.*;
import clus.statistic.*;

public abstract class NominalSplit {

	public double[] createFreqList(double n_tot, ClusStatistic[] s_set, int nbvalues) {
		double[] res = new double[nbvalues];
		for (int i = 0; i < nbvalues; i++)
			res[i] = s_set[i].m_SumWeight / n_tot;
		return res;
	}

	public abstract void initialize(ClusStatManager manager);
	
	public abstract void setSDataSize(int size);
		
	public abstract void findSplit(TestSelector node, NominalAttrType type);
}
