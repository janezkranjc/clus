package clus.ext.beamsearch;

import java.io.IOException;
import java.util.ArrayList;

import clus.data.rows.*;
import clus.data.type.*;
import clus.main.*;
import clus.model.modelio.ClusTreeReader;
import clus.statistic.ClusStatistic;
import clus.util.ClusException;

public class ClusBeamSyntacticConstraint {

	ClusNode m_Constraint;
	ArrayList m_ConstraintPredictions;
	
	public ClusBeamSyntacticConstraint(ClusRun run) throws ClusException, IOException{
			initializeConstraint(run);
			ClusStatManager mgr = run.getStatManager();
			(mgr.getSchema()).attachModel((ClusModel)m_Constraint);
			createConstrStat(m_Constraint, mgr,(RowData)run.getTrainingSet());
			setConstraintPredictions(getPredictions(run));
//			m_Constraint.printTree();
	}
	
	
	public void initializeConstraint(ClusRun run) throws IOException{
		ClusStatManager csm = run.getStatManager();
		ClusTreeReader rdr = new ClusTreeReader();
		ClusSchema cs = csm.getSchema();
		String bconstrFile = run.getStatManager().getSettings().getBeamConstraintFile();
		m_Constraint = rdr.loadTree(bconstrFile, cs);
		m_Constraint.setClusteringStat(csm.createClusteringStat());
		m_Constraint.setTargetStat(csm.createTargetStat());
	}
		
	public void createConstrStat(ClusNode node, ClusStatManager mgr, RowData data){
		if (node.getTest() == null) node.makeLeaf();
			else{
				for (int j = 0; j < node.getNbChildren(); j++){
					ClusNode child = (ClusNode)node.getChild(j);
					RowData subset = data.applyWeighted(node.getTest(), j);
					child.initClusteringStat(mgr, subset);
					child.initTargetStat(mgr, subset);
					child.getTargetStat().calcMean();
					createConstrStat(child, mgr, subset);
				}
			}	
	}
	
	/**Dragi
	 * we call this method always after the ClusBeamModelDistance is initialized
	 * so we can use the static variable from there
	 * 
	 * @param run
	 * @return predictions
	 */
	public ArrayList getPredictions(ClusRun run){
		ClusStatistic stat;
		DataTuple tuple;
		RowData train = (RowData)run.getTrainingSet();
		ArrayList predictions = new ArrayList();
		double[] singleattr;
		boolean isNum = (run.getStatManager().getMode() == 1);
		for (int k = 0; k < ClusBeamModelDistance.m_NbTarget; k++){
			singleattr = new double[train.getNbRows()];
			for (int i = 0; i < (train.getNbRows()); i++){
				tuple = train.getTuple(i);
				stat = m_Constraint.predictWeighted(tuple);
				if (isNum)	singleattr[i] = stat.getNumericPred()[k];
				else	singleattr[i] = stat.getNominalPred()[k];
			}
		predictions.add(singleattr);	
		}
		return predictions;
	}

	public ArrayList getConstraintPredictions(){
		return m_ConstraintPredictions;
	}
	
	public void setConstraintPredictions(ArrayList predictions){
		m_ConstraintPredictions = predictions;
	}
	
}
