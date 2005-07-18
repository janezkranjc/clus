package clus.error;

import java.io.*;
import java.text.*;

import clus.data.attweights.*;
import clus.data.type.NumericAttrType;

public class RMSError extends MSError {

	public RMSError(ClusErrorParent par, NumericAttrType[] num) {
		super(par, num);
	}
	
	public RMSError(ClusErrorParent par, NumericAttrType[] num, ClusAttributeWeights weights) {
		super(par, num, weights);
	}
	
	public RMSError(ClusErrorParent par, NumericAttrType[] num, ClusAttributeWeights weights, boolean printall) {
		super(par, num, weights, printall);
	}
	
	public double getModelError() {
		return Math.sqrt(super.getModelError());
	}
	
	public double getModelErrorComponent(int i) {
		return Math.sqrt(super.getModelErrorComponent(i));
	}
	
	public void showSummaryError(PrintWriter out, boolean detail) {
		NumberFormat fr = getFormat();
		out.println(getPrefix() + "Mean over components RMSE: "+fr.format(getModelError()));
	}
	
	public String getName() {
		if (m_Weights == null) return "Root mean squared error (RMSE)";
		else return "Weighted root mean squared error (RMSE) ("+m_Weights.getName()+")";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new RMSError(par, m_Attrs, m_Weights, m_PrintAllComps);
	}
}
