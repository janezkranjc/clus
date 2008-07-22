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

/*
 * Created on May 10, 2005
 */
package clus.error;

import clus.data.rows.*;
import clus.data.type.*;
import clus.util.*;

public class CorrelationMatrixComputer {

	PearsonCorrelation[][] m_Matrix;

	public void compute(RowData data) {
		ClusSchema schema = data.getSchema();
		NumericAttrType[] attrs = schema.getNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
		int nb_num = attrs.length;
		m_Matrix = new PearsonCorrelation[nb_num][nb_num];
		NumericAttrType[] crtype = new NumericAttrType[1];
		crtype[0] = new NumericAttrType("corr");
		ClusErrorList par = new ClusErrorList();
		for (int i = 0; i < nb_num; i++) {
			for (int j = 0; j < nb_num; j++) {
				m_Matrix[i][j] = new PearsonCorrelation(par, crtype);
			}
		}
		double[] a1 = new double[1];
		double[] a2 = new double[1];
		par.setNbExamples(data.getNbRows());
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			for (int j = 0; j < nb_num; j++) {
				for (int k = 0; k < nb_num; k++) {
					a1[0] = attrs[j].getNumeric(tuple);
					a2[0] = attrs[k].getNumeric(tuple);
					m_Matrix[j][k].addExample(a1, a2);
				}
			}
		}
	}

	public void printMatrixTeX() {
		int nb_num = m_Matrix.length;
		System.out.println("Number of numeric: "+nb_num);
		System.out.println();
		System.out.print("\\begin{tabular}{");
		for (int i = 0; i < nb_num+2; i++) {
			System.out.print("l");
		}
		System.out.println("}");
		for (int i = 0; i < nb_num; i++) {
			System.out.print(" & "+(i+1));
		}
		System.out.println("& Avg.");
		System.out.println("\\\\");
		for (int i = 0; i < nb_num; i++) {
			System.out.print(i+1);
			double avg = 0;
			double cnt = 0;
			for (int j = 0; j < nb_num; j++) {
				double corr = m_Matrix[i][j].getCorrelation(0);
				if (i != j) {
					avg += corr; cnt ++;
				}
				System.out.print(" & "+ClusFormat.THREE_AFTER_DOT.format(corr));
			}
			System.out.print(" & "+ClusFormat.THREE_AFTER_DOT.format(avg/cnt));
			System.out.println("\\\\");
		}
		System.out.println("\\end{tabular}");
	}

}
