package clus.heuristic;

import clus.data.rows.RowData;
import clus.data.rows.DataTuple;
import clus.main.Settings;
import clus.statistic.GeneticDistanceStat;
import clus.statistic.ClusStatistic;
import java.util.*;
import jeans.math.matrix.*;

public class GeneticDistanceHeuristicMatrix extends GeneticDistanceHeuristic {
	
	protected MSymMatrix m_DistMatrix;
	protected double m_SumAllDistances;
	protected HashMap m_HeurComputed = new HashMap();
	protected double[] m_SumDistWithCompl;

	
	public void setInitialData(ClusStatistic stat, RowData data) {
		m_OerData = data;
		m_OerData.addIndices();
		constructMatrix(stat);
	}
	
	public void constructMatrix(ClusStatistic stat) {
		m_DistMatrix = new MSymMatrix(m_OerData.getNbRows());
		GeneticDistanceStat gstat = (GeneticDistanceStat)stat;
		for (int i=0; i<m_OerData.getNbRows(); i++) {
			DataTuple tuple1 = m_OerData.getTuple(i);
			int row = tuple1.getIndex();
			String[] str1 = new String[gstat.m_NbTarget];
			for (int t=0; t<gstat.m_NbTarget; t++) {
				int nomvalue1 = gstat.m_Attrs[t].getNominal(tuple1);
				str1[t] = gstat.m_Attrs[t].getValueOrMissing(nomvalue1);
			}
		
			for (int j=i+1; j<m_OerData.getNbRows(); j++) {
				DataTuple tuple2 = m_OerData.getTuple(j);
				int col = tuple2.getIndex();
				String[] str2 = new String[gstat.m_NbTarget];
				for (int t=0; t<gstat.m_NbTarget; t++) {
					int nomvalue2 = gstat.m_Attrs[t].getNominal(tuple2);
					str2[t] = gstat.m_Attrs[t].getValueOrMissing(nomvalue2);
				}
				double distance = getDistance(str1,str2);
				m_DistMatrix.set_sym(row, col, distance);
			}
		}				
	}
		
	public void setData(RowData data) {
		m_HeurComputed.clear();
		m_Data = data;
		m_DataIndices = constructIndexVector(m_Data);
		m_SumAllDistances = getSumOfDistancesWithin(m_DataIndices);
		m_ComplDataIndices = constructComplIndexVector(m_OerData, m_DataIndices);
		m_SumDistWithCompl = constructComplDistVector(m_DataIndices, m_ComplDataIndices);
	}
		
	public double[] constructComplDistVector(int[] indices, int[] complIndices) {
		int nbindices = indices.length;
		int nbcomplindices = complIndices.length;
		/*System.out.println("indices: ");
		for (int i=0; i<nbindices; i++) {
			System.out.print(indices[i]+" ");
		}
		System.out.println();
		System.out.println("complindices: ");
		for (int i=0; i<nbcomplindices; i++) {
			System.out.print(complIndices[i]+" ");
		}
		System.out.println();*/

		double[] resultvector = new double[nbindices];
		double sumdist;
		for (int i=0; i<nbindices; i++) {
			sumdist=0.0;
			int matrixrow = indices[i];
			for (int j=0; j<nbcomplindices; j++) {
				int matrixcol = complIndices[j];
				sumdist += m_DistMatrix.get(matrixrow,matrixcol);
			}
			resultvector[i] = sumdist;
		}
		return resultvector;
	}
	
	public double getSumOfDistancesWithin(int[] indices) {
		double nb_ex = indices.length;
		double sum = 0.0;
		for (int i=0; i<nb_ex; i++) {
			int row = indices[i];
			for (int j=i+1; j<nb_ex; j++) {
				int col = indices[j];
				sum += m_DistMatrix.get(row,col);
			}
		}	
		return sum;
	}
	
/*	public double getSumOfDistancesBetween(int[] indices1, int[] indices2) {
		double sum = 0.0;
		for (int i=0; i<indices1.length; i++) {
			for (int j=i+1; j<indices2.length; j++) {
				sum += m_DistMatrix.get(i, j);
			}
		}	
		return sum;
	}*/

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {		
		switch (Settings.m_PhylogenyCriterion.getValue()) {
		case Settings.PHYLOGENY_CRITERION_DISTANCES:
			return calcHeuristicDist(c_tstat, c_pstat, missing);
		case Settings.PHYLOGENY_CRITERION_MUTATIONS:
			return calcHeuristicPars(c_tstat, c_pstat, missing);
		}
		return 0.0; // never executed
	}
	
