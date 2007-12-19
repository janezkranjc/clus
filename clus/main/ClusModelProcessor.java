package clus.main;

import clus.data.rows.*;
import clus.data.type.*;
import clus.statistic.*;
import clus.util.*;

import java.io.*;

public abstract class ClusModelProcessor {

	public boolean shouldProcessModel(ClusModelInfo info) {
		return true;
	}
	
	public void addModelInfo(ClusModelInfo info) {		
	}	
	
	public void initialize(ClusModel model, ClusSchema schema) throws IOException, ClusException {
	}
	
	public void initializeAll(ClusSchema schema) throws IOException, ClusException {		
	}
	
	public void terminate(ClusModel model) throws IOException {
	}
	
	public void terminateAll() throws IOException {		
	}	

	public void exampleUpdate(DataTuple tuple) throws IOException {
	}
	
	public void exampleDone() throws IOException {
	}	
	
	public void exampleUpdate(DataTuple tuple, ClusStatistic distr) throws IOException {
	}

	public void modelUpdate(DataTuple tuple, ClusModel model) throws IOException {
	}
	
	public void modelDone() throws IOException {
	}	
	
	public boolean needsModelUpdate() {
		return false;
	}		
	
	public boolean needsInternalNodes() {
		return false;
	}			
}
