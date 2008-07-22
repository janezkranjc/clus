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

import jeans.math.*;

public class GainHeuristic extends ClusHeuristic {

	protected boolean m_GainRatio;

	public GainHeuristic(boolean gainratio) {
		m_GainRatio = gainratio;
	}

	public final boolean isGainRatio() {
		return m_GainRatio;
	}

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		ClassificationStat tstat = (ClassificationStat)c_tstat;
		ClassificationStat pstat = (ClassificationStat)c_pstat;
		// Equal for all target attributes
		int nb = tstat.m_NbTarget;
		double n_tot = tstat.m_SumWeight;
		double n_pos = pstat.m_SumWeight;
		double n_neg = n_tot - n_pos;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Initialize entropy's
		double pos_ent = 0.0;
		double neg_ent = 0.0;
		double tot_ent = 0.0;
		// Entropy?
		for (int i = 0; i < nb; i++) {
			pos_ent += pstat.entropy(i, n_pos);
			tot_ent += tstat.entropy(i, n_tot);
			neg_ent += tstat.entropyDifference(i, pstat, n_neg);
		}
		// Gain?
		double value = tot_ent - (n_pos*pos_ent + n_neg*neg_ent)/n_tot;
		if (value < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
		if (m_GainRatio) {
			double si = ClassificationStat.computeSplitInfo(n_tot, n_pos, n_neg);
			if (si < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
			return value / si;
		}
		return value;
	}

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic[] c_pstat, int nbsplit) {
		ClassificationStat tstat = (ClassificationStat)c_tstat;
		// Acceptable?
/*
		for (int i = 0; i < nbsplit; i++)
			if (((ClassificationStat)c_pstat[i]).m_SumWeight < Settings.MINIMAL_WEIGHT)
				return Double.NEGATIVE_INFINITY;
*/
		// Total Entropy
		double value = 0.0;
		int nb = tstat.m_NbTarget;
		double n_tot = tstat.m_SumWeight;
		for (int i = 0; i < nb; i++)
			value += tstat.entropy(i, n_tot);
		// Subset entropy
		for (int i = 0; i < nbsplit; i++) {
			ClassificationStat s_stat = (ClassificationStat)c_pstat[i];
			double n_set = s_stat.m_SumWeight;
			// Calculate entropy
			double s_ent = 0.0;
			for (int j = 0; j < nb; j++)
				s_ent += tstat.entropy(j, n_set);
			// Update gain
			value -= n_set*s_ent/n_tot;
		}
		if (value < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
		if (m_GainRatio) {
			// Compute split information
			double si = 0;
			for (int i = 0; i < nbsplit; i++) {
				double n_set = ((ClassificationStat)c_pstat[i]).m_SumWeight;
				if (n_set >= MathUtil.C1E_6) {
					double div = n_set/n_tot;
					si -= div*Math.log(div);
				}
			}
			si /= MathUtil.M_LN2;
			// Return calculated gainratio
			if (si < MathUtil.C1E_6) return Double.NEGATIVE_INFINITY;
			return value / si;
		}
		return value;
	}

	public String getName() {
		return m_GainRatio ? "Gainratio" : "Gain";
	}
}

