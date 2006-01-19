/*
 * Created on May 3, 2005
 */
package clus.algo.rules;

import java.io.*;
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

