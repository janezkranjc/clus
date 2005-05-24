package clus.ext.hierarchical;

import jeans.util.array.*;
import jeans.util.*;
import jeans.tree.*;

import java.io.*;

import clus.util.*;

public class ClassHierarchyIO {

	protected StringTable m_Table = new StringTable();
		
	public ClassHierarchy loadHierarchy(String fname) throws ClusException, IOException {		
		ClassHierarchy hier = new ClassHierarchy((ClassTerm)null);
		loadHierarchy(fname, hier);
		return hier;
	}
	
	public ClassHierarchy loadHierarchy(String fname, ClassesAttrType type) throws ClusException, IOException {
		ClassHierarchy hier = new ClassHierarchy(type);
		loadHierarchy(fname, hier);
		return hier;
	}
		
	public void loadHierarchy(String fname, ClassHierarchy hier) throws ClusException, IOException {
		MStreamTokenizer tokens = new MStreamTokenizer(fname);
		String token = tokens.getToken();
		while (token != null) {
			ClassesTuple tuple = new ClassesTuple(token, m_Table);
			tuple.addToHierarchy(hier);
			token = tokens.getToken();
		    
		    }
		tokens.close();
	}
	
	public void saveHierarchy(String fname, ClassHierarchy hier) throws IOException {
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
		CompleteTreeIterator iter = hier.getNoRootIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			wrt.println(node.toString());
		}
		wrt.close();
	}	
	
	public StringTable getStringTable() {
		return m_Table;
	}


}
