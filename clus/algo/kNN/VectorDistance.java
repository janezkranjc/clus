package clus.algo.kNN;

import clus.data.type.ClusAttrType;
import clus.data.rows.DataTuple;

/**
 * This class represents distances between DataTuples
 */

public abstract class VectorDistance {

	private ClusAttrType[] $attrs;
	private double[] $weights;

	public VectorDistance(ClusAttrType[] attrs,double[] weights){
		setAttribs(attrs);
		$weights = weights;
	}



	public int amountAttribs(){
		return $attrs.length;
	}
	public void setAttribs(ClusAttrType[] attrs){
		$attrs = attrs;
	}
	/**
	 * Returns index'th attribute.
	 * Require index :  0 <= index < amountAttribs()
	 */

	public ClusAttrType getAttrib(int idx){
		return $attrs[idx];
	}

	public double getWeight(int idx){
		return $weights[idx];
	}

	/**
	 * Returns the distance between the 2 given tuples
	 */
	public abstract double getDistance(DataTuple a,DataTuple b);

	public abstract String toString();
}