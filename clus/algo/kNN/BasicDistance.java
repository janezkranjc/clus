package clus.algo.kNN;

import clus.data.type.ClusAttrType;
import clus.data.rows.DataTuple;

/**
 * This class represents the distance between values
 * of a certain attribute type.
 */

public abstract class BasicDistance {

	public BasicDistance(){
	}

	/**
	 * Returns the distance for given tuples for given Attribute
	 */
	public abstract double getDistance(ClusAttrType type,DataTuple t1,DataTuple t2);
}