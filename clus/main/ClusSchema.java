package clus.main;

import jeans.util.*;
import java.io.*;
import java.util.*;

import clus.data.type.*;
import clus.data.rows.*;
import clus.util.*;
import clus.selection.*;

public class ClusSchema implements Serializable {

	public final static long serialVersionUID = 1L;
	
	public final static int ROWS = 0;
	public final static int COLS = 1;

	protected String m_Relation;
	protected int m_NbAttrs;
	protected int m_NbRows;
	protected int m_NbInts, m_NbDoubles, m_NbObjects;
	protected MyArray m_Attr = new MyArray();	
	protected ClusAttrType[][] m_AllAttrUse;
	protected NominalAttrType[][] m_NominalAttrUse;
	protected NumericAttrType[][] m_NumericAttrUse;
	protected Settings m_Settings;
	protected IndexAttrType m_TSAttr;
	protected IntervalCollection m_Target = IntervalCollection.EMPTY;
	protected IntervalCollection m_Disabled = IntervalCollection.EMPTY;	
	protected IntervalCollection m_Clustering = IntervalCollection.EMPTY;
	protected IntervalCollection m_Descriptive = IntervalCollection.EMPTY;	
	protected IntervalCollection m_Key = IntervalCollection.EMPTY;
	protected TargetSchema m_TargetSchema;
	
	public ClusSchema(String name) {
		m_Relation = name;
	}
	
	public ClusSchema(String name, String descr) {
		m_Relation = name;
		addFromString(descr);
	}
	
	public void setSettings(Settings sett) {
		m_Settings = sett;
	}
	
	public Settings getSettings() {
		return m_Settings;
	}

	public final String getRelationName() {
		return m_Relation;
	}

	public final void setRelationName(String name) {
		m_Relation = name;
	}
	
	public final TargetSchema getTargetSchema() {
		return m_TargetSchema;
	}
	
	public final int getNbRows() {
		return m_NbRows;
	}	
		
	public final void setNbRows(int nb) {
		m_NbRows = nb;
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType attr = (ClusAttrType)m_Attr.elementAt(j);
			attr.setNbRows(nb);
		}	
	}
		
/***********************************************************************
 * Methods for retrieving attribute types                              *
 ***********************************************************************/	
	
	public final int getNbAttributes() {
		return m_NbAttrs;
	}	
	
	public final ClusAttrType getAttrType(int idx) {
		return (ClusAttrType)m_Attr.elementAt(idx);
	}
	
	public final ClusAttrType getAttrType(String name) {
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType attr = (ClusAttrType)m_Attr.elementAt(j);
			if (name.equals(attr.getName())) return attr;
		}	
		return null;
	}
	
	public final ClusAttrType[] getAllAttrUse(int attruse) {
		return m_AllAttrUse[attruse];
	}
	
	public final int getNbAllAttrUse(int attruse) {
		return m_AllAttrUse[attruse].length;
	}	
	
	public final NominalAttrType[] getNominalAttrUse(int attruse) {
		return m_NominalAttrUse[attruse];
	}

	public final int getNbNominalAttrUse(int attruse) {
		return m_NominalAttrUse[attruse].length;
	}	
	
	public final NumericAttrType[] getNumericAttrUse(int attruse) {
		return m_NumericAttrUse[attruse];
	}
	
	public final int getNbNumericAttrUse(int attruse) {
		return m_NumericAttrUse[attruse].length;
	}	
	
	public final ClusAttrType[] getDescriptiveAttributes() {
		return m_AllAttrUse[ClusAttrType.ATTR_USE_DESCRIPTIVE];
	}	

	public final int getNbDescriptiveAttributes() {
		return getNbAllAttrUse(ClusAttrType.ATTR_USE_DESCRIPTIVE);
	}
	
	public final int getNbTargetAttributes() {
		return getNbAllAttrUse(ClusAttrType.ATTR_USE_TARGET);
	}		
	
	public final int getNbNominalDescriptiveAttributes() {
		return getNbNominalAttrUse(ClusAttrType.ATTR_USE_DESCRIPTIVE);
	}
	
	public final int getNbNumericDescriptiveAttributes() {
		return getNbNumericAttrUse(ClusAttrType.ATTR_USE_DESCRIPTIVE);
	}
	
	public final int getNbNominalTargetAttributes() {
		return getNbNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
	}
	
	public final int getNbNumericTargetAttributes() {
		return getNbNumericAttrUse(ClusAttrType.ATTR_USE_TARGET);
	}	
	
	public final int getNbInts() {
		return m_NbInts;
	}

	public final int getNbDoubles() {
		return m_NbDoubles;
	}

	public final int getNbObjects() {
		return m_NbObjects;
	}	
	
