/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

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
		// System.out.println("in ACCURACY class, error = "+(avg / m_Dim));
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
	
	public void addInvalid(DataTuple tuple) {
	}	
}
