package clus.heuristic;

import clus.data.io.ClusReader;
import clus.data.rows.RowData;
import clus.data.rows.DataTuple;
import clus.ext.sspd.SSPDMatrix;
import clus.main.Settings;
import clus.statistic.GeneticDistanceStat;
import clus.statistic.ClusStatistic;
import jeans.resource.ResourceInfo;

import java.io.IOException;
import java.util.*;

import jeans.math.matrix.*;

public class GeneticDistanceHeuristicMatrix extends GeneticDistanceHeuristic {

	protected MSymMatrix m_DistMatrix;
	protected double m_SumAllDistances;
	protected HashMap m_HeurComputed = new HashMap();
	protected double[] m_SumDistWithCompl;
	protected int m_SampleSize=20;
	protected boolean m_Sampling=false;

	public long m_SetDataTimer=0;
	public long m_HeurTimer=0;

	public void setInitialData(ClusStatistic stat, RowData data) {
		m_OerData = data;
		m_OerData.addIndices();
		constructMatrix(stat);
	}

/*	public MSymMatrix read(String filename, Settings sett) throws IOException {
		ClusReader reader = new ClusReader(filename, sett);
		int nb = (int) reader.readFloat();
		System.out.println("  Loading Distance Matrix: "+filename+" (Size: "+nb+")");
		MSymMatrix matrix = new MSymMatrix(nb);
		for (int i = 0; i < nb; i++) {
			for (int j = 0; j <= i; j++) {
				double value = reader.readFloat();
				matrix.set_sym(i, j, value);
			}
			reader.readTillEol();
			//if (!reader.isEol()) throw new IOException("Distance Matrix is not square");
		}
		reader.close();
		System.out.println("  Matrix loaded");
		return matrix;
	}
*/
	public MSymMatrix read(String filename, Settings sett) throws IOException {
		ClusReader reader = new ClusReader(filename, sett);
		int nb = (int) reader.readFloat();
		System.out.println("  Loading Distance Matrix: "+filename+" (Size: "+nb+")");
		MSymMatrix matrix = new MSymMatrix(nb);
		for (int i = 0; i < nb; i++) {
			reader.readName();
			for (int j = 0; j < nb; j++) {
				double value = reader.readFloat();
//				System.out.println(value);
				if (i<=j) matrix.set_sym(i, j, value);
			}
			reader.readTillEol();
			//if (!reader.isEol()) throw new IOException("Distance Matrix is not square");
		}
		reader.close();
		System.out.println("  Matrix loaded");
		return matrix;
	}



