package clus.ext.hierarchical;

public class HierWPenalty implements HierBasicDistance {

	protected double[] m_Weights;

	public HierWPenalty(int depth, double fac) {
		m_Weights = new double[depth];
		for (int i = 0; i < depth; i++) {
			m_Weights[i] = Math.pow(fac, (double)i);
		}
	}
	
	protected final double getWeight(int level) {
		return m_Weights[level-1];
	}
	
	public double getVirtualRootWeight() {
		return 0.0;
	}

	public double calcDistance(ClassTerm t1, ClassTerm t2) {
		double distance = 0.0;
		int d1 = t1.getLevel();
		int d2 = t2.getLevel();
		int com_d = Math.min(d1, d2);
		while (d1 > com_d) {
			distance = getWeight(d1);
			t1 = t1.getCTParent();
			d1--;
		}
		while (d2 > com_d) {
			distance = getWeight(d2);
			t2 = t2.getCTParent();
			d2--;
		}
		while (com_d >= 0) {
			if (t1 != t2) distance = getWeight(com_d);
			t1 = t1.getCTParent();
			t2 = t2.getCTParent();
			com_d--;
		}
		return distance;
	}
}
