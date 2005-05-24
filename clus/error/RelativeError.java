package clus.error;

import java.io.*;
import java.text.*;

public class RelativeError extends ClusNumericError {

	protected double[] m_SumRel;
	protected double[] m_SumDefRel;

	public RelativeError(ClusErrorParent par) {
		super(par);
		m_SumRel = new double[m_Dim];
		m_SumDefRel = new double[m_Dim];
	}

        public void showModelError(PrintWriter wrt, int nmb) {}
        public void reset() {}

	public void addExample(double[] real, double[] predicted) {
		for (int i = 0; i < m_Dim; i++) {
			if (real[i] != 0.0) {
				m_SumRel[i] += Math.abs(real[i] - predicted[i])/real[i];
				m_SumDefRel[i] += Math.abs(real[i] - m_Default[i])/real[i];
			} else {
				m_SumRel[i] += Double.POSITIVE_INFINITY;
				m_SumDefRel[i] += Double.POSITIVE_INFINITY;
			}
		}
	}

	public void add(ClusError other) {
		RelativeError oe = (RelativeError)other;
		for (int i = 0; i < m_Dim; i++) {
			m_SumRel[i] += oe.m_SumRel[i];
			m_SumDefRel[i] += oe.m_SumDefRel[i];
		}
	}

	public void showDefaultError(PrintWriter out, boolean detail) {
		out.println(getPrefix() + DEFAULT_ERROR + DEFAULT_POSTFIX + showDoubleArray(m_SumDefRel));
	}

	public void showTreeError(PrintWriter out, boolean detail) {
		out.println(getPrefix() + TREE_ERROR + TREE_POSTFIX + showDoubleArray(m_SumRel));
	}

	public void showRelativeError(PrintWriter out, boolean detail) {
		out.println(getPrefix() + RELATIVE_ERROR + RELATIVE_POSTFIX + showDoubleArray(m_SumRel, m_SumDefRel));
	}

	public void showSummaryError(PrintWriter out, boolean detail) {
		NumberFormat fr = getFormat();
		double ss_def = 0.0;
		double ss_tree = 0.0;
		for (int i = 0; i < m_Dim; i++) {
			ss_tree += m_SumRel[i];
			ss_def += m_SumDefRel[i];
		}
		ss_tree /= m_Dim;
		ss_def /= m_Dim;
		double re = ss_def != 0.0 ? ss_tree / ss_def : 0.0;
		out.println(getPrefix() + "Mean over components RE: "+fr.format(re)+" = "+fr.format(ss_tree)+" / "+fr.format(ss_def));
	}

	public void calculate() {
		int nb = getNbExamples();
		for (int i = 0; i < m_Dim; i++) {
			m_SumRel[i] /= nb;
			m_SumDefRel[i] /= nb;
		}
	}

	public String getName() {
		return "Relative error";
	}

	public ClusError getErrorClone(ClusErrorParent par) {
		return new RelativeError(par);
	}
}
