package clus.ext.beamsearch;

import clus.algo.induce.*;
import clus.main.*;
import clus.util.*;
import clus.data.rows.*;
import clus.nominal.split.*;
import clus.error.multiscore.*;

import java.io.*;

import jeans.io.ObjectSaveStream;

public class ClusBeamInduce extends ClusInduce {

    protected NominalSplit m_Split;
    protected ClusBeamSearch m_Search;
 
    public ClusBeamInduce(ClusSchema schema, Settings sett, ClusBeamSearch search) throws ClusException, IOException {
	super(schema, sett);
	m_Search = search;
    }
    
    public void initialize() throws ClusException {
    	super.initialize();
    	m_Search.initialize();
    }

    /*
    public ClusBeamInduce(ClusInduce other, NominalSplit split) {
	super(other);
    }*/

    public ClusData createData() {
	return new RowData(m_Schema);
    }
    
    public boolean isModelWriter() {
	return true;
    }
    
    public void writeModel(ObjectSaveStream strm) throws IOException {
    	m_Search.writeModel(strm);
    }
    
    public ClusNode induce(ClusRun cr, MultiScore score) {
    	try {
    		ClusNode root = m_Search.beamSearch(cr);
    		root.postProc(score);
    		return root;
    	} catch (ClusException e) {
    		System.out.println(e.getMessage());
    		e.printStackTrace();
    	} catch (IOException e) {
    		System.out.println(e.getMessage());
    		e.printStackTrace();
    	}
    	return null;
    }
}
