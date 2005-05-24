/*
 * Created on May 3, 2005
 */
package clus.weka;

import java.util.*;

import jeans.util.cmdline.*;

import clus.*;
import clus.algo.induce.*;
import clus.algo.tdidt.*;
import clus.main.*;
import clus.util.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.statistic.*;

import weka.classifiers.*;
import weka.core.*;

public class ClusWekaClassifier extends ClusClassifier {

	protected String m_Options;
	protected Classifier m_Classifier;
	protected ClusSchema m_Schema;
	protected ArrayList m_NomAttrs = new ArrayList();
	protected ArrayList m_NumAttrs = new ArrayList();
	protected FastVector m_WekaTypes = new FastVector();
	protected ClusStatManager m_Manager;
	protected Instances m_Instances;
	
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
		m_Schema = induce.getSchema();
		for (int i = 0; i < m_Schema.getNbAttributes(); i++) {
			ClusAttrType type = m_Schema.getAttrType(i);			
			if (type instanceof NumericAttrType) {
				m_NumAttrs.add(type);
			} else {
				m_NomAttrs.add(type);
			}			
		}
		for (int j = 0; j < m_NumAttrs.size(); j++) {
			NumericAttrType type = (NumericAttrType)m_NumAttrs.get(j);
			m_WekaTypes.addElement(new Attribute(type.getName()));
		}		
		for (int j = 0; j < m_NomAttrs.size(); j++) {
			NominalAttrType type = (NominalAttrType)m_NomAttrs.get(j);
			FastVector values = new FastVector();
			for (int k = 0; k < type.getNbValues(); k++) {
				values.addElement(type.getValue(k));
			}			
			m_WekaTypes.addElement(new Attribute(type.getName(), values));
		}
		m_Instances = new Instances(m_Schema.getRelationName(), m_WekaTypes, 0);
		m_Instances.setClassIndex(m_WekaTypes.size()-1);
		m_Manager = induce.getStatManager();
	}
	
	public ClusStatistic createStatistic() {
		return m_Manager.createStatistic();
	}
	
	public Instances getDummyData() {
		return m_Instances;
	}

	public Instance convertInstance(DataTuple tuple) {
		double[] values = new double[m_WekaTypes.size()];
		for (int j = 0; j < values.length; j++) {
			values[j] = Instance.missingValue();
		}
		int pos = 0;
		for (int j = 0; j < m_NumAttrs.size(); j++) {
			NumericAttrType type = (NumericAttrType)m_NumAttrs.get(j);
			values[pos++] = type.getNumeric(tuple);
		}
		for (int j = 0; j < m_NomAttrs.size(); j++) {
			NominalAttrType type = (NominalAttrType)m_NomAttrs.get(j);
			values[pos++] = (double)type.getNominal(tuple);
		}		
		return new Instance(tuple.getWeight(), values);
	}
	
	public Instances convertData(RowData data) {
		Instances weka_data = new Instances(m_Schema.getRelationName(), m_WekaTypes, data.getNbRows());
		for (int i = 0; i < data.getNbRows(); i++) {
			weka_data.add(convertInstance(data.getTuple(i)));
		}
		weka_data.setClassIndex(m_WekaTypes.size()-1);
		return weka_data;
	}
	
	public ClusModel induceSingle(ClusRun cr) throws ClusException {
		ClusWekaModel result = new ClusWekaModel();
		RowData data = (RowData)cr.getTrainingSet();
		try {
			Classifier copy = Classifier.makeCopy(m_Classifier);
			copy.buildClassifier(convertData(data));
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
