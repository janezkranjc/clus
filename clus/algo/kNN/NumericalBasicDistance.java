package clus.algo.kNN;

import clus.data.type.ClusAttrType;
import clus.data.type.NumericAttrType;
import clus.data.rows.DataTuple;
import clus.main.Settings;

/**
 * This class represents the distance between 2 values
 * of a certain Numerical Attribute type.
 */
public class NumericalBasicDistance extends BasicDistance {

	public NumericalBasicDistance(){
	}

	/**
	 * Returns the distance for given tuples for given (Numerical) Attribute
	 * Require
	 *		type : must be a NumericAttrType object

	 */
	public double getDistance(ClusAttrType type,DataTuple t1,DataTuple t2){
		NumericAttrType at = (NumericAttrType) type;
		double x = at.getNumeric(t1); //returns the attribute value for given attribute in tuple t1

		//Check if missing value
		if (x == Double.NaN){
			x = at.getStatistic().mean();
		}
		double y = at.getNumeric(t2); //same for t2
		//Check if missing value
		if (y == Double.NaN){
			y = at.getStatistic().mean();
		}
		//normalize if wanted
		if (Settings.kNN_normalized.getValue()){
			NumericStatistic numStat = at.getStatistic();
			double min = numStat.min();
			double max = numStat.max();
			double dif = max - min;
			x = (x - min)/dif;
			y = (y - min)/dif;
		}
		return Math.abs(x-y);
	}
}