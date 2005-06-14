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
	protected int m_NbAttrs, m_NbNormalAttr;
	protected int m_NbRows;
	protected int[][] m_NbType;	
	protected int[] m_NbStatusType;	
	protected int m_NbInts, m_NbDoubles, m_NbObjects;
	protected MyArray m_Attr = new MyArray();	
	protected ClusAttrType[] m_NormalAttr;
	protected ClusAttrType[][] m_AttrByArrayIdx;
	protected TargetSchema m_TargetSchema;
	protected Settings m_Settings;
	protected transient IndexAttrType m_TSAttr;
	protected transient IntervalCollection m_Target = IntervalCollection.EMPTY;
	protected transient IntervalCollection m_Disabled = IntervalCollection.EMPTY;	
	protected transient IntervalCollection m_Key = IntervalCollection.EMPTY;

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

	public final String getRelationName() {
		return m_Relation;
	}
	
	public final double getTotalInputNbMissing() {
		int nb_miss = 0;
		ClusAttrType[] attrs = getNormalAttrs();
		for (int j = 0; j < attrs.length; j++) {
			ClusAttrType at = (ClusAttrType)attrs[j];
			nb_miss += at.getNbMissing();
		}
		return (double)nb_miss;
	}
	
	public final void setTestSet(int id) {
		if (id != -1) {
			System.out.println("Setting test set ID: "+id);			
			ClusAttrType type = (ClusAttrType)m_Attr.elementAt(id);
			m_Attr.setElementAt(m_TSAttr = new IndexAttrType(type.getName()), id);
		}		
	}

	public final void setRelationName(String name) {
		m_Relation = name;
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
	
	public final TargetSchema getTargetSchema() {
		return m_TargetSchema;
	}
	
	public final int getNbAttributes() {
		return m_NbAttrs;
	}

	public final int getNbRows() {
		return m_NbRows;
	}	
	
	public final boolean hasMissing() {		
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType attr = (ClusAttrType)m_Attr.elementAt(j);
			if (attr.hasMissing()) return true;
		}
		return false;
	}		
	
	public final void setNbRows(int nb) {
		m_NbRows = nb;
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType attr = (ClusAttrType)m_Attr.elementAt(j);
			attr.setNbRows(nb);
		}	
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
	
	public final void setTarget(IntervalCollection coll) {		
		m_Target = coll;
		setStatus(coll, ClusAttrType.STATUS_TARGET);
	}
	
	public final void setDisabled(IntervalCollection coll) {		
		m_Disabled = coll;
		setStatus(coll, ClusAttrType.STATUS_DISABLED);
	}	
	
	public final void setKey(IntervalCollection coll) {		
		m_Key = coll;
		setStatus(coll, ClusAttrType.STATUS_KEY);
	}	
	
	public final void addIndices(int type) throws ClusException {
		countAttrTypes();
		updateNormalAttrs();
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
		System.out.println("Idx   Name                          Status    Ref   Type");
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			System.out.print(StringUtils.printInt(j+1, 6));
			System.out.print(StringUtils.printStr(at.getName(), 30));
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
				case ClusAttrType.STATUS_KEY:
					System.out.print("Key       ");
					break;									
				default:
					System.out.print("Error     ");				
					break;
			}
			System.out.print(StringUtils.printInt(at.getSpecialIndex(), 6));
			System.out.print(StringUtils.printStr(at.getTypeName(), 20));			
			System.out.print(StringUtils.printStr(ClusFormat.TWO_AFTER_DOT.format(at.getNbMissing()), 8));			
			System.out.println();
		}
	}
	
	public final int getNbStatus(int status) {
		return m_NbStatusType[status];
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
	
	public final int getNbNom() {
		return m_NbType[ClusAttrType.STATUS_NORMAL][NominalAttrType.THIS_TYPE];
	}	
	
	public final int getNbNum() {
		return m_NbType[ClusAttrType.STATUS_NORMAL][NumericAttrType.THIS_TYPE];
	}	
		
	public final int getNbInput() {
		int sum = 0;
		int[] input = m_NbType[ClusAttrType.STATUS_NORMAL];
		for (int i = 0; i < ClusAttrType.NB_TYPES; i++) {
			sum += input[i];
		}
		return sum;
	}
	
	public final int getNbOutput() {
		int sum = 0;
		int[] input = m_NbType[ClusAttrType.STATUS_TARGET];
		for (int i = 0; i < ClusAttrType.NB_TYPES; i++) {
			sum += input[i];
		}
		return sum;
	}	
	
	public final int getNbTarType(int type) {
		return m_NbType[ClusAttrType.STATUS_TARGET][type];	
	}
	
	public final int getNbTarNom() {
		return getNbTarType(NominalAttrType.THIS_TYPE);
	}

	public final int getNbTarNum() {
		return getNbTarType(NumericAttrType.THIS_TYPE);
	}	

  public final int getNbNumeric() {
    return getNbTarNum() + getNbNum();
  } 

	public final int getNbNormalMissing() {
		int miss = 0;
		for (int j = 0; j < m_NbNormalAttr; j++) 
			miss += m_NormalAttr[j].getNbMissing();
		return miss;
	}
	
	public final int getNbNormalAttr() {
		return m_NbNormalAttr;
	}
	
	public final ClusAttrType[] getNormalAttrs() {
		return m_NormalAttr;
	}
	
	public final ClusAttrType[] getNominalAttrs() {
		return m_AttrByArrayIdx[NominalAttrType.THIS_TYPE];
	}
	
	public final ClusAttrType[] getNumericAttrs() {
		return m_AttrByArrayIdx[NumericAttrType.THIS_TYPE];
	}	
	
	// Used for enabling multi-score
	public final boolean isRegression() {
		return getNbTarNum() > 0;
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
	
	public final void setStatus(IntervalCollection coll, int status) {
		coll.reset();
		while (coll.hasMoreInts()) {
			ClusAttrType at = getAttrType(coll.nextInt()-1);
			at.setStatus(status);
		}			
	}

	private TargetSchema makeTargetSchema() throws ClusException {
		int idx[] = new int[ClusAttrType.NB_TYPES];
		TargetSchema schema = new TargetSchema(m_NbType[ClusAttrType.STATUS_TARGET]);
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			if (at.getStatus() == ClusAttrType.STATUS_TARGET) {
				int mtype = at.getTypeIndex();
				schema.setType(mtype, idx[mtype]++, at);
			}
		}
		return schema;
	}	
	
	private void updateNormalAttrs() {
		int idx = 0;	
		m_NbNormalAttr = 0;
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			if (at.getStatus() == ClusAttrType.STATUS_NORMAL) m_NbNormalAttr++;
		}
		m_NormalAttr = new ClusAttrType[m_NbNormalAttr];
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			if (at.getStatus() == ClusAttrType.STATUS_NORMAL) m_NormalAttr[idx++] = at;
		}
	}	
	
	private void addColsIndex() {
		int idx = 0;	
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			if (at.getStatus() == ClusAttrType.STATUS_NORMAL) at.setSpecialIndex(idx++);
		}
