package clus.heuristic;

import clus.main.Settings;
import clus.statistic.ClassificationStat;
import clus.statistic.ClusStatistic;

public class GeneticDistanceHeuristic extends ClusHeuristic {

	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		ClassificationStat tstat = (ClassificationStat)c_tstat;
		ClassificationStat pstat = (ClassificationStat)c_pstat;
		ClassificationStat nstat = (ClassificationStat)tstat.cloneStat();
		nstat.copy(tstat);
		nstat.subtractFromThis(pstat);
		// Equal for all target attributes
		int nb = tstat.m_NbTarget;
		double n_tot = tstat.m_SumWeight;
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
		System.out.println("proto pos");
		for (int i=0; i<nb; i++) {
			System.out.print(proto_pos[i]);
		}	
		System.out.println();
		System.out.println("proto neg");
		for (int i=0; i<nb; i++) {
			System.out.print(proto_neg[i]);
		}	
		System.out.println();
		
		int p=0;
		for (int i=0; i<nb; i++) {
			if (!proto_pos[i].equals(proto_neg[i])) {
				p++;
			}
		}
		double p_distance = (double)p / (double)nb;
		System.out.println("p_distance: " + p_distance);
		//double genetic_distance = 

		return p_distance;
		
		
		/*
		int ti=0;
		int tv=0;
		for (int i=0; i<nb; i++) {
			if (!proto_pos[i].equals(proto_neg[i])) {
				if (proto_pos[i].equals("A")) {
					if (proto_neg[i].equals("G")) {
						ti++;
					}
					else tv++;
				} else if (proto_pos[i].equals("C")) {
					if (proto_neg[i].equals("T")) {
						ti++;
					}
					else tv++;
				} else if (proto_pos[i].equals("G")) {
					if (proto_neg[i].equals("A")) {
						ti++;
					}
					else tv++;
				} else if (proto_pos[i].equals("T")) {
					if (proto_neg[i].equals("C")) {
						ti++;
					}
					else tv++;
				} 
			}
		}
		double ti_ratio = (double)ti / (double)nb;
		double tv_ratio = (double)tv / (double)nb;
		
		double term1 = Math.log10(1.0/(1.0-2.0*ti_ratio-tv_ratio));
		double term2 = Math.log10(1.0/(1.0-2.0*tv_ratio));
		double kimura = term1+term2;
		
		System.out.println("kimura_distance: " + kimura);

		return kimura;*/
		
	}
	
	public String getName() {
		return "GeneticDistance";
	}

}
