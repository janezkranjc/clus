
package clus.main;

import java.io.IOException;

import jeans.resource.ResourceInfo;
import jeans.util.cmdline.*;
import clus.*;
import clus.util.*;
import clus.algo.induce.*;
import clus.data.type.*;

public abstract class ClusClassifier {

	public final static int REGULAR_TREE = 0; 
	
	// For each type of algorithm there should be a ClusClassifier object
	
	protected Clus m_Clus;
	
	public ClusClassifier(Clus clus) {
		m_Clus = clus;
	}
	
	public Clus getClus() {
		return m_Clus;
	}
	
	public ClusInduce getInduce() {
		return getClus().getInduce();
	}	
	
	public ClusStatManager getStatManager() {
		return getInduce().getStatManager();
	}
	
	public Settings getSettings() {
		return getClus().getSettings();
	}
	
	public abstract ClusInduce createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException;
	
	public void printInfo() {
		System.out.println("Classifier: "+getClass().getName());
	}	
	
	public abstract void pruneAll(ClusRun cr) throws ClusException, IOException;
 
	public abstract ClusModel pruneSingle(ClusModel model, ClusRun cr) throws ClusException, IOException;
	
	public void postProcess(ClusRun cr) throws ClusException, IOException {
		// should be implemented by subclass		
	}	
	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		long start_time = ResourceInfo.getTime();
		getInduce().induceAll(cr);
		long done_time = ResourceInfo.getTime();
		cr.setInductionTime(done_time-start_time);
		pruneAll(cr);
		cr.setPruneTime(ResourceInfo.getTime()-done_time);
		postProcess(cr);
		if (Settings.VERBOSE > 0) {
			  String cpu = ResourceInfo.isLibLoaded() ? " (CPU)" : "";
			  System.out.println("Induction Time: "+(double)cr.getInductionTime()/1000+" sec"+cpu);
			  System.out.println("Pruning Time: "+(double)cr.getPruneTime()/1000+" sec"+cpu);
		}			
	}

	public ClusModel induceSingle(ClusRun cr) throws ClusException, IOException {
		ClusModel unpruned = induceSingleUnpruned(cr);
		return pruneSingle(unpruned, cr);
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		return getInduce().induceSingleUnpruned(cr);
	}
	
	public void saveInformation(String fname) {		
	}
}
