
package clus.ext.hierarchical;

import clus.tools.debug.*;

import clus.error.*;
import clus.data.rows.*;
import clus.statistic.*;
import clus.util.*;
import clus.main.*;

import java.io.*;
import java.util.*;

import jeans.tree.*;
import jeans.util.array.*;

public class HierBinNodeAccuracy extends ClusError {
	
	protected ClassesAttrType m_Attr;
	
	protected double m_Accuracy, m_SumWeight;
	protected double[] m_A, m_B, m_C, m_D;   
	protected transient boolean[] m_Predict;
	protected transient boolean[] m_Actual;    
	
	public HierBinNodeAccuracy(ClusErrorParent par, ClassesAttrType attr) {
		super(par, 0);
		m_Attr = attr;
		int total = m_Attr.getHier().getTotal();
		m_A = new double[total];
		m_B = new double[total];
		m_C = new double[total];
		m_D = new double[total];
		m_Predict = new boolean[total];
		m_Actual  = new boolean[total];
		m_Accuracy = 0.0;
		m_SumWeight = 0.0;
	}
	
	public void add(ClusError other) {
		HierBinNodeAccuracy err = (HierBinNodeAccuracy)other;
		MDoubleArray.add(m_A, err.m_A);
		MDoubleArray.add(m_B, err.m_B);
		MDoubleArray.add(m_C, err.m_C);
		MDoubleArray.add(m_D, err.m_D);
		m_Accuracy += err.m_Accuracy;
		m_SumWeight += err.m_SumWeight;
	}
	
	public void showModelError(PrintWriter out, int detail) {
		int mxdepth = m_Attr.getHier().getMaxDepth();
		double[] nb  = new double[mxdepth];
		double[] acc = new double[mxdepth];
		double[] tp  = new double[mxdepth];	
		double[] fp  = new double[mxdepth];	
		CompleteTreeIterator it_i = m_Attr.getHier().getRootIter();
		while (it_i.hasMoreNodes()) {
			ClassTerm ni = (ClassTerm)it_i.getNextNode();
			int depth = ni.getLevel();
			int idx = ni.getIndex();
			double Tplus = m_A[idx] + m_C[idx];
			double Tmin  = m_B[idx] + m_D[idx];
			acc[depth] += (m_A[idx] + m_D[idx]) / (Tplus + Tmin);
			tp[depth] += m_A[idx] / Tplus;
			fp[depth] += m_B[idx] / Tmin;
			nb[depth]++;
		}
		out.println();
		out.print(getPrefix()+"   "+"ACC: ");	
		ClusFormat.printArray(out, acc, nb, ClusFormat.MM3_AFTER_DOT);	
		out.println();
		out.print(getPrefix()+"   "+"TP:  ");
		ClusFormat.printArray(out, tp, nb, ClusFormat.MM3_AFTER_DOT);
		out.println();
		out.print(getPrefix()+"   "+"FP:  ");
		ClusFormat.printArray(out, fp, nb, ClusFormat.MM3_AFTER_DOT);
		out.println();	
		if (Debug.HIER_ERROR_DEBUG) {
			out.print(getPrefix()+"   "+"A:  ");
			ClusFormat.printArray(out, m_A, ClusFormat.TWO_AFTER_DOT);
			out.println();	
			out.print(getPrefix()+"   "+"B:  ");
			ClusFormat.printArray(out, m_B, ClusFormat.TWO_AFTER_DOT);
			out.println();	
			out.print(getPrefix()+"   "+"C:  ");
			ClusFormat.printArray(out, m_C, ClusFormat.TWO_AFTER_DOT);
			out.println();	
			out.print(getPrefix()+"   "+"D:  ");
			ClusFormat.printArray(out, m_D, ClusFormat.TWO_AFTER_DOT);
			out.println();
		}
		out.print(getPrefix()+"   "+"Accuracy:  "+(m_Accuracy/m_SumWeight));
		out.println();	
	}
	
	public void addExample(ClusData data, int idx, ClusStatistic pred) {
		System.out.println("HierBinNodeAccuracy: addExample/3 not implemented");
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double[] counts = ((HierStatistic)pred).getCounts();
		double totwi = pred.m_SumWeight;
		for (int i = 0; i < counts.length; i++) {
			m_Predict[i] = counts[i]/totwi >= 0.5;
			m_Actual[i] = false;
		}
		ClassesTuple actual = (ClassesTuple)tuple.getObjVal(m_Attr.getSpecialIndex());
		for (int j = 0; j < actual.size(); j++) {
			int idx = actual.elementAt(j).getIndex();
			m_Actual[idx] = true;
		}
		boolean all_equal = true;
		double weight = tuple.getWeight();	
		for (int i = 0; i < counts.length; i++) {
			if (m_Actual[i] != m_Predict[i]) {
				all_equal = false;
			}
			if (m_Actual[i]) {
				if (m_Predict[i]) m_A[i] += weight;
				else m_C[i] += weight;
			} else {
				if (m_Predict[i]) m_B[i] += weight;
				else m_D[i] += weight;
			}
		}
		if (all_equal) m_Accuracy = m_Accuracy + weight;
		m_SumWeight = m_SumWeight + weight;
	}
	
	public double getModelError() {
		System.out.println("HierBinNodeAccuracy: getModelError/0 not implemented");
		return 0.0;
	}
	
	public void reset() {
		Arrays.fill(m_A, 0.0);
		Arrays.fill(m_B, 0.0);	
		Arrays.fill(m_C, 0.0);
		Arrays.fill(m_D, 0.0);		
	}		
	
	public String getName() {
		return "Mean Level Binary Node Accuracy";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new HierBinNodeAccuracy(par, m_Attr);
	}
}
