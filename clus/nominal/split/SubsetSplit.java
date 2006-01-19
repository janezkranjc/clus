package clus.nominal.split;

import clus.main.*;
import clus.data.type.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.heuristic.*;

import java.util.*;

public class SubsetSplit extends NominalSplit {

	ClusStatistic m_PStat, m_CStat, m_MStat;

	public void initialize(ClusStatManager manager) {
		m_PStat = manager.createClusteringStat();
		m_CStat = m_PStat.cloneStat();
		m_MStat = m_PStat.cloneStat();
	}
	
	public void setSDataSize(int size) {
		m_PStat.setSDataSize(size);
		m_CStat.setSDataSize(size);
		m_MStat.setSDataSize(size);		
	}	
	
	public void showTest(NominalAttrType type, boolean[] isin, int add, double mheur, ClusStatistic tot, ClusStatistic pos) {
		int count = 0;
		System.out.print(type.getName()+ " in {");
		for (int i = 0; i < type.getNbValues(); i++) {
			if (isin[i] || i == add) {
				if (count != 0) System.out.print(",");
				System.out.print(type.getValue(i));
				count++;
			}
		}
		tot.calcMean(); pos.calcMean();
		// System.out.println("}: "+mheur+" "+tot+" "+pos);
		System.out.println("}: "+mheur);
	}

	public void findSplit(TestSelector node, NominalAttrType type) {
		double unk_freq = 0.0;		
		int nbvalues = type.getNbValues();
		boolean isin[] = new boolean[nbvalues];
		// If has missing values?
		if (type.hasMissing()) {
			ClusStatistic unknown = node.m_TestStat[nbvalues];
			m_MStat.copy(node.m_TotStat);
			m_MStat.subtractFromThis(unknown);
			unk_freq = unknown.m_SumWeight / node.m_TotStat.m_SumWeight;
		} else {
			m_MStat.copy(node.m_TotStat);
		}
		int card = 0;
		double pos_freq = 0.0;
		double bheur = Double.NEGATIVE_INFINITY;		
		if (nbvalues == 2) {
			// Handle binary splits efficiently
			card = 1;
			isin[0] = true;			
			ClusStatistic CStat = node.m_TestStat[0];
			bheur = node.calcHeuristic(m_MStat, CStat);
			// showTest(type, isin, -1, bheur, m_MStat, m_CStat);			
			pos_freq = CStat.m_SumWeight / m_MStat.m_SumWeight;
		} else {	
			// Try to add values to subsets
			// Each iteration the cardinality increases by at most one
			m_PStat.reset();			
			int bvalue = 0;	
			while (bvalue != -1) {
				bvalue = -1;
				for (int j = 0; j < nbvalues; j++) {
					if (!isin[j]) {
						// Try to add this one to the positive stat
						m_CStat.copy(m_PStat);
						m_CStat.add(node.m_TestStat[j]);
						// Calc heuristic
						double mheur = node.calcHeuristic(m_MStat, m_CStat);
						// showTest(type, isin, j, mheur, m_MStat, m_CStat);
						if (mheur > bheur) {
							bheur = mheur;
							bvalue = j;
							// Calculate pos freq (of current best one)
							pos_freq = m_CStat.m_SumWeight / m_MStat.m_SumWeight;
						}
					}
				}
				if (bvalue != -1) {
					card++;
					isin[bvalue] = true;
					m_PStat.add(node.m_TestStat[bvalue]);
				}
			}
		}
		// Found better test :-)
		if (bheur > node.m_BestHeur + ClusHeuristic.DELTA) {
			node.m_UnknownFreq = unk_freq;
			node.m_BestHeur = bheur;
			node.m_TestType = TestSelector.TYPE_TEST;
			node.m_BestTest = new SubsetTest(type, card, isin, pos_freq);
		}
	}

  public void findRandomSplit(TestSelector node, NominalAttrType type, Random rn) {
    double unk_freq = 0.0;    
    int nbvalues = type.getNbValues();
    boolean isin[] = new boolean[nbvalues];
    // If has missing values?
    if (type.hasMissing()) {
      ClusStatistic unknown = node.m_TestStat[nbvalues];
      m_MStat.copy(node.m_TotStat);
      m_MStat.subtractFromThis(unknown);
      unk_freq = unknown.m_SumWeight / node.m_TotStat.m_SumWeight;
    } else {
      m_MStat.copy(node.m_TotStat);
    }
    int card = 0;
    double pos_freq = 0.0;
    // Generate non-empty and non-full subset
    while (true) {
      for (int i = 0; i < isin.length; i++) {
        isin[i] = rn.nextBoolean();
      }
      int sum = 0;
      for (int i = 0; i < isin.length; i++) {
        if (isin[i]) {
          sum++;
        }
      }
      if (!((sum == 0) || (sum == nbvalues))) {
        card = sum;
        break;
      }
    }
    // Calculate statistics ...
    m_PStat.reset();
    for (int j = 0; j < nbvalues; j++) {
      if (isin[j]) {
         	m_PStat.add(node.m_TestStat[j]);
      }
    }
    pos_freq = m_PStat.m_SumWeight / m_MStat.m_SumWeight;
    node.m_UnknownFreq = unk_freq;
    node.m_BestHeur = node.calcHeuristic(m_MStat, m_PStat);
    node.m_TestType = TestSelector.TYPE_TEST;
    node.m_BestTest = new SubsetTest(type, card, isin, pos_freq);
  }
}
