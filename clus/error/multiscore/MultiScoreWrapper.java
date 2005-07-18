
package clus.error.multiscore;

import clus.error.*;
import clus.main.*;
import clus.statistic.ClusStatistic;
import clus.data.rows.DataTuple;
import clus.data.type.*;

import java.io.*;

public class MultiScoreWrapper extends ClusNumericError {

	protected ClusNominalError m_Child;
	protected byte[] m_Real;
	protected int[] m_Pred;

	public MultiScoreWrapper(ClusNominalError child, NumericAttrType[] num) {
		super(child.getParent(), num);
		int dim = getDimension();
		m_Real = new byte[dim];
		m_Pred = new int[dim];
		m_Child = child;		
	}
	
	public static TargetSchema createTarSchema(TargetSchema schema) {
		int nb = schema.getNbNum();
		TargetSchema ntschema = new TargetSchema(nb, 0);
		for (int j = 0; j < nb; j++) {
			ClusAttrType at = schema.getNumType(j);
			ClusAttrType ntype = new NominalAttrType(at.getName());
			ntschema.setType(NominalAttrType.THIS_TYPE, j, ntype);
		}
		return ntschema;
	}	

	public boolean shouldBeLow() {
		return m_Child.shouldBeLow();
	}	

	public void reset() {
		m_Child.reset();
	}
	
	public double getModelError() {
		return m_Child.getModelError();
	}	
	
	public void addExample(double[] real, double[] predicted) {
		for (int i = 0; i < m_Real.length; i++) {
			m_Real[i] = (byte)(real[i] > 0.5 ? 0 : 1);
			m_Pred[i] = predicted[i] > 0.5 ? 0 : 1;
		}
		// m_Child.addExample(m_Real, m_Pred);
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double[] predicted = pred.getNumericPred();
		for (int i = 0; i < m_Dim; i++) {
			double err = m_Attrs[i].getNumeric(tuple) - predicted[i];
			// m_AbsError[i] += Math.abs(err);		 
		}
	}		
	
	public void add(ClusError other) {
		MultiScoreWrapper oe = (MultiScoreWrapper)other;
		m_Child.add(oe.m_Child);
	}
	
	public void showModelError(PrintWriter out, int detail) {
		m_Child.showModelError(out, detail);
	}
		
//	public boolean hasSummary() {
//		m_Child.hasSummary();
//	}	
	
	public String getName() {
		return m_Child.getName();
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new MultiScoreWrapper((ClusNominalError)m_Child.getErrorClone(par), m_Attrs);
	}
}
