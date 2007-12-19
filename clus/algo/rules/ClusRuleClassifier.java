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

/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;

import jeans.util.cmdline.CMDLineArgs;
import clus.*;
import clus.data.type.*;
import clus.algo.tdidt.ClusDecisionTree;
import clus.main.*;
import clus.util.ClusException;
import clus.algo.induce.*;

public class ClusRuleClassifier extends ClusClassifier {
	
	public ClusRuleClassifier(Clus clus) {
		super(clus);
	}
	
	public ClusInduce createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {
		DepthFirstInduce induce = new DepthFirstInduce(schema, sett);
		induce.getStatManager().setRuleInduce(true);
		induce.getStatManager().initRuleSettings();
		return new ClusRuleInduce(induce);
	}
	
	public void printInfo() {
		if (!getSettings().isRandomRules()) {
			System.out.println("RuleSystem based on CN2");
			System.out.println("Heuristic: "+getStatManager().getHeuristicName());
		} else {
			System.out.println("RuleSystem generating random rules");
		}
	}
	
	public void pruneAll(ClusRun cr) throws ClusException, IOException {
	}
	 
	public ClusModel pruneSingle(ClusModel model, ClusRun cr) throws ClusException, IOException {
		return model;
	}
	
	public void postProcess(ClusRun cr) throws ClusException, IOException {
		ClusModelInfo def_model = cr.addModelInfo(ClusModels.DEFAULT);
		def_model.setModel(ClusDecisionTree.induceDefault(cr));
		def_model.setName("Default");
	}	
}
