/*
 * Created on May 3, 2005
 */
package clus.algo.rules;

import clus.main.*;
import clus.model.test.*;

public class ClusRulesFromTree {

	public ClusRuleSet constructRules(ClusNode node, ClusStatManager mgr) {
		ClusRuleSet res = new ClusRuleSet();
		ClusRule init = new ClusRule();
		constructRecursive(node, init, res);
		res.removeEmptyRules();
		res.simplifyRules();
		res.setDefaultStat(node.getTotalStat());
		return res;
	}
	
	public void constructRecursive(ClusNode node, ClusRule rule, ClusRuleSet set) {
		if (node.atBottomLevel()) {
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
		}	
	}	
}

