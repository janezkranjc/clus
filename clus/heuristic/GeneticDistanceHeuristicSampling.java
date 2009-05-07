package clus.heuristic;

import clus.data.rows.RowData;
import clus.data.rows.DataTuple;
import clus.main.Settings;
import clus.statistic.GeneticDistanceStat;
import clus.statistic.ClusStatistic;
import java.util.*;

public class GeneticDistanceHeuristicSampling extends GeneticDistanceHeuristic {

	protected int m_NbTargets;
	protected int m_SampleSize;

	public void setInitialData(ClusStatistic stat, RowData data) {
		m_OerData = data;
		m_OerData.addIndices();
		GeneticDistanceStat gstat = (GeneticDistanceStat)stat;
		m_NbTargets = gstat.m_NbTarget;
		m_SampleSize = Settings.getPhylogenySampleSize();
		//System.out.println("samplesize: " + m_SampleSize);
	}

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


	// Calculates the total branch lengths, uses sampling (does not use matrix or store results)
	// The test that yields the largest heuristic will be chosen in the end. Since we want to minimize the total branch length,
	// we maximize the inverse of it.
	public double calcHeuristicDist(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		//System.out.print("SamplingHeur ");
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
		double result=0.0;

		int[] posindices = constructIndexVector(m_Data, pstat);
		int[] negindices = constructComplIndexVector(m_Data, posindices);

		double posdist = calculatePairwiseDistanceWithin(tstat, m_OerData, posindices);
		double negdist = calculatePairwiseDistanceWithin(tstat, m_OerData, negindices);
		double betweenpndist = calculatePairwiseDistanceBetween(tstat, m_OerData, posindices, negindices);

		if (m_Data.getNbRows() == m_OerData.getNbRows()) { // root of the tree
			result = betweenpndist + posdist + negdist;
		}
		else {
			betweenpndist = 0.5 * betweenpndist;
			double betweenpcdist = 0.5 * calculatePairwiseDistanceBetween(tstat, m_OerData, posindices, m_ComplDataIndices);
			double betweenncdist = 0.5 * calculatePairwiseDistanceBetween(tstat, m_OerData, negindices, m_ComplDataIndices);

			// for the exact total branch length (disable sampling!):
			//double compdist = calculatePairwiseDistanceWithin(tstat, m_OerData, m_ComplDataIndices);
			//result = compdist + posdist + negdist + betweenpndist + betweenpcdist + betweenncdist;

			// otherwise (without sampling, this gives the double value as the "otherwise" in the matrix version
			result = posdist + negdist + betweenpndist + betweenpcdist + betweenncdist;
		}
		double finalresult = -1.0 * result;
		//System.out.println(finalresult);
		return finalresult;
	}


	// this does not calculate the average pairwise distance within, but [(SUM distances) / nbex]
	// the data tuples that are compared are those from 'data' with index in 'indices'
	public double calculatePairwiseDistanceWithin(GeneticDistanceStat stat, RowData data, int[] indices) {
		int nb_ex = indices.length;
		int nb_pairs = (nb_ex * (nb_ex-1))/2;
		double dist = 0.0;

		if (nb_pairs < m_SampleSize) {
			for (int i=0; i<nb_ex; i++) {
				for (int j=i+1; j<nb_ex; j++) {
					int index1 = indices[i];
					int index2 = indices[j];
					dist += calculateDistance(stat, data, index1, index2);
				}
			}
			dist = dist / nb_ex;
		}
		else {
			Random rnd = new Random();
			for (int i=0; i<m_SampleSize; i++) {
				int rnd1 = rnd.nextInt(nb_ex);
				int rnd2 = rnd1;
				while (rnd2==rnd1) {
					rnd2 = rnd.nextInt(nb_ex);
				}
				int index1 = indices[rnd1];
				int index2 = indices[rnd2];
				double newdist = calculateDistance(stat, data, index1, index2);
				dist += newdist;
			}
			dist = (dist / m_SampleSize) * ((nb_ex-1) / 2);
		}
		return dist;
	}

