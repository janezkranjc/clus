package clus.ext.ensembles;

import java.io.IOException;
import java.util.Arrays;

import clus.algo.*;
import clus.algo.tdidt.*;
import clus.data.attweights.ClusAttributeWeights;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.*;
import clus.main.*;
import clus.model.*;
import clus.statistic.ClusStatistic;
import clus.util.*;

public class ClusBoostingInduce extends ClusInductionAlgorithm {

	public ClusBoostingInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		super(schema, sett);
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException,	IOException {
		ClusForest result = new ClusForest(getStatManager());
		
		RowData trainData = (RowData)cr.getTrainingSet();
		
		DepthFirstInduce tdidt = new DepthFirstInduce(this);
		ClusNode tree = tdidt.induceSingleUnpruned(trainData);

		ClusAttributeWeights weights = getStatManager().getClusteringWeights();
		double[] distance = new double[trainData.getNbRows()];
		for (int i = 0; i < trainData.getNbRows(); i++) {
			DataTuple tuple = trainData.getTuple(i);
			ClusStatistic prediction = tree.predictWeighted(tuple);
			distance[i] = prediction.getSquaredDistance(tuple, weights);			
		}
		double maxDist = 0;
		for (int i = 0; i < distance.length; i++) {
			if (distance[i] > maxDist) maxDist = distance[i]; 
		}

		
		result.addModelToForest(tree);
		
		return result;
	}
}
