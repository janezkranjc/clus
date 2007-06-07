package clus.data.rows;

import jeans.util.sort.*;

import java.util.*;
import java.io.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.model.test.*;
import clus.error.*;
import clus.selection.*;
import clus.statistic.*;

public class RowData extends ClusData implements MSortable {

	public int m_Index;
	public ClusSchema m_Schema;	
	public DataTuple[] m_Data;

	public RowData(ClusSchema schema) {
		m_Schema = schema;
	}
	
	public RowData(ClusSchema schema, int size) {
		m_Schema = schema;
		resizeEmpty(size);
	}
	
	public RowData(Object[] data, int size) {
		m_Data = new DataTuple[size];
		System.arraycopy(data, 0, m_Data, 0, size);
		setNbRows(size);
	}
	
	public RowData(ArrayList list, ClusSchema schema) {
		m_Schema = schema;		
		m_Data = new DataTuple[list.size()];
		for (int i = 0; i < list.size(); i++) {
			m_Data[i] = (DataTuple)list.get(i);
		}
		setNbRows(list.size());
	}
	
	public ArrayList toArray() {
		ArrayList array = new ArrayList();
		addTo(array);
		return array;
	}
	
	public void addTo(ArrayList array) {
		for (int i = 0; i < getNbRows(); i++) {
			array.add(getTuple(i));
		}		
	}
		
	public DataTuple createTuple() {
		return new DataTuple(m_Schema);
	}	
	
	public static RowData readData(String fname, ClusSchema schema) throws ClusException, IOException {
		schema.addIndices(ClusSchema.ROWS);
		RowData data = new RowData(schema);
		ClusReader reader = new ClusReader(fname, schema.getSettings());		
		int nbrows = reader.countRows(); 
		data.resize(nbrows);
		schema.setNbRows(nbrows);
		data.createNormalView(schema).readData(reader, schema);
		reader.close();				
		return data;
	}
	
	public ClusData cloneData() {
		RowData res = new RowData(m_Schema, m_NbRows);
		System.arraycopy(m_Data, 0, res.m_Data, 0, m_NbRows);
		return res;
	}
	
	public ClusData deepCloneData() {
		RowData res = new RowData(m_Schema, m_NbRows);
		for (int i = 0; i < m_NbRows; i++) {
			res.setTuple(m_Data[i].deepCloneTuple(), i);
		}
		return res;
	}
	
	public final ClusSchema getSchema() {
		return m_Schema;
	}	
	
	public void setSchema(ClusSchema schema) {
		m_Schema = schema;		
	}	
	
	public void sortSparse(int idx) {
		m_Index = idx;	
		int nbmiss = 0, nbzero = 0, nbother = 0;
		DataTuple[] missing = new DataTuple[m_NbRows];
		DataTuple[] zero = new DataTuple[m_NbRows];
		DataTuple[] other = new DataTuple[m_NbRows];
		for (int i = 0; i < m_NbRows; i++) {
			double data = m_Data[i].getDoubleVal(idx);
			if (data == 0.0) {
				zero[nbzero++] = m_Data[i];
			} else if (data == NumericAttrType.MISSING) {
				missing[nbmiss++] = m_Data[i];
			} else {
				other[nbother++] = m_Data[i];
			}			
		}
		MSorter.quickSort(new MySortableArray(other), 0, nbother);
		int pos = 0;
		for (int i = 0; i < nbmiss; i++) {
			m_Data[pos++] = missing[i];
		}
		for (int i = 0; i < nbother; i++) {
			m_Data[pos++] = other[i];
		}
		for (int i = 0; i < nbzero; i++) {
			m_Data[pos++] = zero[i];
		}		
	}
	
	public void sort(int idx) {
		m_Index = idx;
		MSorter.quickSort(this, 0, m_NbRows);
	}
	
	public double getDouble(int i) {
		return m_Data[i].getDoubleVal(m_Index);
	}	
	
	public boolean compare(int i, int j) {
		return m_Data[i].getDoubleVal(m_Index) < m_Data[j].getDoubleVal(m_Index);
	}

	public void swap(int i, int j) {
		DataTuple temp = m_Data[i];
		m_Data[i] = m_Data[j];
		m_Data[j] = temp;
	}
	
