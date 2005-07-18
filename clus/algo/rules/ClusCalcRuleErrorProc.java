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
	
	public ClusCalcRuleErrorProc(int subset) {
		m_Subset = subset;
	}
	
	public void modelUpdate(DataTuple tuple, ClusModel model) throws IOException {
		ClusRule rule = (ClusRule)model;
		ClusErrorParent error = rule.getError(m_Subset);
		error.addExample(tuple, rule.getTargetStat());
	}
	
	public void modelDone() throws IOException {
	}	
	
	public boolean needsModelUpdate() {
		return true;
	}		
}
