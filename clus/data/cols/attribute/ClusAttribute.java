package clus.data.cols.attribute;

import jeans.util.*;

import clus.io.*;
import clus.main.*;
import clus.data.type.*;
import clus.data.cols.*;
import clus.selection.*;

public abstract class ClusAttribute extends ClusSerializable implements ClusAttrProxy {

	protected boolean m_Split;

	public void resize(int rows) {
	}
	
	public void setSplit(boolean split) {
		m_Split = split;		
	}
	
	public boolean isSplit() {
		return m_Split;
	}

	public String getName() {
		return getType().getName();
	}
	
	public abstract ClusAttrType getType();
	
	public void prepare() {
	}
	
	public void unprepare() {
	}
	
	public void findBestTest(MyArray leaves, ColTarget target, ClusStatManager smanager) {
	}
	
	public void split(ColTarget target) {		
	}	

	public ClusAttribute select(ClusSelection sel, int nbsel) {
		return null;
	}
	
	public void insert(ClusAttribute attr, ClusSelection sel, int nb_new) {
	}
}
