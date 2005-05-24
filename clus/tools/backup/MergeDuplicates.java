import clus.tools.debug.Debug;

import java.io.*;
import java.util.*;

import jeans.util.*;
import jeans.util.compound.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.data.rows.*;
import clus.selection.*;

public class MergeDuplicates {

	String m_hFName;
	int m_RemoveCnt;
	MyArray m_hArray = new MyArray();
	
	public MergeDuplicates(String fname) {
		m_hFName = fname;
	}
		
	public void outputMatrix(ClusSchema schema) throws IOException {
		int nbattr = schema.getNbAttributes();	
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream("matrix.csv")));
		for (int i = 0; i < m_hArray.size(); i++) {
			DuoObject cr = (DuoObject)m_hArray.elementAt(i);				
			DataTuple tuple = (DataTuple)cr.getObj1();
			String clname = (String)cr.getObj2();
			for (int j = 1; j < nbattr-1; j++) {
				ClusAttrType type = schema.getAttrType(j);
				wrt.print(type.getString(tuple));
				wrt.print(",");
			}
			wrt.println(clname);
		}
		wrt.close();
	}	
	
	public void mergeDuplicates() throws IOException, ClusException {
		MStreamTokenizer tokens = new MStreamTokenizer("realduplicates.csv");
		String token1 = tokens.getToken();
		while (token1 != null) {
			String token2 = tokens.readToken();
			System.out.println("Duplicate: "+token1+" "+token2);
			int idx1 = findTuple(token1);
			if (idx1 != -1) {
				int idx2 = findTuple(token2);
				if (idx2 == -1) throw new ClusException("Can't find duplicate: "+token2);
				DuoObject obj1 = (DuoObject)m_hArray.elementAt(idx1);
				DuoObject obj2 = (DuoObject)m_hArray.elementAt(idx2);
				String cls1 = (String)obj1.getObj2();			
				String cls2 = (String)obj2.getObj2();
				obj1.setObj2(cls1+"@"+cls2);
				System.out.println("New class: "+cls1+"@"+cls2);			
				m_hArray.removeElementAt(idx2);
				m_RemoveCnt++;			
			}
			token1 = tokens.getToken();			
		}
	}
	
	public void removeDuplicateClasses() {
		int max_classes = 0;
		int max_removed = 0;
		int max_after_cls = 0;		
		Hashtable table = new Hashtable();		
		for (int i = 0; i < m_hArray.size(); i++) {
			DuoObject obj = (DuoObject)m_hArray.elementAt(i);
			table.clear();
			int nbcls = 0;
			StringTokenizer tokens = new StringTokenizer((String)obj.getObj2(), "@");
			while (tokens.hasMoreTokens()) {
				table.put(tokens.nextToken(), this);
				nbcls++;
			}
			if (nbcls > max_classes) max_classes = nbcls;
			int nbdiff = 0;
			StringBuffer result = new StringBuffer();
			for (Enumeration e = table.keys(); e.hasMoreElements(); ) {
				String cls = (String)e.nextElement();
				if (nbdiff != 0) result.append("@");
				result.append(cls);
				nbdiff++;
			}
			int nb_removed = nbcls - nbdiff;
			if (nb_removed > max_removed) max_removed = nb_removed;
			if (nbdiff > max_after_cls) max_after_cls = nbdiff;
			obj.setObj2(result.toString());
		}
		System.out.println("Max number of classes: "+max_classes);
		System.out.println("Max number of classes (after removal): "+max_after_cls);
		System.out.println("Max number of classes removed: "+max_removed);
	}
	
	public int findTuple(String search) {
		for (int i = 0; i < m_hArray.size(); i++) {
			DuoObject obj = (DuoObject)m_hArray.elementAt(i);
			DataTuple tuple = (DataTuple)obj.getObj1();
			String key = (String)tuple.getObjVal(0);
			if (search.equals(key)) return i;
		}
		return -1;
	}

	public void process() throws IOException, ClusException {
		int ctr = 0;
		TupleIterator iter = new DiskTupleIterator(m_hFName, null);
		iter.init();
		ClusSchema schema = iter.getSchema();
		int nbattr = schema.getNbAttributes();		
		ClusAttrType lastone = schema.getAttrType(nbattr-1);
		DataTuple tuple = iter.readTuple();					
		while (tuple != null) {		
			ctr++;
			if ((ctr % 100) == 0) {
				System.out.print("."); 
				System.out.flush();
			}
			String cls = lastone.getString(tuple);
			m_hArray.addElement(new DuoObject(tuple, cls));
			tuple = iter.readTuple();			
		}
		iter.close();
		System.out.println(); 
		System.out.println("Removing duplicates");
		mergeDuplicates();
		System.out.println("Removing duplicate classes");		
		removeDuplicateClasses();
		System.out.println("Saving matrix");				
		outputMatrix(schema);
		System.out.println("Processed "+ctr+" examples, removed "+m_RemoveCnt+" duplicates.");
	}

	public static void main(String[] args) {
		MergeDuplicates cd = new MergeDuplicates(args[0]);
		try {
			cd.process();
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		} catch (ClusException e) {
			System.out.println("Clus Error: "+e.getMessage());
		}
	}
}
