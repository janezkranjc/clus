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

package clus.algo.induce;

import java.io.*;

import jeans.util.*;

import clus.main.*;
import clus.util.*;
import clus.data.cols.*;
import clus.data.type.*;
import clus.data.cols.attribute.*;

public class LevelWiseInduce extends ClusInduce {

	public LevelWiseInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		super(schema, sett);
	}
	
	public ColTarget createTarget() {	
		return new ColTarget(m_Schema);
	}	
	
	public ClusData createData() {
		ColData data = new ColData();
		data.setTarget(createTarget());
		int nb = m_Schema.getNbAttributes();
		for (int j = 0; j < nb; j++) {
			// ClusAttrType at = m_Schema.getAttrType(j);
			// at.addToData(data);
		}
		return data;
	}

	public ClusModel induceSingleUnpruned(ClusRun cr) {
		// Create leaf Vector
		MyArray leaves = new MyArray();
		MyArray newleaves = new MyArray();
		ColData data = (ColData)cr.getTrainingSet();
		// ClusNode root = cr.getRoot();
		ClusNode root = null;
		ColTarget target = data.getColTarget();
		// Begin of induction process
		long time = System.currentTimeMillis();
		// Create root node
		leaves.addElement(root);
		// root.create(m_StatManager, m_MaxStats);
		target.addToRoot(root);
		// data.calcTotalStat(root.getTotStat());
		// Put all target values in root node
		int level = 0;
		int nb = leaves.size();
		while (nb > 0) {
			System.out.println("Level: "+level);
			// Stopping criterion for each node
			for (int i = nb-1; i >= 0; i--) {
/*				ClusNode inf = (ClusNode)leaves.elementAt(i);
				if (inf.stopCrit()) {
					inf.finish();
					leaves.removeElementAt(i);
					nb--;
				}*/
			}
			// Remove leaves from attribute lists
			// Find best test for each attribute
			int nbattr = data.getNbAttributes();
			for (int j = 0; j < nbattr; j++) {
				ClusAttribute attr = data.getAttribute(j);
				attr.findBestTest(leaves, target, m_StatManager);
					// Reset positive statistic			
					// For each attribute value			
					// Updata positive statistic
					// Calculate heuristic
					// Updata best test
			}
			// Split nodes
			newleaves.removeAllElements();
			data.resetSplitAttrs();
			// Split nodes with best test
			for (int i = 0; i < nb; i++) {
				ClusNode inf = (ClusNode)leaves.elementAt(i);
				if (inf.hasBestTest()) {
//					inf.preprocTest(ClusMode.LEVEL_WISE);					
					// Create children
					int arity = inf.updateArity();
					for (int j = 0; j < arity; j++) {
						ClusNode child = new ClusNode();
//						child.create(m_StatManager, m_MaxStats);
						inf.setChild(child, j);
						newleaves.addElement(child);
					}
					// Set split attribute
//					inf.m_SplitAttr.setSplit(true);
					inf.cleanup();
					// Output best test
//					System.out.println("Test: "+inf.getTestString()+" -> "+inf.m_BestHeur);
				} else {
//					inf.finish();
				}
			}
			// Split target value's			
			for (int j = 0; j < nbattr; j++) {
				ClusAttribute attr = data.getAttribute(j);
				if (attr.isSplit()) {
					attr.split(target);
						// Split target values according to attr
				}
			}
			System.out.println();
			MyArray tmp = leaves;
			leaves = newleaves;
			newleaves = tmp;
			nb = leaves.size();
			level++;
		}
		cr.setInductionTime(System.currentTimeMillis() - time);
		root.postProc(null);
		return null;
	}
}
