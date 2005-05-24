package clus.ext.hierarchical;

import jeans.math.matrix.*;
import jeans.util.array.*;

import java.io.*;

import clus.main.*;
import clus.error.*;
import clus.data.rows.*;
import clus.statistic.*;

public class HierXtAXError extends ClusError {
	
	protected MSymMatrix m_KM;
	protected String m_Name;
	protected double m_TreeErr;
	protected double m_SumWeight;
	
	public HierXtAXError(ClusErrorParent par, MSymMatrix KM, String name) {
		super(par, 0);
		m_KM = KM;
		m_Name = name;
	}
	
	public void add(ClusError other) {
		HierXtAXError err = (HierXtAXError)other;
		m_TreeErr += err.m_TreeErr;
		m_SumWeight += err.m_SumWeight;
	}
	
	public void showModelError(PrintWriter out, int detail) {
		out.println(m_TreeErr/m_SumWeight);
	}	
	
	public void addExample(ClusData data, int idx, ClusStatistic pred) {
		System.out.println("HierXtAXError: addExample/3 not implemented");
	}
	
	public double calcSquaredDistance(ClassesTuple actual, HierStatistic distr) {
		/*    if (distr.m_MeanTuple == null) distr.calcMean();
		 ClassesTuple predicted = distr.m_MeanTuple;
		 double[] error = new double[m_KM.getRows()];
		 for (int i = 0; i < predicted.size(); i++) error[predicted.getPosition(i)] += 1.0; */
		
		double[] error = MDoubleArray.clone(distr.getCounts());
		MDoubleArray.dotscalar(error, 1/distr.getTotalWeight());
		for (int i = 0; i < actual.size(); i++) error[actual.getPosition(i)] -= actual.getValue(i);
		return m_KM.xtAx(error);
		
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double weight = tuple.getWeight();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
		m_TreeErr += weight*calcSquaredDistance(tp, (HierStatistic)pred);
		m_SumWeight += weight;
	}
	
	public double getModelError() {
		return m_TreeErr/m_SumWeight;
	}
	
	public void reset() {
		m_TreeErr = 0.0;
		m_SumWeight = 0.0;	
	}		
	
	public String getName() {
		return "Hierarchical RE with parameter "+m_Name;
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new HierXtAXError(par, m_KM, m_Name);
	}
}