	public void constructMatrix(ClusStatistic stat) {
		try {
			m_DistMatrix = read("dist", m_OerData.getSchema().getSettings());
		}
		catch (IOException e) {
			m_DistMatrix = new MSymMatrix(m_OerData.getNbRows());
			System.out.println("  Calculating Distance Matrix (Size: "+m_OerData.getNbRows()+")");
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
		/*for (int i=0; i<m_OerData.getNbRows(); i++) {
			for (int j=0; j<=i; j++) {
				System.out.print(m_DistMatrix.get(i, j) + "  ");
			}
			System.out.println();
		}*/
	}

	public void setData(RowData data) {
		long start_time = System.currentTimeMillis();

		m_HeurComputed.clear();
		m_Data = data;
		m_DataIndices = constructIndexVector(m_Data);
		m_SumAllDistances = getSumOfDistancesWithin(m_DataIndices);
		m_ComplDataIndices = constructComplIndexVector(m_OerData, m_DataIndices);
		m_SumDistWithCompl = constructComplDistVector(m_DataIndices, m_ComplDataIndices);

		long stop_time = System.currentTimeMillis();
		long elapsed = stop_time - start_time;
		m_SetDataTimer += elapsed;
	}

	public double[] constructComplDistVector(int[] indices, int[] complIndices) {
		int nbindices = indices.length;
		int nbcomplindices = complIndices.length;

//		Random rnd = new Random();
		double[] resultvector = new double[nbindices];
		double sumdist;

		for (int i=0; i<nbindices; i++) {
			sumdist=0.0;
			int matrixrow = indices[i];
//			if (m_Sampling==false || nbcomplindices < 25*m_SampleSize) {
				for (int j=0; j<nbcomplindices; j++) {
					int matrixcol = complIndices[j];
					sumdist += m_DistMatrix.get(matrixrow,matrixcol);
				}
/*			}
			else {
				System.out.print("sampling: " + nbcomplindices + " -> " + m_SampleSize + " ");
//				if (nbcomplindices > m_SampleSize) {
				//System.out.println("nbcomplindices = " + nbcomplindices + "  sample size = " + m_SampleSize);
//					long start_time2 = ResourceInfo.getTime();
					for (int j=0; j<m_SampleSize; j++) {
					int rnd1 = rnd.nextInt(nbcomplindices);
					//System.out.print(rnd1 + " ");
					int matrixcol = complIndices[rnd1];
					sumdist += m_DistMatrix.get(matrixrow,matrixcol);
				}
				//System.out.println();
				sumdist = sumdist * ((double)nbcomplindices/(double)m_SampleSize);
//				long done_time2 = ResourceInfo.getTime();
//				long timeneeded2 = done_time2-start_time2;
				//System.out.println("  sampled: " + timeneeded2);
//				if (timeneeded2<timeneeded) System.out.println("*** full: " + timeneeded + "  sampled: " + timeneeded2 + "  (nbcomplindices = " + nbcomplindices + "  matrixrow = " + matrixrow +  ")");
//				if (timeneeded<timeneeded2) System.out.println("*** sampling takes longer! :  full: " + timeneeded + "  sampled: " + timeneeded2 + "  (nbcomplindices = " + nbcomplindices + "  matrixrow = " + matrixrow +  ")");
			}*/
				//else System.out.println();
			resultvector[i] = sumdist;
		}
//		long done_time = ResourceInfo.getTime();
//		long timeneeded = done_time-start_time;
//		System.out.println("compvector : " + timeneeded);
		return resultvector;
	}

	public double getSumOfDistancesWithin(int[] indices) {
		int nb_ex = indices.length;
		int nb_pairs = (nb_ex * (nb_ex-1))/2;
		double sum = 0.0;
		if (m_Sampling==false || nb_pairs < 25*m_SampleSize) {
//			System.out.print("no sampling: " + nb_pairs + " ");
//			long start_time = ResourceInfo.getTime();

			for (int i=0; i<nb_ex; i++) {
				int row = indices[i];
				for (int j=i+1; j<nb_ex; j++) {
					int col = indices[j];
					sum += m_DistMatrix.get(row,col);
				}
			}
//			long done_time = ResourceInfo.getTime();
//			long timeneeded = done_time-start_time;
		}
		else {

/*			System.out.print("no sampling: " + nb_pairs + " ");
			long start_time = System.nanoTime();
//			for (int k=0; k<100000; k++) {
				sum = 0.0;
				for (int i=0; i<nb_ex; i++) {
					int row = indices[i];
					for (int j=i+1; j<nb_ex; j++) {
						int col = indices[j];
						sum += m_DistMatrix.get(row,col);
					}
				}
//			}
			long done_time = System.nanoTime();
			long timeneeded = done_time-start_time;
			System.out.println("time = " + timeneeded + " nanosec");


			System.out.print("sampling: " + nb_pairs + " -> " + m_SampleSize + " ");
			long start_time2 = System.nanoTime();*/
//			System.out.print("sampling: " + nb_pairs + " -> " + m_SampleSize + " ");
			Random rnd = new Random();
//			for (int k=0; k<100000; k++) {
			sum = 0.0;
			for (int i=0; i<m_SampleSize; i++) {
				int rnd1 = rnd.nextInt(nb_ex);
				int rnd2 = rnd.nextInt(nb_ex);
				while (rnd2==rnd1) {
					rnd2 = rnd.nextInt(nb_ex);
				}
				int row = indices[rnd1];
				int col = indices[rnd2];
				sum += m_DistMatrix.get(row,col);
			}
			sum = sum * ((double)nb_pairs/(double)m_SampleSize);
//			}
//			long done_time2 = System.nanoTime();
//			long timeneeded2 = done_time2-start_time2;
//			System.out.println("time = " + timeneeded2 + " nanosec");
//			if (timeneeded2<timeneeded) System.out.println("*** full: " + timeneeded + "  sampled: " + timeneeded2 + "  nbpairs: " + nb_pairs);
//			if (timeneeded<timeneeded2) System.out.println("*** sampling takes longer! :  full: " + timeneeded + "  sampled: " + timeneeded2 + "  nbpairs: " + nb_pairs);
		}
//		System.out.println(sum);
		return sum;
	}

/*
	public double getSumOfDistancesWithinLinear(int[] indices) {
//		System.out.print("no sampling: " + nb_pairs + " ");
//		long start_time = ResourceInfo.getTime();

		int nb_ex = indices.length;
		double sum1 = 0.0;
		int indi;
		int indiplus1;
		int ind0 = indices[0];
		for (int i=1; i<nb_ex; i++) {
			indi = indices[i];
			sum1 = sum1 + (m_DistMatrix.get(ind0,indi) * i);
		}
		double sum2 = 0.0;
		for (int i=1; i<nb_ex-1; i++) {
			indi = indices[i];
			indiplus1 = indices[i+1];
			sum2 = sum2 + ((m_DistMatrix.get(indi,indiplus1) - m_DistMatrix.get(ind0,indiplus1)) * (nb_ex - i - 1));
		}
//			long done_time = ResourceInfo.getTime();
//			long timeneeded = done_time-start_time;

		double suma = sum1+sum2;
//		System.out.println(suma);

		int[] revindices = new int[indices.length];
		for (int i=0; i<indices.length; i++) {
			revindices[i] = indices[indices.length-1-i];
		}

		double sum3 = 0.0;
		ind0 = revindices[0];
		for (int i=1; i<nb_ex; i++) {
			indi = revindices[i];
			sum3 = sum3 + (m_DistMatrix.get(ind0,indi) * i);
		}
		double sum4 = 0.0;
		for (int i=1; i<nb_ex-1; i++) {
			indi = revindices[i];
			indiplus1 = revindices[i+1];
			sum4 = sum4 + ((m_DistMatrix.get(indi,indiplus1) - m_DistMatrix.get(ind0,indiplus1)) * (nb_ex - i - 1));
		}
//			long done_time = ResourceInfo.getTime();
//			long timeneeded = done_time-start_time;

		double sumb = sum3+sum4;
//		System.out.println(sumb);

		double sum = (suma+sumb)/2.0;
//		System.out.println(sum);
		return sum;
	}
*/


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

		String key = pstat.getBits().toString();
		Double value = (Double) m_HeurComputed.get(key);
		if (value!=null) {
			return value.doubleValue();
		}

		int[] posindices = constructIndexVector(m_Data, pstat);
		int[] negindices = constructComplIndexVector(m_Data, posindices);


/*		double result;
		result = (m_SumAllDistances + (n_neg-1) * getSumOfDistancesWithin(posindices) + (n_pos-1) * getSumOfDistancesWithin(negindices)) / (n_pos*n_neg);

		double finalresult = -1.0 * result;
		m_HeurComputed.put(key,new Double(finalresult));
		return finalresult;*/



		//double result = calculatePairwiseDistance(pstat,m_Data,nstat,m_Data);
		//return result;
		//return calculatePrototypeDistance(pstat,nstat);

		//double interiordist = calculateMutations(tstat.m_NbTarget,pstat,m_Data,nstat,m_Data);
//		double interiordist = calculatePrototypeDistance(pstat,nstat);
//		double interiordist = calculatePairwiseDistance(pstat,m_Data,nstat,m_Data);
//		double interiordist = calculateTotalDistanceBetweenPrototypeMatrices(tstat.m_NbTarget,pstat,nstat);
		double interiordist = calcPWSLDistance(posindices, negindices);
	/*	if (m_Data.getNbRows() != m_OerData.getNbRows()) {
			interiordist = (interiordist + calcPWSLDistance(posindices, m_ComplDataIndices) + calcPWSLDistance(m_ComplDataIndices, negindices))/2;
		}*/
	//	System.out.println("heur = " + interiordist);

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

//		double result = calcTotalDistanceWithSlAsProto(posindices, negindices);
		//System.out.println("posdist: " + posdist + " (max: " + maxposdist + ", min: " + minposdist + ") " + " negdist: " + negdist + " (max: " + maxnegdist + ", min: " + minnegdist + ") "+ " interior: " + interiordist + " result: " + result);

		//return 0.0 - result;
		return result;
	}


