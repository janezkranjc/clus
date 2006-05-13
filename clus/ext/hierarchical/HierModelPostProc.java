package clus.ext.hierarchical;

/*
 * Collect target values in each leave 
 *
 */

public class HierModelPostProc /*extends ClusModelProcessor*/ {
/*
	
	public ClassHierarchy m_Hier;

	public HierModelPostProc(ClassHierarchy hier) {
		m_Hier = hier;
	}

	public void initialize(ClusModel model, ClusSchema schema) {
		LeafTreeIterator iter = new LeafTreeIterator((Node)model);
		while (iter.hasMoreNodes()) {
			ClusNode node = (ClusNode)iter.getNextNode();
			node.setVisitor(new MyArray());
		}
	}
		
	public void terminate(ClusModel model) throws IOException {
		LeafTreeIterator iter = new LeafTreeIterator((Node)model);
		while (iter.hasMoreNodes()) {
			// ClusNode node = (ClusNode)iter.getNextNode();
			// MyArray examples = (MyArray)node.getVisitor();
			// ClassesTuple prediction = findBestPrediction(examples);
			// DuoObject visitor = new DuoObject(prediction, examples);
		}
	}

	public void modelUpdate(DataTuple tuple, ClusModel model) {
		ClusNode node = (ClusNode)model;
		MyArray visitor = (MyArray)node.getVisitor();
		// FIXME -- target attribute should be zero.
		DoubleObject example = new DoubleObject(tuple.getWeight(), tuple.getObjVal(0));		
		visitor.addElement(example);
	}
	
	public ClassesTuple findBestPrediction(MyArray examples) {		
		double bestvalue = Double.POSITIVE_INFINITY;
		ClassesTuple besttuple = new ClassesTuple(0);
		System.out.println("Finding best prediction for set of "+examples.size()+" examples.");
		for (int i = 0; i < examples.size(); i++) {
			DoubleObject obj = (DoubleObject)examples.elementAt(i);
			ClassesTuple actual = (ClassesTuple)obj.getObject();
			System.out.println("  -  "+actual);
		}		
		// Try all 1-class tuples
		ClassesTuple current = new ClassesTuple(1);
		LeafTreeIterator iter = m_Hier.getLeavesIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			current.setItemAt(node, 0);
			double value = evaluateTuple(current, examples);
			if (value < bestvalue) {
				besttuple.cloneFrom(current);
				bestvalue = value;
			}
		}
		System.out.println("  -> "+besttuple+" "+bestvalue);
		// Try all 2-class tuples
		return besttuple;	
	}
	
	public double evaluateTuple(ClassesTuple pred, MyArray examples) {
		MSymMatrix km = m_Hier.getKMatrix();
		double aiAai = 0.0;
		double sumweight = 0.0;
		double[] Aai = new double[km.getRows()];
		for (int i = 0; i < examples.size(); i++) {
			DoubleObject obj = (DoubleObject)examples.elementAt(i);
			double weight = obj.getValue();
			ClassesTuple actual = (ClassesTuple)obj.getObject();
			sumweight += weight;
			for (int j = 0; j < actual.size(); j++) {			
				int index = actual.getPosition(j);
				km.addRowWeighted(Aai, index, weight);
			}
			aiAai += weight * km.xtAx(actual);
		}
		double piApi = km.xtAx(pred);		
		return sumweight*piApi - 2 * MSymMatrix.dot(pred, Aai) + aiAai;
	}
*/	
}

