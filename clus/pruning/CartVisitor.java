
package clus.pruning;

public class CartVisitor extends ErrorVisitor {

	public double delta_u1;    // number of leaves
	public double delta_u2;    // training set error
	public double lambda;
	public double lambda_min;

	public ErrorVisitor createInstance() {
		return new CartVisitor();
	}
}
