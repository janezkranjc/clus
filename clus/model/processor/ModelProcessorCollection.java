package clus.model.processor;

import clus.main.*;
import clus.data.rows.*;
import clus.statistic.*;

import jeans.util.*;

import java.io.*;

public class ModelProcessorCollection extends MyArray {
	
	public final void addModelProcessor(ClusModelProcessor proc) {
		addElement(proc);
	}
	
	public final void initialize(ClusModel model, ClusSchema schema) throws IOException {
		if (model != null) {
			for (int i = 0; i < size(); i++) {
				ClusModelProcessor proc = (ClusModelProcessor)elementAt(i);
				proc.initialize(model, schema);
			}
		}
	}
	
	public final void terminate(ClusModel model) throws IOException {
		if (model != null) {
			for (int i = 0; i < size(); i++) {
				ClusModelProcessor proc = (ClusModelProcessor)elementAt(i);
				proc.terminate(model);
			}
		}
	}
	
	public final void modelDone() throws IOException {
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(j);
			proc.modelDone();
		}
	}
	
	public final void exampleUpdate(DataTuple tuple, ClusStatistic distr) throws IOException {
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(j);
			proc.exampleUpdate(tuple, distr);
		}	
	}
	
	public final boolean needsModelUpdate() throws IOException {
		for (int j = 0; j < size(); j++) {
			ClusModelProcessor proc = (ClusModelProcessor)elementAt(j);
			if (proc.needsModelUpdate()) return true;
		}
		return false;
	}	
	
	public final ClusModelProcessor getModelProcessor(int i) {
		return (ClusModelProcessor)elementAt(i);
	}	
}
