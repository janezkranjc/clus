package clus.algo.kNN;

import clus.data.rows.DataTuple;
import clus.data.type.ClusAttrType;

/**
 * This class represents Euclidian distances between DataTuples.
 */

public class EuclidianDistance extends VectorDistance {

	public EuclidianDistance(ClusAttrType[] attrs,double[] weights){
		super(attrs,weights);
	}

	/**
	 * Returns the (Euclidian) distance between the 2 given tuples
	 */
	public double getDistance(DataTuple t1,DataTuple t2){
		double dist = 0;
		double curDist;
		for (int i = 0; i < amountAttribs();i++){
			//calculate distance for current attribute
			curDist = getAttrib(i).getBasicDistance(t1,t2);
			//add to total
			//if(){
				dist += getWeight(i) * Math.pow(curDist,2);
			//}
			//dist += Math.pow(curDist,2);
		}
		return Math.sqrt(dist);
	}

	public String toString(){
		return "Euclidian";
	}
}