	// The test that yields the largest heuristic will be chosen in the end. Since we want to minimize the total branch length,
	// we maximize the inverse of it.
	public double calcHeuristicPars(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		// first create all needed statistics and data
		GeneticDistanceStat tstat = (GeneticDistanceStat)c_tstat;
		GeneticDistanceStat pstat = (GeneticDistanceStat)c_pstat;
		GeneticDistanceStat nstat = (GeneticDistanceStat)tstat.cloneStat();
		nstat.copy(tstat);
		nstat.subtractFromThis(pstat);
		
		double n_pos = pstat.m_SumWeight;
		double n_neg = nstat.m_SumWeight;
//		System.out.println("nb pos examples: " + n_pos);
//		System.out.println("nb neg examples: " + n_neg);
		
		// Acceptable test?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		
		// If split position missing for some sequence, don't use it in split (probably this approach is not optimal)
		if (Math.round(n_pos) != n_pos || Math.round(n_neg) != n_neg) {
			return Double.NEGATIVE_INFINITY;
		}
	// -------------
		
		//double result = calculatePairwiseDistance(pstat,m_Data,nstat,m_Data);
		//return result;
		//return calculatePrototypeDistance(pstat,nstat);
		
		//double interiordist = calculateMutations(tstat.m_NbTarget,pstat,m_Data,nstat,m_Data);
		//double interiordist = calculatePrototypeDistance(pstat,nstat);
		double interiordist = calculatePairwiseDistance(pstat,m_Data,nstat,m_Data);
		
		/*double posdist;
		double negdist;
		if(n_pos==1)
			posdist = 0;
		else
			posdist = 2 * n_pos * (calculatePairwiseDistanceWithin(pstat,m_Data) / (n_pos-1));
		
		if(n_neg==1)
			negdist = 0;
		else
			negdist = 2 * n_neg * (calculatePairwiseDistanceWithin(nstat,m_Data)/ (n_neg-1));*/
		
		//double maxposdist = calculateTotalDistanceToPrototype(tstat.m_NbTarget, pstat,m_Data);
		//double maxnegdist = calculateTotalDistanceToPrototype(tstat.m_NbTarget, nstat,m_Data);
		//double posdist = calculateStarDistance(pstat,m_Data);
		//double negdist = calculateStarDistance(nstat,m_Data);
		//double minposdist = calculateMutationsWithin(tstat.m_NbTarget,pstat,m_Data);
		//double minnegdist = calculateMutationsWithin(tstat.m_NbTarget,nstat,m_Data);
		//double posdist = (maxposdist + minposdist) / 2;
		//double negdist = (maxnegdist + minnegdist) / 2;
		
		double result = interiordist;
		
		//System.out.println("posdist: " + posdist + " (max: " + maxposdist + ", min: " + minposdist + ") " + " negdist: " + negdist + " (max: " + maxnegdist + ", min: " + minnegdist + ") "+ " interior: " + interiordist + " result: " + result);
		
		//return 0.0 - result;
		return result;
	}
	
	
	// Calculates the total branch lengths, uses distance matrix and stores previously computed results
	// The test that yields the largest heuristic will be chosen in the end. Since we want to minimize the total branch length,
	// we maximize the inverse of it.
	public double calcHeuristicDist(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		// first create all needed statistics and data
		GeneticDistanceStat tstat = (GeneticDistanceStat)c_tstat;
		GeneticDistanceStat pstat = (GeneticDistanceStat)c_pstat;
		GeneticDistanceStat nstat = (GeneticDistanceStat)tstat.cloneStat();
		nstat.copy(tstat);
		nstat.subtractFromThis(pstat);
		
		double n_pos = pstat.m_SumWeight;
		double n_neg = nstat.m_SumWeight;
		
		// Acceptable test?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		
		// If position missing for some sequence, don't use it in split (probably this approach is not optimal)
		if (Math.round(n_pos) != n_pos || Math.round(n_neg) != n_neg) {
			return Double.NEGATIVE_INFINITY;
		}
		//-----------
		
		String key = pstat.getBits().toString();
		Double value = (Double) m_HeurComputed.get(key);
		if (value!=null) {
			return value.doubleValue();
		}
		
		int[] posindices = constructIndexVector(m_Data, pstat);
		int[] negindices = constructComplIndexVector(m_Data, posindices);
		
		double result;
		// root of the tree
		if (m_Data.getNbRows() == m_OerData.getNbRows()) { 
			result = (m_SumAllDistances + (n_neg-1) * getSumOfDistancesWithin(posindices) + (n_pos-1) * getSumOfDistancesWithin(negindices)) / (n_pos*n_neg);		
		}
		
		// other nodes
		else {		
			double sumDistPosToCompl = 0.0;
			double sumDistNegToCompl = 0.0;
			for (int i=0; i<tstat.m_SumWeight; i++) {
				if (pstat.getBits().getBit(i)) {
					sumDistPosToCompl += m_SumDistWithCompl[i];
				}
				else {
					sumDistNegToCompl += m_SumDistWithCompl[i];
				}
			}
			double n_compl = m_ComplDataIndices.length;				
			
			// for the exact total branch length:
			// double compdist = getSumOfDistancesWithin(m_ComplDataIndices); // if you want to compute exact total branch lengths, add this to result
			// result = ((sumDistNegToCompl / (n_neg*n_compl)) + (sumDistPosToCompl / (n_pos*n_compl)) + (m_SumAllDistances / (n_pos*n_neg)) + (getSumOfDistancesWithin(posindices) * (2*n_neg - 1) / (n_pos*n_neg)) + (getSumOfDistancesWithin(negindices) * (2*n_pos - 1) / (n_pos*n_neg)))/2 + (compdist / n_compl);

			// otherwise:
			result = (sumDistNegToCompl / (n_neg*n_compl)) + (sumDistPosToCompl / (n_pos*n_compl)) + (m_SumAllDistances / (n_pos*n_neg)) + (getSumOfDistancesWithin(posindices) * (2*n_neg - 1) / (n_pos*n_neg)) + (getSumOfDistancesWithin(negindices) * (2*n_pos - 1) / (n_pos*n_neg));
		}
		double finalresult = -1.0 * result;
		m_HeurComputed.put(key,new Double(finalresult));
		return finalresult;
	}

	
	public String getName() {
		return "GeneticDistanceHeuristicMatrix";
	}

}
