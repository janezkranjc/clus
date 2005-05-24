package clus.nominal.split;

import clus.main.*;
import clus.data.type.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.heuristic.*;

public class NArySplit extends NominalSplit {

	ClusStatistic m_MStat;

	public void initialize(ClusStatManager manager) {
		m_MStat = manager.createStatistic();	
	}
	
	public void setSDataSize(int size) {
		m_MStat.setSDataSize(size);
	}	
	
	public void findSplit(TestSelector node, NominalAttrType type) {
		double unk_freq = 0.0;
		int nbvalues = type.getNbValues();
		// If has missing values?
		if (type.hasMissing()) {
			ClusStatistic unknown = node.m_TestStat[nbvalues];
			m_MStat.copy(node.m_TotStat);
			m_MStat.subtractFromThis(unknown);
			unk_freq = unknown.m_SumWeight / node.getTotWeight();
		} else {
			m_MStat.copy(node.m_TotStat);		
		}
		// Calculate heuristic
		double mheur = node.calcHeuristic(m_MStat, node.m_TestStat, nbvalues);
		if (mheur > node.m_BestHeur + ClusHeuristic.DELTA) {
			node.m_UnknownFreq = unk_freq;
			node.m_BestHeur = mheur;
			node.m_TestType = TestSelector.TYPE_TEST;
			double[] freq = createFreqList(m_MStat.m_SumWeight, node.m_TestStat, nbvalues);
			node.m_BestTest = new NominalTest(type, freq);
		}
	}
}
