package clus.model.test;

import clus.main.*;
import clus.data.rows.*;
import clus.data.cols.attribute.*;

public abstract class SoftTest extends NodeTest {

	public abstract int softPredictNb(DataTuple tuple, int branch);
	
	public abstract int softPredict(RowData res, DataTuple tuple, int idx, int branch);
	
	public abstract int softPredictNb2(DataTuple tuple, int branch);
	
	public abstract int softPredict2(RowData res, DataTuple tuple, int idx, int branch);	
	
	public int predict(ClusAttribute attr, int idx) {
		return ClusNode.NO;
	}
		
	public int predictWeighted(DataTuple tuple) {
		return ClusNode.NO;	
	}
	
	public boolean equals(NodeTest test) {
		return false;
	}	

	public boolean isSoft() {
		return true;
	}	
}
