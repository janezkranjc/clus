package clus.heuristic;

import clus.data.rows.RowData;
import clus.data.rows.DataTuple;
import clus.main.Settings;
import clus.statistic.GeneticDistanceStat;
import clus.statistic.ClusStatistic;
import java.util.Random;
import java.util.*;
import jeans.math.matrix.*;
import jeans.list.BitList;

public class GeneticDistanceHeuristic extends ClusHeuristic {
	
	protected RowData m_Data;
	protected RowData m_OerData; // the complete data set at the root of the tree, this is needed for taking the complement of the data in this node
	protected RowData m_CompData; // complement of data in this node: m_OerData - m_Data
	protected int counter; // for debugging purposes
	protected MSymMatrix m_DistMatrix;
	protected boolean m_MatrixFilled;
	protected double m_SumAllDistances;
	protected HashMap m_HeurComputed = new HashMap();
	
	public void setData(RowData data) {
		m_Data = data;
		counter = 0;
		if (m_OerData==null) { // remember complete data set at the root
			m_OerData = m_Data;
		}
		// compute the complement of all data in tstat
		m_CompData = new RowData(m_OerData.getSchema()); 
//		System.out.println("adding complement");
		m_CompData.addComplement(m_OerData, m_Data);
//		System.out.println("added");
		
		m_DistMatrix = new MSymMatrix(m_Data.getNbRows()+1,true);
		m_MatrixFilled = false;
	}
	
	public double getMatrixElement(int row, int col) {
		double el =  m_DistMatrix.get(row,col);
		el = el/2;
		return Math.sqrt(el);
	}
	
	public RowData getRowData(GeneticDistanceStat stat) {
		int nb = (int)stat.m_SumWeight;
		DataTuple[] tup = new DataTuple[nb];
		for (int i=0; i<nb; i++) {
			int index = stat.getTupleIndex(i);
			tup[i] = m_Data.getTuple(index);
		}
		return new RowData(tup,nb);		
	}
	
	// The test that yields the largest heuristic will be chosen in the end. Since we want to minimize the total branch length,
	// we maximize the inverse of it.
	public double calcHeuristicpars(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
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
		
		return calculatePairwiseDistance(pstat,m_Data,nstat,m_Data);
		//return calculatePrototypeDistance(pstat,nstat);
		
		//double interiordist = calculateMutations(tstat.m_NbTarget,pstat,m_Data,nstat,m_Data);
		//double interiordist = calculatePrototypeDistance(pstat,nstat);
		
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
		
		//double result = interiordist - minposdist - minnegdist;
		
		//System.out.println("posdist: " + posdist + " (max: " + maxposdist + ", min: " + minposdist + ") " + " negdist: " + negdist + " (max: " + maxnegdist + ", min: " + minnegdist + ") "+ " interior: " + interiordist + " result: " + result);
		
		//return 0.0 - result;
		//return result;
	}
	
	
	// new (more efficient) code for calcheuristicdist
	// The test that yields the largest heuristic will be chosen in the end. Since we want to minimize the total branch length,
	// we maximize the inverse of it.
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
			
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
		
		if (m_MatrixFilled == false) {
			m_HeurComputed.clear();
		}
		String key = pstat.getBits().toString();
		Double value = (Double) m_HeurComputed.get(key);
		if (value!=null) {
			return value.doubleValue();
		}
		
		double result;
		// root of the tree
		if (m_Data.getNbRows() == m_OerData.getNbRows()) { 
			
			// if first calculation for node, fill distance matrix and at same time calculate total sum of distances
			if (m_MatrixFilled == false) {
				m_SumAllDistances = 0.0;
				for (int i=0; i<m_Data.getNbRows(); i++) {
					DataTuple tuple1 = m_Data.getTuple(i);
					String[] str1 = new String[tstat.m_NbTarget];
					for (int t=0; t<tstat.m_NbTarget; t++) {
						int nomvalue1 = tstat.m_Attrs[t].getNominal(tuple1);
						str1[t] = tstat.m_Attrs[t].getValueOrMissing(nomvalue1);
					}
				
					for (int j=i+1; j<m_Data.getNbRows(); j++) {
						DataTuple tuple2 = m_Data.getTuple(j);
						String[] str2 = new String[tstat.m_NbTarget];
						for (int t=0; t<tstat.m_NbTarget; t++) {
							int nomvalue2 = tstat.m_Attrs[t].getNominal(tuple2);
							str2[t] = tstat.m_Attrs[t].getValueOrMissing(nomvalue2);
						}
						double distance = getDistance(str1,str2);
						m_DistMatrix.set_sym(i, j, distance);
						m_SumAllDistances += distance;
					}
				}
				m_MatrixFilled = true;
			}			
			
			result = (m_SumAllDistances + (n_neg-1) * getSumOfDistancesWithin(pstat) + (n_pos-1) * getSumOfDistancesWithin(nstat)) / (n_pos*n_neg);
		}
		
