/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.ext.hierarchical;

import jeans.util.array.*;

import java.io.*;
import java.util.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.data.io.ClusReader;
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
		// this builds the hierarchy based on the data
		// and adds intermediate class nodes to each example
		pps.addPreproc(new ClassHierarchyPreproc(this, true));
	}
	
	public void initializeHierarchy(String atype) throws ClusException, IOException {
		// hierarchy is given in @attribute specification in .arff
		// this means we can initialize the hierarchy and lock it
		// so that no classes can be added afterwards
		String classes = atype.substring("HIERARCHICAL".length()).trim();
		String[] cls = classes.split("\\s*\\,\\s*");
		if (getSettings().hasDefinitionFile()) {
			m_Hier.loadDAG(getSettings().getDefinitionFile());
		} else {
			if (getSettings().getHierType() == Settings.HIERTYPE_DAG) {
				m_Hier.loadDAG(cls);
			} else {
				for (int i = 0; i < cls.length; i++) {
					if (!cls[i].equals(ClassesValue.EMPTY_SET_INDICATOR)) {
						ClassesValue val = new ClassesValue(cls[i], m_Table);
						m_Hier.addClass(val);
					}
				}
			}
		}
		m_Hier.initialize();		
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
		ArrayList list;
		if (getSettings().getHierType() == Settings.HIERTYPE_DAG) {
			list = getHier().getAllParentChildTuples();
		} else {
			list = getHier().getAllPaths();
		}
		wrt.print("hierarchical ");
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
