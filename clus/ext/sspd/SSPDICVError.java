package clus.ext.sspd;

import java.io.*;

import clus.error.*;
import clus.data.rows.*;
import clus.main.*;
import clus.model.test.*;

public class SSPDICVError extends ClusError {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected double m_Value;
	protected SSPDDistance m_Dist;
	
	public SSPDICVError(ClusErrorParent par, SSPDDistance dist) {
		super(par);
		m_Dist = dist;
	}
	
	public void computeRecursive(ClusNode node, RowData data) {
		int nb = node.getNbChildren();
		if (nb == 0) {
			double variance = SSPD.computeSSPDVariance(m_Dist, data);
			double sumweight = data.getSumWeights();
			m_Value += sumweight * variance;
		} else {
			NodeTest tst = node.getTest();
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				RowData subset = data.applyWeighted(tst, i);
				computeRecursive(child, subset);
			}
		}
	}
	
	public void compute(RowData data, ClusModel model) {
		if (model instanceof ClusNode) {
			ClusNode tree = (ClusNode)model;
			computeRecursive(tree, data);
			m_Value /= data.getSumWeights();
		}
	}
	
	public void showModelError(PrintWriter wrt, int detail) {
		wrt.println("SSPD-ICV: "+m_Value);
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new SSPDICVError(getParent(), m_Dist);
	}
	
	public String getName() {
		return "SSPDICV";		
	}
}