	public double calcPWSLDistance(int[] posindices, int[] negindices) {
		// look for 2 ancestors with minimal distance
		double dist = Double.MAX_VALUE;
		int posanc = Integer.MAX_VALUE;
		int neganc = Integer.MAX_VALUE;
		for (int i=0; i<posindices.length; i++) {
			for (int j=0; j<negindices.length; j++) {
				int row = posindices[i];
				int col = negindices[j];
				double distance = m_DistMatrix.get(row, col);
				if (distance < dist) {
					posanc = row;
					neganc = col;
					dist = distance;
				}
			}
		}
		return dist;

	}


	public double calcTotalDistanceWithSlAsProto(int[] posindices, int[] negindices) {
		// look for 2 ancestors with minimal distance
		double dist = Double.MAX_VALUE;
		int posanc = Integer.MAX_VALUE;
		int neganc = Integer.MAX_VALUE;
		for (int i=0; i<posindices.length; i++) {
			for (int j=0; j<negindices.length; j++) {
				int row = posindices[i];
				int col = negindices[j];
				double distance = m_DistMatrix.get(row, col);
				if (distance < dist) {
					posanc = row;
					neganc = col;
					dist = distance;
				}
			}
		}

		double posdist = 0.0;
		for (int i=0; i<posindices.length; i++) {
			int index = posindices[i];
			posdist += m_DistMatrix.get(posanc, index);
		}

		double negdist = 0.0;
		for (int i=0; i<negindices.length; i++) {
			int index = negindices[i];
			negdist += m_DistMatrix.get(neganc, index);
		}

		double result = dist + posdist + negdist;
		return result;

	}


