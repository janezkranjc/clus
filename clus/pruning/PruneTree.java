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

package clus.pruning;

import clus.data.rows.RowData;
import clus.main.*;
import clus.util.ClusException;

public class PruneTree {
	
	protected ClusNode m_CrTree, m_OrigTree;

	public void prune(ClusNode node) throws ClusException {
	}
	
	public void setTrainingData(RowData data) {
	}
	
// Methods for pruners that may return more than one result
	public int getNbResults() {
		return 1;
	}
	
	public String getPrunedName(int i) {
		if (getNbResults() == 1) {
			return "Pruned";
		} else {
			return "P"+(i+1);
		}
	}
	
	public void prune(int result, ClusNode node) throws ClusException {
		prune(node);
	}
	
// Methods for sequence based pruners, such as CartPruning and SizeConstraintPruning
	public void sequenceInitialize(ClusNode node) {
	}

	public void sequenceReset() {
	}
	
	public ClusNode sequenceNext() {
		return null;
	}
	
	public void sequenceToElemK(ClusNode node, int k) {
	}
	
	public ClusNode getCurrentTree() {
		return m_CrTree;
	}
	
	public void setCurrentTree(ClusNode node) {
		m_CrTree = node;
	}
	
	public ClusNode getOriginalTree() {
		return m_OrigTree;
	}
	
	public void setOriginalTree(ClusNode node) {
		m_OrigTree = node;
	}
}
