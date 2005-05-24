import clus.tools.debug.Debug;

import jeans.util.*;
import jeans.tree.*;

import java.io.*;
import java.util.*;
import jeans.util.array.*;

import clus.util.*;
import clus.main.*;
import clus.ext.hierarchical.*;

public class HierPrologWriter {

	public int m_Number;
	public Vector m_Keys = new Vector();
	public Hashtable m_Classes = new Hashtable();

	public void execute(String fname, String examplefile) throws IOException, ClusException {
		HierIO io = new HierIO();
		ClassHierarchy hier = io.readHierarchy(fname);
		String mainname = FileUtil.getName(FileUtil.removePath(fname));
		mainname = StringUtils.replaceChars(mainname, '-', '_');
		hier.numberHierarchy();
		io.writeHierarchy(hier.getRoot());
		PrintWriter prolog = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mainname+".pl")));
		prolog.print(mainname+"(");
		io.writePrologTerm(hier.getRoot(), prolog);
		prolog.println(").");
		prolog.println();		
		io.writePrologGraph(mainname, hier.getRoot(), prolog);		
		prolog.close();
		
		ClassesValue.PATH_ORDER = false;
		ClassesValue.LOAD_HSEP = "|";
		
		// This could be made more efficient
		mainname = FileUtil.getName(FileUtil.removePath(examplefile));
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(examplefile)));
		String line = reader.readLine();
		while (line != null) {
			int idx = line.indexOf(',');
			if (idx == -1) throw new ClusException("Error in data line: "+line);
			String key = line.substring(0,idx);
			String value = line.substring(idx+1, line.length());
			ClassesValue myclass = new ClassesValue(value, io.getTable());
			ClassTerm term = hier.getCheckClassTerm(myclass);
			Vector terms = (Vector)m_Classes.get(key);
			if (terms == null) {
				terms = new Vector();
				m_Classes.put(key, terms);
			}
			terms.addElement(term);
			m_Keys.addElement(key);			
			line = reader.readLine();
		}
		reader.close();
		
		boolean[] matrix = new boolean[hier.getTotal()];
		PrintWriter pr1 = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mainname+".pl")));
		for (int i = 0; i < m_Keys.size(); i++) {
			String key = (String)m_Keys.elementAt(i);
			Vector terms = (Vector)m_Classes.get(key);		
			for (int j = 0; j < hier.getTotal(); j++) {
				matrix[j] = false;
			}
			for (int j = 0; j < terms.size(); j++) {
				ClassTerm cr = (ClassTerm)terms.elementAt(j);
				while (cr != null) {
					matrix[cr.getIndex()] = true;
					cr = cr.getCTParent();
				}	
			}
			CompleteTreeIterator it_i = hier.getRootIter();
			while (it_i.hasMoreNodes()) {
				ClassTerm cr = (ClassTerm)it_i.getNextNode();
				int idx = cr.getIndex();
				if (matrix[idx]) {
					pr1.println(mainname+"_class('"+key+"',"+idx+").");
				}
			}
			pr1.print(mainname+"_term('"+key+"',");
			HierIO.writePrologTerm(hier.getRoot(), matrix, pr1);
			pr1.println(").");						
		}
		pr1.close();
	}
	
	public static void main(String[] args) {
		try {
			HierPrologWriter wrt = new HierPrologWriter();
			wrt.execute(args[0], args[1]);
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		} catch (ClusException e) {
			System.out.println("Clus Error: "+e.getMessage());		
		}
	}	

}
