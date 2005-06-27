package clus.ext.hierarchical;

import jeans.util.*;
import jeans.util.array.*;

import java.io.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.data.rows.*;

public class ClassesAttrType extends ClusAttrType {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;	
	
	public final static int THIS_TYPE = 2;
	public final static String THIS_TYPE_NAME = "Classes";	
	
	protected MyArray m_TargetValues = new MyArray();
	
	protected transient StringTable m_Table = new StringTable();	
	protected ClassHierarchy m_Hier;	
	protected int m_NbValues;
	
	public ClassesAttrType(String name) {
		super(name);
		m_Hier = new ClassHierarchy(this);
	}
	
	public ClassesAttrType(String name, String atype) throws ClusException {
		this(name);
		initializeHierarchy(atype);
	}	
	
	public ClassHierarchy getHier() {
		return m_Hier;
	}
	
	public ClusAttrType cloneType() {
		return null;
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
	
	public int addTargetValue(Object target) {
		m_TargetValues.addElement(target);
		return m_NbValues++; 
	}
	
	public String getString(DataTuple tuple) {
		ClassesTuple ct = (ClassesTuple)tuple.m_Objects[m_ArrayIndex];
		return "["+ct.toStringHumanNoIntermediate(m_Hier)+"]";
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
			ClassesValue val = new ClassesValue(cls[i], m_Table);
			m_Hier.addClass(val);			
		}		
	}
	
	public void initializeFrom(ClusAttrType other_type) {
		ClassesAttrType other = (ClassesAttrType)other_type;
		m_Hier = other.getHier();
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
