/*
 * Created on May 26, 2005
 */
package clus.ext.hierarchical;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Arrays;

import clus.util.*;
import clus.data.rows.*;
import clus.error.*;
import clus.main.*;
import clus.statistic.*;

public class HierClassWiseAccuracy extends ClusError {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected ClassHierarchy m_Hier;
	protected double[] m_Predicted;
	protected double[] m_Correct;
	protected double[] m_Default;
	
	public HierClassWiseAccuracy(ClusErrorParent par, ClassHierarchy hier) {
		super(par, hier.getTotal());
		m_Hier = hier;
		m_Predicted = new double[m_Dim];
		m_Correct = new double[m_Dim];
		//m_Perfect = new double[m_Dim];
		m_Default = new double[m_Dim];
	}

	public void addExample(DataTuple tuple, ClusStatistic pred) {
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);		
		double[] predarr = ((WHTDStatistic)pred).getDiscretePred();
		for (int i = 0; i < m_Dim; i++) {
			if (predarr[i] > 0.5) {
				/* Predicted this class, was it correct? */
				m_Predicted[i] += 1.0;
				if (tp.hasClass(i)) {
					m_Correct[i] += 1.0;
				}
			}
		}
		tp.updateDistribution(m_Default, 1.0);		
	}
	
	public void addInvalid(DataTuple tuple) {
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
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
	
	public double getAveragePrecision() {
		int cnt = 0;
		double avg = 0.0;
		for (int i = 0; i < m_Dim; i++) {
			if (m_Predicted[i] != 0) {
				cnt++;
				avg += m_Correct[i] / m_Predicted[i];
			}
		}
		return cnt == 0 ? 0 : avg/cnt;		
	}	
	
	public double getRecall() {
		double tot_corr = 0.0;
		double tot_def = 0.0;
		for (int i = 0; i < m_Dim; i++){
			tot_corr += m_Correct[i];
			tot_def += m_Default[i];
		}
		return tot_def == 0 ? 0.0 : tot_corr / tot_def;
	}
	
	public int getTP(){
		int tot_corr = 0;
		for (int i =0; i < m_Dim; i++){
			tot_corr += m_Correct[i];
		}
		return tot_corr;
	}
	
	public int getFP(){
		int tot_pred = 0;
		int tot_corr = 0;
		for (int i = 0; i < m_Dim; i++) {
			tot_pred += m_Predicted[i];
			tot_corr += m_Correct[i];
		}
		return (tot_pred - tot_corr);
	}
	
	public int getFN(){
		int tot_def = 0;
		int tot_corr = 0;
		for (int i = 0; i < m_Dim; i++) {
			tot_def += m_Default[i];
			tot_corr += m_Correct[i];
		}
		return (tot_def - tot_corr);
	}
	
	public int getNbPosExamples(){
		int tot_def = 0;
		for (int i = 0; i < m_Dim; i++) {
			tot_def += m_Default[i];
		}
		return tot_def;
	}
	
	public int getNbPosExamplesCheck(){
		return getTP() + getFN();
	}
	
	public void reset() {
		Arrays.fill(m_Correct, 0.0);
		Arrays.fill(m_Predicted, 0.0);
		Arrays.fill(m_Default, 0.0);
	}
	
	public void add(ClusError other) {
		HierClassWiseAccuracy acc = (HierClassWiseAccuracy)other;
		for (int i = 0; i < m_Dim; i++) {
			m_Correct[i] += acc.m_Correct[i];
			m_Predicted[i] += acc.m_Predicted[i];
			m_Default[i] += acc.m_Default[i];
		}		
	}
	
	// For errors computed on a subset of the examples, it is sometimes useful
	// to also have information about all the examples, this information is
	// passed via this method in the global error measure "global"
	public void updateFromGlobalMeasure(ClusError global) {
		HierClassWiseAccuracy other = (HierClassWiseAccuracy)global;
		System.arraycopy(other.m_Default, 0, m_Default, 0, m_Default.length);
	}
	
	public void printNonZeroAccuracies(NumberFormat fr, PrintWriter out, ClassTerm node) {
		for (int i = 0; i < node.getNbChildren(); i++) {
			printNonZeroAccuraciesRec(fr, out, (ClassTerm)node.getChild(i));
		}
	}
	
	//prints the evaluation results for each single predicted class
	//added a value for recall (next to def and acc)
	public void printNonZeroAccuraciesRec(NumberFormat fr, PrintWriter out, ClassTerm node) {
		int idx = node.getIndex();
		if (m_Predicted[idx] != 0.0) {
			int nb = getNbTotal();
			double def = nb == 0 ? 0.0 : m_Default[idx]/nb;
			//added a test
			double prec = m_Predicted[idx] == 0.0 ? 0.0 : m_Correct[idx]/m_Predicted[idx];
			//this line is added
			double rec = m_Default[idx] == 0.0 ? 0.0 : m_Correct[idx]/m_Default[idx];			
			//added some more lines for calculationg, TP, FP, nbPosExamples
			int TP = (int)m_Correct[idx];
			int FP = (int)(m_Predicted[idx] - m_Correct[idx]); //TODO: some kind of checking?
			int nbPos = (int)m_Default[idx];
			ClassesValue val = new ClassesValue(node);
			//adapted output somewhat for clarity
			out.print("      "+val.toPathString());
			out.print(", def: "+fr.format(def));
//			out.print(" ("+m_Default[idx]+"/"+nb+")");
			out.print(", prec: "+fr.format(prec));
//			out.print(" ("+m_Correct[idx]+"/"+m_Predicted[idx]+")");			
			out.print(", rec: "+fr.format(rec));
//			out.print(" ("+m_Correct[idx]+"/"+m_Default[idx]+")");			
			out.print(", TP: "+fr.format(TP)+", FP: "+fr.format(FP)+", nbPos: "+fr.format(nbPos));
			out.println();
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			printNonZeroAccuraciesRec(fr, out, (ClassTerm)node.getChild(i));
		}
	}
	
	// does it make sense to make averages of TP, FP and nbPos (look into this: methods implemented but not used)
	public void showModelError(PrintWriter out, int detail) {
		NumberFormat fr1 = getFormat();
		NumberFormat fr2 = ClusFormat.SIX_AFTER_DOT;
		out.print("precision: "+fr2.format(getAccuracy()));
		out.print(", recall: "+fr2.format(getRecall()));
		out.print(", coverage: "+fr2.format(getCoverage()));
		out.println();
		printNonZeroAccuracies(fr1, out, m_Hier.getRoot());
	}
	
	public String getName() {
		return "Hierarchical accuracy by class";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new HierClassWiseAccuracy(par, m_Hier);
	}	
}
