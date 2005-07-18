/*
 * Created on Jul 8, 2005
 *
 */
package clus.algo.rules;

import java.io.IOException;

import clus.data.rows.*;
import clus.data.type.*;
import clus.model.test.*;
import clus.main.*;
import clus.util.*;
import clus.ext.hierarchical.*;

/**
 * Create one rule for each value of each nominal attribute
 */
public class ClusRulesForAttrs {
	
	double m_SigLevel = 0.05;	

	public ClusRuleSet constructRules(ClusRun cr) throws IOException, ClusException {
		ClusStatManager mgr = cr.getStatManager();
    ClusRuleSet res = new ClusRuleSet(mgr);
    RowData train = (RowData)cr.getTrainingSet();
    RowData valid = (RowData)cr.getPruneSet();
    WHTDStatistic global_valid = (WHTDStatistic)mgr.createStatistic(ClusAttrType.ATTR_USE_TARGET);
    valid.calcTotalStatBitVector(global_valid);
    global_valid.calcMean();
    ClusSchema schema = train.getSchema();
    NominalAttrType[] descr = schema.getNominalAttrUse(ClusAttrType.ATTR_USE_DESCRIPTIVE);
    for (int i = 0; i < descr.length; i++) {
    	NominalAttrType attr = descr[i];
    	for (int j = 0; j < attr.getNbValues(); j++) {
    		boolean[] isin = new boolean[attr.getNbValues()];
    		isin[j] = true;
    		ClusRule rule = new ClusRule(mgr);
    		rule.addTest(new SubsetTest(attr, 1, isin, 0.0));
    		WHTDStatistic stat = (WHTDStatistic)mgr.createStatistic(ClusAttrType.ATTR_USE_TARGET);
    		rule.computeCoverStat(train, stat);
    		WHTDStatistic valid_stat = (WHTDStatistic)mgr.createStatistic(ClusAttrType.ATTR_USE_TARGET);
    		rule.computeCoverStat(valid, valid_stat);
    		valid_stat.calcMean();
  			stat.setValidationStat(valid_stat);
  			stat.setGlobalStat(global_valid);
 				stat.setSigLevel(m_SigLevel);				
  			stat.calcMean();
  			if (stat.isValidPrediction()) {
  				rule.setTargetStat(stat);
  				res.add(rule);
  			}
    	}    	
    }    
    res.removeEmptyRules();
    res.simplifyRules();
    return res;
  }
}
