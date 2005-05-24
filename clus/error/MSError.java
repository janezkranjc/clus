/*
 * Created on May 12, 2005
 */
package clus.error;

import java.io.PrintWriter;
import java.text.NumberFormat;

// import jeans.util.array.*;

import clus.statistic.*;

public class MSError extends ClusNumericError {
	
	protected double[] m_SqError;
	protected TargetWeightProducer m_Weights;
	protected boolean m_PrintAllComps;
	
	public MSError(ClusErrorParent par) {
		this(par, null, true);
	}
	
	public MSError(ClusErrorParent par, TargetWeightProducer weights) {
		this(par, weights, true);
	}
	
	public MSError(ClusErrorParent par, TargetWeightProducer weights, boolean printall) {
		super(par);
		m_SqError = new double[m_Dim];
		m_Weights = weights;
		m_PrintAllComps = printall;
	}
	
	public MSError(ClusErrorParent par, TargetWeightProducer weights, boolean printall, int dim) {
		super(par, dim);
		m_SqError = new double[m_Dim];
		m_Weights = weights;
		m_PrintAllComps = printall;
	}
	
	public void reset() {
		for (int i = 0; i < m_Dim; i++) {
			m_SqError[i] = 0.0;
		}				
	}
	
	public void setWeights(TargetWeightProducer weights) {
		m_Weights = weights;
	}
	
	public double getErrorComp(int i) {
		int nb = getNbExamples();
		double err = nb != 0.0 ? m_SqError[i]/nb : 0.0;
		if (m_Weights != null) err *= m_Weights.m_NumWeights[i];
		return err;
	}
	
	public double getModelError() {
		double ss_tree = 0.0;
		int nb = getNbExamples();
		for (int i = 0; i < m_Dim; i++) {
			if (m_Weights != null) ss_tree += m_SqError[i]*m_Weights.m_NumWeights[i];			
			else ss_tree += m_SqError[i];
		}
		return nb != 0.0 ? ss_tree/nb/m_Dim : 0.0;
	}	
	
	public void addExample(double[] real, double[] predicted) {
		for (int i = 0; i < m_Dim; i++) {
			double err = real[i] - predicted[i];			
			// System.out.println("Err "+MDoubleArray.toString(real)+" - "+MDoubleArray.toString(predicted));
			m_SqError[i] += err * err;
		}
	}
	
	public void add(ClusError other) {
		MSError oe = (MSError)other;
		for (int i = 0; i < m_Dim; i++) {
			m_SqError[i] += oe.m_SqError[i];
		}		
	}	
	
	public void showModelError(PrintWriter out, int detail) {
		NumberFormat fr = getFormat();
		StringBuffer buf = new StringBuffer();
		if (m_PrintAllComps) {
			buf.append("[");
			int nb = getNbExamples();
			for (int i = 0; i < m_Dim; i++) {
				if (i != 0) buf.append(",");
				buf.append(fr.format(getErrorComp(i)));
			}
			if (m_Dim > 1) buf.append("]: ");
			else buf.append("]: ");
		}
		if (m_Dim > 1 || !m_PrintAllComps) {
			buf.append(fr.format(getModelError()));		
		}
		out.println(buf.toString());
	}
	
	public void showSummaryError(PrintWriter out, boolean detail) {
		NumberFormat fr = getFormat();
		out.println(getPrefix() + "Mean over components MSE: "+fr.format(getModelError()));
	}
	
	public String getName() {
		if (m_Weights == null) return "Mean squared error (MSE)";
		else return "Weighted mean squared error (MSE) ("+m_Weights.getName()+")";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new MSError(par, m_Weights, m_PrintAllComps, m_Dim);
	}
}
