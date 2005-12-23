/*
 * Created on May 17, 2005
 */
package clus.ext.hierarchical;

import java.io.*;

import org.apache.commons.math.distribution.*;
import org.apache.commons.math.*;

import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;
import clus.util.*;

import jeans.util.array.*;

public class WHTDStatistic extends RegressionStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected static DistributionFactory m_Fac = DistributionFactory.newInstance();
	
	protected ClassHierarchy m_Hier;
	protected ClassesTuple m_MeanTuple;
	protected double[] m_DiscrMean;	
	protected WHTDStatistic m_Global, m_Validation;
	protected double m_SigLevel;
	protected double m_Threshold = 50.0;
		
	public WHTDStatistic(ClassHierarchy hier) {
		this(hier, false);
	}

	public WHTDStatistic(ClassHierarchy hier, boolean onlymean) {
		super(hier.getDummyAttrs(), onlymean);
		m_Hier = hier;		
	}
	
	public void setValidationStat(WHTDStatistic valid) {
		m_Validation = valid;
	}
	
	public void setGlobalStat(WHTDStatistic global) {
		m_Global = global;
	}	
	
	public void setSigLevel(double sig) {
		m_SigLevel = sig;
	}
	
	public void setThreshold(double threshold) {
		m_Threshold = threshold;
	}
	
	public ClusStatistic cloneStat() {
		return new WHTDStatistic(m_Hier, false);
	}
	
	public ClusStatistic cloneSimple() {
		WHTDStatistic res = new WHTDStatistic(m_Hier, true);
		res.m_Threshold = m_Threshold;
		if (m_Validation != null) {
			res.m_Validation = (WHTDStatistic)m_Validation.cloneSimple();
			res.m_Global = m_Global;
			res.m_SigLevel = m_SigLevel;
		}
		return res;
	}
	
	public void copyAll(ClusStatistic other) {
		super.copy(other);
		WHTDStatistic my_other = (WHTDStatistic)other; 
		m_Global = my_other.m_Global;
		m_Validation = my_other.m_Validation;
		m_SigLevel = my_other.m_SigLevel;
	}	

	public void addPrediction(ClusStatistic other, double weight) {
		WHTDStatistic or = (WHTDStatistic)other;
		super.addPrediction(other, weight);
		if (m_Validation != null) {
			m_Validation.addPrediction(or.m_Validation, weight);
		}
	}
	
	public void updateWeighted(DataTuple tuple, double weight) {
		int sidx = m_Hier.getType().getArrayIndex();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
		m_SumWeight += weight;
		// Add one to the elements in the tuple, zero to the others
		for (int j = 0; j < tp.size(); j++) {
			ClassesValue val = tp.elementAt(j);
			int idx = val.getIndex();
			// if (Settings.VERBOSE > 10) System.out.println("idx = "+idx+" weight = "+weight);
			m_SumValues[idx] += weight;
			m_SumSqValues[idx] += weight;
		}
		// No missing values in the hierarchy
		for (int i = 0; i < m_SumWeights.length; i++) {
			m_SumWeights[i] += weight;
		}
	}
	
	public boolean isValid() {
		// There is a valid prediction the weight of a non-root class is non-zero
		for (int i = 1; i < m_NbAttrs; i++) {
			if (getMean(i) >= 1) return true;
		}
		return false;
	}
	
	public final ClassHierarchy getHier() {
		return m_Hier;
	}
	
	public int getNbPredictedClasses() {
		int count = 0;
		for (int i = 1; i < m_DiscrMean.length; i++) {
			if (m_DiscrMean[i] > 0.5) {
				count++;
			}
		}
		return count;			
	}
	
	public void calcMean() {
		super.calcMean();
		m_MeanTuple = m_Hier.getBestTupleMaj(m_Means, m_Threshold);
		m_DiscrMean = m_MeanTuple.getVectorWithParents(m_Hier);
		performSignificanceTest();
	}
	
	public void performSignificanceTest() {
		if (m_Validation != null) {
			for (int i = 0; i < m_DiscrMean.length; i++) {
				if (m_DiscrMean[i] > 0.5) {
					/* Predicted class i, check sig? */
					int pop_tot = (int)Math.round(m_Global.getTotalWeight());
					int pop_cls = (int)Math.round(m_Global.getTotalWeight()*m_Global.m_Means[i]);
					int rule_tot = (int)Math.round(m_Validation.getTotalWeight());
					int rule_cls = (int)Math.round(m_Validation.getTotalWeight()*m_Validation.m_Means[i]);
					int upper = Math.min(rule_tot, pop_cls);
					int nb_other = pop_tot - pop_cls;
					int min_this = rule_tot - nb_other;
					int lower = Math.max(rule_cls, min_this);
					if (rule_cls < min_this || lower > upper) {
						System.err.println("BUG?");
						System.out.println("rule = "+m_Validation.getTotalWeight()*m_Validation.m_Means[i]);
						System.out.println("pop_tot = "+pop_tot+" pop_cls = "+pop_cls+" rule_tot = "+rule_tot+" rule_cls = "+rule_cls);
					}
					HypergeometricDistribution dist = m_Fac.createHypergeometricDistribution(pop_tot, pop_cls, rule_tot);
					try {
						double stat = dist.cumulativeProbability(lower, upper);
						if (stat >= m_SigLevel) {
							m_DiscrMean[i] = 0.0;
						}
					} catch (MathException me) {
						System.err.println("Math error: "+me.getMessage());
					}
				}
			}
			// Treshold of 0.5 is ok because components of m_DiscrMean are 0 or 1.
			m_MeanTuple = m_Hier.getBestTupleMaj(m_DiscrMean, 0.5);
		}	
	}
	
	public void setMeanTuple(ClassesTuple tuple) {
		m_MeanTuple = tuple;
		m_DiscrMean = m_MeanTuple.getVector(m_Hier);
		for (int i = 0; i < m_DiscrMean.length; i++) {
			if (m_DiscrMean[i] > 0.5) m_Means[i] = 1.0;
		}
	}
	
	public ClassesTuple getMeanTuple() {
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
		return m_MeanTuple.toStringHuman()+" ["+ClusFormat.TWO_AFTER_DOT.format(getTotalWeight())+"]";
	}
	
	public String getPredictString() {
		return "["+m_MeanTuple.toStringHuman()+"]";
	}
	
	public boolean isValidPrediction() {
		return m_MeanTuple.isValidPrediction();
	}
	
	public void showRootInfo() {
		try {
			double[] weights = m_Hier.getWeights();
			PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream("hierarchy.txt")));
			wrt.println("Hier #nodes: "+m_Hier.getTotal());
			wrt.println("Hier classes by level: "+MIntArray.toString(m_Hier.getClassesByLevel()));
			wrt.println("Root weighs: "+weights[m_Hier.getRoot().getIndex()]);
			m_Hier.print(wrt, m_SumValues, null);
			wrt.close();
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		}
	}
	
	public void printDistributionRec(PrintWriter out, ClassTerm node) {
		int idx = node.getIndex();
		ClassesValue val = new ClassesValue(node);
		out.println(val.toPathString()+", "+m_Means[idx]);
		for (int i = 0; i < node.getNbChildren(); i++) {
			printDistributionRec(out, (ClassTerm)node.getChild(i));
		}
	}
	
	public void printDistribution(PrintWriter wrt) throws IOException {
		wrt.println("Total: "+m_SumWeight);
		printDistributionRec(wrt, m_Hier.getRoot());
	}

	public void getExtraInfoRec(ClassTerm node, double[] discrmean, StringBuffer out) {
		if (m_Validation != null) {
			int i = node.getIndex();
			if (discrmean[i] > 0.5) {
				/* Predicted class i, check sig? */
				int pop_tot = (int)Math.round(m_Global.getTotalWeight());
				int pop_cls = (int)Math.round(m_Global.getTotalWeight()*m_Global.m_Means[i]);
				int rule_tot = (int)Math.round(m_Validation.getTotalWeight());
				int rule_cls = (int)Math.round(m_Validation.getTotalWeight()*m_Validation.m_Means[i]);
				int upper = Math.min(rule_tot, pop_cls);
				int nb_other = pop_tot - pop_cls;
				int min_this = rule_tot - nb_other;
				int lower = Math.max(rule_cls, min_this);				
				HypergeometricDistribution dist = m_Fac.createHypergeometricDistribution(pop_tot, pop_cls, rule_tot);
				try {
					double stat = dist.cumulativeProbability(lower, upper);
					out.append(node.toString()+":");
					out.append(" pop_tot = "+String.valueOf(pop_tot));
					out.append(" pop_cls = "+String.valueOf(pop_cls));
					out.append(" rule_tot = "+String.valueOf(rule_tot));
					out.append(" rule_cls = "+String.valueOf(rule_cls));
					out.append(" upper = "+String.valueOf(upper));					
					out.append(" prob = "+ClusFormat.FOUR_AFTER_DOT.format(stat));
					out.append("\n");
				} catch (MathException me) {
					System.err.println("Math error: "+me.getMessage());
				}
			}
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			getExtraInfoRec((ClassTerm)node.getChild(i), discrmean, out);
		}
	}

	public String getExtraInfo() {
		StringBuffer res = new StringBuffer();		
		ClassesTuple meantuple = m_Hier.getBestTupleMaj(m_Means, 50.0);
		double[] discrmean = meantuple.getVectorWithParents(m_Hier);
		getExtraInfoRec(m_Hier.getRoot(), discrmean, res);
		return res.toString();
	}
}
