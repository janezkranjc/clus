package clus.ext.ensembles;

import java.io.IOException;
import java.util.ArrayList;

import clus.data.rows.DataTuple;
import clus.data.rows.TupleIterator;
import clus.ext.hierarchical.WHTDStatistic;
import clus.model.ClusModel;
import clus.statistic.ClusStatistic;
import clus.util.ClusException;

public class ClusEnsembleInduceOptimization {

	static int[] m_HashCodeTuple;
	static double[][] m_AvgPredictions;
	
	public ClusEnsembleInduceOptimization(TupleIterator train, TupleIterator test, int nb_tuples) throws IOException, ClusException{
		m_HashCodeTuple = new int[nb_tuples];
		int count = 0;
		if (train != null){
			train.init();
			DataTuple train_tuple = train.readTuple();
			while (train_tuple != null){
				m_HashCodeTuple[count] = train_tuple.hashCode();
				count++;
				train_tuple = train.readTuple();
			}
		}
		if (test != null){
			test.init();
			DataTuple test_tuple = test.readTuple();
			while (test_tuple != null){
				m_HashCodeTuple[count] = test_tuple.hashCode();
				count++;
				test_tuple = test.readTuple();
			}
		}
	}

	public ClusEnsembleInduceOptimization(TupleIterator train, TupleIterator test) throws IOException, ClusException{
		ArrayList<Integer> tuple_hash = new ArrayList<Integer>();
		if (train != null){
			train.init();
			DataTuple train_tuple = train.readTuple();
			while (train_tuple != null){
				tuple_hash.add(train_tuple.hashCode());
				train_tuple = train.readTuple();
			}
			train.init();
		}
		if (test != null){
			test.init();//restart the iterator
			DataTuple test_tuple = test.readTuple();
			while (test_tuple != null){
				tuple_hash.add(test_tuple.hashCode());
				test_tuple = test.readTuple();
			}
			test.init();//restart the iterator
		}
		int nb_tuples = tuple_hash.size();
		m_HashCodeTuple = new int[nb_tuples];
		for (int k = 0; k < nb_tuples; k++)
			m_HashCodeTuple[k] = tuple_hash.get(k);
	}

	public void initPredictions(ClusStatistic stat){
		m_AvgPredictions = new double[m_HashCodeTuple.length][stat.getNbAttributes()];
	}

	public void initModelPredictionForTuples(ClusModel model, TupleIterator train, TupleIterator test) throws IOException, ClusException{
		if (train != null){
			train.init();
			DataTuple train_tuple = train.readTuple();
			while (train_tuple != null){
				int position = locateTuple(train_tuple);
				WHTDStatistic stat = (WHTDStatistic) model.predictWeighted(train_tuple);
				m_AvgPredictions[position] = stat.getNumericPred();
				train_tuple = train.readTuple();
			}
			train.init();
		}
		if (test != null){
			test.init();
			DataTuple test_tuple = test.readTuple();
			while (test_tuple != null){
				int position = locateTuple(test_tuple);
				WHTDStatistic stat = (WHTDStatistic) model.predictWeighted(test_tuple);
				m_AvgPredictions[position] = stat.getNumericPred();
				test_tuple = test.readTuple();
			}
			test.init();
		}
	}

	public void addModelPredictionForTuples(ClusModel model, TupleIterator train, TupleIterator test, int nb_models) throws IOException, ClusException{
		if (train != null){
			train.init();
			DataTuple train_tuple = train.readTuple();
			while (train_tuple != null){
				int position = locateTuple(train_tuple);
				WHTDStatistic stat = (WHTDStatistic) model.predictWeighted(train_tuple);
				m_AvgPredictions[position] = incrementPredictions(m_AvgPredictions[position], stat.getNumericPred(), nb_models);
				train_tuple = train.readTuple();
			}
			train.init();
		}
		if (test != null){
			test.init();
			DataTuple test_tuple = test.readTuple();
			while (test_tuple != null){
				int position = locateTuple(test_tuple);
				WHTDStatistic stat = (WHTDStatistic) model.predictWeighted(test_tuple);
				m_AvgPredictions[position] = incrementPredictions(m_AvgPredictions[position], stat.getNumericPred(), nb_models);
				test_tuple = test.readTuple();
			}
			test.init();
		}
	}

	
	public static int locateTuple(DataTuple tuple){
		int position = -1;
		boolean found  = false;
		int i = 0;
		//search for the tuple
		while (!found && i < m_HashCodeTuple.length){
			if (m_HashCodeTuple[i] == tuple.hashCode()) {
				position = i;
				found = true;
			}
			i++;
		}
		return position;
	}

	public static double[] incrementPredictions(double[] avg_predictions, double[] predictions, double nb_models){
		//the current averages are stored in the avg_predictions
		int plength = avg_predictions.length;
		double[] result = new double[plength];
		for (int i = 0; i < plength; i++)
			result[i] = avg_predictions[i]+(predictions[i]-avg_predictions[i])/nb_models;
		return result;
	}

	public static double[][] incrementPredictions(double[][] sum_predictions, double[][] predictions){
		//the current sums are stored in sum_predictions
		double[][] result = new double[sum_predictions.length][];
		for (int i = 0; i < sum_predictions.length; i++){
			result[i] = new double[sum_predictions[i].length];
			for (int j = 0; j < sum_predictions[i].length; j++){
				result[i][j] = sum_predictions[i][j] + predictions[i][j];
			}
		}
		return result;
	}
	
	public static int getPredictionLength(int tuple){
		return m_AvgPredictions[tuple].length;
	}
	
	public static double getPredictionValue(int tuple, int attribute){
		return m_AvgPredictions[tuple][attribute];
	}

}
