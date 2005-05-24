package clus.error;

import java.io.*;
import java.text.*;

import clus.statistic.*;

public class RMSError extends MSError {

	public RMSError(ClusErrorParent par) {
		super(par);
	}
	
	public RMSError(ClusErrorParent par, TargetWeightProducer weights) {
		super(par, weights);
	}
	
	public RMSError(ClusErrorParent par, TargetWeightProducer weights, boolean printall) {
		super(par, weights, printall);
	}
	
	public RMSError(ClusErrorParent par, TargetWeightProducer weights, boolean printall, int dim) {
		super(par, weights, printall, dim);
	}			

	public double getModelError() {
		return Math.sqrt(super.getModelError());
	}
	
	public double getErrorComp(int i) {
		return Math.sqrt(super.getErrorComp(i));
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
		return new RMSError(par, m_Weights, m_PrintAllComps, m_Dim);
	}
}
