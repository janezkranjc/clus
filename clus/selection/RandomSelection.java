package clus.selection;

import clus.util.*;

public class RandomSelection extends ClusSelection {

	protected int m_NbSelected;
	protected boolean[] m_Selection;

	public RandomSelection(int nbrows, double sel) {
		super(nbrows);
		makeSelection(nbrows, (int)Math.round((double)sel*nbrows));
	}
	
	public RandomSelection(int nbrows, int nbsel) {
		super(nbrows);
		makeSelection(nbrows, nbsel);
	}	

	public int getNbSelected() {
		return m_NbSelected;
	}
	
	public boolean isSelected(int row) {
		return m_Selection[row];
	}
	
	private final void makeSelection(int nbrows, int nbsel) {
		m_NbSelected = nbsel;
		m_Selection = new boolean[nbrows];
		for (int i = 0; i < m_NbSelected; i++) {
			int j = 0;
			int p = ClusRandom.nextInt(ClusRandom.RANDOM_SELECTION, nbrows-i)+1; // Select one of the remaining positions
			while (p > 0 && j < nbrows) {
				if (!m_Selection[j]) {
					p--;
					if (p == 0) m_Selection[j] = true;
				}
				j++;			
			}		
		}				
	}
}
