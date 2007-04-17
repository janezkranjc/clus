package clus.main;

import java.io.IOException;

import jeans.util.cmdline.CMDLineArgs;

import clus.Clus;
import clus.algo.induce.*;
import clus.util.ClusException;

public class ClusForestClassifier extends ClusClassifier {

	public ClusForestClassifier(Clus clus) {
		super(clus);
		// TODO Auto-generated constructor stub
	}

	public ClusInduce createInduce(ClusSchema schema, Settings sett,
			CMDLineArgs cargs) throws ClusException, IOException {
		// TODO Auto-generated method stub
		return new ClusForestInduce(schema,sett,m_Clus);
	}

	public ClusModel pruneSingle(ClusModel model, ClusRun cr)
			throws ClusException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void pruneAll(ClusRun cr) throws ClusException, IOException {
		// TODO Auto-generated method stub
	}
	
	public void printInfo() {
		System.out.println("Classifier: Bagging");
	}	
	
}
