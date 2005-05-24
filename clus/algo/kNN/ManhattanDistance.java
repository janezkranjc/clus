package clus.algo.kNN;

import clus.data.rows.DataTuple;
import clus.data.type.ClusAttrType;

/**
 * This class represents Manhattan distances between DataTuples.
 */

public class ManhattanDistance extends VectorDistance {

	public ManhattanDistance(ClusAttrType[] attrs,double[] weights){
		super(attrs,weights);
	}

	/**
	 * Returns the (Manhattan) distance between the 2 given tuples
	 */
	public double getDistance(DataTuple t1,DataTuple t2){
		double dist = 0;
		double curDist;

		for (int i = 0; i < amountAttribs();i++){
			//calculate distance for current attribute
			curDist = getAttrib(i).getBasicDistance(t1,t2);
			//add to total
			//if(){
				dist += getWeight(i) * curDist;
			//}
			//dist += curDist;
		}
		return dist;
	}
	public String toString(){
		return "Manhattan";
	}
}