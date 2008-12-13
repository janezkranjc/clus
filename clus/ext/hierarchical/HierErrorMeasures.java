package clus.ext.hierarchical;

import java.io.PrintWriter;
import java.text.NumberFormat;

import clus.data.rows.DataTuple;
import clus.error.BinaryPredictionList;
import clus.error.ClusError;
import clus.error.ClusErrorList;
import clus.error.ROCAndPRCurve;
import clus.main.Settings;
import clus.statistic.ClusStatistic;
import clus.util.ClusFormat;

public class HierErrorMeasures extends ClusError {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected ClassHierarchy m_Hier;
	protected boolean[] m_EvalClass;
	protected BinaryPredictionList[] m_ClassWisePredictions;
	protected ROCAndPRCurve[] m_ROCAndPRCurves;

	public HierErrorMeasures(ClusErrorList par, ClassHierarchy hier) {
		super(par, hier.getTotal());
		m_Hier = hier;
		m_EvalClass = hier.getEvalClassesVector();
		// m_EvalClass = new boolean[hier.getTotal()];
		// m_EvalClass[35] = true;
		m_ClassWisePredictions = new BinaryPredictionList[hier.getTotal()];
		m_ROCAndPRCurves = new ROCAndPRCurve[hier.getTotal()];
		for (int i = 0; i < hier.getTotal(); i++) {
			BinaryPredictionList predlist = new BinaryPredictionList();
			m_ClassWisePredictions[i] = predlist;
			m_ROCAndPRCurves[i] = new ROCAndPRCurve(predlist);
		}
	}

	public void addExample(DataTuple tuple, ClusStatistic pred) {
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
		double[] predarr = ((WHTDStatistic)pred).getNumericPred();
		boolean[] actual = tp.getVectorBooleanNodeAndAncestors(m_Hier);
		for (int i = 0; i < m_Dim; i++) {
			m_ClassWisePredictions[i].addExample(actual[i], predarr[i]);
		}
	}

	public void addInvalid(DataTuple tuple) {
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
		boolean[] actual = tp.getVectorBooleanNodeAndAncestors(m_Hier);
		for (int i = 0; i < m_Dim; i++) {
			m_ClassWisePredictions[i].addInvalid(actual[i]);
		}
	}

	public boolean isComputeForModel(String name) {
		if (name.equals("Original")) return true;
		if (name.equals("Pruned")) return true;
		return false;
	}

	public boolean shouldBeLow() {
		return true;
	}

	public boolean isEvalClass(int idx) {
		// Don't include trivial classes (with only pos or only neg examples)
		return m_EvalClass[idx]; // && m_ClassWisePredictions[idx].hasBothPosAndNegEx();
	}

	public void reset() {
		for (int i = 0; i < m_Dim; i++) {
			m_ClassWisePredictions[i].clear();
		}
	}

	public void add(ClusError other) {
		BinaryPredictionList[] olist = ((HierErrorMeasures)other).m_ClassWisePredictions;
		for (int i = 0; i < m_Dim; i++) {
			m_ClassWisePredictions[i].add(olist[i]);
		}
	}

	// For errors computed on a subset of the examples, it is sometimes useful
	// to also have information about all the examples, this information is
	// passed via this method in the global error measure "global"
	public void updateFromGlobalMeasure(ClusError global) {
		BinaryPredictionList[] olist = ((HierErrorMeasures)global).m_ClassWisePredictions;
		for (int i = 0; i < m_Dim; i++) {
			m_ClassWisePredictions[i].copyActual(olist[i]);
		}
	}

	// prints the evaluation results for each single predicted class
	public void printResultsRec(NumberFormat fr, PrintWriter out, ClassTerm node, boolean[] printed) {
		int idx = node.getIndex();
		// avoid printing a given node several times
		if (printed[idx]) return;
		printed[idx] = true;
		if (isEvalClass(idx)) {
			ClassesValue val = new ClassesValue(node);
			out.print("      "+idx+": "+val.toStringWithDepths(m_Hier));
			out.print(", AUROC: "+fr.format(m_ROCAndPRCurves[idx].getAreaROC()));
			out.print(", AUPRC: "+fr.format(m_ROCAndPRCurves[idx].getAreaPR()));
			out.print(", Freq: "+fr.format(m_ClassWisePredictions[idx].getFrequency()));
			out.println();
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			printResultsRec(fr, out, (ClassTerm)node.getChild(i), printed);
		}
	}

	public void printResults(NumberFormat fr, PrintWriter out, ClassHierarchy hier) {
		ClassTerm node = hier.getRoot();
		boolean[] printed = new boolean[hier.getTotal()];
		for (int i = 0; i < node.getNbChildren(); i++) {
			printResultsRec(fr, out, (ClassTerm)node.getChild(i), printed);
		}
	}

	public boolean isMultiLine() {
		return true;
	}

	public void showModelError(PrintWriter out, int detail) {
		NumberFormat fr1 = ClusFormat.SIX_AFTER_DOT;
		NumberFormat fr2 = ClusFormat.SIX_AFTER_DOT;
		System.out.println("Recomputing all curve based error measures ...");
		for (int i = 0; i < m_Dim; i++) {
			if (isEvalClass(i)) {
				m_ClassWisePredictions[i].sort();
				m_ROCAndPRCurves[i].computeCurves();
			}
		}
		// Compute averages
		int cnt = 0;
		double sumAUROC = 0.0;
		double sumAUPRC = 0.0;
		double sumAUPRCw = 0.0;
		double sumFrequency = 0.0;
		for (int i = 0; i < m_Dim; i++) {
			if (isEvalClass(i)) {
				double freq = m_ClassWisePredictions[i].getFrequency();
				sumAUROC += m_ROCAndPRCurves[i].getAreaROC();
				sumAUPRC += m_ROCAndPRCurves[i].getAreaPR();
				sumAUPRCw += freq*m_ROCAndPRCurves[i].getAreaPR();
				sumFrequency += freq;
				cnt++;
			}
		}
		double averageAUROC = sumAUROC / cnt;
		double averageAUPRC = sumAUPRC / cnt;
		double wavgAUPRC = sumAUPRCw / sumFrequency;
		out.println("Average AUROC: "+fr2.format(averageAUROC)+", Average AURPC: "+fr2.format(averageAUPRC)+", Average AURPC (weighted): "+fr2.format(wavgAUPRC));
		printResults(fr1, out, m_Hier);
	}

	public String getName() {
		return "Hierarchical error measures";
	}

	public ClusError getErrorClone(ClusErrorList par) {
		return new HierErrorMeasures(par, m_Hier);
	}
}
