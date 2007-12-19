package clus.ext.ensembles;

import java.io.IOException;

import jeans.util.cmdline.CMDLineArgs;

import clus.main.*;
import clus.Clus;
import clus.algo.induce.*;
import clus.data.type.*;
import clus.util.ClusException;

public class ClusEnsembleClassifier extends ClusClassifier {

	public ClusEnsembleClassifier(Clus clus) {
		super(clus);
		// TODO Auto-generated constructor stub
	}

	public ClusInduce createInduce(ClusSchema schema, Settings sett,
			CMDLineArgs cargs) throws ClusException, IOException {
		// TODO Auto-generated method stub
		return new ClusEnsembleInduce(schema,sett,m_Clus);
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
		System.out.println("Ensemble Classifier");
	}	
	
}
