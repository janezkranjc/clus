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

package clus.algo.rules;

import clus.main.*;
import clus.statistic.*;
import clus.data.rows.*;
import clus.data.type.ClusAttrType;
import clus.heuristic.*;
import clus.data.attweights.*;

public class ClusRuleHeuristicSSD extends ClusHeuristic {

	protected RowData m_Data;
	protected String m_BasicDist;
	protected ClusStatistic m_NegStat;
	protected ClusAttributeWeights m_TargetWeights;
	protected ClusStatManager m_StatManager;

	// Copied from SSDHeuristic.java
	public ClusRuleHeuristicSSD(ClusStatManager statManager, String basicdist, 
			ClusStatistic negstat, ClusAttributeWeights targetweights) {
		m_StatManager = statManager;
		m_BasicDist = basicdist;
		m_NegStat = negstat;
		m_TargetWeights = targetweights;
	}

	// Copied from SSDHeuristic.java
	public void setData(RowData data) {
		m_Data = data;
	}

	// Larger values are better!
	// Only the second parameter make sense for rules, i.e., statistic for covered examples
	public double calcHeuristic(ClusStatistic tstat, ClusStatistic pstat, ClusStatistic missing) {
		double n_pos = pstat.m_SumWeight;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		// Calculate value
		// System.out.println("Inside calcHeuristic()");
		double value = pstat.getSS(m_TargetWeights, m_Data);
		//System.out.print("SS: "+value);
	    // Coverage part
	  	double train_sum_w = m_StatManager.getTrainSetStat(ClusAttrType.ATTR_USE_CLUSTERING).getTotalWeight();
	    double cov_par = m_StatManager.getSettings().getHeurCoveragePar();
	    value *= Math.pow(n_pos/train_sum_w, cov_par);
		//System.out.println(", cov: "+n_pos+"/"+train_sum_w+", val: "+value); //+" -> -"+value);
	    if (value < 1e-6) return Double.NEGATIVE_INFINITY;
		return -value;
	}

	public String getName() {
		return "SS Reduction for Rules ("+m_BasicDist+", "+m_TargetWeights.getName()+")";
	}
}
