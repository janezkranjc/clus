package clus.ext.hierarchical;

public class OldHierXtAXError { /* extends ClusError {

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
	
	public double calcSquaredDistance(ClassesTuple ex, SPMDStatistic pred) {
		int nb = ex.size();	
		double[] counts = pred.getCounts();
		double[] error = MDoubleArray.clone(counts);
		MDoubleArray.dotscalar(error, 1.0/pred.m_SumWeight);
		for (int i = 0; i < nb; i++) {
			int index = ex.elementAt(i).getIndex();
			error[index] -= 1.0;
		}
		return m_KM.xtAx(error);		
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		double weight = tuple.getWeight();
		ClassesTuple tp = (ClassesTuple)tuple.getObjVal(0);
		m_TreeErr += weight*calcSquaredDistance(tp, (SPMDStatistic)pred);
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
	*/
}
