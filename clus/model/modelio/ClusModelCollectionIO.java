/*
 * Created on Jun 8, 2005
 */
package clus.model.modelio;

import java.io.*;
import java.util.*;

import jeans.io.*;

import clus.main.*;

public class ClusModelCollectionIO implements Serializable {
	
	public final static long serialVersionUID = 1L;	

	protected ArrayList m_ModelInfos = new ArrayList();

	public int getNbModels() {
		return m_ModelInfos.size();
	}	
	
	public void addModel(ClusModelInfo model) {
		m_ModelInfos.add(model);
	}

	public void insertModel(int idx, ClusModelInfo model) {
		m_ModelInfos.add(null);
		for (int i = m_ModelInfos.size()-1; i >= idx+1; i--) {
			m_ModelInfos.set(i, m_ModelInfos.get(i-1));
		}
		m_ModelInfos.set(idx, model);
	}	
	
	public ClusModelInfo getModelInfo(int index) {
		return (ClusModelInfo)m_ModelInfos.get(index);
	}

	public ClusModel getModel(int index) {
		ClusModelInfo info = (ClusModelInfo)m_ModelInfos.get(index);
		return info.getModel();
	}
	
	public ClusModel getModel(String name) {
		for (int i = 0; i < getNbModels(); i++) {
			ClusModelInfo info = (ClusModelInfo)m_ModelInfos.get(i);
			if (info.getName().equals(name)) return info.getModel();
		}
		return null;
	}
	
	public void save(String filename) throws IOException {
		ObjectSaveStream strm = new ObjectSaveStream(new FileOutputStream(filename));
		strm.writeObject(this);
		strm.close();
	}
	
	public static ClusModelCollectionIO load(String filename) throws IOException, ClassNotFoundException {
		ObjectLoadStream strm = new ObjectLoadStream(new FileInputStream(filename));
		ClusModelCollectionIO result = (ClusModelCollectionIO)strm.readObject();
		strm.close();
		return result;
	}
}