/***********************************************************************
 * Methods for adding attributes to the schema                         *
 ***********************************************************************/		
	
	public final void addAttrType(ClusAttrType attr) {
		m_Attr.addElement(attr);
		attr.setIndex(m_NbAttrs++);
		attr.setSchema(this);
	}	
	
	public final void setAttrType(ClusAttrType attr, int idx) {
		m_Attr.setElementAt(attr, idx);
		attr.setIndex(idx);
		attr.setSchema(this);
	}
	
	public final void addFromString(String descr) {
		StringTokenizer tokens = new StringTokenizer(descr, "[]");
		while (tokens.hasMoreTokens()) {
			String type = tokens.nextToken();
			String name = tokens.hasMoreTokens() ? tokens.nextToken() : "";
			if (type.equals("f")) {
				addAttrType(new NumericAttrType(name));
			}
		}
	}

/***********************************************************************
 * Methods concerning missing values                                   *
 ***********************************************************************/		
	
	public final boolean hasMissing() {		
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType attr = (ClusAttrType)m_Attr.elementAt(j);
			if (attr.hasMissing()) return true;
		}
		return false;
	}
	
	public final double getTotalInputNbMissing() {
		int nb_miss = 0;
		ClusAttrType[] attrs = getDescriptiveAttributes();
		for (int j = 0; j < attrs.length; j++) {
			ClusAttrType at = (ClusAttrType)attrs[j];
			nb_miss += at.getNbMissing();
		}
		return (double)nb_miss;
	}	
	
