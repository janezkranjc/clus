package clus.io;

import jeans.util.*;
import java.io.*;

import clus.main.*;
import clus.data.type.*;
import clus.util.*;

// FIXME - use plugin system
import clus.ext.hierarchical.*;

public class ARFFFile {

	protected final static String TAG_ERROR = " tag not found in ARFF file";
	protected final static String[] TAG_NAME = {"@RELATION", "@ATTRIBUTE", "@DATA"};

	protected ClusReader m_Reader;
	protected int m_DataLine = -1;

	public ARFFFile(ClusReader reader) {
		m_Reader = reader;
	}

	public ClusSchema read() throws IOException, ClusException {
		int expected = 0;
		ClusSchema schema = new ClusSchema(m_Reader.getName());
		MStreamTokenizer tokens = m_Reader.getTokens();
		String token = tokens.getToken().toUpperCase();
		while (expected < 3) {
			if (token == null) {
				throw new IOException("End of ARFF file before "+TAG_NAME[expected]+" tag");
			}
			if (token.equals(TAG_NAME[0])) {
				schema.setRelationName(tokens.readTillEol().trim());
				// System.out.println("Relation name: "+schema.getRelationName());
				expected = 1;				
			} else if (token.equals(TAG_NAME[1])) {
				if (expected == 0) throw new IOException(TAG_NAME[expected]+TAG_ERROR);
				String aname = tokens.getDelimToken('\"','\"');
				String atype = tokens.readTillEol();
				int idx = atype.indexOf('%');
				if (idx != -1) atype = atype.substring(0,idx-1);				
				atype = atype.trim();
				addAttribute(schema, aname, atype);
				expected = 2;
			} else if (token.equals(TAG_NAME[2])) {
				if (expected != 2) throw new IOException(TAG_NAME[expected]+TAG_ERROR);
				m_DataLine = tokens.getLine();
				expected = 3;
			} else {
				throw new IOException(TAG_NAME[expected]+TAG_ERROR);
			}
			if (expected < 3) token = tokens.getToken().toUpperCase();
		}
		// System.out.println("Number of attributes: "+schema.getNbAttributes());
		return schema;
	}
	
	public void skipTillData() throws IOException {
		boolean error = false;
		MStreamTokenizer tokens = m_Reader.getTokens();
		String token = tokens.getToken().toUpperCase();		
		while (token != null) {
			if (m_DataLine != -1 && tokens.getLine() > m_DataLine) {
				error = true;
				break;
			}
			if (token.equals(TAG_NAME[2])) {
				break;
			}				
			token = tokens.getToken().toUpperCase();
		}
		if (token == null || error) {
			throw new IOException("Unexpected ARFF reader error looking for @data tag");		
		}
	}	
		
	protected void addAttribute(ClusSchema schema, String aname, String atype) throws IOException, ClusException {
		String uptype = atype.toUpperCase();
		if (uptype.equals("NUMERIC") || uptype.equals("REAL")) {
			schema.addAttrType(new NumericAttrType(aname));
		} else if (uptype.equals("CLASSES")) {
			schema.addAttrType(new ClassesAttrType(aname));
		} else if (uptype.startsWith("HIERARCHICAL")) {
			schema.addAttrType(new ClassesAttrType(aname, atype));			
		} else if (uptype.equals("STRING")) {
			schema.addAttrType(new StringAttrType(aname));
		} else if (uptype.equals("KEY")) {
			StringAttrType key = new StringAttrType(aname);
			schema.addAttrType(key);
			key.setStatus(ClusAttrType.STATUS_KEY);
		} else {
			if (uptype.equals("BINARY")) atype = "{1,0}";
			int tlen = atype.length();
			if (tlen > 2 && atype.charAt(0) == '{' && atype.charAt(tlen-1) == '}') {
				schema.addAttrType(new NominalAttrType(aname, atype));
			} else {
				throw new IOException("Attribute '"+aname+"' has unknown type '"+atype+"'");
			}				
		}
	}
}
