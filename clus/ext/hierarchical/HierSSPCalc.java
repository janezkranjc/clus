package clus.ext.hierarchical;

import jeans.math.matrix.*;

import clus.data.rows.*;

public class HierSSPCalc {

	protected MSymMatrix m_KM;
	protected double[] m_SumAai;			
	protected double m_SumaiAai = 0.0;
	protected double m_SumWeight = 0.0;
	protected int m_Index;
	
	public HierSSPCalc(MSymMatrix km, ClassesAttrType type) {
		this(km, type.getArrayIndex());
	}
	
	public HierSSPCalc(MSymMatrix km, int index) {
		m_KM = km;
		m_Index = index;		
		m_SumAai = new double[km.getRows()];
	}	

	public final void addTuple(DataTuple tuple) {
		ClassesTuple actual = (ClassesTuple)tuple.getObjVal(m_Index);				
		addTarget(tuple.getWeight(), actual);
	}
	
	public final void addTarget(double weight, ClassesTuple actual) {
		m_SumWeight += weight;
		for (int j = 0; j < actual.size(); j++) {			
			int index = actual.getPosition(j);
			m_KM.addRowWeighted(m_SumAai, index, weight);
		}
		m_SumaiAai += weight * m_KM.xtAx(actual);
	}	
	
	public final double getValue(ClassesTuple pred) {
		double piApi = m_KM.xtAx(pred);
		return m_SumWeight*piApi - 2 * MSymMatrix.dot(pred, m_SumAai) + m_SumaiAai;
	} 

}
