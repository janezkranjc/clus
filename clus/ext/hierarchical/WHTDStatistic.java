/*
 * Created on May 17, 2005
 */
package clus.ext.hierarchical;

import java.io.*;

import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;
import clus.util.*;

public class WHTDStatistic extends RegressionStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected ClassHierarchy m_Hier;
	protected ClassesTuple m_MeanTuple;
	protected double[] m_DiscrMean;
		
	public WHTDStatistic(ClassHierarchy hier) {
		this(hier, false);
	}

	public WHTDStatistic(ClassHierarchy hier, boolean onlymean) {
		super(hier.getTotal(), onlymean);
		m_Hier = hier;		
	}
	
	public ClusStatistic cloneStat() {
		return new WHTDStatistic(m_Hier, false);
	}
	
	public ClusStatistic cloneSimple() {
		return new WHTDStatistic(m_Hier, true);		
	}
		
	public void updateWeighted(DataTuple tuple, double weight) {
		int sidx = m_Hier.getType().getSpecialIndex();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
		m_SumWeight += weight;
		// Add one to the elements in the tuple, zero to the others
		for (int j = 0; j < tp.size(); j++) {
			ClassesValue val = tp.elementAt(j);
			int idx = val.getIndex();
			m_SumValues[idx] += weight;
			m_SumSqValues[idx] += weight;
		}
		// No missing values in the hierarchy
		for (int i = 0; i < m_SumWeights.length; i++) {
			m_SumWeights[i] += weight;
		}
	}
	
	public final ClassHierarchy getHier() {
		return m_Hier;
	}
	
	public void calcMean() {
		super.calcMean();
		m_MeanTuple = m_Hier.getBestTupleMaj(m_Means);
		m_DiscrMean = m_MeanTuple.getVectorWithParents(m_Hier);
	}
	
	protected ClassesTuple getMeanTuple() {
		return m_MeanTuple;
	}
	
	public double[] getDiscretePred() {
		return m_DiscrMean;
	}
	
	public void printTree() {
		m_Hier.print(ClusFormat.OUT_WRITER, m_SumValues);
		ClusFormat.OUT_WRITER.flush();
	}
	
	public String getString() {
//		return m_MeanTuple.toStringHuman()+" "+super.getString();
		return m_MeanTuple.toStringHuman()+" "+getTotalWeight();
	}
	
	public void showRootInfo() {
		try {
			double[] weights = m_Hier.getWeights();
			PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream("hierarchy.txt")));
			wrt.println("Hier #nodes: "+m_Hier.getTotal());
			wrt.println("Root weighs: "+weights[m_Hier.getRoot().getIndex()]);
			m_Hier.print(wrt, m_SumValues, null);
			wrt.close();
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		}
	}	
}