// FIXME: COLS mode currently disabled :-(		
//		m_NbNum = calcNbXStatus(NumericAttrType.class, ClusAttrType.STATUS_NORMAL);
//		m_NbNom = calcNbXStatus(NominalAttrType.class, ClusAttrType.STATUS_NORMAL);		
	}
	
	private void countAttrTypes() {
		m_NbType = new int[ClusAttrType.NB_STATUS][ClusAttrType.NB_TYPES];
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);				
			m_NbType[at.getStatus()][at.getTypeIndex()]++;
		}
		m_NbStatusType = new int[ClusAttrType.NB_STATUS];		
		for (int i = 0; i < ClusAttrType.NB_STATUS; i++) {
			int[] crtype = m_NbType[i];
			for (int j = 0; j < ClusAttrType.NB_TYPES; j++) {
				m_NbStatusType[i] += crtype[j];			
			}
		} 		
	}
	
	private void addRowsIndex() {
		int[] nbvt = new int[ClusAttrType.NB_VALUE_TYPES];
		for (int i = ClusAttrType.STATUS_TARGET; i <= ClusAttrType.STATUS_KEY; i++) {
			for (int j = 0; j < m_NbAttrs; j++) {
				ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);				
				if (at.getStatus() == i) {
					int vtype = at.getValueType();
					int sidx = nbvt[vtype]++;
					at.setSpecialIndex(sidx);				
				}
			}
		}
		m_NbInts = nbvt[ClusAttrType.VALUE_TYPE_INT];
		m_NbDoubles = nbvt[ClusAttrType.VALUE_TYPE_DOUBLE];
		m_NbObjects = nbvt[ClusAttrType.VALUE_TYPE_OBJECT];
		m_AttrByArrayIdx = new ClusAttrType[ClusAttrType.NB_TYPES][];
		m_AttrByArrayIdx[NominalAttrType.THIS_TYPE] = new ClusAttrType[m_NbInts];  
		m_AttrByArrayIdx[NumericAttrType.THIS_TYPE] = new ClusAttrType[m_NbDoubles];
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttrType at = (ClusAttrType)m_Attr.elementAt(j);
			if (at.getStatus() == ClusAttrType.STATUS_NORMAL || at.getStatus() == ClusAttrType.STATUS_TARGET) {
				m_AttrByArrayIdx[at.getTypeIndex()][at.getSpecialIndex()] = at;
			}
		}
	}
}
