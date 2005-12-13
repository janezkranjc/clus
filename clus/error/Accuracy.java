package clus.error;

import java.io.*;

import clus.data.rows.DataTuple;
import clus.data.type.NominalAttrType;
import clus.main.Settings;
import clus.statistic.ClusStatistic;

public class Accuracy extends ClusNominalError {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected int[] m_NbCorrect;	

	public Accuracy(ClusErrorParent par, NominalAttrType[] nom) {
		super(par, nom);
		m_NbCorrect = new int[m_Dim];
	}
	
	public boolean shouldBeLow() {
		return false;
	}	
	
	public void reset() {
		for (int i = 0; i < m_Dim; i++) {
			m_NbCorrect[i] = 0;
		}
	}
		
	public void add(ClusError other) {
		Accuracy acc = (Accuracy)other;
		for (int i = 0; i < m_Dim; i++) {
			m_NbCorrect[i] += acc.m_NbCorrect[i];
		}
	}	
			
	public void showSummaryError(PrintWriter out, boolean detail) {		
		showModelError(out, detail ? 1 : 0);
	}	
	
	public double getAccuracy(int i) {
		return getModelErrorComponent(i);
	}
	
	public double getModelErrorComponent(int i) {
		return ((double)m_NbCorrect[i]) / getNbExamples();
	}
		
	public double getModelError() {
		double avg = 0.0;
		for (int i = 0; i < m_Dim; i++) {
			avg += getModelErrorComponent(i);
		}
		return avg / m_Dim;
	}
	
	public String getName() {
		return "Accuracy";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new Accuracy(par, m_Attrs);
	}

	public void addExample(DataTuple tuple, ClusStatistic pred) {
		int[] predicted = pred.getNominalPred();
		for (int i = 0; i < m_Dim; i++) {
			if (getAttr(i).getNominal(tuple) == predicted[i]) m_NbCorrect[i]++; 
		}		
	}
}