		// other nodes
		else {		
			GeneticDistanceStat compStat = new GeneticDistanceStat(tstat.m_Attrs);
			m_CompData.calcTotalStatBitVector(compStat);
			double compStar = calculateStarDistance(compStat,m_CompData);
			
			// if first calculation for node, fill distance matrix and at same time calculate total sum of distances
			if (m_MatrixFilled == false) {
				m_SumAllDistances = 0.0;
				for (int i=0; i<m_Data.getNbRows(); i++) {
					DataTuple tuple1 = m_Data.getTuple(i);
					String[] str1 = new String[tstat.m_NbTarget];
					for (int t=0; t<tstat.m_NbTarget; t++) {
						int nomvalue1 = tstat.m_Attrs[t].getNominal(tuple1);
						str1[t] = tstat.m_Attrs[t].getValueOrMissing(nomvalue1);
					}
				
					for (int j=i+1; j<m_Data.getNbRows(); j++) {
						DataTuple tuple2 = m_Data.getTuple(j);
						String[] str2 = new String[tstat.m_NbTarget];
						for (int t=0; t<tstat.m_NbTarget; t++) {
							int nomvalue2 = tstat.m_Attrs[t].getNominal(tuple2);
							str2[t] = tstat.m_Attrs[t].getValueOrMissing(nomvalue2);
						}
						double distance = getDistance(str1,str2);
						m_DistMatrix.set_sym(i, j, distance);
						m_SumAllDistances += distance;
					}
					
					double avgcompdist = 0.0;
					for (int k=0; k<m_CompData.getNbRows(); k++) {
						DataTuple tuplek = m_CompData.getTuple(k);
						String[] strk = new String[tstat.m_NbTarget];
						for (int t=0; t<tstat.m_NbTarget; t++) {
							int nomvaluek = tstat.m_Attrs[t].getNominal(tuplek);
							strk[t] = tstat.m_Attrs[t].getValueOrMissing(nomvaluek);
						}
						avgcompdist += getDistance(str1,strk);
					}
					avgcompdist = (avgcompdist - compStar) / m_CompData.getNbRows();			
					m_DistMatrix.set_sym(i, m_Data.getNbRows(), avgcompdist);				
				}
				m_MatrixFilled = true;
			}			
			result = (getSumOfDistancesToComplement(nstat) / n_neg) + (getSumOfDistancesToComplement(pstat) / n_pos) + (m_SumAllDistances / (n_pos*n_neg)) + 
			(getSumOfDistancesWithin(pstat) * (2*n_neg - 1) / (n_pos*n_neg)) + (getSumOfDistancesWithin(nstat) * (2*n_pos - 1) / (n_pos*n_neg));
		}
		double finalresult = -1.0 * result;
		m_HeurComputed.put(key,new Double(finalresult));
		return finalresult;
	}
	
	//old code
	// The test that yields the largest heuristic will be chosen in the end. Since we want to minimize the total branch length,
	// we maximize the inverse of it.
	public double calcHeuristicdist(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
			
		// first create all needed statistics and data
		GeneticDistanceStat tstat = (GeneticDistanceStat)c_tstat;
		GeneticDistanceStat pstat = (GeneticDistanceStat)c_pstat;
		GeneticDistanceStat nstat = (GeneticDistanceStat)tstat.cloneStat();
		nstat.copy(tstat);
		nstat.subtractFromThis(pstat);
		
		double n_pos = pstat.m_SumWeight;
		double n_neg = nstat.m_SumWeight;
		double n_tot = tstat.m_SumWeight;
		
		// Acceptable test?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		
		// If position missing for some sequence, don't use it in split (probably this approach is not optimal)
		if (Math.round(n_pos) != n_pos || Math.round(n_neg) != n_neg) {
			return Double.NEGATIVE_INFINITY;
		}
		
		double posdist = calculatePairwiseDistanceWithin(pstat,m_Data);
		double negdist = calculatePairwiseDistanceWithin(nstat,m_Data);
			
		if (m_Data.getNbRows() == m_OerData.getNbRows()) { // root of the tree
			double betweendist = calculatePairwiseDistance(pstat, m_Data, nstat, m_Data);		
			double result = betweendist + posdist + negdist;
			
		/*	// only for output purposes from here
			double starpdist = calculateStarDistance(pstat,m_Data);
			double starndist = calculateStarDistance(nstat,m_Data);
			double sumpairdistpnroot = n_pos * n_neg * betweendist;
			double interiorroot = (sumpairdistpnroot - (n_neg * starpdist) - (n_pos * starndist)) / (n_pos * n_neg);
			double resultcheck = interiorroot + starpdist + starndist;
			System.out.println("starp: " + starpdist + " starn: " + starndist + " interior: " + interiorroot + " result: " + result + " check: " + resultcheck);
			*/			
			return 0.0 - result;
		}
		else {
			GeneticDistanceStat compStat = new GeneticDistanceStat(tstat.m_Attrs);
			m_CompData.calcTotalStatBitVector(compStat);
			
			double betweenpndist = 0.5 * calculatePairwiseDistance(pstat, m_Data, nstat, m_Data);
			double betweenpcdist = 0.5 * calculatePairwiseDistance(pstat, m_Data, compStat, m_CompData);
			double betweenncdist = 0.5 * calculatePairwiseDistance(nstat, m_Data, compStat, m_CompData);
			double compdist = calculatePairwiseDistanceWithin(compStat,m_CompData);	
			
//			double tdist = calculatePairwiseDistanceWithin(tstat,m_Data) * n_tot / (2*n_pos*n_neg);	
			
//			 compdist not really needed to pick best test, but including it gives right total branch length of phylo tree
//			double result = compdist + posdist * ((2*n_neg-1) / (2*n_neg)) + negdist * ((2*n_pos-1) / (2*n_pos)) + betweenpcdist + betweenncdist + tdist;
			
			// compdist not really needed to pick best test, but including it gives right total branch length of phylo tree
			double result = compdist + posdist + negdist + betweenpndist + betweenpcdist + betweenncdist;
			
		/*	// only for output purposes from here
			double starpdist = calculateStarDistance(pstat,m_Data);
			double starndist = calculateStarDistance(nstat,m_Data);
			double starcdist = calculateStarDistance(compStat,m_CompData);
			double starsum = starpdist + starndist + starcdist;
			double interior = result - starsum;
			//System.out.println("starp: " + starpdist + " starn: " + starndist + " starc: " + starcdist + " sum: " + starsum + " interior: " + interior + " heur: " + result);
			
			double interiornc = (2*betweenncdist) - (starcdist/compStat.m_SumWeight) - (starndist/n_neg);
			double interiorpc = (2*betweenpcdist) - (starcdist/compStat.m_SumWeight) - (starpdist/n_pos);
			double interiorpn = (2*betweenpndist) - (starndist/n_neg) - (starpdist/n_pos);
			double interiorcm = (interiornc + interiorpc - interiorpn) / 2;
			double interiorpm = (interiorpn + interiorpc - interiornc) / 2;
			double interiornm = (interiornc + interiorpn - interiorpc) / 2;
			//double heurcheck = starpdist + starndist + starcdist + interiorcm + interiorpm + interiornm;
			//System.out.println("heur: " + result + "  heurcheck: " + heurcheck); 
			
			System.out.println("starp: " + starpdist + " starn: " + starndist + " starc: " + starcdist + " interiorpm: " + interiorpm + " interiornm: " + interiornm + " interiorcm: " + interiorcm + " heur: " + result);
				*/	
			return 0.0 - result;
		}
	}

	
	public int getMatrixIndex(GeneticDistanceStat stat, int index) {
		return stat.getTupleIndex(index);
	}
	
	public double getSumOfDistancesWithin(GeneticDistanceStat stat) {
		double nb_ex = stat.m_SumWeight;
		double sum = 0.0;
		for (int i=0; i<nb_ex; i++) {
			int indexi = getMatrixIndex(stat,i);
			for (int j=i+1; j<nb_ex; j++) {
				int indexj = getMatrixIndex(stat,j);
				sum += m_DistMatrix.get(indexi, indexj);
			}
		}	
		return sum;
	}
	
	public double getSumOfDistancesToComplement(GeneticDistanceStat stat) {
		double nb_ex = stat.m_SumWeight;
		int matrixsize = m_DistMatrix.getSize();
		double sum = 0.0;
		for (int i=0; i<nb_ex; i++) {
			int indexi = getMatrixIndex(stat,i);
			sum += m_DistMatrix.get(indexi, matrixsize-1);
		}	
		return sum;
	}
	
	
	public double calculateStarDistance(GeneticDistanceStat stat, RowData data) {
		int nb_tg = stat.m_NbTarget;
		double nb_ex = stat.m_SumWeight;		
		double dist = 0.0;
		for (int i=0; i<nb_ex; i++) {
			for (int j=i+1; j<nb_ex; j++) {
				double newdist = calculateDistance(nb_tg, stat, data, i, stat, data, j);
				dist += newdist;
			}
		}
		if (nb_ex == 1)
			dist = 0.0;
		else dist = dist / (nb_ex-1);
		return dist;
	}
	
	public double calculatePairwiseDistanceWithin(GeneticDistanceStat stat, RowData data) {
		int nb_tg = stat.m_NbTarget;
		double nb_ex = stat.m_SumWeight;		
		double dist = 0.0;
		for (int i=0; i<nb_ex; i++) {
			for (int j=i+1; j<nb_ex; j++) {
				double newdist = calculateDistance(nb_tg, stat, data, i, stat, data, j);
				dist += newdist;
			}
		}
		dist = dist / nb_ex;
		return dist;
	}
	
	public double calculatePairwiseDistance(GeneticDistanceStat pstat, RowData pdata, GeneticDistanceStat nstat, RowData ndata) {
		// Equal for all target attributes
		int nb = pstat.m_NbTarget;
		double n_pos = pstat.m_SumWeight;
		double n_neg = nstat.m_SumWeight;	
	
		double dist=0.0;
		Random rnd = new Random();
		switch (Settings.m_PhylogenyLinkage.getValue()) {
			case Settings.PHYLOGENY_LINKAGE_SINGLE: 
				// maximize the minimal distance
				dist = Double.MAX_VALUE;
				if (n_pos * n_neg < 100) {
					for (int i=0; i<n_pos; i++) {
						for (int j=0; j<n_neg; j++) {
							dist = Math.min(dist, calculateDistance(nb, pstat, pdata, i, nstat, ndata, j));
						}
					}
				}
				else {
					for (int i=0; i<100; i++) {
						int rndpos = rnd.nextInt((int)n_pos);
						int rndneg = rnd.nextInt((int)n_neg);
						dist = Math.min(dist, calculateDistance(nb, pstat, pdata, rndpos, nstat, ndata, rndneg));
					}
				}
				break;
			
			case Settings.PHYLOGENY_LINKAGE_AVERAGE: 
				// maximize the average distance
				dist = 0.0;
				if (n_pos * n_neg < 100) {
					for (int i=0; i<n_pos; i++) {
						for (int j=0; j<n_neg; j++) {
							dist += calculateDistance(nb, pstat, pdata, i, nstat, ndata, j);
						}
					}
					dist = dist / (n_pos * n_neg);
				}
				else {
					for (int i=0; i<100; i++) {
						int rndpos = rnd.nextInt((int)n_pos);
						int rndneg = rnd.nextInt((int)n_neg);
						dist += calculateDistance(nb, pstat, pdata, rndpos, nstat, ndata, rndneg);
					}
					dist = dist / 100.0;
				}
				break;
			
			case Settings.PHYLOGENY_LINKAGE_COMPLETE: 
				// maximize the maximal distance
				dist = Double.MIN_VALUE;
				if (n_pos * n_neg < 100) {
					for (int i=0; i<n_pos; i++) {
						for (int j=0; j<n_neg; j++) {
							dist = Math.max(dist, calculateDistance(nb, pstat, pdata, i, nstat, ndata, j));
						}
					}
				}
				else {
					for (int i=0; i<100; i++) {
						int rndpos = rnd.nextInt((int)n_pos);
						int rndneg = rnd.nextInt((int)n_neg);
						dist = Math.max(dist, calculateDistance(nb, pstat, pdata, rndpos, nstat, ndata, rndneg));
					}
				}
				break;		
		}
			
		return dist;
	}
	
	public int getOriginalIndex(DataTuple tuple) {
		//System.out.println("target tuple: " + tuple.toString());
		String str = tuple.toString();
		for(int i=0; i<m_OerData.getNbRows(); i++) {
			DataTuple oertuple = m_OerData.getTuple(i);
			String oerstr = oertuple.toString();
			//System.out.println("tuple: " + i + " : " + oertuple.toString());
			if (str.equals(oerstr))
				return i;
		}
		System.out.println("*************** original tupleindex not found *****************");
		return -1;
	}
	
	public double calculateMutations(int nbtargets, GeneticDistanceStat pstat, RowData pdata, GeneticDistanceStat nstat, RowData ndata) {
		double nbmutations = 0;
		double n_pos = pstat.m_SumWeight;
		double n_neg = nstat.m_SumWeight;	
		HashMap[] poshash = new HashMap[nbtargets];
		HashMap[] neghash = new HashMap[nbtargets];
		for (int p=0; p<nbtargets; p++) {
			poshash[p] = new HashMap();
			neghash[p] = new HashMap();
		}
		
		for (int i=0; i<n_pos; i++) {
			int posindex = pstat.getTupleIndex(i);
			DataTuple postuple = pdata.getTuple(posindex);
			String ch = new String();
			for (int p=0; p<nbtargets; p++) {
				int posnomvalue = pstat.m_Attrs[p].getNominal(postuple);
				ch = pstat.m_Attrs[p].getValueOrMissing(posnomvalue);
				poshash[p].put(ch, true);
			}
		}
		for (int i=0; i<n_neg; i++) {
			int negindex = nstat.getTupleIndex(i);
			DataTuple negtuple = ndata.getTuple(negindex);
			String ch = new String();
			for (int p=0; p<nbtargets; p++) {
				int negnomvalue = nstat.m_Attrs[p].getNominal(negtuple);
				ch = nstat.m_Attrs[p].getValueOrMissing(negnomvalue);
				neghash[p].put(ch, true);
			}
		}
		
		for (int p=0; p<nbtargets; p++) {
			Set posset = poshash[p].keySet();
			Set negset = neghash[p].keySet();
			//System.out.println("pos " + p + " : posset = " + posset.toString() + " negset = " + negset.toString());
			int nbintersection = intersectionSize(posset,negset);
			// if nbintersection == 0 -> for sure a mutation happened
			// if nbintersection == 1 -> for sure no mutation happened
			// if nbintersection > 1 -> maybe a mutation happened
			if (nbintersection == 0) nbmutations++;
			else if (nbintersection == 2) nbmutations = nbmutations + 0.5;
			else if (nbintersection == 3) nbmutations = nbmutations + 0.6667;
			else if (nbintersection == 4) nbmutations = nbmutations + 0.75;
			else if (nbintersection == 5) nbmutations = nbmutations + 0.8;
		}
		
		return nbmutations;
	}
	
	
	public int calculateMutationsWithin(int nbtargets, GeneticDistanceStat stat, RowData data) {
		int nbmutations = 0;
		double nb_ex = stat.m_SumWeight;
		HashMap[] hash = new HashMap[nbtargets];
		for (int p=0; p<nbtargets; p++) {
			hash[p] = new HashMap();
		}
		
		for (int i=0; i<nb_ex; i++) {
			int index = stat.getTupleIndex(i);
			DataTuple tuple = data.getTuple(index);
			String ch = new String();
			for (int p=0; p<nbtargets; p++) {
				int nomvalue = stat.m_Attrs[p].getNominal(tuple);
				ch = stat.m_Attrs[p].getValueOrMissing(nomvalue);
				hash[p].put(ch, true);
			}
		}
	
		for (int p=0; p<nbtargets; p++) {
			Set set = hash[p].keySet();
			//System.out.println("pos " + p + " : posset = " + posset.toString() + " negset = " + negset.toString());
			int mutationsatposition = set.size()-1;
			nbmutations += mutationsatposition;
		}		
		return nbmutations;
	}
	
	
	public double calculateTotalDistanceToPrototype(int nbtargets, GeneticDistanceStat stat, RowData data) {
		double dist = 0.0;
		double dist2 = 0.0;
		
		double[][] protomatrix = stat.getProbabilityPrediction();
		
		System.out.println("protomatrix");
		for (int j=0; j<nbtargets; j++) {
			for (int k=0; k<5; k++) {
				System.out.print(protomatrix[j][k] + " ");
			}
			System.out.println();
		}
		System.out.println();
		
		
		String[] proto = new String[nbtargets];
		for (int i=0; i<nbtargets; i++) {
			proto[i] = stat.getPredictedClassName(i);
		}
		
		System.out.print("proto: ");
		for (int j=0;j<proto.length; j++)
			System.out.print(proto[j]+" ");
		System.out.println();
		System.out.println();
		
		double nb_ex = stat.m_SumWeight;
		for (int i=0; i<nb_ex; i++) {
			int index = stat.getTupleIndex(i);
			DataTuple tuple = data.getTuple(index);
			String[] ch = new String[nbtargets];
			for (int p=0; p<nbtargets; p++) {
				int nomvalue = stat.m_Attrs[p].getNominal(tuple);
				dist += (1 - protomatrix[p][nomvalue]);
				ch[p] = stat.m_Attrs[p].getValueOrMissing(nomvalue);
			}
			dist2 += getDistance(proto,ch);
			
			System.out.print("seq: ");
			for (int j=0;j<ch.length; j++)
				System.out.print(ch[j]+" ");
			System.out.println();
		}
		System.out.println("dist: " + dist + " dist2: " + dist2);
		return dist2;
	}
	
	public int maxArrayIndex(int[] t) {
	    int maximum = t[0]; 
	    int maxindex = 0;
	    for (int i=1; i<t.length; i++) {
	        if (t[i] > maximum) {
	            maximum = t[i]; 
	            maxindex = i;
	        }
	    }
	    return maxindex;
	}
	
	public boolean emptyStringIntersection(Set set1, Set set2) {
		Object[] arr1 = set1.toArray();
		Object[] arr2 = set2.toArray();
		for (int i=0; i<arr1.length; i++) {
			String s1 = (String)arr1[i];
			for (int j=0; j<arr2.length; j++) {
				String s2 = (String)arr2[j];
				if (s1.equals(s2))
					return false;
			}
		}
		return true;
	}
	
	public int intersectionSize(Set set1, Set set2) {
		int intersectionnb = 0;
		Object[] arr1 = set1.toArray();
		Object[] arr2 = set2.toArray();
		for (int i=0; i<arr1.length; i++) {
			String s1 = (String)arr1[i];
			for (int j=0; j<arr2.length; j++) {
				String s2 = (String)arr2[j];
				if (s1.equals(s2))
					intersectionnb++;
			}
		}
		return intersectionnb;
	}
	
	public double calculateDistance(int nbtargets, GeneticDistanceStat pstat, RowData pdata, int randompos, GeneticDistanceStat nstat, RowData ndata, int randomneg) {
		int posindex = pstat.getTupleIndex(randompos);
		int negindex = nstat.getTupleIndex(randomneg);
		
		// make pos string
		DataTuple postuple = pdata.getTuple(posindex);
		String[] posstring = new String[nbtargets];
		for (int i=0; i<nbtargets; i++) {
			int posnomvalue = pstat.m_Attrs[i].getNominal(postuple);
			posstring[i] = pstat.m_Attrs[i].getValueOrMissing(posnomvalue);
		}

		// make neg string
		DataTuple negtuple = ndata.getTuple(negindex);
		String[] negstring = new String[nbtargets];
		for (int i=0; i<nbtargets; i++) {
			int negnomvalue = nstat.m_Attrs[i].getNominal(negtuple);
			negstring[i] = nstat.m_Attrs[i].getValueOrMissing(negnomvalue);
		}
		return getDistance(posstring, negstring);
		
		/*int row = getOriginalIndex(postuple);
		int col = getOriginalIndex(negtuple);
		return getMatrixElement(row, col);*/
	}
	
	
	public double calculatePrototypeDistance(GeneticDistanceStat pstat, GeneticDistanceStat nstat) {
		// Equal for all target attributes
		int nb = pstat.m_NbTarget;
		double n_pos = pstat.m_SumWeight;
		double n_neg = nstat.m_SumWeight;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT || n_neg < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}

		String[] proto_pos = new String[nb];
		String[] proto_neg = new String[nb];
		for (int i=0; i<nb; i++) {
			proto_pos[i] = pstat.getPredictedClassName(i);
			proto_neg[i] = nstat.getPredictedClassName(i);
		}
		
		return getDistance(proto_pos,proto_neg);		
	}
	
	// Remark: In the distance calculations, string positions with "?" (missing values) or "-" (gaps) are discarded.
	// This is to be consistent with the Phylip program (at least, for Jukes-Cantor; Kimura still gives different output).
	
	
	public double getDistance(String[] seq1, String[] seq2) {
		return getEditDistance(seq1,seq2);	
		/*switch (Settings.m_PhylogenyDM.getValue()) {
		case Settings.PHYLOGENY_DISTANCE_MEASURE_PDIST:
			return getPDistance(seq1,seq2);
		case Settings.PHYLOGENY_DISTANCE_MEASURE_JC:
			return getJukesCantorDistance(seq1,seq2);
		case Settings.PHYLOGENY_DISTANCE_MEASURE_KIMURA:
			return getKimuraDistance(seq1,seq2);
		}
		return 0.0; // is never executed*/
	}
	
	public double getEditDistance(String[] seq1, String[] seq2) {
		double p=0;
		for (int i=0; i<seq1.length; i++) {
			if (!seq1[i].equals(seq2[i])) {
				p++;
			}
		}
		return p;
	}

	public double getPDistance(String[] seq1, String[] seq2) {
		double p=0;
		int nb = 0;
		for (int i=0; i<seq1.length; i++) {
			if (((seq1[i].equals("?") || seq2[i].equals("?")) || seq1[i].equals("-")) || seq2[i].equals("-")) {
			}
			else {
				if (!seq1[i].equals(seq2[i])) {
					p++;
					nb++;
				}
				else {
					nb++;
				}
			}			
		}
		double p_distance = (double)p / (double)nb;
		if (p_distance == Double.POSITIVE_INFINITY) System.out.println("p: " + p + " nb: " + nb + " " + seq1 + " " + seq2);
		if (p_distance == Double.NEGATIVE_INFINITY) System.out.println("p: " + p + " nb: " + nb + " " + seq1 + " " + seq2);
		return p_distance;
	}
	
	// yields same tree as p-distance, but different - more realistic - distances
	public double getJukesCantorDistance(String[] seq1, String[] seq2) {
		double p_distance = getPDistance(seq1, seq2);
		double jk_distance;
		if (p_distance > 0.749) {
			jk_distance = 2.1562; // not defined for >= 0.75
			System.out.println("Warning: infinite distances");
		}
		else jk_distance = -0.75 * Math.log(1.0-((4.0*p_distance)/3.0));
		return jk_distance;
	}
	
	public double getKimuraDistance(String[] seq1, String[] seq2) {
		int nb = 0;
		int ti=0;
		int tv=0;
		for (int i=0; i<seq1.length; i++) {
			if (((seq1[i].equals("?") || seq2[i].equals("?")) || seq1[i].equals("-")) || seq2[i].equals("-")) {
			}
			else {
			nb++;
			if (!seq1[i].equals(seq2[i])) {
				if (seq1[i].equals("A")) {
					if (seq2[i].equals("G")) {
						ti++;
					}
					else tv++;
				} else if (seq1[i].equals("C")) {
					if (seq2[i].equals("T")) {
						ti++;
					}
					else tv++;
				} else if (seq1[i].equals("G")) {
					if (seq2[i].equals("A")) {
						ti++;
					}
					else tv++;
				} else if (seq1[i].equals("T")) {
					if (seq2[i].equals("C")) {
						ti++;
					}
					else tv++;
				}
			}
			}
		}
		double ti_ratio = (double)ti / (double)nb;
		double tv_ratio = (double)tv / (double)nb;

		double term1 = Math.log10(1.0/(1.0-2.0*ti_ratio-tv_ratio));
		double term2 = Math.log10(1.0/(1.0-2.0*tv_ratio));
		double kimura = term1+term2;

		//System.out.println("kimura_distance: " + kimura);
		return kimura;			
	}
	

	public String getName() {
		return "GeneticDistance";
	}

}