/***********************************************************************
 * Methods for working with interval collections of attributes         *
 ***********************************************************************/	
	
	public final IntervalCollection getTarget() {
		return m_Target;
	}	

	public final IntervalCollection getDisabled() {
		return m_Disabled;
	}	
	
	public final IntervalCollection getClustering() {
		return m_Clustering;
	}
	
	public final IntervalCollection getDescriptive() {
		return m_Descriptive;
	}
			
	public final void setTarget(IntervalCollection coll) {		
		m_Target = coll;
		
	}
	
	public final void setDisabled(IntervalCollection coll) {		
		m_Disabled = coll;
	}
	
	public final void setClustering(IntervalCollection coll) {		
		m_Clustering = coll;
	}	

	public final void setDescriptive(IntervalCollection coll) {		
		m_Descriptive = coll;
	}		
	
	public final void setKey(IntervalCollection coll) {		
		m_Key = coll;		
	}
	
	public final void updateAttributeUse() {
		if (m_Target.isDefault()) {
			// By default, the last non-disabled attribute is the target
			m_Target.clear();
			boolean[] bits = new boolean[getNbAttributes()+1];
			m_Disabled.toBits(bits);
			int target = bits.length-1;
			while (target >= 0 && bits[target]) target--;
			if (target > 0) m_Target.addInterval(target, target);			
		} else {
			// Target and all other settings have precedence over disabled
			m_Disabled.subtract(m_Target);
		}
		if (m_Descriptive.isDefault()) {
			// By default all attributes that are not target and not disabled
			m_Descriptive.clear();
			m_Descriptive.addInterval(1,getNbAttributes());
			m_Descriptive.subtract(m_Target);
			m_Descriptive.subtract(m_Disabled);
		} else {
			m_Disabled.subtract(m_Descriptive);
		}
		if (m_Clustering.isDefault()) {
			// By default same as target attributes
			m_Clustering.copyFrom(m_Target);
		} else {
			m_Disabled.subtract(m_Clustering);
		}
		m_Disabled.subtract(m_Key);
		setStatusAll(ClusAttrType.STATUS_NORMAL);
		setStatus(m_Disabled, ClusAttrType.STATUS_DISABLED, true);
		setStatus(m_Target, ClusAttrType.STATUS_TARGET, true);
		setStatus(m_Clustering, ClusAttrType.STATUS_CLUSTER_NO_TARGET, false);
		setStatus(m_Key, ClusAttrType.STATUS_KEY, true);
		setDescriptiveAll(false);
		setDescriptive(m_Descriptive, true);
	}
	
	public final void setStatus(IntervalCollection coll, int status, boolean force) {
		coll.reset();
		while (coll.hasMoreInts()) {
			ClusAttrType at = getAttrType(coll.nextInt()-1);
			if (force || at.getStatus() == ClusAttrType.STATUS_NORMAL) {
				at.setStatus(status);
			}
		}			
	}
	
	public final void setStatusAll(int status) {
		for (int i = 0; i < getNbAttributes(); i++) {
			ClusAttrType at = getAttrType(i);
			at.setStatus(status);
		}			
	}
	
	public final void setDescriptive(IntervalCollection coll, boolean descr) {
		coll.reset();
		while (coll.hasMoreInts()) {
			ClusAttrType at = getAttrType(coll.nextInt()-1);
			at.setDescriptive(descr);
		}
	}
	
	public final void setDescriptiveAll(boolean descr) {
		for (int i = 0; i < getNbAttributes(); i++) {
			ClusAttrType at = getAttrType(i);
			at.setDescriptive(descr);
		}			
	}	
	
	public final void addIndices(int type) throws ClusException {
		if (type == COLS) {
// FIXME: COLS mode currently disabled :-(		
//			m_NbTarNum = makeSpecialIndex(NumericAttrType.class, ClusAttrType.STATUS_TARGET);
//			m_NbTarNom = makeSpecialIndex(NominalAttrType.class, ClusAttrType.STATUS_TARGET);
			addColsIndex();
		} else {
			addRowsIndex();
		}
		m_TargetSchema = makeTargetSchema();
	}
	
	public final void showDebug() {
		System.out.println("Nb ints: "+getNbInts());
		System.out.println("Nb double: "+getNbDoubles());		
		System.out.println("Nb obj: "+getNbObjects());		
		System.out.println("Idx   Name                          Descr Status    Ref   Type");
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			System.out.print(StringUtils.printInt(j+1, 6));
			System.out.print(StringUtils.printStr(at.getName(), 30));
			if (at.isDescriptive()) System.out.print("Yes   ");
			else System.out.print("No    ");
			switch (at.getStatus()) {
				case ClusAttrType.STATUS_NORMAL:			
					System.out.print("          ");
					break;				
				case ClusAttrType.STATUS_DISABLED:
					System.out.print("Disabled  ");
					break;				
				case ClusAttrType.STATUS_TARGET:
					System.out.print("Target    ");
					break;				
				case ClusAttrType.STATUS_CLUSTER_NO_TARGET:
					System.out.print("Cluster   ");
					break;				
				case ClusAttrType.STATUS_KEY:
					System.out.print("Key       ");
					break;									
				default:
					System.out.print("Error     ");				
					break;
			}
			System.out.print(StringUtils.printInt(at.getArrayIndex(), 6));
			System.out.print(StringUtils.printStr(at.getTypeName(), 20));			
			System.out.print(StringUtils.printStr(ClusFormat.TWO_AFTER_DOT.format(at.getNbMissing()), 8));			
			System.out.println();
		}
	}
			
	// Used for enabling multi-score
	public final boolean isRegression() {
		return getNbNumericTargetAttributes() > 0;
	}
	
	public final void setReader(boolean start_stop) {	
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType attr = (ClusAttrType)m_Attr.elementAt(j);
			if (attr.getStatus() != ClusAttrType.STATUS_DISABLED) attr.setReader(start_stop);
		}
	}
	
	public final void getPreprocs(DataPreprocs pps, boolean single) {
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType attr = (ClusAttrType)m_Attr.elementAt(j);
			if (attr.getStatus() != ClusAttrType.STATUS_DISABLED) attr.getPreprocs(pps, single);
		}	
	}	
	
	
	public final int getMaxNbStats() {
		int max = 0;
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType attr = (ClusAttrType)m_Attr.elementAt(j);
			if (attr.getStatus() == ClusAttrType.STATUS_NORMAL) {
				max = Math.max(attr.getMaxNbStats(), max);
			}				
		}	
		return max;
	}
	
	public final XValMainSelection getXValSelection(ClusData data) throws ClusException {
		if (m_TSAttr == null) {
			return new XValRandomSelection(data.getNbRows(), getSettings().getXValFolds());
		} else {
			return new XValDataSelection(m_TSAttr);
		}
	}
		
	public final void setTestSet(int id) {
		if (id != -1) {
			System.out.println("Setting test set ID: "+id);			
			ClusAttrType type = (ClusAttrType)m_Attr.elementAt(id);
			m_Attr.setElementAt(m_TSAttr = new IndexAttrType(type.getName()), id);
		}		
	}
	
	public final void attachModel(ClusModel model) throws ClusException {
		Hashtable table = buildAttributeHash();
		model.attachModel(table);
	}
	
	public final Hashtable buildAttributeHash() {
		Hashtable hash = new Hashtable();
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			hash.put(at.getName(), at);
		}
		return hash;
	}	
	
	private TargetSchema makeTargetSchema() throws ClusException {
		int idx[] = new int[ClusAttrType.NB_TYPES];
		TargetSchema schema = new TargetSchema(getNbNominalTargetAttributes(), getNbNumericTargetAttributes());
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			if (at.getStatus() == ClusAttrType.STATUS_TARGET) {
				int mtype = at.getTypeIndex();
				schema.setType(mtype, idx[mtype]++, at);
			}
		}
		return schema;
	}	
	
	private void addColsIndex() {
		int idx = 0;	
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			if (at.getStatus() == ClusAttrType.STATUS_NORMAL) at.setArrayIndex(idx++);
		}
