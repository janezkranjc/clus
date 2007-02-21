/*
 * Created on Apr 5, 2005
 */
package clus.ext.beamsearch;

import clus.main.*;

import java.io.Serializable;
import java.util.ArrayList;

public class ClusBeamModel implements Comparable, Serializable {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected transient int m_HashCode = -1;
	protected int m_ParentIndex;
	protected boolean m_Refined, m_Finished;
	protected double m_Value;
	protected ClusModel m_Root;
	protected Object m_Refinement;
	
	protected double m_SimilarityWithBeam; //stores the Similarity to beam
	protected ArrayList m_Predictions; //stores the predictions for each target attribute for each row

	public ClusBeamModel() {
	}
	
	public ClusBeamModel(double value, ClusModel root) {
		m_Value = value;
		m_Root = root;
	}
		
	public void setValue(double value) {
		m_Value = value;
	}
	
	public double getValue() {
		return m_Value;
	}
	
	public ClusModel getModel() {
		return m_Root;
	}
	
	public void setModel(ClusNode root) {
		m_Root = root;
	}
	
	public Object getRefinement() {
		return m_Refinement;
	}
	
	public void setRefinement(Object refinement) {
		m_Refinement = refinement;
	}
	
	public String toString() {
		return "" + m_Value;
	}
	
	public final boolean isRefined() {
		return m_Refined;
	}
	
	public final void setRefined(boolean ref) {
		m_Refined = ref;
	}
	
	public final boolean isFinished() {
		return m_Finished;
	}
	
	public final void setFinished(boolean finish) {
		m_Finished = finish;
	}
	
	public void setParentModelIndex(int parent) {
		m_ParentIndex = parent;
	}
	
	public int getParentModelIndex() {
		return m_ParentIndex;
	}
	
	public int compareTo(Object e2) {
		ClusBeamModel m2 = (ClusBeamModel)e2;
		if (m2.m_Value != m_Value) {
			return m2.m_Value < m_Value ? -1 : 1;
		} else {
			return 0;
		}		
	}
	
	public int hashCode() {
		if (m_HashCode == -1) {
			m_HashCode = m_Root.hashCode();
			if (m_HashCode == -1) m_HashCode = 0;
		}
		return m_HashCode;
	}
	
	public boolean equals(Object other) {
		ClusBeamModel o = (ClusBeamModel)other;
		if (hashCode() != o.hashCode()) {
			return false;
		} else {
			return m_Root.equals(o.m_Root);
		}
	}
	
	public ClusBeamModel cloneNoModel() {
		ClusBeamModel res = new ClusBeamModel();
		res.m_ParentIndex = m_ParentIndex;
		return res;
	}
	
	public ClusBeamModel cloneModel() {
		ClusBeamModel res = new ClusBeamModel();
		res.m_ParentIndex = m_ParentIndex;
		res.m_Root = m_Root;
		return res;
	}	
	
	public void setSimilarityWithBeam(double similarity){
		m_SimilarityWithBeam = similarity;
	}
	
	public double getSimilarityWithBeam(){
		return m_SimilarityWithBeam;
	}
	
	public void setModelPredictions(ArrayList predictions){
		m_Predictions = predictions;
	}
	
	public ArrayList getModelPredictions(){
		return m_Predictions;
	}
}
