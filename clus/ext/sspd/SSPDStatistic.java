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

import jeans.list.*;

import clus.main.Settings;
import clus.statistic.*;
import clus.util.ClusException;
import clus.data.rows.*;
import clus.data.type.ClusAttrType;
import clus.data.type.IntegerAttrType;
import clus.data.attweights.*;

// This is used in combination with SSPDHeuristic
// Pairwise distances are taken from matrix

public class SSPDStatistic extends BitVectorStat {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected SSPDMatrix m_Matrix;
	protected IntegerAttrType m_Target;
	protected double m_Value;

	public SSPDStatistic(SSPDMatrix mtrx, ClusAttrType[] target) throws ClusException {
		if (target.length != 1) {
			throw new ClusException("Only one target allowed in SSPD modus");
		}
		m_Matrix = mtrx;
		m_Target = (IntegerAttrType)target[0];
	}

	public SSPDStatistic(SSPDMatrix mtrx, IntegerAttrType target) {
		m_Matrix = mtrx;
		m_Target = target;
	}

	public ClusStatistic cloneStat() {
		SSPDStatistic stat = new SSPDStatistic(m_Matrix, m_Target);
		stat.cloneFrom(this);
		return stat;
	}

	public void copy(ClusStatistic other) {
		SSPDStatistic or = (SSPDStatistic)other;
		super.copy(or);
		m_Value = or.m_Value;
	}

	public void optimizePreCalc(RowData data) {
		if (!m_Modified) return;
		m_Value = 0.0;
		int nb = m_Bits.size();
		int idx = m_Target.getArrayIndex();
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i)) {
				DataTuple a = data.getTuple(i);
				double a_weight = a.getWeight();
				int a_idx = a.m_Ints[idx];
				for (int j = 0; j <= i; j++) {
					if (m_Bits.getBit(j)) {
						DataTuple b = data.getTuple(j);
						m_Value += a_weight*b.getWeight()*m_Matrix.get(a_idx, b.m_Ints[idx]);
					}
				}
			}
		}
		m_Modified = false;
	}

	public double getSS(RowData data) {
		optimizePreCalc(data);
		return m_Value;
	}

	public double getError(ClusAttributeWeights scale, RowData data) {
		return getSS(data);
	}

	public double getDiffSS(SSPDStatistic pos, RowData data) {
		double value = 0.0;
		int nb = m_Bits.size();
		BitList posbits = pos.m_Bits;
		int idx = m_Target.getArrayIndex();
		for (int i = 0; i < nb; i++) {
			if (m_Bits.getBit(i) && (!posbits.getBit(i))) {
				DataTuple a = data.getTuple(i);
				double a_weight = a.getWeight();
				int a_idx = a.m_Ints[idx];
				for (int j = 0; j <= i; j++) {
					if (m_Bits.getBit(j) && (!posbits.getBit(j))) {
						DataTuple b = data.getTuple(j);
						value += a_weight*b.getWeight()*m_Matrix.get(a_idx, b.m_Ints[idx]);
					}
				}
			}
		}
		return value;
	}
}