	// Calculates the total branch lengths, uses distance matrix and stores previously computed results
	// The test that yields the largest heuristic will be chosen in the end. Since we want to minimize the total branch length,
	// we maximize the inverse of it.
	public double calcHeuristicDist(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {

		long start_time = System.currentTimeMillis();

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
			long stop_time = System.currentTimeMillis();
			long elapsed = stop_time - start_time;
			m_HeurTimer += elapsed;

			return Double.NEGATIVE_INFINITY;
		}

		// If position missing for some sequence, don't use it in split (probably this approach is not optimal)
		if (Math.round(n_pos) != n_pos || Math.round(n_neg) != n_neg) {
			long stop_time = System.currentTimeMillis();
			long elapsed = stop_time - start_time;
			m_HeurTimer += elapsed;

			return Double.NEGATIVE_INFINITY;
		}
		//-----------

		// we check whether this split has been computed before
		String key = pstat.getBits().toString();
		Double value = (Double) m_HeurComputed.get(key);
		if (value!=null) {
			//System.out.println("found");
			long stop_time = System.currentTimeMillis();
			long elapsed = stop_time - start_time;
			m_HeurTimer += elapsed;

			return value.doubleValue();
		}
		// we also check whether the complement split has been computed before (left subtree <-> right subtree)
		key = nstat.getBits().toString();
		value = (Double) m_HeurComputed.get(key);
		if (value!=null) {
			//System.out.println("found");
			long stop_time = System.currentTimeMillis();
			long elapsed = stop_time - start_time;
			m_HeurTimer += elapsed;

			return value.doubleValue();
		}

//		long start_time = System.currentTimeMillis();

		int[] posindices = constructIndexVector(m_Data, pstat);
		int[] negindices = constructIndexVector(m_Data, nstat);




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

/*		long stop_time = System.nanoTime();
		long elapsed = stop_time - start_time;
		System.out.println("time = " + elapsed + " nanosec");*/

		double finalresult = -1.0 * result;
		m_HeurComputed.put(key,new Double(finalresult));


		long stop_time = System.currentTimeMillis();
		long elapsed = stop_time - start_time;
		m_HeurTimer += elapsed;

		return finalresult;
	}


	public String getName() {
		return "GeneticDistanceHeuristicMatrix";
	}

}
