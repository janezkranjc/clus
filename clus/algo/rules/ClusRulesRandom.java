/*
 * Created on June 22, 2005
 */
package clus.algo.rules;

import java.io.*;

import clus.data.rows.RowData;
import clus.main.*;
import clus.util.*;

public class ClusRulesRandom {

  public ClusRulesRandom(ClusRun cr) {
   }

/**
  * Constructs the random rules.
  * 
  * @param cr ClusRun
  * @return rule set
  * @throws ClusException 
  * @throws IOException 
  */
  public ClusRuleSet constructRules(ClusRun cr) throws IOException, ClusException {
    ClusRuleSet res = new ClusRuleSet(cr.getStatManager());
    ClusRule init = new ClusRule(cr.getStatManager());
    constructRandomly(init, res);
    res.removeEmptyRules();
    res.simplifyRules();
    // res.setDefaultStat(node.getTotalStat());
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
	public void constructRandomly(ClusRule rule, ClusRuleSet set) {
/*		if (node.atBottomLevel()) {
			rule.setDefaultStat(node.getTotalStat());
			set.add(rule);
		} else {
			NodeTest test = node.getTest();
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				NodeTest branchTest = test.getBranchTest(i);
				ClusRule child_rule = rule.cloneRule();
				child_rule.addTest(branchTest);
				constructRecursive(child, child_rule, set);
			}
		}	*/
	}	
}

