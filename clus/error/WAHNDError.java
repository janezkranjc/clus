package clus.error;

import java.io.*;

import clus.main.*;
import clus.data.rows.*;
import clus.statistic.*;
import clus.ext.hierarchical.*;

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
	
	
	public double calcSquaredDistance(ClassesTuple actual, HierStatistic distr) {
		
		//Sort actual tupel
		ClassesTuple.quickSort(actual,0,actual.size()-1);
		// Calculate mean
		ClassHierarchy hier = distr.getHier();
		double sumWeights = distr.getTotalWeight();
		ClassesTuple mean = hier.getBestTuple(distr.getCounts(),sumWeights);
		
		int j = 0;
		ClassesValue valActual = actual.elementAt(j);
		int idxActual = valActual.getIndex();
		double abActual = valActual.getAbundance();
		double abundanceActual;
		
		double temp = 0;
		
		for (int i = 0; i < mean.size(); i++) {
			
			ClassesValue valMean = mean.elementAt(i);
			int idxMean = valMean.getIndex();
			double abMean = valMean.getAbundance();
			
			//System.out.println("Gemiddelde: " + idxMean + " " + abMean);
			//System.out.println("Actueel: " + idxActual + " " + abActual);
			
			if (idxMean == idxActual) {
				abundanceActual = abActual; 
				j++;
				try {
					valActual = actual.elementAt(j);
					idxActual = valActual.getIndex();
					abActual = valActual.getAbundance();
				}
				catch (ArrayIndexOutOfBoundsException aiofb) {}
			}		
			
			else abundanceActual = 0;
			
			temp += hier.getErrorWeight(idxMean,m_Weight) * Math.abs(abundanceActual - abMean);
			
			
		}
		
		double result = temp * temp;
		
		return result;
		
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double weight = tuple.getWeight();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
		m_TreeErr += weight*calcSquaredDistance(tp, (WAHNDStatistic)pred);
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
