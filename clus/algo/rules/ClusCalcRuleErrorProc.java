/*
 * Created on Jun 27, 2005
 */
package clus.algo.rules;

import java.io.IOException;

import clus.data.rows.*;
import clus.main.*;
import clus.error.*;

public class ClusCalcRuleErrorProc extends ClusModelProcessor {

	protected int m_Subset;
	protected ClusErrorParent m_Global;
	
	public ClusCalcRuleErrorProc(int subset, ClusErrorParent global) {
		m_Subset = subset;
		m_Global = global;
	}
	
	public void modelUpdate(DataTuple tuple, ClusModel model) throws IOException {
		ClusRule rule = (ClusRule)model;
		ClusErrorParent error = rule.getError(m_Subset);
		error.addExample(tuple, rule.getTargetStat());
	}
	
	public void terminate(ClusModel model) throws IOException {
		ClusRuleSet set = (ClusRuleSet)model;
		for (int i = 0; i < set.getModelSize(); i++) {
			ClusRule rule = set.getRule(i);
			rule.getError(m_Subset).updateFromGlobalMeasure(m_Global);
		}
	}
	
	public boolean needsModelUpdate() {
		return true;
	}		
}
