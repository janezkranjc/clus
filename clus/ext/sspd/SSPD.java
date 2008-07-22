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

package clus.ext.sspd;

import clus.data.rows.*;

public class SSPD {

	public static double computeSSPDVariance(SSPDDistance dist, RowData data) {
		double sum = 0.0;
		double sumWiDiag = 0.0;
		double sumWiTria = 0.0;
		int nb = data.getNbRows();
		for (int j = 0; j < nb; j++) {
			DataTuple t1 = data.getTuple(j);
			double w1 = t1.getWeight();
			for (int i = 0; i < j; i++) {
				DataTuple t2 = data.getTuple(i);
				double wi = w1 * t2.getWeight();
				double d = dist.calcDistance(t1, t2);
				sum += wi * d * d;
				sumWiTria += wi;
			}
			sumWiDiag += w1 * w1;
		}
		return sum / (2 * sumWiTria + sumWiDiag);
	}

}
