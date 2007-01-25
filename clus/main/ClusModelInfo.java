package clus.main;

import clus.error.*;
import clus.model.processor.*;
import clus.util.*;

import java.io.*;

public class ClusModelInfo implements Serializable {
	
	public final static long serialVersionUID = 1L;
	
	public final static int TRAIN_ERR = 0;
	public final static int TEST_ERR = 1;		
	public final static int VALID_ERR = 2;
	
	protected String m_Name;
	protected boolean m_HasName = false;
	protected boolean m_ShouldSave = true;
	protected boolean m_ShouldPruneInvalid;
	protected int m_ModelSize, m_NbModels;
	protected double m_Score;
	protected ClusModel m_Model;
	protected ClusErrorParent m_TrainErr, m_TestErr, m_ValidErr, m_ExtraErr;
	protected ClusStatManager m_Manager;
	protected transient ModelProcessorCollection m_TrainModelProc, m_TestModelProc;

	public ClusModelInfo(String name) {
		m_Name = name;
		m_HasName = false;
	}
	
	public void setAllErrorsClone(ClusErrorParent train, ClusErrorParent test, ClusErrorParent valid) {
		m_TrainErr = null; m_TestErr = null; m_ValidErr = null;
		if (train != null) m_TrainErr = train.getErrorClone();
		if (test != null) m_TestErr = test.getErrorClone();
		if (valid != null) m_ValidErr = valid.getErrorClone();	
	}
	
	public final String getName() {
		return m_Name;
	}
	
	public final ClusModel getModel() {
		return m_Model;
	}	
	
	public final double getScore() {
		return m_Score;
	}
	
	public ClusStatManager getStatManager() {
		return m_Manager;
	}

	public ClusSchema getSchema() {
		return m_Manager.getSchema();
	}

	public Settings getSettings() {
		return m_Manager.getSettings();
	}
	
	public final ClusErrorParent getTrainingError() {
		return m_TrainErr;
	}
	
	public final ClusErrorParent getTestError() {
		return m_TestErr;
	}
	
	public final ClusErrorParent getValidationError() {
		return m_ValidErr;
	}	
		
	public void setStatManager(ClusStatManager mgr) {
		m_Manager = mgr;
	}
	
	public final void setScore(double score) {
		m_Score = score;
	}
	
	public void check() {
		System.out.println("MI = "+m_TestErr);
		System.exit(1);
	}
	
	public void clearAll() {
		m_TrainModelProc = null;
		m_TestModelProc = null;
	}
	
	public final void addModelProcessor(int type, ClusModelProcessor proc) {
		if (type == TRAIN_ERR) {
			if (m_TrainModelProc == null) m_TrainModelProc = new ModelProcessorCollection();
			m_TrainModelProc.addElement(proc);
		} else if (type == TEST_ERR) {
			if (m_TestModelProc == null) m_TestModelProc = new ModelProcessorCollection();
			m_TestModelProc.addElement(proc);
		}
	}
	
	public final ModelProcessorCollection getModelProcessors(int type) {
		if (type == TRAIN_ERR) return m_TrainModelProc;
		else if (type == TEST_ERR) return m_TestModelProc;
		else return null;
	}
	
	public final void initModelProcessors(int type, ClusSchema schema) throws IOException, ClusException {
		ModelProcessorCollection coll = getModelProcessors(type);
		if (coll != null) coll.initialize(m_Model, schema);
	}
	
	public final void termModelProcessors(int type) throws IOException {
		ModelProcessorCollection coll = getModelProcessors(type);
		if (coll != null) coll.terminate(m_Model);
	}	
				
	public final ClusModelInfo cloneModelInfo() {
		ClusModelInfo clone = new ClusModelInfo(m_Name);
		clone.setAllErrorsClone(m_TrainErr, m_TestErr, m_ValidErr);
		clone.setShouldSave(m_ShouldSave);
		clone.setPruneInvalid(m_ShouldPruneInvalid);
		return clone;
	}
		
	public final void setModel(ClusModel model) {
		m_Model = model;
	}
	
	public final void setTestError(ClusErrorParent err) {
		m_TestErr = err;
	}
	
	public final void setTrainError(ClusErrorParent err) {
		m_TrainErr = err;
	}
	
	public final void setValidationError(ClusErrorParent err) {
		m_ValidErr = err;
	}		

	public void setExtraError(int type, ClusErrorParent parent) {
		m_ExtraErr = parent;
	}
	
	public boolean hasExtraError(int type) {
		return m_ExtraErr != null;
	}
	
	public ClusErrorParent getExtraError(int type) {
		return m_ExtraErr;
	}
	
	public final void setName(String name) {
		m_Name = name;
		m_HasName = true;
	}
	
	public final boolean hasName() {
		return m_HasName;
	}
	
	public final void setShouldSave(boolean save) {
		m_ShouldSave = save;
	}
	
	public final boolean shouldSave() {
		return m_ShouldSave;
	}
	
	public final void setPruneInvalid(boolean prune) {
		m_ShouldPruneInvalid = prune;
	}
	
	public final boolean shouldPruneInvalid() {
		return m_ShouldPruneInvalid;
	}	
	
	public final ClusErrorParent getError(int traintest) {
		if (traintest == TRAIN_ERR) return m_TrainErr;
		else if (traintest == VALID_ERR) return m_ValidErr; 
		else if (traintest == TEST_ERR) return m_TestErr;
		else return null;
	}
	
	public final ClusErrorParent getCreateTestError() {
		if (m_TestErr == null) m_TestErr = m_TrainErr.getErrorClone();
		return m_TestErr;
	}	
	
	public final boolean hasTestError() {
		return m_TestErr != null;
	}
	
	public final boolean hasTrainError() {
		return m_TrainErr != null;
	}
	
	public final boolean hasValidError() {
		return m_ValidErr != null;
	}		
	
	public final String getModelInfo() {
		if (m_Model == null) return "No model available";
		else return m_Model.getModelInfo();
	}
	
	public final int getModelSize() {
		if (m_Model == null) return m_ModelSize;
		else return m_Model.getModelSize();
	}
	
	public final int getNbModels() {
		if (m_Model == null) return m_NbModels;
		else return 1;
	}
	
	public final boolean hasModel() {
		return getNbModels() > 0;
	}
	
	public final void updateName(ClusModelInfo other) throws ClusException {
		if (hasName()) {
			if (other.hasName() && !getName().equals(other.getName())) {
				throw new ClusException("Combining error measures of different models: "+getName()+" <> "+other.getName());
			}
		} else {
			if (other.hasName()) setName(other.getName());
		}
	}
	
	public final void add(ClusModelInfo other) throws ClusException {
		updateName(other);
		m_ModelSize += other.getModelSize();
		m_NbModels += other.getNbModels();
		if (other.hasTrainError()) {
			m_TrainErr.add(other.getTrainingError());
		}
		if (other.hasValidError()) {
			m_ValidErr.add(other.getValidationError());
		}		
		if (other.hasTestError()) {
			ClusErrorParent mytesterr = getCreateTestError();
			mytesterr.add(other.getTestError());		
		}		
	}
		
	public String toString() {
		return "ModelInfo '"+getName()+"' Size: "+getModelSize();
	}
}
