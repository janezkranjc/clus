/*
 * Created on Jan 2, 2006
 */

import clus.algo.rules.*;
import clus.data.rows.*;
import clus.main.*;
import clus.model.test.*;

public class AmandaRule extends ClusRule {

  public AmandaRule(ClusStatManager statManager) {
    super(statManager);
  }
  
  public boolean doTest(NodeTest test, DataTuple tuple) {
		if (test.isUnknown(tuple)) {
			// Amanda does someting weird with unknown values
			// (because of the perl implementation of > and <=)  
			if (test instanceof InverseNumericTest) {
			  // <= succeeds for ? <= 26
			  // <= fails for ? <= -26
				if (!(0.0 <= ((NumericTest)test).getBound())) return false;
			} else if (test instanceof NumericTest) {
				// > fails for ? > 26					
			  // > succeeds for ? > -26
				if (!(0.0 > ((NumericTest)test).getBound())) return false; 
			} else {
				return false;
			}
		} else {
			if (test.predictWeighted(tuple) != ClusNode.YES) return false;
		}
		return true;
  }

	public boolean covers(DataTuple tuple) {
		for (int i = 0; i < getModelSize(); i++) {
			NodeTest test = getTest(i);
			if (!doTest(test, tuple)) return false;
		}
		return true;
	}		
}
