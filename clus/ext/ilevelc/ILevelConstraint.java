
package clus.ext.ilevelc;

import java.io.*;
import java.util.*;

import clus.data.rows.*;
import clus.util.ClusException;

public class ILevelConstraint {

	public final static int ILevelCMustLink = 0;
	public final static int ILevelCCannotLink = 1;	
	
	protected int m_Type;
	protected DataTuple m_T1, m_T2;
	
	public ILevelConstraint(DataTuple t1, DataTuple t2, int type) {
		m_T1 = t1; m_T2 = t2; m_Type = type;
	}
	
	public DataTuple getT1() {
		return m_T1;
	}
	
	public DataTuple getT2() {
		return m_T2;
	}
		
	public int getType() {
		return m_Type;
	}

	public int getOtherTupleIdx(DataTuple tuple) {
		return tuple == m_T1 ? m_T2.getIndex() : m_T1.getIndex();
	}
	
	public boolean isSideOne(DataTuple tuple) {
		return tuple == m_T1;
	}
	
	public static void loadConstraints(String fname, ArrayList constr, ArrayList points) throws IOException {
		LineNumberReader rdr = new LineNumberReader(new InputStreamReader(new FileInputStream(fname)));
		rdr.readLine();
		String line = rdr.readLine();
		while (line != null) {
			StringTokenizer tokens = new StringTokenizer(line, "\t");
			int t1 = Integer.parseInt(tokens.nextToken());
			int t2 = Integer.parseInt(tokens.nextToken());
			int type = Integer.parseInt(tokens.nextToken()) == 1 ? ILevelCMustLink : ILevelCCannotLink; 
			constr.add(new ILevelConstraint((DataTuple)points.get(t1), (DataTuple)points.get(t2), type));			
			line = rdr.readLine();
		}					
		rdr.close();					
	}
	
	public static ArrayList loadConstraints(String fname, ArrayList points) throws ClusException {
		ArrayList constr = new ArrayList();
		try {
			ILevelConstraint.loadConstraints(fname, constr, points);
			return constr;
		} catch (IOException e) {
			throw new ClusException("Error opening '"+fname+"': "+e.getMessage()); 
		} 
	}
}
