package clus.heuristic;

import clus.data.rows.RowData;
import clus.data.rows.DataTuple;
import clus.main.Settings;
import clus.statistic.GeneticDistanceStat;
import clus.statistic.ClusStatistic;
import java.util.Random;

public class GeneticDistanceHeuristic extends ClusHeuristic {
	
	protected RowData m_Data;
	protected RowData m_OerData; // the complete data set at the root of the tree, this is needed for taking the complement of the data in this node
	protected RowData m_CompData; // complement of data in this node: m_OerData - m_Data
	protected int counter; // for debugging purposes
	
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
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		counter ++;
//		System.out.println("counter: " + counter);
			
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
		
		// try to simulate neighbour joining by forcing a split with max 2 sequences in one of the subsets
		if (n_pos > 2 && n_neg > 2) {
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
			return 0.0 - result;
		}
		else {
			GeneticDistanceStat compStat = new GeneticDistanceStat(tstat.m_Attrs);
			m_CompData.calcTotalStatBitVector(compStat);
			double betweenpndist = 0.5 * calculatePairwiseDistance(pstat, m_Data, nstat, m_Data);
			double betweenpcdist = 0.5 * calculatePairwiseDistance(pstat, m_Data, compStat, m_CompData);
			double betweenncdist = 0.5 * calculatePairwiseDistance(nstat, m_Data, compStat, m_CompData);
			double compdist = calculatePairwiseDistanceWithin(compStat,m_CompData);
			// compdist not really needed to pick best test, but including it gives right total branch length of phylo tree
			double result = compdist + posdist + negdist + betweenpndist + betweenpcdist + betweenncdist;
			return 0.0 - result;
		}
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
	
	
	public double calculateDistance(int nbtargets, GeneticDistanceStat pstat, RowData pdata, int randompos, GeneticDistanceStat nstat, RowData ndata, int randomneg) {
		int posindex = pstat.getTupleIndex(randompos);
		int negindex = nstat.getTupleIndex(randomneg);
		
		// make pos string
		String[] posstring = new String[nbtargets];
		DataTuple postuple = pdata.getTuple(posindex);
		for (int i=0; i<nbtargets; i++) {
			int posnomvalue = pstat.m_Attrs[i].getNominal(postuple);
			posstring[i] = pstat.m_Attrs[i].getValueOrMissing(posnomvalue);
		}

		// make neg string
		String[] negstring = new String[nbtargets];
		DataTuple negtuple = ndata.getTuple(negindex);
		for (int i=0; i<nbtargets; i++) {
			int negnomvalue = nstat.m_Attrs[i].getNominal(negtuple);
			negstring[i] = nstat.m_Attrs[i].getValueOrMissing(negnomvalue);
		}
		return getDistance(posstring, negstring);
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
		switch (Settings.m_PhylogenyDM.getValue()) {
		case Settings.PHYLOGENY_DISTANCE_MEASURE_PDIST:
			return getPDistance(seq1,seq2);
		case Settings.PHYLOGENY_DISTANCE_MEASURE_JC:
			return getJukesCantorDistance(seq1,seq2);
		case Settings.PHYLOGENY_DISTANCE_MEASURE_KIMURA:
			return getKimuraDistance(seq1,seq2);
		}
		return 0.0; // is never executed
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
//		if (jk_distance == Double.POSITIVE_INFINITY || jk_distance == Double.NEGATIVE_INFINITY) System.out.println("jk_distance: " + jk_distance + " p_distance: " + p_distance + " " + seq1 + " " + seq2);
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
