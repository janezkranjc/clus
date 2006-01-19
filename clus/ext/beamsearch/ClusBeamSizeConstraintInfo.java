/*
 * Created on Apr 25, 2005
 */
package clus.ext.beamsearch;

public class ClusBeamSizeConstraintInfo {

	public Object visitor;
	public double[]  realcost;
	public double[]  lowcost;
	public double[]  bound;
	public boolean[] computed;
	boolean marked;
	
	public ClusBeamSizeConstraintInfo(int size) {
		realcost = new double[size+1];
		lowcost = new double[size+1];
		bound = new double[size+1];
		computed = new boolean[size+1];
	} 
}
