
package clus.main;

import jeans.util.cmdline.*;
import clus.*;
import clus.util.*;
import clus.algo.induce.*;

public abstract class ClusClassifier {

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
	
	public void initializeInduce(ClusInduce induce, CMDLineArgs cargs) {
	}

	public abstract void printInfo();
	
	public ClusModel induceSingle(ClusRun cr) throws ClusException {
		System.out.println(">>> Error: induceSingle/1 not implemented");
		return null;
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException {
		System.out.println(">>> Error: induceSingleUnpruned/1 not implemented");		
		return null;
	}
	
	public void saveInformation(String fname) {		
	}

	public abstract void induce(ClusRun cr) throws ClusException;

	public abstract void initializeSummary(ClusSummary summ);

}
