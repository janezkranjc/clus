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

import jeans.io.ini.INIFileNominalOrDoubleOrVector;
import jeans.util.cmdline.CMDLineArgs;
import clus.*;
import clus.data.type.*;
import clus.algo.*;
import clus.algo.tdidt.*;
import clus.main.*;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.util.ClusException;

public class ClusRuleClassifier extends ClusInductionAlgorithmType {

	public ClusRuleClassifier(Clus clus) {
		super(clus);
	}

	public ClusInductionAlgorithm createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {

		ClusInductionAlgorithm induce;
		if (sett.getCoveringMethod() == Settings.COVERING_METHOD_RULES_FROM_TREE) {
			induce = (ClusInductionAlgorithm) new ClusRuleFromTreeInduce(schema, sett, getClus());
		} else {
			induce = (ClusInductionAlgorithm) new ClusRuleInduce(schema, sett);
		}
		induce.getStatManager().setRuleInduce(true); // Tells that the rule is the way to go 
		induce.getStatManager().initRuleSettings();
		return induce;
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
		ClusRuleSet model = (ClusRuleSet) cr.getModel(ClusModel.PRUNED);
		if (model.m_StatManager.getMode() == ClusStatManager.MODE_HIERARCHICAL) {
			// Clone the ruleset model for each treshold value specified in the settings
			INIFileNominalOrDoubleOrVector class_thr = getSettings().getClassificationThresholds();
			double[] tresholds = class_thr.getDoubleVector();
			for (int t=0; t<tresholds.length; t++) {
				ClusRuleSet model2 = model.cloneRuleSetWithThreshold(t);
				ClusModelInfo modelInfo2 = cr.addModelInfo();
				modelInfo2.setModel(model2);
				modelInfo2.setName("T(" + tresholds[t] + ")");
			}
		}
	}

	public ClusModel pruneSingle(ClusModel model, ClusRun cr) throws ClusException, IOException {
		return model;
	}

	public void postProcess(ClusRun cr) throws ClusException, IOException {
		ClusModelInfo def_model = cr.addModelInfo(ClusModel.DEFAULT);
		def_model.setModel(ClusDecisionTree.induceDefault(cr));
		def_model.setName("Default");
	}
}
