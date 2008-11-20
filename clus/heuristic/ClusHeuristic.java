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

import clus.statistic.*;
import clus.data.rows.*;

public abstract class ClusHeuristic {

	public final static double DELTA = 1e-6;
	
	// Access to the training data
	protected RowData m_TrainData;
	
	// Value of the heuristic on the training data
	protected double m_TrainDataHeurValue;

	public void setData(RowData data) {
	}

	public void setRootStatistic(ClusStatistic stat) {
	}

	public abstract double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing);

	public abstract String getName();

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic[] c_pstat, int nbsplit) {
		return Double.NEGATIVE_INFINITY;
	}

	public static double nonZero(double val) {
		if (val < 1e-6) return Double.NEGATIVE_INFINITY;
		return val;
	}

	public RowData getTrainData() {
		return m_TrainData;
	}

	public void setTrainData(RowData data) {
		m_TrainData = data;
	}

	public double getTrainDataHeurValue() {
		return m_TrainDataHeurValue;
	}

	public void setTrainDataHeurValue(double value) {
		m_TrainDataHeurValue = value;
	}
	
}
