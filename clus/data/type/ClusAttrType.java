package clus.data.type;

import java.io.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.rows.*;
import clus.data.cols.*;
import clus.data.cols.attribute.*;

import clus.algo.kNN.BasicDistance;

public abstract class ClusAttrType implements ClusAttrProxy, Serializable {

	public final static int STATUS_TARGET = 0;
	public final static int STATUS_NORMAL = 1;
	public final static int STATUS_SPECIAL = 2;
	public final static int STATUS_KEY = 3;
	public final static int STATUS_DISABLED = 4;
	public final static int NB_STATUS = 5;

  // TODO: Check if this makes sense, perhaps move to some other class
  public final static int ATTR_USE_ALL = -1;
  public final static int ATTR_USE_DESCRIPTIVE = 0;
  public final static int ATTR_USE_CLUSTERING = 1;
  public final static int ATTR_USE_TARGET = 2;
  
	public final static int VALUE_TYPE_NONE = -1;
	public final static int VALUE_TYPE_INT = 0;
	public final static int VALUE_TYPE_DOUBLE = 1;
	public final static int VALUE_TYPE_OBJECT = 2;
	public final static int NB_VALUE_TYPES = 3;

	public final static int NB_TYPES = 5;
	public final static int THIS_TYPE = 0;

	protected String m_Name;
	protected int m_Index, m_SpecialIdx;
	protected int m_NbMissing;
	protected ClusSchema m_Schema;

	protected int m_Status = STATUS_NORMAL;

	public ClusAttrType(String name) {
		m_Name = name;
		m_Index = -1;
		m_SpecialIdx = -1;
	}

	public void setSchema(ClusSchema schema) {
		m_Schema = schema;
	}
	
	public ClusSchema getSchema() {
		return m_Schema;
	}
	
	public Settings getSettings() {
		return m_Schema.getSettings();
	}
	
	public abstract ClusAttrType cloneType();

	public void cloneType(ClusAttrType type) {
		type.m_NbMissing = m_NbMissing;
	}

	public void copyIndices(ClusAttrType type) {
		m_Index = type.m_Index;
		m_SpecialIdx = type.m_SpecialIdx;
		m_Status = type.m_Status;
	}

	public abstract int getTypeIndex();

	public abstract int getValueType();

	public abstract String getTypeName();

	public int intHasMissing() {
		return m_NbMissing > 0 ? 1 : 0;
	}

	public boolean hasMissing() {
		return m_NbMissing > 0;
	}

	public int getNbMissing() {
		return m_NbMissing;
	}

	public void incNbMissing() {
		m_NbMissing++;
	}

	public String getName() {
		return m_Name;
	}

	public void setName(String name) {
		m_Name = name;
	}

	public int getIndex() {
		return m_Index;
	}

	public void setIndex(int idx) {
		m_Index = idx;
	}

	public int getSpecialIndex() {
		return m_SpecialIdx;
	}

	public void setSpecialIndex(int idx) {
		m_SpecialIdx = idx;
	}

	public int getStatus() {
		return m_Status;
	}

	public void setStatus(int status) {
		m_Status = status;
	}

	public boolean isTarget() {
		return m_Status == ClusAttrType.STATUS_TARGET;
	}

	public boolean isNormal() {
		return m_Status == ClusAttrType.STATUS_NORMAL;
	}

	public int getMaxNbStats() {
		return 0;
	}

	public void setReader(boolean start_stop) {
	}
/*
	public boolean addToData(ColData data) {
		return getStatus() != STATUS_NORMAL;
	}
*/
	public ClusAttrType getType() {
		return this;
	}

	public void setSplit(boolean split) {
	}

	public void setNbRows(int nbrows) {
	}

	public int getNominal(DataTuple tuple) {
		return -1;
	}

	public double getNumeric(DataTuple tuple) {
		return Double.POSITIVE_INFINITY;
	}
	
	public boolean isMissing(DataTuple tuple) {
		return true;
	}	

	public String getString(DataTuple tuple) {
		return "err";
	}

	public int compareValue(DataTuple t1, DataTuple t2) {
		return -5;
	}

	public void getPreprocs(DataPreprocs pps, boolean single) {
	}

	public ClusSerializable createRowSerializable(RowData data) throws ClusException {
		throw new ClusException("Attribute "+getName()+" does not support row data");
	}

	public ClusAttribute createTargetAttr(ColTarget target) throws ClusException {
		throw new ClusException("Attribute "+getName()+" can not be target: incompatible type");
	}

	public String toString() {
		return getName();
	}
//--------------------------------New-------------------------------------

	private BasicDistance $dist;

	/**
	 * Returns the distance between the 2 given tuples for this attribute.
	 */
	public double getBasicDistance(DataTuple t1,DataTuple t2){
		return $dist.getDistance(this,t1,t2);
	}

	/**
	 * Sets the BasicDistance for this AttributeType
	 */
	public void setBasicDistance(BasicDistance dist){
		$dist = dist;
	}

}

