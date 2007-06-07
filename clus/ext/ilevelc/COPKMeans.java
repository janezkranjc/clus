
package clus.ext.ilevelc;

import java.util.*;

import jeans.util.DisjointSetForest;

import clus.statistic.*;
import clus.util.ClusRandom;
import clus.data.type.*;
import clus.data.attweights.ClusNormalizedAttributeWeights;
import clus.data.rows.*;
import clus.main.*;

public class COPKMeans {

	protected int m_K;
	protected RowData m_Data;
	protected RowData m_OrigData;
	protected ClusStatManager m_Mgr;
	protected ArrayList m_Constraints;
	protected int[][] m_ConstraintsIndex;
	protected COPKMeansCluster[] m_Clusters;
	protected ClusNormalizedAttributeWeights m_Scale;
	
	public COPKMeans(int maxNbClasses, ClusStatManager mgr) {
		m_K = maxNbClasses;
		m_Mgr = mgr;
		m_Scale = (ClusNormalizedAttributeWeights)mgr.getClusteringWeights();
	}
	
	public ClusStatManager getStatManager() {
		return m_Mgr;
	}

	public void createInitialClusters(RowData data, ArrayList constr) {
		m_OrigData = data;
		/* create initial clusters */
		m_Clusters = new COPKMeansCluster[m_K];
		/* perform ML optimization */
		ArrayList points = data.toArray();
		DerivedConstraintsComputer comp = new DerivedConstraintsComputer(points, constr);
		comp.indexPoints();
		DisjointSetForest dsf = comp.createDSFWithMustLinks();
		ArrayList[] comps = comp.assignPointsToComponents(dsf);
		HashSet set = comp.createCannotLinkSet(dsf);
		ClusSchema schema = data.getSchema();
		RowData new_data = new RowData(schema, comps.length);
		NominalAttrType classtype = (NominalAttrType)schema.getAttrType(schema.getNbAttributes()-1);
		ClusStatManager mgr = getStatManager();
		RegressionStat stat = (RegressionStat)mgr.getStatistic(ClusAttrType.ATTR_USE_CLUSTERING);
		for (int i = 0; i < comps.length; i++) {
			ArrayList crcomp = comps[i];
			/* compute average of component */
			RegressionStat avg = (RegressionStat)stat.cloneStat();
			for (int j = 0; j < crcomp.size(); j++) {
				DataTuple elem = (DataTuple)crcomp.get(j);
				avg.updateWeighted(elem, elem.getWeight());
				/* mapping from old to new data! */
				elem.setIndex(i);
			}
			avg.calcMean();
			/* create new tuple, copying avgs as values */
			DataTuple tuple = new DataTuple(schema);
			tuple.setWeight(avg.getTotalWeight());
			for (int j = 0; j < avg.getNbAttributes(); j++) {
				NumericAttrType att = avg.getAttribute(j);
				tuple.setDoubleVal(avg.getMean(j), att.getArrayIndex());
			}
			/* copy class attribute */
			DataTuple elem = (DataTuple)crcomp.get(0);
			tuple.setIntVal(elem.getIntVal(classtype.getArrayIndex()), classtype.getArrayIndex());
			new_data.setTuple(tuple, i);
		}
		new_data.addIndices();
		/* create new cannot-links */
		m_Data = new_data;
		m_Constraints = new ArrayList();
		Iterator edges = set.iterator();
		while (edges.hasNext()) {
			int[] edge = (int[])edges.next();			
			DataTuple tj = new_data.getTuple(edge[0]);
			DataTuple tk = new_data.getTuple(edge[1]);
			m_Constraints.add(new ILevelConstraint(tj, tk, ILevelConstraint.ILevelCCannotLink));
		}
		m_ConstraintsIndex = ILevelCUtil.createConstraintsIndex(new_data.getNbRows(), m_Constraints);
		boolean[] used_tuples = new boolean[new_data.getNbRows()];
		/* create cluster centers */
		int nb_has = 0;
		while (nb_has < m_K) {
			/* find candidate CL constraints */
			ArrayList poss_constr = new ArrayList();
			for (int i = 0; i < m_Constraints.size(); i++) {
				ILevelConstraint ilc = (ILevelConstraint)m_Constraints.get(i);
				int t1i = ilc.getT1().getIndex();
				int t2i = ilc.getT2().getIndex();
				int extra_pts = 0;
				if (!used_tuples[t1i]) extra_pts++;
				if (!used_tuples[t2i]) extra_pts++;
				if (extra_pts > 0 && nb_has + extra_pts <= m_K) {
					poss_constr.add(ilc);
				}
			}
			if (poss_constr.size() > 0) {
				/* select random candidate CL constraint */
				int ci = ClusRandom.nextInt(ClusRandom.RANDOM_ALGO_INTERNAL, poss_constr.size());
				ILevelConstraint ilc = (ILevelConstraint)poss_constr.get(ci);
				int t1i = ilc.getT1().getIndex();
				int t2i = ilc.getT2().getIndex();
				if (!used_tuples[t1i]) {
					used_tuples[t1i] = true;
					m_Clusters[nb_has++] = new COPKMeansCluster(new_data.getTuple(t1i), mgr);
				}
				if (!used_tuples[t2i]) {
					used_tuples[t2i] = true;
					m_Clusters[nb_has++] = new COPKMeansCluster(new_data.getTuple(t2i), mgr);
				}				
			} else {
				ArrayList poss_pts = new ArrayList();
				for (int i = 0; i < used_tuples.length; i++) {
					if (!used_tuples[i]) {
						poss_pts.add(new_data.getTuple(i));
					}
				}
				if (poss_pts.size() > 0) {
					int pi = ClusRandom.nextInt(ClusRandom.RANDOM_ALGO_INTERNAL, poss_pts.size());
					DataTuple sel_pt = (DataTuple)poss_pts.get(pi);
					used_tuples[sel_pt.getIndex()] = true;
					m_Clusters[nb_has++] = new COPKMeansCluster(sel_pt, mgr);
				} else {
					int pi = ClusRandom.nextInt(ClusRandom.RANDOM_ALGO_INTERNAL, used_tuples.length);
					m_Clusters[nb_has++] = new COPKMeansCluster(new_data.getTuple(pi), mgr);
				}
			}			
		}
	}

