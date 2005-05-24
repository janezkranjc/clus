package clus.main;

import jeans.util.*;
import jeans.io.*;

import clus.statistic.*;
import clus.data.rows.*;
import clus.util.*;

import java.io.*;
import java.util.*;

public interface ClusModel {

	public ClusStatistic predictWeighted(DataTuple tuple);
	
	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException;
	
	public int getModelSize();
	
	public String getModelInfo();
	
	public void printModel(PrintWriter wrt);
	
	public void saveModel(ObjectSaveStream strm) throws IOException;
	
	public void attachModel(Hashtable table) throws ClusException;
}
