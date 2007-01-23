/*
 * Created on May 11, 2005
 */
package clus.pruning;

public class SizeConstraintVisitor extends ErrorVisitor {

	public double[] cost;
	public int[] left;
	public boolean[] computed;
	public double error;
	
	public SizeConstraintVisitor(int size) {
		cost = new double[size+1];
		left = new int[size+1];
		computed = new boolean[size+1];
	}
}
