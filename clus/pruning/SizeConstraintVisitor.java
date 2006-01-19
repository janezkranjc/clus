/*
 * Created on May 11, 2005
 */
package clus.pruning;

public class SizeConstraintVisitor extends ErrorVisitor {

	public double[] cost;
	int[] left;
	boolean[] computed;
	
	public SizeConstraintVisitor(int size) {
		cost = new double[size+1];
		left = new int[size+1];
		computed = new boolean[size+1];
	}
}
