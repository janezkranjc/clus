package clus.ext.hierarchical;

public interface HierBasicDistance {

	public double getVirtualRootWeight();

	public double calcDistance(ClassTerm a, ClassTerm b);	

}
