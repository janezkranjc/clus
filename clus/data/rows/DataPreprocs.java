package clus.data.rows;

import clus.util.ClusException;
import jeans.util.*;

public class DataPreprocs {

	protected MyArray m_Preprocs = new MyArray();

	public void addPreproc(TuplePreproc pp) {
		m_Preprocs.addElement(pp);	
	}
	
	public int getNbPasses() {
		int passes = 0;
		int nb = m_Preprocs.size();
		for (int i = 0; i < nb; i++) {
			TuplePreproc pp = (TuplePreproc)m_Preprocs.elementAt(i);
			passes = Math.max(passes, pp.getNbPasses());
		}
		return passes;
	}

	public void preproc(int pass, DataTuple tuple) throws ClusException {
		int nb = m_Preprocs.size();
		for (int i = 0; i < nb; i++) {
			TuplePreproc pp = (TuplePreproc)m_Preprocs.elementAt(i);
			if (pass < pp.getNbPasses()) pp.preproc(pass, tuple);
		}
	}
	
	public void preprocSingle(DataTuple tuple) throws ClusException {
		int nb = m_Preprocs.size();
		for (int i = 0; i < nb; i++) {
			TuplePreproc pp = (TuplePreproc)m_Preprocs.elementAt(i);
			pp.preprocSingle(tuple);
		}
	}	
	
	public void done(int pass) throws ClusException {
		int nb = m_Preprocs.size();
		for (int i = 0; i < nb; i++) {
			TuplePreproc pp = (TuplePreproc)m_Preprocs.elementAt(i);
			if (pass < pp.getNbPasses()) pp.done(pass);
		}
	}	
}
