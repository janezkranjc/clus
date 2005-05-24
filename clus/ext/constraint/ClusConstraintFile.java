/*
 * Created on Apr 26, 2005
 */
package clus.ext.constraint;

import java.io.*;
import java.util.*;

import clus.main.*;
import clus.model.modelio.*;

public class ClusConstraintFile {

	public static ClusConstraintFile m_Instance;
	HashMap m_Constraints = new HashMap();
	
	public static ClusConstraintFile getInstance() {
		if (m_Instance == null) m_Instance = new ClusConstraintFile();
		return m_Instance;
	}
	
	public ClusNode get(String fname) {
		return (ClusNode)m_Constraints.get(fname);
	}
	
	public ClusNode getClone(String fname) {
		return (ClusNode)get(fname).cloneTree();
	}
	
	public void load(String fname, ClusSchema schema) throws IOException {
		ClusTreeReader rdr = new ClusTreeReader();
		ClusNode root = rdr.loadTree(fname, schema);
		System.out.println("Constraint: ");
		root.printTree();
		m_Constraints.put(fname, root);
	}	
}
