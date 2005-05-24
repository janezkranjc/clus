package clus.selection;

public abstract class ClusSelection {

	protected int m_NbRows;
	
	public ClusSelection(int nbrows) {
		m_NbRows = nbrows;
	}

	public int getNbRows() {
		return m_NbRows;
	}
	
	public boolean supportsReplacement() {
		return false;
	}
	
	public boolean changesDistribution() {
		return false;
	}
	
	public double getWeight(int row) {
		return 1.0;
	}
	
	public int getIndex(int i) {
		return 0;
	}

	public abstract int getNbSelected();
	
	public abstract boolean isSelected(int row);

}
