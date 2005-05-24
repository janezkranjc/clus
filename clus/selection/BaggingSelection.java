package clus.selection;

import clus.util.*;

public class BaggingSelection extends ClusSelection {

	protected int[] m_Counts;
	protected int m_NbSel;

	public BaggingSelection(int nbrows) {
		super(nbrows);
		m_Counts = new int[nbrows];
		for (int i = 0; i < nbrows; i++) {
			m_Counts[ClusRandom.nextInt(ClusRandom.RANDOM_SELECTION, nbrows)]++;
		}
		for (int i = 0; i < nbrows; i++) {
			if (m_Counts[i] != 0) m_NbSel++;
		}		
	}
	
	public boolean changesDistribution() {
		return true;
	}
	
	public double getWeight(int row) {
		return (double)m_Counts[row];
	}
	
	public int getNbSelected() {
		return m_NbSel;
	}
	
	public boolean isSelected(int row) {
		return m_Counts[row] != 0;
	}
	
	public final int getCount(int row) {
		return m_Counts[row];	
	}	
}
