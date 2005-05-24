/*
 * Created on May 11, 2005
 */
package clus.pruning;

import clus.error.*;

public class SizeConstraintVisitor {

	public double[] cost;
	int[] left;
	boolean[] computed;
	
	public ClusError testerr;

	public SizeConstraintVisitor(int size) {
		cost = new double[size+1];
		left = new int[size+1];
		computed = new boolean[size+1];
	}	
}