	public DataTuple findTupleByKey(String key_value) {
		ClusAttrType[] key = getSchema().getAllAttrUse(ClusAttrType.ATTR_USE_KEY);
		if (key.length > 0) {
			ClusAttrType key_attr = key[0]; 
			for (int i = 0; i < getNbRows(); i++) {
				DataTuple tuple = getTuple(i);
				if (key_attr.getString(tuple).equals(key_value)) return tuple;
			}
		}	
		return null;
	}	
	
	// Does not change original distribution
	public ClusData selectFrom(ClusSelection sel) {
		int nbsel = sel.getNbSelected();	
		RowData res = new RowData(m_Schema, nbsel);
		if (sel.supportsReplacement()) {
			for (int i = 0; i < nbsel; i++) {
				res.setTuple(m_Data[sel.getIndex(i)], i);
			}
		} else {
			int s_subset = 0;
			for (int i = 0; i < m_NbRows; i++) {
				if (sel.isSelected(i)) res.setTuple(m_Data[i], s_subset++);
			}
		}
		return res;
	}
	
	public ClusData select(ClusSelection sel) {
		int s_data = 0;
		int s_subset = 0;
		DataTuple[] old = m_Data;	
		int nbsel = sel.getNbSelected();	
		m_Data = new DataTuple[m_NbRows - nbsel];
		RowData res = new RowData(m_Schema, nbsel);
		for (int i = 0; i < m_NbRows; i++) {
			if (sel.isSelected(i)) res.setTuple(old[i], s_subset++);
			else setTuple(old[i], s_data++);				
		}
		m_NbRows -= nbsel;
		return res;
	}
	
	public void update(ClusSelection sel) {
		int s_data = 0;
		DataTuple[] old = m_Data;	
		int nbsel = sel.getNbSelected();
		m_Data = new DataTuple[nbsel];		
		for (int i = 0; i < m_NbRows; i++) {
			if (sel.isSelected(i)) {
				DataTuple nt = old[i].multiplyWeight(sel.getWeight(i));
				setTuple(nt, s_data++);
			}
		}
		m_NbRows = nbsel;
	}	
	
	public final double getSumWeights() {
		double sum = 0.0;
		for (int i = 0; i < m_NbRows; i++) {
			sum += m_Data[i].getWeight();
		}
		return sum;
	}
	
	public final boolean containsFold(DataTuple tuple, int[] folds) {
		for (int i = 0; i < folds.length; i++) {
			if (tuple.m_Folds[folds[i]] > 0) return true;
		}
		return false;
	}
	
	public final void optimize2(int[] folds) {
		int nbsel = 0;
		int s_data = 0;		
		for (int i = 0; i < m_NbRows; i++) {
			if (containsFold(m_Data[i], folds)) nbsel++;
		}	
		DataTuple[] old = m_Data;	
		m_Data = new DataTuple[nbsel];		
		for (int i = 0; i < m_NbRows; i++) {
			if (containsFold(old[i], folds)) {
				setTuple(old[i], s_data++);
			}
		}
		m_NbRows = nbsel;
	}		
	
	public void insert(ClusData data, ClusSelection sel) {
		int s_data = 0;
		int s_subset = 0;
		DataTuple[] old = m_Data;		
		RowData other = (RowData)data;
		resizeEmpty(m_NbRows + sel.getNbSelected());
		for (int i = 0; i < m_NbRows; i++) {		
			if (sel.isSelected(i)) setTuple(other.getTuple(s_subset++), i);
			else setTuple(old[s_data++], i);
		}
	}
	