	public void numberClusters() {
		for (int i = 0; i < m_K; i++) {
			m_Clusters[i].setIndex(i);
		}
	}	
	
	public void clearDataFromClusters() {
		for (int i = 0; i < m_K; i++) {
			m_Clusters[i].clearData();
		}
	}
	
	public void updateClusterCenters() {
		for (int i = 0; i < m_K; i++) {
			m_Clusters[i].updateCenter();
		}		
	}
	
	public double computeVariance() {
		double variance = 0;
		for (int i = 0; i < m_K; i++) {
			variance += m_Clusters[i].getCenter().getSS(m_Scale);
		}		
		return variance;
	}	
	
	public boolean checkConstraints(DataTuple tuple, int clid, int[] assign) {
		int[] cidx = m_ConstraintsIndex[tuple.getIndex()];				
		if (cidx != null) {
			for (int j = 0; j < cidx.length; j++) {
				/* CL constraint with otidx */
				ILevelConstraint cons = (ILevelConstraint)m_Constraints.get(cidx[j]);
				int otidx = cons.getOtherTupleIdx(tuple);
				int oclid = assign[otidx];
				if (clid == oclid) {
					/* cannot link constraint violated ! */
					return false;
				}				
			}
			return true;
		} else {
			return true;
		}		
	} 

	public boolean assignDataToClusters(int[] assign) {
		for (int i = 0; i < m_Data.getNbRows(); i++) {
			int best_cl = -1;
			double min_dist = Double.POSITIVE_INFINITY;
			DataTuple tuple = m_Data.getTuple(i);
			for (int j = 0; j < m_K; j++) {
				boolean ok = checkConstraints(tuple, j, assign);
				if (ok) {
					double dist = m_Clusters[j].computeDistance(tuple);
					if (dist < min_dist) {
						best_cl = j;
						min_dist = dist;
					}
				}
			}
			/* no suitable cluster found */
			if (best_cl == -1) {
				return false;
			} else {
				assign[tuple.getIndex()] = best_cl;
				m_Clusters[best_cl].addData(tuple);
			}
		}
		return true;
	}
	
	public double computeRandIndex(int[] assign) {
		int a = 0;
		int b = 0;
		int nbex = m_OrigData.getNbRows();
		ClusSchema schema = m_OrigData.getSchema(); 
		NominalAttrType classtype = (NominalAttrType)schema.getAttrType(schema.getNbAttributes()-1);
		for (int i = 0; i < nbex; i++) {
			DataTuple ti = m_OrigData.getTuple(i);
			int cia = ti.getIntVal(classtype.getArrayIndex());
			int cib = assign[m_Data.getTuple(ti.getIndex()).getIndex()];
			for (int j = i+1; j < nbex; j++) {
				DataTuple tj = m_OrigData.getTuple(j);
				int cja = tj.getIntVal(classtype.getArrayIndex());
				int cjb = assign[m_Data.getTuple(tj.getIndex()).getIndex()];
				if (cia == cja && cib == cjb) a++;
				if (cia != cja && cib != cjb) b++;
			}
		}
		double rand = 1.0 * (a+b) / (nbex*(nbex-1)/2);
		System.out.println("Rand = "+rand+" (nbex = "+nbex+")");
		return rand;		
	}
		
	public ClusModel induce(RowData data, ArrayList constr) {
		COPKMeansModel model = new COPKMeansModel();
		model.setK(m_K);
		//System.out.println("Creating initial clusters...");
		createInitialClusters(data, constr);
		//System.out.println("Number clusters...");
		numberClusters();
		int[] prev_assign = null;
		int[] assign = new int[m_Data.getNbRows()];		
		for (int k = 0; k < 1000000; k++) {
			clearDataFromClusters();			
			Arrays.fill(assign, -1);
			if (!assignDataToClusters(assign)) {
				/* cluster assignment fails - constraints violated! */
				model.setIllegal(true);
				return model;
			}
			updateClusterCenters();
			/* check */
			if (prev_assign != null && Arrays.equals(assign, prev_assign)) {
				model.setIterations(k+1);
				break;
			}
			/* update assignment */
			if (prev_assign == null) prev_assign = new int[m_Data.getNbRows()];
			System.arraycopy(assign, 0, prev_assign, 0, m_Data.getNbRows());
		}
		clearDataFromClusters();
		model.setClusters(m_Clusters);
		model.setRandIndex(computeRandIndex(assign));
		return model;
	}
}
