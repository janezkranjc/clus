/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;

import jeans.util.cmdline.CMDLineArgs;
import clus.*;
import clus.data.type.*;
import clus.algo.tdidt.ClusDecisionTree;
import clus.main.*;
import clus.util.ClusException;
import clus.algo.induce.*;

public class ClusRuleClassifier extends ClusClassifier {
	
	public ClusRuleClassifier(Clus clus) {
		super(clus);
	}
	
	public ClusInduce createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {
		DepthFirstInduce induce = new DepthFirstInduce(schema, sett);
		induce.getStatManager().setRuleInduce(true);
		induce.getStatManager().initRuleSettings();
		return new ClusRuleInduce(induce);
	}
	
	public void printInfo() {
		if (!getSettings().isRandomRules()) {
			System.out.println("RuleSystem based on CN2");
			System.out.println("Heuristic: "+getStatManager().getHeuristicName());
		} else {
			System.out.println("RuleSystem generating random rules");
		}
	}
	
	public void pruneAll(ClusRun cr) throws ClusException, IOException {
	}
	 
	public ClusModel pruneSingle(ClusModel model, ClusRun cr) throws ClusException, IOException {
		return model;
	}
	
	public void postProcess(ClusRun cr) throws ClusException, IOException {
		ClusModelInfo def_model = cr.addModelInfo(ClusModels.DEFAULT);
		def_model.setModel(ClusDecisionTree.induceDefault(cr));
		def_model.setName("Default");
	}	
}
