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
 * Created on May 3, 2005
 */
package clus.algo.rules;

import java.io.*;

import clus.algo.tdidt.ClusNode;
import clus.data.rows.RowData;
import clus.main.*;
import clus.model.test.*;
import clus.util.*;

public class ClusRulesFromTree {

	protected boolean m_Validated;
	
	public ClusRulesFromTree(boolean onlyValidated) {
		m_Validated = onlyValidated;
	}
	
 /**
  * Same as constructRules(ClusNode node, ClusStatManager mgr) but
  * with additional parameter - ClusRun to get access to the data set.
  * 
  * @param cr ClusRun
  * @param node ClusNode
  * @param mgr ClusStatmanager
  * @return rule set
  */
  public ClusRuleSet constructRules(ClusRun cr, ClusNode node, ClusStatManager mgr)
                                    throws ClusException, IOException {
    ClusRuleSet res = constructRules(node, mgr);
    RowData data = (RowData)cr.getTrainingSet();
    RowData testdata;
    res.addDataToRules(data);
    res.computeCompactness(ClusModel.TRAIN);
    res.removeDataFromRules();
    if (cr.getTestIter() != null) {
      testdata = (RowData)cr.getTestSet();
      res.addDataToRules(testdata);
      res.computeCompactness(ClusModel.TEST);
      res.removeDataFromRules();
    }
    return res;
  }
  
	public ClusRuleSet constructRules(ClusNode node, ClusStatManager mgr) {
		ClusRuleSet res = new ClusRuleSet(mgr);
		ClusRule init = new ClusRule(mgr);
		constructRecursive(node, init, res);
		res.removeEmptyRules();
		res.simplifyRules();
		res.setTargetStat(node.getTargetStat());
		return res;
	}
	
	public void constructRecursive(ClusNode node, ClusRule rule, ClusRuleSet set) {
		if (node.atBottomLevel()) {
			if (!m_Validated || node.getTargetStat().isValidPrediction()) {
				rule.setTargetStat(node.getTargetStat());
				rule.setID(node.getID());
				set.add(rule);
			}
		} else {
			NodeTest test = node.getTest();
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				NodeTest branchTest = test.getBranchTest(i);
				ClusRule child_rule = rule.cloneRule();
				child_rule.addTest(branchTest);
				constructRecursive(child, child_rule, set);
			}
		}	
	}	
}

