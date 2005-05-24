package clus.ext.hierarchical;

public class HierWeightSPath implements HierBasicDistance {

	protected double[] m_Weights;
	protected double m_RootDelta = 1.0;
    protected double fac;

	public HierWeightSPath(int depth, double fac) {
	    this.fac = fac;
	    m_Weights = new double[depth];
		for (int i = 0; i < depth; i++) {
			m_Weights[i] = Math.pow(fac, (double)i);
		}
	}


    protected final double getFac() {
	return fac;
    }

	protected final double getWeight(int level) {
		return m_Weights[level];
	}

	public double getVirtualRootWeight() {
		return m_RootDelta;
	}

	public double calcDistance(ClassTerm t1, ClassTerm t2) {
		double distance = 0.0;
		int d1 = t1.getDepth();
		int d2 = t2.getDepth();
		int com_d = Math.min(d1, d2);
		while (d1 > com_d) {
			distance += getWeight(d1);
			t1 = t1.getCTParent();
			d1--;
		}
		while (d2 > com_d) {
			distance += getWeight(d2);
			t2 = t2.getCTParent();
			d2--;
		}
		while (t1 != t2) {
			distance += 2.0*getWeight(com_d);
			t1 = t1.getCTParent();
			t2 = t2.getCTParent();
			com_d--;
		}
		return distance;
	}
}
