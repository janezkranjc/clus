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
	protected int m_Compatibility;
	protected int m_OptimizeMeasure;

	protected double m_AverageAUROC;
	protected double m_AverageAUPRC;
	protected double m_WAvgAUPRC;
	protected double m_PooledAUPRC;

	public HierErrorMeasures(ClusErrorList par, ClassHierarchy hier, int compat, int optimize) {
		super(par, hier.getTotal());
		m_Hier = hier;
		m_Compatibility = compat;
		m_OptimizeMeasure = optimize;
		m_EvalClass = hier.getEvalClassesVector();
		// m_EvalClass = new boolean[hier.getTotal()];
		// m_EvalClass[19] = true;
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
		return false;
	}

	public double getModelError() {
		computeAll();
		switch (m_OptimizeMeasure) {
			case Settings.HIERMEASURE_AUROC:
				return m_AverageAUROC;
			case Settings.HIERMEASURE_AUPRC:
				return m_AverageAUPRC;
			case Settings.HIERMEASURE_WEIGHTED_AUPRC:
				return m_WAvgAUPRC;
			case Settings.HIERMEASURE_POOLED_AUPRC:
				return m_PooledAUPRC;
		}
		return 0.0;
	}

	public boolean isEvalClass(int idx) {
		// Don't include trivial classes (with only pos or only neg examples)
		return m_EvalClass[idx] && includeZeroFreqClasses(idx); // && m_ClassWisePredictions[idx].hasBothPosAndNegEx();
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

	public void compatibility(ROCAndPRCurve[] curves, ROCAndPRCurve pooled) {
		double[] thr = null;
		if (m_Compatibility <= Settings.COMPATIBILITY_MLJ08) {
			thr = new double[51];
			for (int i = 0; i <= 50; i++) {
				thr[i] = (double)2*i/100.0;
			}
		}
		for (int i = 0; i < curves.length; i++) {
			curves[i].setThresholds(thr);
		}
		pooled.setThresholds(thr);
	}

	public boolean includeZeroFreqClasses(int idx) {
		// Averages never include classes with zero frequency in test set
		return m_ClassWisePredictions[idx].getNbPos() > 0;
	}

	public void computeAll() {
		BinaryPredictionList pooled = new BinaryPredictionList();
		ROCAndPRCurve pooledCurve = new ROCAndPRCurve(pooled);
		compatibility(m_ROCAndPRCurves, pooledCurve);
		for (int i = 0; i < m_Dim; i++) {
			if (isEvalClass(i)) {
				m_ClassWisePredictions[i].sort();
				m_ROCAndPRCurves[i].computeCurves();
				pooled.add(m_ClassWisePredictions[i]);
			}
		}
		pooled.sort();
		pooledCurve.computeCurves();
		// Compute averages
		int cnt = 0;
		double sumAUROC = 0.0;
		double sumAUPRC = 0.0;
		double sumAUPRCw = 0.0;
		double sumFrequency = 0.0;
		for (int i = 0; i < m_Dim; i++) {
			// In compatibility mode, averages never include classes with zero frequency in test set
			if (isEvalClass(i)) {
				double freq = m_ClassWisePredictions[i].getFrequency();
				sumAUROC += m_ROCAndPRCurves[i].getAreaROC();
				sumAUPRC += m_ROCAndPRCurves[i].getAreaPR();
				sumAUPRCw += freq*m_ROCAndPRCurves[i].getAreaPR();
				sumFrequency += freq;
				cnt++;
			}
		}
		m_AverageAUROC = sumAUROC / cnt;
		m_AverageAUPRC = sumAUPRC / cnt;
		m_WAvgAUPRC = sumAUPRCw / sumFrequency;
		m_PooledAUPRC = pooledCurve.getAreaPR();
	}

	public void showModelError(PrintWriter out, int detail) {
		NumberFormat fr1 = ClusFormat.SIX_AFTER_DOT;
		computeAll();
		out.println("Average AUROC: "+m_AverageAUROC+", Average AURPC: "+m_AverageAUPRC+", Average AURPC (weighted): "+m_WAvgAUPRC+", Pooled AURPC: "+m_PooledAUPRC);
		if (detail != ClusError.DETAIL_VERY_SMALL) {
			printResults(fr1, out, m_Hier);
		}
	}

	public String getName() {
		return "Hierarchical error measures";
	}

	public ClusError getErrorClone(ClusErrorList par) {
		return new HierErrorMeasures(par, m_Hier, m_Compatibility, m_OptimizeMeasure);
	}
}
