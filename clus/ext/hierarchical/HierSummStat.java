package clus.ext.hierarchical;

import jeans.tree.*;
import jeans.util.*;
import jeans.util.compound.*;

import clus.data.cols.*; 
import clus.data.rows.*; 
import clus.util.*; 
import clus.statistic.*;

public class HierSummStat extends ClusStatistic {

	HierStatistic m_Old, m_S1, m_S2, m_TrD, m_TsD;

	public HierSummStat(HierStatistic old, HierStatistic s1, HierStatistic s2, HierStatistic ts_def, HierStatistic tr_def) {
		m_Old = old;
		m_S1 = s1;
		m_S2 = s2;
		m_TrD = tr_def;
		m_TsD = ts_def;		
	}
	
	public ClusStatistic cloneStat() {
		return null;
	}

	public void update(ColTarget target, int idx) {
	}
	
	public void updateWeighted(DataTuple tuple, int idx) {
	}

	public void calcMean() {
	}
	
	public double[] calcAccumulated(ClassHierarchy hier, HierStatistic stat) {
		return hier.calcAccumulated(stat.getCounts(), stat.m_SumWeight);
	}
	
	public double getWi(HierStatistic stat, int idx) {
		return stat.m_Counts[idx]/stat.m_SumWeight;
	}	

	public String getString() {
		StringBuffer buf = new StringBuffer();
		buf.append("size: " );		
		buf.append(ClusFormat.ONE_AFTER_DOT.format(m_Old.m_SumWeight));
		buf.append(", " );
		buf.append(ClusFormat.ONE_AFTER_DOT.format(m_S1.m_SumWeight));		
		buf.append(", " );		
		buf.append(ClusFormat.ONE_AFTER_DOT.format(m_S2.m_SumWeight));

		ClassHierarchy hier = m_Old.getHier();		
		MyArray res = new MyArray();
/*		
		double[] accu_tr = calcAccumulated(hier, m_Old);
		double[] accu_ts1 = calcAccumulated(hier, m_S1);
		double[] accu_ts2 = calcAccumulated(hier, m_S2);
		double[] accu_def = calcAccumulated(hier, m_TsD);
*/		
		CompleteTreeIterator iter = hier.getNoRootIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			int idx = node.getIndex();
			if (getWi(m_S1,idx) >= 0.0) {				
				DoubleObject ob = new DoubleObject(getWi(m_S1,idx), node);
				res.addElement(ob);
			}		
		}
		res.sort();		
		for (int i = 0; i < res.size() && i < 20; i++) {
			buf.append(" ");
			DoubleObject ob = (DoubleObject)res.elementAt(i);
			ClassTerm node = (ClassTerm)ob.getObject();
			buf.append("[");
			buf.append(node.toString());
			buf.append(": ");			
			int index = node.getIndex();
			buf.append(ClusFormat.FOUR_AFTER_DOT.format(getWi(m_S1,index)));
			buf.append(", ");
			buf.append(ClusFormat.FOUR_AFTER_DOT.format(getWi(m_S2,index)));
			buf.append(", ");
			buf.append(ClusFormat.FOUR_AFTER_DOT.format(getWi(m_TsD,index)));
			buf.append("]");
		}		
/*		
		ClassesTuple pred = m_Old.m_MeanTuple;		
		for (int i = 0; i < pred.size(); i++) {
			buf.append(" acc: " );	
			double oldacc = calcAcc(m_Old, pred, i);
			double s1acc = calcAcc(m_S1, pred, i);						
			double s2acc = calcAcc(m_S2, pred, i);
			buf.append(ClusFormat.FOUR_AFTER_DOT.format(oldacc));
			buf.append(", " );
			buf.append(ClusFormat.FOUR_AFTER_DOT.format(s1acc));		
			buf.append(", " );		
			buf.append(ClusFormat.FOUR_AFTER_DOT.format(s2acc));
		}			
		for (int i = 0; i < pred.size(); i++) {
			buf.append(" def: " );	
			double tracc = calcAcc(m_TrD, pred, i);						
			double tsacc = calcAcc(m_TsD, pred, i);
			buf.append(ClusFormat.FOUR_AFTER_DOT.format(tracc));
			buf.append(", " );
			buf.append(ClusFormat.FOUR_AFTER_DOT.format(tsacc));		
		}			
*/		
		return buf.toString();
	}

	public double calcAcc(HierStatistic stat, ClassesTuple pred, int idx) {
		double[] counts = stat.getCounts();
		double w_class = counts[pred.getPosition(idx)];
		double w_tot = stat.m_SumWeight;
		return w_class / w_tot;
	}

	public void reset() {
	}

	public void copy(ClusStatistic other) {
	}
	
	public void addPrediction(ClusStatistic other, double weight) {
	}
	
	public void add(ClusStatistic other) {
	}
	
	public void addScaled(double scale, ClusStatistic other) {
	}	
	
	public void subtractFromThis(ClusStatistic other) {
	}
	
	public void subtractFromOther(ClusStatistic other) {
	}
	
	public double[] getNumericPred() {
		return null;
	}

	public int[] getNominalPred() {
		return null;	
	}
}
