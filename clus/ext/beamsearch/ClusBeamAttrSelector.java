/*
 * Created on Apr 25, 2005
 */
package clus.ext.beamsearch;

import clus.data.rows.*;
import clus.model.test.*;

public class ClusBeamAttrSelector {

	public RowData data;
	public boolean stopcrit;
	public NodeTest[] besttests;

	public final boolean hasEvaluations() {
		return besttests != null;
	}

	public final NodeTest[] getBestTests() {
		return besttests;
	}
	
	public final void setData(RowData data) {
		this.data = data;
	}
	
	public final RowData getData() {
		return data;
	}
	
	public final void setStopCrit(boolean stopcrit) {
		this.stopcrit = stopcrit;
	}
	
	public final boolean isStopCrit() {
		return stopcrit;
	}
	
	public final void newEvaluations(int nb) {
		besttests = new NodeTest[nb];
	}
	
	public final void setBestTest(int i, NodeTest test) {
		besttests[i] = test;
	}
}
