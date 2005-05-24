package clus.gui;

import jeans.graph.swing.drawable.*;
import jeans.graph.tree.*;
import java.awt.*;

import clus.main.*;
import clus.model.test.*;

public class ClusDummyPainter extends MyNodePainter {

	protected Drawable m_Label;
		
	public ClusDummyPainter(ClusNode node, int idx) {
		super(null);
		m_Label = createLabel(node, idx);
	}	
	
	public MyNodePainter createPainter(MyDrawableNode node) {
		return null;
	}	
	
	public Drawable getLabel() {
		return m_Label;
	}		
	
	public void calcSize(Graphics2D g, FontMetrics fm, DrawableCanvas cnv) {
		if (m_Label != null) m_Label.calcSize(g, fm, cnv);
		wd = 20;
		hi = 0;
	}

	public void draw(Graphics2D g, DrawableCanvas cnv, int xofs, int yofs) {		
	}	
	
	private Drawable createLabel(ClusNode parent, int idx) {
		NodeTest test = parent.getTest();
		if (test.hasBranchLabels()) {
			String label = test.getBranchLabel(idx);
			DrawableLines res = new DrawableLines(label);
			res.setBackground(SystemColor.control);
			return res;
		} else {
			return null;
		}
	}
}
