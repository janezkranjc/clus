package clus.selection;

public class XValSelection extends ClusSelection {

	protected int m_Fold, m_NbSel;
	protected XValMainSelection m_Sel;
	
	public XValSelection(XValMainSelection sel, int fold) {
		super(sel.getNbRows());
		m_Sel = sel;		
		m_Fold = fold;
		m_NbSel = sel.getNbSelected(fold);
	}

	public int getNbSelected() {
		return m_NbSel;
	}
	
	public boolean isSelected(int row) {
		return m_Sel.isSelected(row, m_Fold);
	}

}
