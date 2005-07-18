package clus.data.cols;

import java.util.*;

import clus.io.*;
import clus.main.*;
import clus.data.type.*;
import clus.util.*;
import clus.data.rows.*;
import clus.error.*;
import clus.selection.*;
import clus.statistic.*;
import clus.data.cols.attribute.*;

public class ColData extends ClusData {

	protected ColTarget m_Target;
	protected int m_NbAttrs;
	protected Vector m_Attr = new Vector();

	public ClusData select(ClusSelection sel) {
		ColData res = new ColData();
		int nbsel = sel.getNbSelected();
		for (int i = 0; i < m_NbAttrs; i++) {
			ClusAttribute attr = getAttribute(i);
			res.addAttribute(attr.select(sel, nbsel));
		}
		res.setTarget(m_Target.select(sel, nbsel));
		res.setNbRows(nbsel);		
		setNbRows(m_NbRows - nbsel);
		return res;
	}
	
	public ClusData cloneData() {
		return null;
	}
	
	public void insert(ClusData data, ClusSelection sel) {
		ColData other = (ColData)data;
		int nb_new = m_NbRows + sel.getNbSelected();	
		for (int i = 0; i < m_NbAttrs; i++) {
			ClusAttribute attr = other.getAttribute(i);			
			getAttribute(i).insert(attr, sel, nb_new);
		}
		m_Target.insert(other.getColTarget(), sel, nb_new);
		setNbRows(nb_new);
	}

	public ClusRun partition(ClusSummary summary) {
		return new ClusRun(this, summary);
	}
	
	public void unpartition(ClusRun cr) {
/*		ClusData tset = cr.getTestSet();
		if (tset != null) {
			ClusSelection sel = cr.getTestSelection();
			insert(tset, sel);
		}*/
	}
	
	public int getNbAttributes() {
		return m_NbAttrs;
	}
	
	public void setTarget(ColTarget target) {
		m_Target = target;
	}
	
	public ColTarget getColTarget() {
		return m_Target;
	}
	
	public ClusAttribute getAttribute(int idx) {
		return (ClusAttribute)m_Attr.elementAt(idx);
	}

	public void addAttribute(ClusAttribute attr) {
		m_Attr.addElement(attr);
		m_NbAttrs++;
	}
	
	public void resetSplitAttrs() {
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttribute attr = (ClusAttribute)m_Attr.elementAt(j);
			attr.setSplit(false);
		}	
	}

	public void resize(int nbrows) {
		m_NbRows = nbrows;
		m_Target.resize(nbrows);
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttribute attr = (ClusAttribute)m_Attr.elementAt(j);
			attr.resize(nbrows);
		}
	}	

	public void prepareAttributes() {
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttribute attr = (ClusAttribute)m_Attr.elementAt(j);
			attr.prepare();
		}
	}
	
	public void unPrepareAttributes() {
		for (int j = 0; j < m_NbAttrs; j++) {
			ClusAttribute attr = (ClusAttribute)m_Attr.elementAt(j);
			attr.unprepare();
		}
	}	
	
	public ClusView createNormalView(ClusSchema schema) throws ClusException {
		int my_idx = 0;
		ClusView view = new ClusView();
		int nb = schema.getNbAttributes();
		for (int j = 0; j < nb; j++) {
			ClusAttrType at = schema.getAttrType(j);
			switch (at.getStatus()) {
				case ClusAttrType.STATUS_DISABLED:
					view.addAttribute(new DummySerializable());
					break;
				case ClusAttrType.STATUS_TARGET:
					view.addAttribute(at.createTargetAttr(m_Target));
					break;
				default:
					view.addAttribute(getAttribute(my_idx++));
			}
		}
		return view;
	}	

	public void attach(ClusNode node) {
/*		ClusAttrType tpe = node.getBestTest().getType();
		node.m_SplitAttr = getAttribute(tpe.getSpecialIndex());*/			
	}	
	
	public void calcError(ClusNode node, ClusErrorParent par) {
/*		node.attachData(this);
		for (int i = 0; i < m_NbRows; i++) {
			ClusNode pred = node.predict(i);
			ClusStatistic stat = pred.getTotalStat();
			par.addExample(this, i, stat);
		}
		node.detachData();	*/
	}	
	
	public void calcTotalStat(ClusStatistic stat) {
		m_Target.calcTotalStat(stat);
	}
	
	public double[] getNumeric(int idx) {
		return m_Target.m_Numeric[idx];
	}
	
	public int[] getNominal(int idx) {
		return null; // m_Target.m_Nominal[idx];	
	}		
	
	public void preprocess(int pass, DataPreprocs pps) {
	}	
}
