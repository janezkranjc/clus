package clus.io;

import jeans.util.*;
import java.io.*;

import clus.main.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.util.*;

// FIXME - use plugin system
import clus.ext.hierarchical.*;

public class ARFFFile {

	protected final static String TAG_ERROR = " tag not found in ARFF file, found instead: '";
	protected final static String[] TAG_NAME = {"@RELATION", "@ATTRIBUTE", "@DATA"};

	protected ClusReader m_Reader;
	protected int m_DataLine = -1;

	public ARFFFile(ClusReader reader) {
		m_Reader = reader;
	}

	public ClusSchema read(Settings sett) throws IOException, ClusException {
		int expected = 0;
		ClusSchema schema = new ClusSchema(m_Reader.getName());
		schema.setSettings(sett);
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
				if (expected == 0) throw new IOException(TAG_NAME[expected]+TAG_ERROR+token+"'");
				String aname = tokens.getDelimToken('\"','\"');
				String atype = tokens.readTillEol();
				int idx = atype.indexOf('%');
				if (idx != -1) atype = atype.substring(0,idx-1);				
				atype = atype.trim();
				addAttribute(schema, aname, atype);
				expected = 2;
			} else if (token.equals(TAG_NAME[2])) {
				if (expected != 2) throw new IOException(TAG_NAME[expected]+TAG_ERROR+token+"'");
				m_DataLine = tokens.getLine();
				expected = 3;
			} else {
				throw new IOException(TAG_NAME[expected]+TAG_ERROR+token+"'");
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
		if (uptype.equals("NUMERIC") || uptype.equals("REAL") || uptype.equals("INTEGER")) {
			schema.addAttrType(new NumericAttrType(aname));
		} else if (uptype.equals("CLASSES")) {
			ClassesAttrType type = new ClassesAttrType(aname);
			schema.addAttrType(type);
			type.initSettings(schema.getSettings());
		} else if (uptype.startsWith("HIERARCHICAL")) {
			ClassesAttrType type = new ClassesAttrType(aname);
			schema.addAttrType(type);
			type.initSettings(schema.getSettings());
			type.initializeHierarchy(atype);
		} else if (uptype.equals("STRING")) {
			schema.addAttrType(new StringAttrType(aname));
		} else if (uptype.equals("KEY")) {
			StringAttrType key = new StringAttrType(aname);
			schema.addAttrType(key);
			key.setStatus(ClusAttrType.STATUS_KEY);
		} else if (uptype.equals("TIMESERIES")) {
			TimeSeriesAttrType tsat = new TimeSeriesAttrType(aname);
			schema.addAttrType(tsat);			
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
	
	public static void writeArffHeader(PrintWriter wrt, ClusSchema schema) throws IOException, ClusException {
		wrt.println("@RELATION "+schema.getRelationName());
		wrt.println();
		for (int i = 0; i < schema.getNbAttributes(); i++) {
			ClusAttrType type = schema.getAttrType(i);
			if (!type.isDisabled()) {
					wrt.print("@ATTRIBUTE ");
					wrt.print(StringUtils.printStr(type.getName(), 40));
					if (type.isKey()) {
						wrt.print("key");
					} else {
						type.writeARFFType(wrt);
					}
					wrt.println();
			}
		}
		wrt.println();
	}
	
	public static RowData readArff(String fname) throws IOException, ClusException {
			ClusReader reader = new ClusReader(fname, null);
			ARFFFile arff = new ARFFFile(reader);
			ClusSchema schema = arff.read(null);
			schema.setNbRows(reader.countRows());
			schema.initialize();
			arff.skipTillData();
			RowData data = new RowData(schema);
			data.resize(schema.getNbRows());
			ClusView view = data.createNormalView(schema);
			view.readData(reader, schema);
			reader.close();
			return data;
	}
	
	public static void writeArff(String fname, RowData data) throws IOException, ClusException {
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
		ClusSchema schema = data.getSchema();
		writeArffHeader(wrt, schema);
		wrt.println("@DATA");
		for (int j = 0; j < data.getNbRows(); j++) {
			DataTuple tuple = data.getTuple(j);
			int aidx = 0;
			for (int i = 0; i < schema.getNbAttributes(); i++) {
				ClusAttrType type = schema.getAttrType(i);
				if (!type.isDisabled()) {
					if (aidx != 0) wrt.print(",");
					wrt.print(type.getString(tuple));
					aidx++;
				}
			}
			wrt.println();
		}		
		wrt.close();
	}

	// Can be deleted ...
	public static void writeCN2Data(String fname, RowData data) throws IOException, ClusException {
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
		ClusSchema schema = data.getSchema();
		wrt.println("**EXAMPLE FILE**\n");
		for (int j = 0; j < data.getNbRows(); j++) {
			DataTuple tuple = data.getTuple(j);
			int aidx = 0;
			for (int i = 0; i < schema.getNbAttributes(); i++) {
				ClusAttrType type = schema.getAttrType(i);
				if (!type.isDisabled()) {
					if (aidx != 0) wrt.print("\t");
					if (type instanceof NominalAttrType) {
            String label = type.getString(tuple);
            label = label.replace("^2","two");
            label = label.replace("<","le");
            label = label.replace(">","gt");
            label = label.replace("-","_");
            label = label.replace("&","");
            if (!label.equals("?")) {
            	wrt.print("_"+label);
            } else {
            	wrt.print(label);	
            }
					} else {
						wrt.print(type.getString(tuple));						
					}
					aidx++;
				}
			}
			wrt.println(";");
		}		
		wrt.close();
	}

}
