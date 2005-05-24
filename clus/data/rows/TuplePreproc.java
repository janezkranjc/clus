package clus.data.rows;

import clus.util.ClusException;

public interface TuplePreproc {

	public int getNbPasses();

	public void preproc(int pass, DataTuple tuple) throws ClusException;
	
	public void preprocSingle(DataTuple tuple) throws ClusException;
	
	public void done(int pass);	

}
