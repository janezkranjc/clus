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

public class SSReductionHeuristic extends ClusHeuristic {
	
	private ClusAttributeWeights m_TargetWeights;
	private ClusAttrType[] m_Attrs;
	
	public SSReductionHeuristic(ClusAttributeWeights prod, ClusAttrType[] attrs) {
		m_TargetWeights = prod;
		m_Attrs = attrs;
	}
	
	public double calcHeuristic(ClusStatistic tstat, ClusStatistic pstat, ClusStatistic missing) {
		double n_tot = tstat.m_SumWeight;
		
		double n_pos = pstat.m_SumWeight; 
		double n_neg = n_tot - n_pos;
		
		
		
		// Acceptable?
		/*
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		*/
		if(pstat.m_nbEx <= 2 || (tstat.m_nbEx - pstat.m_nbEx) <= 2){
			return Double.NEGATIVE_INFINITY;
		}
		
		// Compute SS
		double s_ss_tot = tstat.getSS(m_TargetWeights);		
		double s_ss_pos = pstat.getSS(m_TargetWeights);
		double s_ss_neg = tstat.getSSDiff(m_TargetWeights, pstat);
		return FTest.calcSSHeuristic(n_tot, s_ss_tot, s_ss_pos, s_ss_neg);
	}
	
	public String getName() {
		return "SS-Reduction (ftest: "+Settings.FTEST_VALUE+", "+m_TargetWeights.getName(m_Attrs)+")";
	}
}
