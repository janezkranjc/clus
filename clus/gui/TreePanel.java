package clus.gui;

import jeans.graph.swing.drawable.*;
import jeans.graph.tree.*;
import jeans.tree.*;

import java.util.*;
import java.awt.*;
import javax.swing.*;

import clus.main.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.pruning.*;

import clus.gui.statvis.*;

public class TreePanel extends JPanel {

	TreeFrame m_Frame;
	MyDrawableNode m_Root;
	ClusNode m_ClusRoot;	
	DrawableScrollableCanvas m_Canvas;
	HashMap m_StatVis = createStatVis();
	MyNodePainter m_Paint = new ClusNodePainter();
	NodePainterSettings m_Sett = new NodePainterSettings();
	ClusStatManager m_Manager;
	boolean m_HorzVert = true;	

	private final static int[] XGAPS = {10, 5,  3,  3,  2};

	public TreePanel() {
		this(null);
	}

	public TreePanel(ClusNode root) {
		this(root, null);
	}

	public TreePanel(ClusNode root, String[] info) {
		System.out.println("Setting node settings");
		m_Sett.NODE_COLOR = new Color(228,186,143);
		m_Sett.LEAF_COLOR = new Color(172,193,232);
		m_Sett.NODE_BORDER_COLOR = Color.red;
		m_Sett.LEAF_BORDER_COLOR = Color.blue;
		/* Settings for different zoom levels */
		m_Sett.XGAP   = XGAPS;
		m_Sett.setDocument(this);
		setLayout(new BorderLayout());
		add(m_Canvas = new DrawableScrollableCanvas(), BorderLayout.CENTER);
		m_Canvas.setBackground(new Color(204,204,204));
		setTree(root, false);
	}
	
	public void setStatManager(ClusStatManager mgr) {
		m_Manager = mgr;
	}
	
	public ClusStatManager getStatManager() {
		return m_Manager;
	}	

	public TargetWeightProducer createTargetWeightProducer() {
		return m_Manager.createTargetWeightProducer();
	}
	
	public void setHorzVert(boolean toggle) {
		System.out.println("Set horz/vert: "+toggle);
		DrawableCanvas cnv = m_Canvas.getCanvas();
		TreeRenderer rend = (TreeRenderer)cnv.getRenderer();
		if (rend != null) {
			rend.setHorzVert(toggle);
			cnv.setRenderState(DrawableCanvas.STATE_RENDER);
			cnv.repaint();
		}
		m_HorzVert = toggle;
	}

	public void setBackGroundColor(Color color){
		m_Canvas.setBackground(color);
	}


	public void setScrollTo(int[] path, int skip) {
		MyDrawableNode node = (MyDrawableNode)m_Root.fromPath(path, skip);
		if (node != null) m_Canvas.setScrollTo(node.getPainter());
	}

	public void setTree(ClusNode root) {
		setTree(root, true);
	}

	public void setTree(ClusNode root, boolean redraw) {
		m_ClusRoot = root;
		DrawableCanvas cnv = m_Canvas.getCanvas();
		if (root != null) {
			m_Root = createTree(root, m_Sett, m_Paint);
			TreeRenderer renderer = new TreeRenderer(m_Root);
			renderer.setHorzVert(m_HorzVert);
			renderer.setZoomLevels(5);
			cnv.setRenderer(renderer);
		} else {
			cnv.setRenderer(null);
		}
		if (redraw) doRender();
	}


	public void setFrame(TreeFrame frame) {
		m_Frame = frame;
	}

	public void showInfo(ClusNode node) {
		if (m_Frame != null) m_Frame.showInfo(node);
	}

	public Drawable createStatVisualiser(ClusStatistic stat) {
		String name = stat.getClass().getName();
		ClusStatVisualizer statvis = (ClusStatVisualizer)m_StatVis.get(name);
		if (statvis == null) return null;
		else return statvis.createInstance(stat);
	}

	public void doRender() {
		DrawableCanvas cnv = m_Canvas.getCanvas();
		cnv.setRenderState(DrawableCanvas.STATE_RENDER);
		cnv.repaint();
	}

	public void collapseChildren(MyDrawableNode node) {
		for (int i = 0; i < node.getNbChildren(); i++) {
			MyDrawableNode child = (MyDrawableNode)node.getChild(i);
			child.recursiveFakeLeaf(true);
		}
	}
	
	public void pruneTree(ClusNode node, int size) {
		SizeConstraintPruning pruner = new SizeConstraintPruning(size, m_Manager.createTargetWeightProducer());
		pruner.prune(node);
		recursiveUpdate(m_Root);
		doRender();
	}
	
	public static void recursiveUpdate(MyDrawableNode node) {
		ClusNode cnode = (ClusNode)node.getVisitor(0);
		if (cnode.atBottomLevel() && !node.atBottomLevel()) {
			node.removeAllChildren();
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			recursiveUpdate((MyDrawableNode)node.getChild(i));
		}		
	}

	private HashMap createStatVis() {
		HashMap res = new HashMap();
		res.put(ClassificationStat.class.getName(), new ClassStatVis());
		return res;
	}

	private static MyDrawableNode createTree(Node node, NodePainterSettings par, MyNodePainter paint) {
		par.addVisitor();
		MyDrawableNode root = new MyDrawableNode(par);
		recursiveCreateTree(root, node, paint);
		if (root.getNbNodes() > 10) {
			root.recursiveZoom(1);			
		}		
		return root;
	}

	private static void recursiveCreateTree(MyDrawableNode root, Node node, MyNodePainter paint) {
		root.setVisitor(node, 0);
		root.setPainter(paint.createPainter(root));
		ClusNode cnode = (ClusNode)node;
		NodeTest test = cnode.getTest();
		if (test != null) {
			int n_arity = node.getNbChildren();
			int t_arity = test.getNbChildren();
			root.setNbChildren(t_arity);
			for (int i = 0; i < n_arity; i++) {
				MyDrawableNode child = new MyDrawableNode(root.getVisParent());
				root.setChild(child, i);
				child.setIndex(i);
				recursiveCreateTree(child, node.getChild(i), paint);
			}
			for (int i = n_arity; i < t_arity; i++) {
				MyDrawableNode child = new MyDrawableNode(root.getVisParent());
				root.setChild(child, i);
				child.setIndex(i);
				child.setPainter(new ClusDummyPainter(cnode, i));
			}
		}
	}
}
