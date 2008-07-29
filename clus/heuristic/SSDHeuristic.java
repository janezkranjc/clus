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

package clus.heuristic;

import clus.main.*;
import clus.statistic.*;
import clus.data.rows.*;
import clus.data.attweights.*;

public class SSDHeuristic extends ClusHeuristic {

	protected RowData m_Data;
	protected String m_BasicDist;
	protected ClusStatistic m_NegStat;
	protected ClusAttributeWeights m_TargetWeights;

	public SSDHeuristic(String basicdist, ClusStatistic negstat, ClusAttributeWeights targetweights) {
		m_BasicDist = basicdist;
		m_NegStat = negstat;
		m_TargetWeights = targetweights;
	}

	public void setData(RowData data) {
		m_Data = data;
	}

	public double calcHeuristic(ClusStatistic tstat, ClusStatistic pstat, ClusStatistic missing) {
		double n_tot = tstat.m_SumWeight;
		double n_pos = pstat.m_SumWeight;
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Calculate value
		//System.out.println("Inside calcHeuristic()");
		double ss_tot = tstat.getSS(m_TargetWeights, m_Data);
		//System.out.println("SS-tot: "+ss_tot);
		double ss_pos = pstat.getSS(m_TargetWeights, m_Data);
		//System.out.println("SS-pos: "+ss_pos);
		m_NegStat.copy(tstat);
		m_NegStat.subtractFromThis(pstat);
		double ss_neg = m_NegStat.getSS(m_TargetWeights, m_Data);
		//System.out.println("SS-neg: "+ss_neg);
		//System.out.println("DONE.");
		double value = FTest.calcSSHeuristic(n_tot, ss_tot, ss_pos+ss_neg);
		if (Settings.VERBOSE >= 10) {
			System.out.println("TOT: "+tstat.getDebugString());
			System.out.println("POS: "+pstat.getDebugString());
			System.out.println("NEG: "+m_NegStat.getDebugString());
			System.out.println("-> ("+ss_tot+", "+ss_pos+", "+ss_neg+") "+value);
		}
		if (value < 1e-6) return Double.NEGATIVE_INFINITY;
		return value;
	}

	public String getName() {
		return "SS Reduction ("+m_BasicDist+", "+m_TargetWeights.getName()+") (FTest = "+FTest.getSettingSig()+")";
	}
}
