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

import clus.main.*;
import clus.statistic.*;
import clus.heuristic.*;
import clus.data.rows.*;

// This is used in combination with SSPDStatistic
// Pairwise distances are taken from matrix

public class SSPDHeuristic extends ClusHeuristic {

	protected RowData m_Data;

	public SSPDHeuristic() {
	}

	public void setData(RowData data) {
		m_Data = data;
	}

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		SSPDStatistic tstat = (SSPDStatistic)c_tstat;
		SSPDStatistic pstat = (SSPDStatistic)c_pstat;
		double n_tot = tstat.m_SumWeight;
		double n_pos = pstat.m_SumWeight;
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Calculate value
		double ss_tot = tstat.getSS(m_Data);
		double ss_pos = pstat.getSS(m_Data);
		double ss_neg = tstat.getDiffSS(pstat, m_Data);
		return FTest.calcSSHeuristic(n_tot, ss_tot, ss_pos, ss_neg);
	}

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic[] c_pstat, int nbsplit) {
		return Double.NEGATIVE_INFINITY;
	}

	public String getName() {
		return "Sum of Squared Pairwise Distances (SSPD)";
	}
}
