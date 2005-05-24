package clus.selection;

public abstract class XValMainSelection {

	protected int m_NbFolds;
	protected int m_NbRows;
	
	public XValMainSelection(int nbfolds, int nbrows) {
		m_NbFolds = nbfolds;
		m_NbRows = nbrows;
	}

	public int getNbFolds() {
		return m_NbFolds;
	}
	
	public int getNbRows() {
		return m_NbRows;
	}	

	public int getNbSelected(int fold) {
		int nb = 0;
		for (int i = 0; i < m_NbRows; i++) if (getFold(i) == fold) nb++;
		return nb;
	}	
	
	public boolean isSelected(int row, int fold) {
		return getFold(row) == fold;
	}
	
	public abstract int getFold(int row);
}