	// calculates the min/avg/max pairwise distance between data tuples with index in 'indices1' and 'indices2' in 'data'
	public double calculatePairwiseDistanceBetween(GeneticDistanceStat stat, RowData data, int[] indices1, int[] indices2) {
		int n1 = indices1.length;
		int n2 = indices2.length;
		int nb_pairs = n1*n2;

		int index1;
		int index2;

		double dist=0.0;
		Random rnd = new Random();

		switch (Settings.m_PhylogenyLinkage.getValue()) {
			case Settings.PHYLOGENY_LINKAGE_SINGLE:
				// calculate the minimal distance
				dist = Double.MAX_VALUE;
				if (nb_pairs < m_SampleSize) {
					for (int i=0; i<n1; i++) {
						index1 = indices1[i];
						for (int j=0; j<n2; j++) {
							index2 = indices2[j];
							dist = Math.min(dist, calculateDistance(stat, data, index1, index2));
						}
					}
				}
				else {
					for (int i=0; i<m_SampleSize; i++) {
						int rndpos = rnd.nextInt(n1);
						int rndneg = rnd.nextInt(n2);
						index1 = indices1[rndpos];
						index2 = indices2[rndneg];
						dist = Math.min(dist, calculateDistance(stat, data, index1, index2));
					}
				}
				break;

			case Settings.PHYLOGENY_LINKAGE_AVERAGE:
				// calculate the average distance
				dist = 0.0;
				if (nb_pairs < m_SampleSize) {
					for (int i=0; i<n1; i++) {
						index1 = indices1[i];
						for (int j=0; j<n2; j++) {
							index2 = indices2[j];
							dist += calculateDistance(stat, data, index1, index2);
						}
					}
					dist = dist / (nb_pairs);
				}
				else {
					for (int i=0; i<m_SampleSize; i++) {
						int rndpos = rnd.nextInt(n1);
						int rndneg = rnd.nextInt(n2);
						index1 = indices1[rndpos];
						index2 = indices2[rndneg];
						dist += calculateDistance(stat, data, index1, index2);
					}
					dist = dist / m_SampleSize;
				}
				break;

			case Settings.PHYLOGENY_LINKAGE_COMPLETE:
				// calculate the maximal distance
				dist = Double.MIN_VALUE;
				if (nb_pairs < m_SampleSize) {
					for (int i=0; i<n1; i++) {
						index1 = indices1[i];
						for (int j=0; j<n2; j++) {
							index2 = indices2[j];
							dist = Math.max(dist, calculateDistance(stat, data, index1, index2));
						}
					}
				}
				else {
					for (int i=0; i<m_SampleSize; i++) {
						int rndpos = rnd.nextInt(n1);
						int rndneg = rnd.nextInt(n2);
						index1 = indices1[rndpos];
						index2 = indices2[rndneg];
						dist = Math.max(dist, calculateDistance(stat, data, index1, index2));
					}
				}
				break;
		}
		return dist;
	}


	// calculates distance between two tuples of data (given are the indices of the tuples in the data)
	public double calculateDistance(GeneticDistanceStat stat, RowData data, int index1, int index2) {
		// make string1
		DataTuple tuple1 = data.getTuple(index1);
		String[] string1 = new String[m_NbTargets];
		for (int i=0; i<m_NbTargets; i++) {
			int nomvalue1 = stat.m_Attrs[i].getNominal(tuple1);
			string1[i] = stat.m_Attrs[i].getValueOrMissing(nomvalue1);
		}
		// make string2
		DataTuple tuple2 = data.getTuple(index2);
		String[] string2 = new String[m_NbTargets];
		for (int i=0; i<m_NbTargets; i++) {
			int nomvalue2 = stat.m_Attrs[i].getNominal(tuple2);
			string2[i] = stat.m_Attrs[i].getValueOrMissing(nomvalue2);
		}
		return getDistance(string1, string2);
	}


	public String getName() {
		return "GeneticDistanceHeuristicSampling";
	}

}