	public final RowData getFoldData(int fold) {
		int idx = 0;	
		int nbsel = 0;	
		// Count examples for fold
		for (int i = 0; i < m_NbRows; i++)
			if (m_Data[i].getIndex() != fold) nbsel++;
		// Select examples
		RowData res = new RowData(m_Schema, nbsel);
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			if (tuple.getIndex() != fold) res.setTuple(tuple, idx++);
		}
		return res;
	}	
	
	// Only used in efficient XVal code
	// Could be a bug: changeWeight -> multiplyWeight
	public final RowData getFoldData2(int fold) {
		int idx = 0;	
		int nbsel = 0;	
		// Count examples for fold
		for (int i = 0; i < m_NbRows; i++)
			if (m_Data[i].m_Folds[fold] != 0) nbsel++;
		// Select examples
		RowData res = new RowData(m_Schema, nbsel);
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			int factor = m_Data[i].m_Folds[fold];
			if (factor != 0) {		
				DataTuple t2 = factor == 1 ? tuple : tuple.changeWeight(tuple.getWeight() * factor);
				res.setTuple(t2, idx++);
			}
		}
		return res;
	}	
	
	public final RowData getOVFoldData(int fold) {
		int idx = 0;	
		int nbsel = 0;	
		// Count examples for fold
		for (int i = 0; i < m_NbRows; i++) {
			int efold = m_Data[i].m_Index;
			if (efold != -1) {
				if (efold != fold) nbsel++;
			} else {
				if (Arrays.binarySearch(m_Data[i].m_Folds, fold) >= 0) nbsel++;
			}
		}
		// Select examples
		RowData res = new RowData(m_Schema, nbsel);
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			int efold = tuple.m_Index;
			if (efold != -1) {
				if (efold != fold) res.setTuple(tuple, idx++);
			} else {
				if (Arrays.binarySearch(m_Data[i].m_Folds, fold) >= 0) res.setTuple(tuple, idx++);
			}
		}
		return res;
	}
	
	public final boolean checkData() {
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			if (tuple.m_Index == -1 && tuple.m_Folds == null) return false;
			if (tuple.m_Index != -1 && tuple.m_Folds != null) return false;			
		}
		return true;
	}		
	
	public final DataTuple getTuple(int i) {
		return m_Data[i];
	}
	
	public final void setTuple(DataTuple tuple, int i) {		
		m_Data[i] = tuple;
	}	

	public final RowData applyWeighted(NodeTest test, int branch) {
		int nb = 0;
		for (int i = 0; i < m_NbRows; i++) {
			int pred = test.predictWeighted(m_Data[i]);
			if (pred == branch || pred == NodeTest.UNKNOWN) nb++;
		}
		int idx = 0;
		RowData res = new RowData(m_Schema, nb);
		double prop = test.getProportion(branch);
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			int pred = test.predictWeighted(tuple);
			if (pred == branch) {
				res.setTuple(tuple, idx++);
			} else if (pred == NodeTest.UNKNOWN) {
				DataTuple ntuple = tuple.multiplyWeight(prop);
				res.setTuple(ntuple, idx++);
			}
		}
		return res;
	}

	public final RowData apply(NodeTest test, int branch) {
		int nb = 0;
		for (int i = 0; i < m_NbRows; i++) {
			int pred = test.predictWeighted(m_Data[i]);
			if (pred == branch) nb++;
		}
		int idx = 0;
		RowData res = new RowData(m_Schema, nb);
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			int pred = test.predictWeighted(tuple);
			if (pred == branch) res.setTuple(tuple, idx++);
		}
		return res;
	}
	
	public final RowData applySoft(SoftTest test, int branch) {
		int nb = 0;
		for (int i = 0; i < m_NbRows; i++) 
			nb += test.softPredictNb(m_Data[i], branch);
		int idx = 0;
		RowData res = new RowData(m_Schema, nb);
		for (int i = 0; i < m_NbRows; i++) 
			idx = test.softPredict(res, m_Data[i], idx, branch);
		return res;
	}
	
	public final RowData applySoft2(SoftTest test, int branch) {
		int nb = 0;
		for (int i = 0; i < m_NbRows; i++) 
			nb += test.softPredictNb2(m_Data[i], branch);
		int idx = 0;
		RowData res = new RowData(m_Schema, nb);
		for (int i = 0; i < m_NbRows; i++) 
			idx = test.softPredict2(res, m_Data[i], idx, branch);
		return res;
	}	
		
	public ClusView createNormalView(ClusSchema schema) throws ClusException {
		ClusView view = new ClusView();
		int nb = schema.getNbAttributes();
		for (int j = 0; j < nb; j++) {
			ClusAttrType at = schema.getAttrType(j);
			int status = at.getStatus();
			if (status == ClusAttrType.STATUS_DISABLED) {
				view.addAttribute(new DummySerializable());
			} else {				
//				boolean target = (status == ClusAttrType.STATUS_TARGET);				
				view.addAttribute(at.createRowSerializable(this));
			}
		}
		return view;
	}
		
	public void resize(int nbrows) {
		m_Data = new DataTuple[nbrows];
		for (int i = 0; i < nbrows; i++) m_Data[i] = new DataTuple(m_Schema);
		m_NbRows = nbrows;
	}
	
	public void resizeEmpty(int nbrows) {
		m_Data = new DataTuple[nbrows];
		m_NbRows = nbrows;
	}
	
	public void showDebug(ClusSchema schema) {
		System.out.println("Data: "+m_NbRows+" Size: "+m_Data.length);	
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = getTuple(i);
			if (tuple == null) { 
				System.out.println("? ");
			} else {
				ClusAttrType at = schema.getAttrType(0);				
				System.out.println(at.getString(tuple));
/*				if (tuple.m_Index == -1) {
					System.out.println(" Folds: "+MyIntArray.print(tuple.m_Folds));
				} else {
					System.out.println(" LO: "+tuple.m_Index);
				}*/
			}
		}
		System.out.println();	
	}	
			
	public void attach(ClusNode node) {
	}
	
	public void calcTotalStatBitVector(ClusStatistic stat) {
		stat.setSDataSize(getNbRows());
		calcTotalStat(stat);
		stat.optimizePreCalc(this);
	}	
	
	public void calcTotalStat(ClusStatistic stat) {
		for (int i = 0; i < m_NbRows; i++) {
			stat.updateWeighted(m_Data[i], i);
		}
	}
	
	public final void calcPosAndMissStat(NodeTest test, int branch, ClusStatistic pos, ClusStatistic miss) {
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			int pred = test.predictWeighted(tuple);
			if (pred == branch) {
				pos.updateWeighted(m_Data[i], i);
			} else if (pred == NodeTest.UNKNOWN) {
				miss.updateWeighted(m_Data[i], i);
			}
		}
	}	
	
	public final boolean isSoft() {
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			if (tuple.m_Index == -1) return true;
		}
		return false;
	}
	
	public final void calcXValTotalStat(ClusStatistic[] tot) {
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			tot[tuple.getIndex()].updateWeighted(tuple, i);
		}
	}
	
	public final void calcXValTotalStat(ClusStatistic[] tot, ClusStatistic[] extra) {		
		for (int i = 0; i < extra.length; i++) extra[i].reset();
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			if (tuple.m_Index != -1) {
				tot[tuple.m_Index].updateWeighted(tuple, i);
			} else {
				int[] folds = tuple.m_Folds;
				for (int j = 0; j < folds.length; j++)
					extra[folds[j]].updateWeighted(tuple, i);
			}
		}
	}	
	
	public void calcError(ClusNode node, ClusErrorParent par) {
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = getTuple(i);
			ClusStatistic stat = node.predictWeighted(tuple);
			par.addExample(tuple, stat);
		}
	}
	
	public void preprocess(int pass, DataPreprocs pps) throws ClusException {
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = m_Data[i];
			pps.preproc(pass, tuple);
		}		
	}	
	
	public final void showTable() {	
		for (int i = 0; i < m_Schema.getNbAttributes(); i++) {
			ClusAttrType type = m_Schema.getAttrType(i);
			if (i != 0) System.out.print(",");
			System.out.print(type.getName());
		}	
		System.out.println();		
		for (int i = 0; i < m_NbRows; i++) {
			DataTuple tuple = getTuple(i);						
			for (int j = 0; j < m_Schema.getNbAttributes(); j++) {
				ClusAttrType type = m_Schema.getAttrType(j);
				if (j != 0) System.out.print(",");
				System.out.print(type.getString(tuple));
			}
			System.out.println();
		}
	}
	
	public double[] getNumeric(int idx) {
		return m_Data[idx].m_Doubles;
	}
	
	public int[] getNominal(int idx) {
		return m_Data[idx].m_Ints;
	}	
	
	public MemoryTupleIterator getIterator() {
		return new MemoryTupleIterator(this);
	}
	
	public void addIndices() {
		for (int i = 0; i < m_NbRows; i++) {
			m_Data[i].setIndex(i);
		}
	}
	
	public class MySortableArray implements MSortable {
		
		DataTuple[] tuples;
		
		public MySortableArray(DataTuple[] data) {
			tuples = data;
		}
		
		public double getDouble(int i) {
			return tuples[i].getDoubleVal(m_Index);		
		}
		
		public void swap(int i, int j) {
			DataTuple obj_i = tuples[i];
			tuples[i] = tuples[j];
			tuples[j] = obj_i;
		}
	}
}
