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

public class ComposeDuplicates {

	String m_hFName;
	int m_NbDuplicates;	
	MyArray m_hArray = new MyArray();
	
	public ComposeDuplicates(String fname) {
		m_hFName = fname;
	}
	
	public void addTuple(DataTuple tuple, ClusSchema schema) {
		int nbattr = schema.getNbAttributes();
		ClusAttrType lastone = schema.getAttrType(nbattr-1);
		String cls = lastone.getString(tuple);		
		ClusAttrType firstone = schema.getAttrType(0);
		String filename = firstone.getString(tuple);
		DuoObject found = null;
		for (int i = 0; i < m_hArray.size() && found == null; i++) {
			DuoObject cr = (DuoObject)m_hArray.elementAt(i);
			DataTuple tuple2 = (DataTuple)cr.getObj1();
			if (compareTuple(tuple, tuple2, schema)) {
				found = cr;
			}
		}
		if (found == null) {
			MyArray dups = new MyArray();
			dups.addElement(filename);
			DuoObject obj2 = new DuoObject(cls, dups);
			m_hArray.addElement(new DuoObject(tuple, obj2));
		} else {
			m_NbDuplicates++;
			DuoObject obj2 = (DuoObject)found.getObj2();
			String newclass = (String)obj2.getObj1();
			newclass = newclass + "@" + cls;
			obj2.setObj1(newclass);
			MyArray dups = (MyArray)obj2.getObj2();
			dups.addElement(filename);
		}
	}
	
	public void outputDuplicates(int nbtot, int nbdup) throws IOException {
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream("duplicates.csv")));
		double val = (double)nbdup/nbtot*100.0;
		System.out.println("Number of tuples: "+nbtot);
		System.out.println("Number of duplicates: "+nbdup+" "+val+"%");		
		wrt.println("Number of tuples: "+nbtot);
		wrt.println("Number of duplicates: "+nbdup+" "+val+"%");
		for (int i = 0; i < m_hArray.size(); i++) {
			DuoObject cr = (DuoObject)m_hArray.elementAt(i);				
			DuoObject obj2 = (DuoObject)cr.getObj2();
			MyArray dups = (MyArray)obj2.getObj2();
			if (dups.size() > 1) {
				wrt.println("$$$ --------------------------------------------- ");
				String clname = (String)obj2.getObj1();
				StringTokenizer tokens = new StringTokenizer(clname, "@");
				for (int j = 0; j < dups.size(); j++) {
					String fname = (String)dups.elementAt(j);
					wrt.print(StringUtils.printStr(fname, 50));
					wrt.println(tokens.nextToken());
				}
			}						
		}
		wrt.close();
	}	
	
	public void outputMatrix(ClusSchema schema) throws IOException {
		int nbattr = schema.getNbAttributes();	
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream("matrix.csv")));
		for (int i = 0; i < m_hArray.size(); i++) {
			DuoObject cr = (DuoObject)m_hArray.elementAt(i);				
			DataTuple tuple = (DataTuple)cr.getObj1();
			DuoObject obj2 = (DuoObject)cr.getObj2();
			String clname = (String)obj2.getObj1();			
			for (int j = 1; j < nbattr-1; j++) {
				ClusAttrType type = schema.getAttrType(j);
				wrt.print(type.getString(tuple));
				wrt.print(",");
			}
			wrt.println(clname);
		}
		wrt.close();
	}	
	
	public boolean compareTuple(DataTuple t1, DataTuple t2, ClusSchema schema) {
		int nbattr = schema.getNbAttributes();	
		for (int i = 1; i < nbattr-1; i++) {
			ClusAttrType type = schema.getAttrType(i);
			if (type.compareValue(t1, t2) != 0) return false;
		}
		return true;
	}

	public void process() throws IOException, ClusException {
		int ctr = 0;
		TupleIterator iter = new DiskTupleIterator(m_hFName, null);
		iter.init();
		ClusSchema schema = iter.getSchema();
		DataTuple tuple = iter.readTuple();
		while (tuple != null) {		
			ctr++;
			if ((ctr % 100) == 0) {
				System.out.print("."); 
				System.out.flush();
			}
			addTuple(tuple, schema);			
			tuple = iter.readTuple();			
		}
		iter.close();
		System.out.println(); 
		outputDuplicates(ctr, m_NbDuplicates);
		outputMatrix(schema);
	}

	public static void main(String[] args) {
		ComposeDuplicates cd = new ComposeDuplicates(args[0]);
		try {
			cd.process();
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		} catch (ClusException e) {
			System.out.println("Clus Error: "+e.getMessage());
		}
	}
}
