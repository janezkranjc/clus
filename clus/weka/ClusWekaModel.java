/*
 * Created on May 3, 2005
 */
package clus.weka;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import jeans.io.*;
import jeans.util.*;
import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;

import weka.classifiers.*;
import weka.core.*;

public class ClusWekaModel implements ClusModel {

	Classifier m_Classifier;
	ClusWekaClassifier m_Parent;
	
	public ClusStatistic predictWeighted(DataTuple tuple) {
		Instance weka_tuple = m_Parent.convertInstance(tuple);		
	    Instance classMissing = (Instance)weka_tuple.copy();
	    classMissing.setDataset(m_Parent.getDummyData());
	    classMissing.setClassMissing();
	    try {
	    	double[] dist = m_Classifier.distributionForInstance(classMissing);
	    	ClassificationStat stat = (ClassificationStat)m_Parent.createStatistic();
	    	stat.initSingleTargetFrom(dist);
	    	stat.calcMean();
	    	return stat;		
	    } catch (Exception e) {
	    	System.out.println("Weka Error: "+e.getClass().getName()+": "+e.getMessage());
	    }
		return null;
	}
	
	public void setParent(ClusWekaClassifier parent) {
		m_Parent = parent;
	}
	
	public void setClassifier(Classifier classifier) {
		m_Classifier = classifier;
	}
	
	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
	}
	
	public int getModelSize() {
		return 0;
	}
	
	public String getModelInfo() {
		return "Weka Model";
	}
	
	public void printModel(PrintWriter wrt) {
	}
	
	public void saveModel(ObjectSaveStream strm) throws IOException {
	}
	
	public void attachModel(Hashtable table) {
		System.err.println(getClass().getName()+"attachModel() not implemented");
	}
}
