package clus.ext.hierarchical;

import java.util.*;
import jeans.math.matrix.*;
import jeans.util.array.*;

import clus.statistic.*;
import clus.data.rows.*;

public class SPMDStatistic extends HierStatistic {

	protected transient double[] m_SumKEi;
	protected transient double m_SumEiKEi;
	protected ClassTerm m_Mean;

	public SPMDStatistic(ClassHierarchy hier) {
		this(hier, false);
	}

	public SPMDStatistic(ClassHierarchy hier, boolean simple) {
		super(hier);
		if (!simple) m_SumKEi = new double[m_Counts.length];
	}

	public final double getSS() {
		MSymMatrix KM = m_Hier.getKMatrix();
		double val = KM.xtAx(m_Counts) / m_SumWeight;
		val -= 2 * MSymMatrix.dot(m_SumKEi, m_Counts) / m_SumWeight;
		return val + m_SumEiKEi;
	}

	public final double getDiffSS(SPMDStatistic pstat) {
		MSymMatrix KM = m_Hier.getKMatrix();
		double diffw = m_SumWeight - pstat.m_SumWeight;
		double val = KM.xtAx_delta(m_Counts, pstat.m_Counts) / diffw;
		val -= 2 * MSymMatrix.dot_delta(m_SumKEi, pstat.m_SumKEi, m_Counts, pstat.m_Counts) / diffw;
		return val + m_SumEiKEi - pstat.m_SumEiKEi;
	}

	public ClusStatistic cloneStat() {
		return new SPMDStatistic(m_Hier, false);
	}

	public ClusStatistic cloneSimple() {
		return new SPMDStatistic(m_Hier, true);
	}

	public void updateWeighted(DataTuple tuple, int idx) {
		double weight = tuple.getWeight();
		MSymMatrix KM = m_Hier.getKMatrix();
		ClassesAttrType type = m_Hier.getType();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(type.getArrayIndex());
		m_SumWeight += weight;
		m_SumEiKEi += weight * KM.xtAx(tp);
		int nb = tp.size();
		for (int i = 0; i < nb; i++) {
		    double value = tp.getValue(i);
                    int index = tp.elementAt(i).getIndex();
		    m_Counts[index] += weight*value;
		    KM.addRowWeighted(m_SumKEi, index, weight*value);
		}
	}




	public void updateSpecial(DataTuple tuple) {
		double weight = tuple.getWeight();
		ClassesAttrType type = m_Hier.getType();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(type.getArrayIndex());
		int nb = tp.size();
		m_SumWeight += weight;
		double[] madd = new double[m_Counts.length];
		for (int i = 0; i < nb; i++) {
			int index = tp.getPosition(i);
			madd[index] += weight;
		}
		m_Hier.getRoot().maxWeight(madd);
		MDoubleArray.add(m_Counts, madd);
	}

	public void showUpdateSpecial(DataTuple tuple) {
		double weight = tuple.getWeight();
		ClassesAttrType type = m_Hier.getType();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(type.getArrayIndex());
		int nb = tp.size();
		m_SumWeight += weight;
		double[] madd = new double[m_Counts.length];
		for (int i = 0; i < nb; i++) {
			int index = tp.getPosition(i);
			madd[index] += weight;
		}
		m_Hier.getRoot().maxShowWeight(madd);
		MDoubleArray.add(m_Counts, madd);
	}

	public void calcMean() {
	  
	    m_Mean = m_Hier.getBestNode(m_Counts);
	    m_MeanTuple = m_Hier.getBestTuple(m_Counts, m_SumWeight);
	    
        }

	

	public String getLeafPrediction() {
		ClassTerm leaf = m_Hier.getBestLeaf(m_Counts);
		String leafstr = leaf == null ? "none" : leaf.toString();
		String meanstr = m_Mean == null ? "none" : m_Mean.toString();
		String meanstr2 = m_MeanTuple == null ? "none" : m_MeanTuple.toString();
		return meanstr + "," + leafstr + "," + meanstr2;
	}

	public void reset() {
		m_SumWeight = 0.0;
		m_SumEiKEi = 0.0;
		Arrays.fill(m_Counts, 0.0);
		Arrays.fill(m_SumKEi, 0.0);
	}

	public void copy(ClusStatistic other) {
		SPMDStatistic or = (SPMDStatistic)other;
		m_SumWeight = or.m_SumWeight;
		m_SumEiKEi = or.m_SumEiKEi;
		System.arraycopy(or.m_Counts, 0, m_Counts, 0, m_Counts.length);
		System.arraycopy(or.m_SumKEi, 0, m_SumKEi, 0, m_SumKEi.length);
	}

	public void add(ClusStatistic other) {
		SPMDStatistic or = (SPMDStatistic)other;
		m_SumWeight += or.m_SumWeight;
		m_SumEiKEi += or.m_SumEiKEi;
		MDoubleArray.add(m_Counts, or.m_Counts);
		MDoubleArray.add(m_SumKEi, or.m_SumKEi);
	}
	
	public void addScaled(double scale, ClusStatistic other) {
		System.err.println("addScaled not implemented");
	}	

	public void subtractFromThis(ClusStatistic other) {
		SPMDStatistic or = (SPMDStatistic)other;
		m_SumWeight -= or.m_SumWeight;
		m_SumEiKEi -= or.m_SumEiKEi;
		MDoubleArray.subtractFromThis(m_Counts, or.m_Counts);
		MDoubleArray.subtractFromThis(m_SumKEi, or.m_SumKEi);
	}

	public void subtractFromOther(ClusStatistic other) {
		SPMDStatistic or = (SPMDStatistic)other;
		m_SumWeight = or.m_SumWeight - m_SumWeight;
		m_SumEiKEi = or.m_SumEiKEi - m_SumEiKEi;
		MDoubleArray.subtractFromOther(m_Counts, or.m_Counts);
		MDoubleArray.subtractFromOther(m_SumKEi, or.m_SumKEi);
	}
}
