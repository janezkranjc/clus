package clus.selection;

import clus.util.*;

public class OverSample extends ClusSelection {

	protected int m_NbSelected;

	public OverSample(int nbrows, double sel) {
		super(nbrows);
		m_NbSelected = (int)Math.ceil((double)sel*nbrows);
	}

	public boolean supportsReplacement() {
		return true;
	}
	
	public int getIndex(int i) {
		return ClusRandom.nextInt(ClusRandom.RANDOM_SELECTION, m_NbRows);
	}

	public int getNbSelected() {
		return m_NbSelected;
	}
	
	public boolean isSelected(int row) {
		return false;
	}

}
