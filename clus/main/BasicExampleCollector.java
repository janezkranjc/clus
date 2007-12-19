package clus.main;

import jeans.util.*;

import clus.data.rows.*;
import clus.data.type.*;

import java.io.*;

public class BasicExampleCollector extends ClusModelProcessor {

        public boolean needsModelUpdate() {
            return true;
        }
        
	public void initialize(ClusModel model, ClusSchema schema) {
		ClusNode root = (ClusNode)model;
		recursiveInitialize(root);
	}
	
	public void terminate(ClusModel model) throws IOException {
	}

	public void modelUpdate(DataTuple tuple, ClusModel model) {
		ClusNode node = (ClusNode)model;
		MyArray visitor = (MyArray)node.getVisitor();
		visitor.addElement(tuple);
	}
	
	private void recursiveInitialize(ClusNode node) {
		if (node.atBottomLevel()) {
			node.setVisitor(new MyArray());
		} else {
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				recursiveInitialize(child);
			}
		}
	}
}
