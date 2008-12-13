package clus.error;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import clus.main.Settings;

import jeans.util.compound.DoubleBoolean;

public class BinaryPredictionList implements Serializable {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected int m_NbPos, m_NbNeg;
	protected ArrayList m_Values = new ArrayList();

	public void addExample(boolean actual, double predicted) {
		DoubleBoolean value = new DoubleBoolean(predicted, actual);
		m_Values.add(value);
		if (actual) m_NbPos++;
		else m_NbNeg++;
	}

	public void addInvalid(boolean actual) {
		if (actual) m_NbPos++;
		else m_NbNeg++;
	}

	public void sort() {
		Collections.sort(m_Values);
	}

	public int size() {
		return m_Values.size();
	}

	public DoubleBoolean get(int i) {
		return (DoubleBoolean)m_Values.get(i);
	}

	public void clear() {
		m_NbPos = 0;
		m_NbNeg = 0;
		m_Values.clear();
	}

	public int getNbPos() {
		return m_NbPos;
	}

	public int getNbNeg() {
		return m_NbNeg;
	}

	public double getFrequency() {
		return (double)m_NbPos / (m_NbPos + m_NbNeg);
	}

	public boolean hasBothPosAndNegEx() {
		return m_NbPos != 0 && m_NbNeg != 0;
	}

	public void add(BinaryPredictionList other) {
		m_NbPos += other.getNbPos();
		m_NbNeg += other.getNbNeg();
		m_Values.addAll(other.m_Values);
	}

	public void copyActual(BinaryPredictionList other) {
		m_NbPos = other.getNbPos();
		m_NbNeg = other.getNbNeg();
	}
}
