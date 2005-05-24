package clus.selection;

public class BitMapSelection extends ClusSelection {

	protected int m_NbSelected;
	protected boolean[] m_Selection;

	public BitMapSelection(int nbrows) {
		super(nbrows);
		m_Selection = new boolean[nbrows];
	}

	public int getNbSelected() {
		return m_NbSelected;
	}
	
	public boolean isSelected(int row) {
		return m_Selection[row];
	}

	public void select(int row) {
		if (!m_Selection[row]) {
			m_Selection[row] = true;
			m_NbSelected++;
		}
	}	
}
