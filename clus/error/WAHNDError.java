package clus.error;

import java.io.*;

import clus.main.*;
import clus.data.rows.*;
import clus.statistic.*;

public class WAHNDError extends ClusError {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	//m_Dim from ClusError isn't instantiated. Bad?
	// Probably not?
	
	protected double m_Weight;
	protected double m_TreeErr;
	protected double m_SumWeight;
	
	public WAHNDError(ClusErrorParent par, double weight) {
		super(par, 0);
		m_Weight = weight;
	}
	
	public void add(ClusError other) {
		WAHNDError err = (WAHNDError)other;
		m_TreeErr += err.m_TreeErr;
		m_SumWeight += err.m_SumWeight;
	}
	
	public void showModelError(PrintWriter out, int detail) {
		out.println(m_TreeErr/m_SumWeight);
	}	
	
	public void addExample(ClusData data, int idx, ClusStatistic pred) {
		System.out.println("WAHNDError: addExample/3 not implemented");
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double weight = tuple.getWeight();
		// ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
		// m_TreeErr += weight*calcSquaredDistance(tp, (WAHNDStatistic)pred);
		m_SumWeight += weight;
	}
	
	public void addInvalid(DataTuple tuple) {
	}
	
	public double getModelError() {
		return m_TreeErr/m_SumWeight;
	}
	
	public void reset() {
		m_TreeErr = 0.0;
		m_SumWeight = 0.0;	
	}		
	
	public String getName() {
		return "WAHND RE with parameter "+ m_Weight;
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new WAHNDError(par, m_Weight);
	}
}
