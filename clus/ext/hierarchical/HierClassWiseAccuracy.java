/*
 * Created on May 26, 2005
 */
package clus.ext.hierarchical;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Arrays;

import clus.data.rows.DataTuple;
import clus.error.ClusError;
import clus.error.ClusErrorParent;
import clus.statistic.ClusStatistic;

public class HierClassWiseAccuracy extends ClusError {
	
	protected ClassHierarchy m_Hier;
	protected double[] m_Predicted;
	protected double[] m_Correct;
	protected double[] m_Perfect; //the classes that should have been predicted
	protected double[] m_Default;
	protected double m_Cover;
	
	public HierClassWiseAccuracy(ClusErrorParent par, ClassHierarchy hier) {
		super(par, hier.getTotal());
		m_Hier = hier;
		m_Predicted = new double[m_Dim];
		m_Correct = new double[m_Dim];
		m_Perfect = new double[m_Dim];
		m_Default = new double[m_Dim];
	}

	public void addExample(DataTuple tuple, ClusStatistic pred) {
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);		
		if (!((WHTDStatistic)pred).getMeanTuple().isRoot()) {			
			double[] predarr = ((WHTDStatistic)pred).getDiscretePred();
			for (int i = 1; i < m_Dim; i++) {
				if (predarr[i] > 0.5) {
					/* Predicted this class, was it correct? */
					m_Predicted[i] += 1.0;
					if (tp.hasClass(i)) {
						m_Correct[i] += 1.0;
					}
				}
			}
			m_Cover += 1.0;
			//new variable m_Perfect to calculate recall
			//for (int i = 1; i < m_Dim; i++){
			//	if (tp.hasClass(i)) m_Perfect[i] += 1.0;
			//}
		}
		tp.updateDistribution(m_Default, 1.0);		
	}
	
	public double getModelError() {		
		return 1.0 - getAccuracy();
	}
	
	public boolean shouldBeLow() {
		return true;
	}	
	
	/* actually returns the precision */
	public double getAccuracy() {
		double tot_pred = 0.0;
		double tot_corr = 0.0;
		for (int i = 0; i < m_Dim; i++) {
			tot_pred += m_Predicted[i];
			tot_corr += m_Correct[i];
		}
		return tot_pred == 0.0 ? 0.0 : tot_corr/tot_pred;
	}
	
	public double getRecallOld() { // there is a mistake in working with m_Perfect
		double tot_corr = 0.0;
		double tot_perf = 0.0;
		//need variable with the number of classes that had to be predicted (m_Perfect)
		for (int i = 0; i < m_Dim; i++){
			tot_corr += m_Correct[i];
			tot_perf += m_Perfect[i];
		}
		//System.out.println("corr: "+tot_corr+" perf: "+tot_perf);
		return tot_perf == 0 ? 0.0 : tot_corr / tot_perf;
	}
	
	//temporary method for debugging purposes
	public double getRecall() {
		double tot_corr = 0.0;
		double tot_def = 0.0;
		for (int i = 0; i < m_Dim; i++){
			tot_corr += m_Correct[i];
			tot_def += m_Default[i];
		}
		return tot_def == 0 ? 0.0 : tot_corr / tot_def;
	}
		
	public double getCoverage() {
		int nb = getNbExamples();
		return nb == 0 ? 0.0 : m_Cover / nb;		
	}
	
	public void reset() {
		Arrays.fill(m_Correct, 0.0);
		Arrays.fill(m_Predicted, 0.0);
		Arrays.fill(m_Default, 0.0);
		m_Cover = 0.0;
	}
	
	
	//TODO: add m_Perfect
	public void add(ClusError other) {
		HierClassWiseAccuracy acc = (HierClassWiseAccuracy)other;
		m_Cover += acc.m_Cover;
		for (int i = 0; i < m_Dim; i++) {
			m_Correct[i] += acc.m_Correct[i];
			m_Predicted[i] += acc.m_Predicted[i];
			m_Default[i] += acc.m_Default[i];
		}		
	}
	
	//prints the evaluation results for each single predicted class
	//added a value for recall (next to def and acc)
	public void printNonZeroAccuracies(NumberFormat fr, PrintWriter out, ClassTerm node) {
		int idx = node.getIndex();
		if (m_Predicted[idx] != 0.0) {
			int nb = getNbExamples();
			double def = nb == 0 ? 0.0 : m_Default[idx]/nb;
			//added a test
			double acc = m_Predicted[idx] == 0.0 ? 0.0 : m_Correct[idx]/m_Predicted[idx];
			//this line is added
			double rec = m_Default[idx] == 0.0 ? 0.0 : m_Correct[idx]/m_Default[idx];
			
			ClassesValue val = new ClassesValue(node);
			//adapted output somewhat for clarity
			out.println("      "+val.toPathString()+", def: "+fr.format(def)+", acc: "+fr.format(acc)+", rec: "+fr.format(rec));
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			printNonZeroAccuracies(fr, out, (ClassTerm)node.getChild(i));
		}
	}
	
	/*added getRecall2()*/
	public void showModelError(PrintWriter out, int detail) {
		NumberFormat fr = getFormat();
		out.println("precision: "+getAccuracy()+", recall: "+getRecall()+", coverage: "+getCoverage());
		printNonZeroAccuracies(fr, out, m_Hier.getRoot());
	}
	
	public String getName() {
		return "Hierarchical accuracy by class";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new HierClassWiseAccuracy(par, m_Hier);
	}	
}
