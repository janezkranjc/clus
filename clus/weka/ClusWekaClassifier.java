/*
 * Created on May 3, 2005
 */
package clus.weka;

import jeans.util.cmdline.*;

import clus.*;
import clus.algo.induce.*;
import clus.algo.tdidt.*;
import clus.main.*;
import clus.util.*;
import clus.data.rows.*;
import clus.statistic.*;

import weka.classifiers.*;
import weka.core.*;

public class ClusWekaClassifier extends ClusClassifier {

	protected String m_Options;
	protected Classifier m_Classifier;
	protected ClusToWekaData m_Data;
	protected ClusStatManager m_Manager;
	
	public ClusWekaClassifier(Clus clus, String opts) throws ClusException {
		super(clus);
		m_Options = opts;
		String[] split = opts.split("\\s+");
		String[] options = new String[split.length-1];
		System.arraycopy(split, 1, options, 0, options.length);
		try {
			System.out.println("Loading classifier: "+split[0]);
			m_Classifier = Classifier.forName(split[0], options);
		} catch (Exception e) {
			throw new ClusException("Weka Error: "+e.getClass().getName()+": "+e.getMessage());
		}
	}

	public void printInfo() {
		System.out.println("Weka Classifier: "+m_Options);
	}
	
	public void initializeInduce(ClusInduce induce, CMDLineArgs cargs) {
		m_Data = new ClusToWekaData(induce.getSchema());
		m_Manager = induce.getStatManager();
	}
	
	public ClusStatistic createStatistic() {
		return m_Manager.createTargetStatistic();
	}
	
	public Instances getDummyData() {
		return m_Data.getDummyData();
	}

	public ClusModel induceSingle(ClusRun cr) throws ClusException {
		ClusWekaModel result = new ClusWekaModel();
		RowData data = (RowData)cr.getTrainingSet();
		try {
			Classifier copy = Classifier.makeCopy(m_Classifier);
			copy.buildClassifier(m_Data.convertData(data));
			result.setClassifier(copy);
			result.setParent(this);
			return result;
		} catch (Exception e) {
			throw new ClusException("Weka Error: "+e.getClass().getName()+": "+e.getMessage());
		}
	}
	
	public void induce(ClusRun cr) throws ClusException {
		ClusModel model = induceSingle(cr);
		cr.getModelInfo(ClusModels.PRUNED).setModel(model);
		ClusModel defmodel = ClusDecisionTree.induceDefault(cr);
		cr.getModelInfo(ClusModels.DEFAULT).setModel(defmodel);				
	}

	public void initializeSummary(ClusSummary summ) {
		ClusModels.DEFAULT = summ.addModel("Default");
		ClusModels.PRUNED = summ.addModel("Pruned");		
	}
}
