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
import clus.data.attweights.*;
import clus.data.type.*;

public class SSReductionHeuristicMissing extends ClusHeuristic {
	
	private ClusAttributeWeights m_TargetWeights;
	private ClusAttrType[] m_Attrs;
	protected ClusStatistic m_Pos, m_Neg, m_Tot;
	
	public SSReductionHeuristicMissing(ClusAttributeWeights prod, ClusAttrType[] attrs, ClusStatistic stat) {
		m_TargetWeights = prod;
		m_Attrs = attrs;
		m_Pos = stat.cloneStat();
		m_Neg = stat.cloneStat();
		m_Tot = stat.cloneStat();		
	}
	
	public double calcHeuristic(ClusStatistic tstat, ClusStatistic pstat, ClusStatistic missing) {
		double n_tot = tstat.m_SumWeight;
		double n_pos = pstat.m_SumWeight; 
		double n_neg = n_tot - n_pos;
		// Acceptable?		
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		/*
		if(pstat.m_nbEx <= 2 || (tstat.m_nbEx - pstat.m_nbEx) <= 2){
			return Double.NEGATIVE_INFINITY;
		}
		*/
		// Compute SS		
		double pos_freq = n_pos / n_tot;
		m_Pos.copy(pstat);
		m_Neg.copy(tstat);
		m_Tot.copy(tstat);
		m_Tot.add(missing);
		m_Neg.subtractFromThis(pstat);
		m_Pos.addScaled(pos_freq, missing);			
		m_Neg.addScaled(1.0-pos_freq, missing);
		double s_ss_pos = m_Pos.getSS(m_TargetWeights);
		double s_ss_neg = m_Neg.getSS(m_TargetWeights);		
		double s_ss_tot = m_Tot.getSS(m_TargetWeights);		
		return FTest.calcSSHeuristic(n_tot, s_ss_tot, s_ss_pos, s_ss_neg);
	}
	
	public String getName() {
		return "SS-Reduction Missing Values (ftest: "+Settings.FTEST_VALUE+", "+m_TargetWeights.getName(m_Attrs)+")";
	}
}