// FIXME: COLS mode currently disabled :-(		
//		m_NbNum = calcNbXStatus(NumericAttrType.class, ClusAttrType.STATUS_NORMAL);
//		m_NbNom = calcNbXStatus(NominalAttrType.class, ClusAttrType.STATUS_NORMAL);		
	}
		
	public static ClusAttrType[] vectorToAttrArray(ArrayList list) {
		ClusAttrType[] res = new ClusAttrType[list.size()];
		for (int i = 0; i < list.size(); i++) {
			res[i] = (ClusAttrType)list.get(i);
		}
		return res;
	}
	
	public static NominalAttrType[] vectorToNominalAttrArray(ArrayList list) {
		NominalAttrType[] res = new NominalAttrType[list.size()];
		for (int i = 0; i < list.size(); i++) {
			res[i] = (NominalAttrType)list.get(i);
		}
		return res;
	}

	public static NumericAttrType[] vectorToNumericAttrArray(ArrayList list) {
		NumericAttrType[] res = new NumericAttrType[list.size()];
		for (int i = 0; i < list.size(); i++) {
			res[i] = (NumericAttrType)list.get(i);
		}
		return res;
	}
	
	protected ArrayList collectAttributes(int attruse, int attrtype) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < getNbAttributes(); i++) {
			ClusAttrType type = getAttrType(i);
			if (attrtype == ClusAttrType.THIS_TYPE || attrtype == type.getTypeIndex()) {
				switch (attruse) {
					case ClusAttrType.ATTR_USE_ALL:
						if (type.getStatus() != ClusAttrType.STATUS_DISABLED && type.getStatus() != ClusAttrType.STATUS_KEY) {
							result.add(type);
						}
						break;						
					case ClusAttrType.ATTR_USE_CLUSTERING:
						if (type.getStatus() == ClusAttrType.STATUS_TARGET || type.getStatus() == ClusAttrType.STATUS_CLUSTER_NO_TARGET) {
							result.add(type);
						}						
						break;						
					case ClusAttrType.ATTR_USE_DESCRIPTIVE:
						if (type.isDescriptive()) {
							result.add(type);
						}						
						break;						
					case ClusAttrType.ATTR_USE_TARGET:
						if (type.getStatus() == ClusAttrType.STATUS_TARGET) {
							result.add(type);
						}						
						break;
				}				
			}
		}
		return result;
	}
	
	protected void addRowsIndex() {
		// Allocate attributes to arrays m_Ints, m_Doubles, m_Objects
		int[] nbvt = new int[ClusAttrType.NB_VALUE_TYPES];
		for (int i = ClusAttrType.STATUS_TARGET; i <= ClusAttrType.STATUS_KEY; i++) {
			for (int j = 0; j < m_NbAttrs; j++) {
				ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);				
				if (at.getStatus() == i) {
					int vtype = at.getValueType();
					int sidx = nbvt[vtype]++;
					at.setArrayIndex(sidx);				
				}
			}
		}
		m_NbInts = nbvt[ClusAttrType.VALUE_TYPE_INT];
		m_NbDoubles = nbvt[ClusAttrType.VALUE_TYPE_DOUBLE];
		m_NbObjects = nbvt[ClusAttrType.VALUE_TYPE_OBJECT];
		// Collect attributes into arrays m_Allattruse, m_Nominalattruse, m_Numericattruse
		// Sorted in order that they occur in the .arff file (to be consistent with weight vector order)
		m_AllAttrUse = new ClusAttrType[ClusAttrType.NB_ATTR_USE][];
		m_NominalAttrUse = new NominalAttrType[ClusAttrType.NB_ATTR_USE][];
		m_NumericAttrUse = new NumericAttrType[ClusAttrType.NB_ATTR_USE][];
		for (int attruse = ClusAttrType.ATTR_USE_ALL; attruse <= ClusAttrType.ATTR_USE_TARGET; attruse++) {
			m_AllAttrUse[attruse] = vectorToAttrArray(collectAttributes(attruse, ClusAttrType.THIS_TYPE));
			m_NominalAttrUse[attruse] = vectorToNominalAttrArray(collectAttributes(attruse, NominalAttrType.THIS_TYPE));
			m_NumericAttrUse[attruse] = vectorToNumericAttrArray(collectAttributes(attruse, NumericAttrType.THIS_TYPE));
		}
	}	
}
