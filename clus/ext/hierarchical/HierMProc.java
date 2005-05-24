package clus.ext.hierarchical;

import jeans.util.compound.*;

import clus.data.rows.*;
import clus.main.*;

public class HierMProc extends ClusModelProcessor {

	public void initialize(ClusModel model, ClusSchema schema) {
	}
	
	public void terminate(ClusModel model) {
	}

	public void modelUpdate(DataTuple tuple, ClusModel model) {
		ClusNode node = (ClusNode)model;
		DuoObject obj = (DuoObject)node.getVisitor();
		SPMDStatistic s1 = (SPMDStatistic)obj.getObj1();
		SPMDStatistic s2 = (SPMDStatistic)obj.getObj2();
		s1.updateSpecial(tuple);
		if (tuple.getWeight() == 1.0) s2.updateSpecial(tuple);
	}
}
