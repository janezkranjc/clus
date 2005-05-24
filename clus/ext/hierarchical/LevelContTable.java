package clus.ext.hierarchical;

public class LevelContTable {
	/*
	extends ClusError {
}

	protected ClassHierarchy m_Hier;
	protected ContingencyTable m_Table;	
	
	public LevelContTable(ClusErrorParent par, ClassHierarchy hier) {
		super(par, 0);
		m_Hier = hier;
		m_Table = makeContTable(par, hier);
	}	
	
	public void add(ClusError other) {
		HierXtAXError err = (HierXtAXError)other;
		m_DefErr += err.m_DefErr;
		m_TreeErr += err.m_TreeErr;
	}

	public void showSummaryError(PrintWriter out, boolean detail) {
		NumberFormat nf = ClusFormat.TWO_AFTER_DOT;
		double re = m_DefErr != 0.0 ? m_TreeErr / m_DefErr : 0.0;
		out.println(getPrefix() + "RE: "+nf.format(re)+" = Tree: "+nf.format(m_TreeErr)+" / Default: "+nf.format(m_DefErr));
	}
	
	// Calculate my own default :-)		
	public void setDefault(ClusStatistic stat) {
	}
		
	public void addExample(ClusData data, int idx, ClusStatistic pred) {
		System.out.println("LevelContTable: addExample/3 not implemented");
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
		m_DefErr += weight*calcSquaredDistance(tp, m_Default);
		m_TreeErr += weight*calcSquaredDistance(tp, (SPMDStatistic)pred);
	}
	
	public void calculate() {	
		int nb = getNbExamples();	
		m_DefErr /= nb;
		m_TreeErr /= nb;		
	}
	
	public String getName() {
		return "Hierarchical Level 0 Contingency table";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new LevelContTable(par, m_Hier);
	}
	
	public final static ContingencyTable makeContTable(ClusErrorParent par, ClassHierarchy hier) {
		ClassTerm root = hier.getRoot();
		int nb = root.getNbChildren();
		TargetSchema tschema = new TargetSchema(1, 0);
		NominalAttrType type =  new NominalAttrType(nb);		
		tschema.setType(NominalAttrType.THIS_TYPE, 0, type);
		for (int i = 0; i < nb; i++) {
			type.setValue(i, String.valueOf(root.getID(i)));
		}
		return new ContingencyTable(par, tschema);
	}
*/
	
}
