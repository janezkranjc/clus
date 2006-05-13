package clus.ext.hierarchical;

import jeans.util.array.*;

import java.io.*;
import java.util.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.data.rows.*;

public class ClassesAttrType extends ClusAttrType {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;	
	
	public final static int THIS_TYPE = 2;
	public final static String THIS_TYPE_NAME = "Classes";	
	
	protected transient StringTable m_Table = new StringTable();	
	protected ClassHierarchy m_Hier;	
	
	public ClassesAttrType(String name) {
		super(name);
		m_Hier = new ClassHierarchy(this);
	}
	
	public ClassesAttrType(String name, ClassHierarchy hier) {
		super(name);
		m_Hier = hier;
	}	
	
	public ClassesAttrType(String name, String atype) throws ClusException {
		this(name);
		initializeHierarchy(atype);
	}	
	
	public StringTable getTable() {
		return m_Table;
	}
	
	public ClassHierarchy getHier() {
		return m_Hier;
	}
	
	public ClusAttrType cloneType() {
		ClassesAttrType at = new ClassesAttrType(m_Name, m_Hier);
		cloneType(at);
		return at;
	}
	
	public int getTypeIndex() {
		return THIS_TYPE;
	}	
	
	public String getTypeName() {
		return THIS_TYPE_NAME;
	}		
	
	public int getValueType() {
		return VALUE_TYPE_OBJECT;
	}	
		
	public String getString(DataTuple tuple) {
		ClassesTuple ct = (ClassesTuple)tuple.m_Objects[m_ArrayIndex];
		return ct.toStringData(m_Hier);
	}
	
	public ClusSerializable createRowSerializable(RowData data) throws ClusException {
		return new MySerializable(data);
	}
	
	public void getPreprocs(DataPreprocs pps, boolean single) {
		int mode = getSettings().getHierMode();
		if (mode == Settings.HIERMODE_TREE_DIST_WEUCLID ||
			mode == Settings.HIERMODE_TREE_DIST_ABS_WEUCLID) {
			pps.addPreproc(new ClassHierIntPreproc(this, single));
		} else {
			pps.addPreproc(new ClassHierarchyPreproc(this));
		}
	}
	
	public void initializeHierarchy(String atype) throws ClusException {
		String classes = atype.substring("HIERARCHICAL".length()).trim();
		String[] cls = classes.split("\\s*\\,\\s*");
		for (int i = 0; i < cls.length; i++) {
			if (!cls[i].equals(ClassesValue.EMPTY_SET_INDICATOR)) {
				ClassesValue val = new ClassesValue(cls[i], m_Table);
				m_Hier.addClass(val);
			}
		}		
	}
	
	public void initializeFrom(ClusAttrType other_type) {
		ClassesAttrType other = (ClassesAttrType)other_type;
		m_Hier = other.getHier();
	}
	
	// Some attributes initialize differently based on some user settings
	// For HMC, this is whether it uses a tree or DAG representation
	public void initSettings(Settings sett) {
		if (sett.getHierType() == Settings.HIERTYPE_DAG) {
			getHier().setHierType(ClassHierarchy.DAG);
		}
	}
	
	public void writeARFFType(PrintWriter wrt) throws ClusException {
		ArrayList list = getHier().getAllPaths();
		wrt.print("hierarchial ");
		for (int i = 0; i < list.size(); i++) {
			if (i != 0) wrt.print(",");
			wrt.print((String)list.get(i));
		}
	}	
	
	public class MySerializable extends RowSerializable {
		
		public MySerializable(RowData data) {
			super(data);
		}
		
		public void read(ClusReader data, DataTuple tuple) throws IOException {
			String val = data.readString();
			ClassesTuple ct;
			try {
				ct = new ClassesTuple(val, m_Table);
				ct.setAllIntermediate(false);
				tuple.setObjectVal(ct, getArrayIndex());
			}
			catch (ClusException e) {
				throw new IOException("Error parsing attribute "+getName()+" '"+val+"' at row: "+(data.getRow()+1));
			}
		}
	}
}
