package clus.algo.kNN;

import clus.data.type.ClusAttrType;
import clus.data.type.NominalAttrType;
import clus.data.rows.DataTuple;

/**
 * This class represents the distance between 2 values
 * of a certain Nominal Attribute type.
 */
public class NominalBasicDistance extends BasicDistance {

	public NominalBasicDistance(){
		super();
	}
	/**
	 * Returns the distance for given tuples for given (Nominal) Attribute
	 * Require
	 *		type : must be a NominalAttrType object
	 */
	public double getDistance(ClusAttrType type,DataTuple t1,DataTuple t2){
		NominalAttrType at = (NominalAttrType) type;
		int x = at.getNominal(t1); //returns the attribute value for given attribute in tuple t1
		//Check if missing value
		if (x == at.getNbValues()){
			x = at.getStatistic().mean();
		}
		int y = at.getNominal(t2); //same for t2
		//Check if missing value
		if (y == at.getNbValues()){
			y = at.getStatistic().mean();
		}
		if (x!=y) return 1;
		else return 0;
	}

}