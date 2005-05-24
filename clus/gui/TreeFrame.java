package clus.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import clus.ext.*;
import clus.ext.hierarchical.*;

import java.io.*;
import jeans.util.*;
import jeans.graph.*;
import jeans.io.*;

import clus.main.*;
import clus.util.*;
import clus.statistic.*;
import clus.ext.beamsearch.*;

public class TreeFrame extends JFrame {
	
	public final static int INITIAL_WD = 800;
	public final static int INITIAL_HI = 600;	
	
	public final static int[] m_Permute = {1,2,0};
	
	double m_VPerc = 0.75;
	double m_HPerc = 0.8; 
	
	JSplitPane m_HSplit, m_VSplit, m_HRSplit;
	ShowHierarchy m_ShowHier;	
	TreePanel m_TreePanel;
	
	JTextField m_Stat;
	JTextArea m_Info;
	JSlider m_Fac, m_Sig;
	JCheckBoxMenuItem m_Rel, m_Horz;
	JTree m_Tree;
	DefaultTreeModel m_TreeModel;
	DefaultMutableTreeNode m_Root;
	JList m_DSList;
	DefaultListModel m_DSListModel;
	JTextArea m_TextArea = new JTextArea();
	JMenuItem m_Find, m_Open;
	JFileChooser m_FileChoose = new JFileChooser();
	TreeMap m_Files = new TreeMap();
	
	public TreeFrame(String title, TreePanel tpanel, ShowHierarchy sh) {
		super(title);
		m_ShowHier = sh;
		m_TreePanel = tpanel;
		JPanel panel = new JPanel();
		//		m_HSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, m_TreePanel, makeBottomPanel());
		//		m_VSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_HSplit, makeRightPanel()); 	
		//		m_VSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new MySplitListener(1));		
		panel.setLayout(new BorderLayout(3,3));
		m_TextArea.setEditable(false);
		m_HSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, m_TreePanel, m_TextArea);	
		m_VSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_HSplit, makeRightPanel()); 			
		panel.add(m_VSplit, BorderLayout.CENTER);
		//		panel.add(m_TreePanel, BorderLayout.CENTER);		
		setContentPane(panel);
		setJMenuBar(createMenu());
		addComponentListener(new MyResizeListener());
		m_FileChoose.setCurrentDirectory(FileUtil.getCurrentDir());
	}
	
	public JMenuBar createMenu() {
		JMenuBar menu = new JMenuBar();
		JMenu file = new JMenu("File");
		//file.add(m_Open = new JMenuItem("Open Data Set"));
		//m_Open.addActionListener(new MyOpenListener());		
		file.add(m_Find = new JMenuItem("Find Data Sets"));
		m_Find.addActionListener(new MyFindListener());
		JMenu sett = new JMenu("Settings");
		sett.add(m_Horz = new JCheckBoxMenuItem("Horizontal"));
		m_Horz.addActionListener(new MyHorzListener());
		m_Horz.setSelected(true);
		menu.add(file);
		menu.add(sett);
		return menu;
	}
	
	public void init() {
		pack();
		setSize(INITIAL_WD, INITIAL_HI);
	}
	
	public void setDividers(int wd, int hi) {
		int vval = (int)(m_VPerc*wd);
		int hval = (int)(m_HPerc*hi);		
		m_VSplit.setDividerLocation(vval);
		m_HSplit.setDividerLocation(hval);
		m_HRSplit.setDividerLocation((int)(0.25*hi));
	}
	
	public JPanel makeRightPanel() {
		JPanel panel = new JPanel();
		if (m_ShowHier != null) {
			panel.setLayout(new PercentLayout("100% p p p p", 3, PercentLayout.ALL, true));		
			panel.add(m_ShowHier);
			//			m_Rel = new JCheckBox("Relative");
			//			panel.add(m_Rel);		
			panel.add(new JLabel("Scale factor"));
			panel.add(m_Fac = new JSlider(JSlider.HORIZONTAL, 0, 100, 10));
			m_Fac.addChangeListener(new MyScaleListener());
			panel.add(new JLabel("Significance level"));
			panel.add(m_Sig = new JSlider(JSlider.HORIZONTAL, 0, 100, 50));
			m_Sig.addChangeListener(new MySignificanceListener());		
		} else {		
			m_Root = new DefaultMutableTreeNode("Root");
			m_TreeModel = new DefaultTreeModel(m_Root);
			m_Tree = new JTree(m_TreeModel);
			// m_Tree.setShowsRootHandles(false);
			// m_Tree.setRootVisible(false);
			m_Tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			m_Tree.addTreeSelectionListener(new MyFileTreeListener());			
			JScrollPane treepane = new JScrollPane(m_Tree);
			m_DSListModel = new DefaultListModel();
			m_DSList = new JList(m_DSListModel);
			m_DSList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			m_DSList.addListSelectionListener(new MyListListener());
			JScrollPane listpane = new JScrollPane(m_DSList);
			m_HRSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listpane, treepane);
			panel.setLayout(new PercentLayout("100%", 3, PercentLayout.ALL, true));
			panel.add(m_HRSplit);
		}
		return panel;
	}
	
	public JPanel makeBottomPanel() {
		JPanel panel = new JPanel();
		
		JPanel spanel = new JPanel();
		spanel.setLayout(new PercentLayout("p 100%", 3, PercentLayout.ALL, false));
		spanel.add(new JLabel("Statistic: "));
		spanel.add(m_Stat = new JTextField());
		m_Stat.setEditable(false);
		
		JPanel ipanel = new JPanel();
		ipanel.setLayout(new PercentLayout("p 100%", 3, PercentLayout.ALL, false));
		JPanel sub = new JPanel();
		sub.setLayout(new PercentLayout("p 100%d", 3, 0, true));
		sub.add(new JLabel("Info: "));
		ipanel.add(sub);
		ipanel.add(m_Info = new JTextArea());
		m_Info.setEditable(false);
		m_Info.setFont(new Font("MonoSpaced", Font.PLAIN, 12));	
		
		panel.setLayout(new BorderLayout(3,3));
		panel.add(spanel, BorderLayout.NORTH);
		panel.add(ipanel, BorderLayout.CENTER);	
		
		return panel;
	}
	
	public DefaultMutableTreeNode getParentFNode(String[] path) {
		int pos = 0;
		DefaultMutableTreeNode curr = m_Root;
		while (pos < path.length-1) {
			String pstr = path[pos];
			boolean found = false;
			for (int i = 0; i < curr.getChildCount(); i++) {
				DefaultMutableTreeNode ch = (DefaultMutableTreeNode)curr.getChildAt(i);
				if (pstr.equals(ch.getUserObject())) {
					found = true;
					curr = ch;
					break;
				}
			}
			if (!found) {
				int ipos = 0;
				while (ipos < curr.getChildCount() && 
						cmpName(curr.getChildAt(ipos).toString(), pstr)) {
					ipos++;
				}				
				DefaultMutableTreeNode ch = new DefaultMutableTreeNode(pstr);
				m_TreeModel.insertNodeInto(ch, curr, ipos);				
				curr = ch;
			}
			pos++;
		}
		return curr;
	}
	
	public static boolean isNumber(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) return false;
		}
		return true;
	}
	
	public boolean cmpName(String s1, String s2) {
		if (isNumber(s2) && isNumber(s1)) {
			try {
				return Integer.parseInt(s1) <= Integer.parseInt(s2);
			} catch (NumberFormatException e) {
				return false;
			}
		} else {
			return s1.compareTo(s2) <= 0;
		}
	}
	
	public void openDataSet(String file) throws IOException {
		String path = (String)m_Files.get(file);
		if (path != null) {
			openDir(path, file);
		}
	}	
	
	public String[] doPermute(String[] input) {
		String[] res = new String[input.length];
		for (int i = 0; i < input.length; i++) {
			res[i] = input[i];
		}
		int max = 0;
		for (int i = 0; i < m_Permute.length; i++) {
			max = Math.max(max, m_Permute[i]);
		}
		if (max < input.length && m_Permute.length <= input.length) {
			for (int i = 0; i < m_Permute.length; i++) {
				res[i] = input[m_Permute[i]];
			}
		}
		return res;
	}
	
	public String correctUnderscores(String fn, String ds) {
		if (ds != null) {
			int pos = fn.indexOf(ds);
			if (pos != -1) {
				StringBuffer res = new StringBuffer(fn);
				for (int j = 0; j < ds.length(); j++) {
					if (res.charAt(pos+j) == '-') {
						res.setCharAt(pos+j, '_');
					}
				}
				return res.toString();
			}
		}
		return fn;
	}
	
	public void openDir(String dir, String ds) throws IOException {
		for (int i = m_Root.getChildCount()-1; i >= 0; i--) {
			DefaultMutableTreeNode ch = (DefaultMutableTreeNode)m_Root.getChildAt(i);
			m_TreeModel.removeNodeFromParent(ch);
		}
		File dir_file = new File(dir);
		ArrayList files = new ArrayList();
		System.out.println("Searching for models in: "+dir);
		FileUtil.recursiveFindAll(dir_file, ".model", files);
		FileUtil.recursiveFindAll(dir_file, ".tree", files);
		for (int i = 0; i < files.size(); i++) {
			String full = (String)files.get(i);
			String fn = FileUtil.removePath(full);
			fn = correctUnderscores(fn, ds);
			fn = correctUnderscores(fn, "beam-10");
			fn = correctUnderscores(fn, "beam-100");
			fn = correctUnderscores(fn, "beam_pp-100");			
			fn = correctUnderscores(fn, "beam_c-75");			
			String[] name = FileUtil.getName(fn).split("\\-");
			name = doPermute(name);
			String chname = name[name.length-1];
			DefaultMutableTreeNode parent = getParentFNode(name);
			ClusFileTreeElem elem = new ClusFileTreeElem(chname, full);
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(elem);
			int ipos = 0;
			while (ipos < parent.getChildCount() && 
					cmpName(parent.getChildAt(ipos).toString(), chname)) {
				ipos++;
			}
			m_TreeModel.insertNodeInto(child, parent, ipos);
		}
		for (int i = 0; i < m_Root.getChildCount(); i++) {
			DefaultMutableTreeNode ch = (DefaultMutableTreeNode)m_Root.getChildAt(i);
			m_Tree.expandPath(new TreePath(ch.getPath()));
			System.out.println("Expanding: "+ch);
		}
	}
	
	public void setTree(ClusNode root) {
		m_TreePanel.setTree(root, true);
		showInfo(root);		
	}
	
	public void showInfo(ClusNode root) {
		StringBuffer buf = new StringBuffer();
		buf.append("Size: "+root.getModelSize()+"\n");
		buf.append("Leaves: "+root.getNbLeaves()+"\n");
		TargetWeightProducer scale = m_TreePanel.createTargetWeightProducer();
		scale.setTotalStat(root.getTotalStat());
		String relerr = ClusFormat.SIX_AFTER_DOT.format(root.estimateError(scale));
		String abserr = ""+root.estimateErrorAbsolute(scale);
		buf.append("Error: "+relerr+" ("+abserr+") ss = "+root.estimateSS(scale)+"\n");
		buf.append("Statistic: "+root.getTotalStat());
		m_TextArea.setText(buf.toString());
	}			
	
	public void addBeam(ArrayList beam, ClusFileTreeElem elem, DefaultMutableTreeNode node) {
		int pos = 0;
		elem.setObject1(beam);
		for (int i = beam.size()-1; i >= 0; i--) {
			ClusBeamModel m = (ClusBeamModel)beam.get(i);
			ClusFileTreeElem celem = new ClusFileTreeElem(""+m.getValue(), "");
			celem.setObject1(m);
			celem.setType(ClusExtension.BEAM);
			DefaultMutableTreeNode ch = new DefaultMutableTreeNode(celem); 
			m_TreeModel.insertNodeInto(ch, node, pos++);			
		}	
	}
	
	public void loadModelType(ClusFileTreeElem elem, DefaultMutableTreeNode node) {
		ClusBeamModel m = (ClusBeamModel)elem.getObject1();
		ClusNode root = (ClusNode)m.getModel();
		root.updateTree();
		setTree(root);
	}
	
	public void loadModelType2(ClusFileTreeElem elem) {
		ArrayList beam = (ArrayList)elem.getObject1();
		if (beam.size() > 0) {
			ClusBeamModel m = (ClusBeamModel)beam.get(beam.size()-1);
			ClusNode root = (ClusNode)m.getModel();
			root.updateTree();
			setTree(root);
		}
	}
	
	public void loadModel(ClusFileTreeElem elem, DefaultMutableTreeNode node) {
		try {
			if (elem.getType() != -1) {
				loadModelType(elem, node);
				return;
			}
			System.out.println("Name: "+elem.getFullName());			
			ObjectLoadStream open = new ObjectLoadStream(new FileInputStream(elem.getFullName()));
			ClusStatManager manager = (ClusStatManager)open.readObject();
			Integer type = (Integer)open.readObject();
			try {
				switch (type.intValue()) {
				case ClusExtension.REGULAR_TREE:
					ClusNode root = (ClusNode)open.readObject();
				setTree(root);
				break;
				case ClusExtension.BEAM:
					ArrayList beam = (ArrayList)open.readObject();
				addBeam(beam, elem, node);
				loadModelType2(elem);
				break;
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Error loading model: "+elem.getFullName());
				System.out.println("Error loading model: "+elem.getFullName()+" "+e.getMessage());
			}
			open.close();			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Can't open model: "+elem.getFullName());
			System.err.println("IOError: "+e.getMessage());
		} catch (ClassNotFoundException e) {
			System.err.println("Error "+e.getMessage());
			e.printStackTrace();
		}
	}	
	
	private class MyScaleListener implements ChangeListener {
		
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider)e.getSource();
			if (!source.getValueIsAdjusting()) {
				int val = (int)source.getValue();
				m_ShowHier.setFac((double)val/10.0);		
				m_ShowHier.updateScreen();		     
			}
		}
	}	
	
	private class MySignificanceListener implements ChangeListener {
		
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider)e.getSource();
			if (!source.getValueIsAdjusting()) {
				int val = (int)source.getValue();
				m_ShowHier.setSig((double)val/100.0);		
				m_ShowHier.renewTree();		     
			}
		}
	}				
	
	private class MyHorzListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem)e.getSource();
			m_TreePanel.setHorzVert(source.isSelected());
		}
	}				
	
	private class MyFileTreeListener implements TreeSelectionListener {
		
		public void valueChanged(TreeSelectionEvent e) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_Tree.getLastSelectedPathComponent();
			if (node == null) return;
			if (node.isLeaf()) {
				System.out.println("Class = "+node.getUserObject().getClass().getName());
				ClusFileTreeElem elem = (ClusFileTreeElem)node.getUserObject();
				loadModel(elem, node);
			} else if (node.getUserObject() instanceof ClusFileTreeElem) {
				ClusFileTreeElem elem = (ClusFileTreeElem)node.getUserObject();
				loadModelType2(elem);
			}
		}		
	}
	
	/*	
	 private class MySplitListener implements PropertyChangeListener {
	 
	 protected MySplitListener(int which) {
	 }
	 
	 public void  propertyChange(PropertyChangeEvent evt)  {
	 System.out.println(evt.getNewValue());
	 }
	 }				
	 */	
	private class MyResizeListener implements ComponentListener {
		
		public void componentHidden(ComponentEvent e) {}
		
		public void componentMoved(ComponentEvent e) {}
		
		public void componentResized(ComponentEvent e) {
			Dimension d = ((JFrame)e.getSource()).getSize();	
			setDividers(d.width, d.height);
		}
		
		public void componentShown(ComponentEvent e) {}
	}
	
	private class MyOpenListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			
		}
	}
	
	private class MyListListener implements ListSelectionListener {
		
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting() == false) {
				String file = (String)m_DSList.getSelectedValue();
				if (file != null) {
					try {
						openDataSet(file);
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(TreeFrame.this, "Error: "+ex.getMessage());
					}
				}				
			}
		}
	}
	
	private class MyFindListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			m_FileChoose.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int res = m_FileChoose.showOpenDialog(TreeFrame.this);
			if (res == JFileChooser.APPROVE_OPTION) {
				try {
					File file = m_FileChoose.getSelectedFile();
					System.out.println("Opening: " + file.getName());
					ArrayList list = FileUtil.recursiveFind(file, ".s");
					for (int i = 0; i < list.size(); i++) {
						String full = (String)list.get(i);
						String dsname = FileUtil.getName(FileUtil.removePath(full));
						String dspath = FileUtil.getPath(full);
						m_Files.put(dsname, dspath);
						System.out.println("Name = "+dsname);
					}
					m_DSListModel.clear();					
					Iterator iter = m_Files.keySet().iterator();
					while (iter.hasNext()) {
						String name = (String)iter.next();
						m_DSListModel.addElement(name);
					}					
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(TreeFrame.this, "Error: "+ex.getMessage());
				}
			}			
		}		
	}
	
	public static TreeFrame createFrame(ClusStatManager manager, ClusNode root, ClassHierarchy hier) {
		TreeFrame frame;
		String[] lines = new String[0];
		TreePanel tpanel = new TreePanel(root, lines);
		tpanel.setStatManager(manager);
		ClusSchema schema = manager.getSchema();
		if (hier != null) {
			ShowHierarchy sh = new ShowHierarchy(root, hier);
			frame = new TreeFrame(schema.getRelationName(), tpanel, sh);
		} else {
			frame = new TreeFrame(schema.getRelationName(), tpanel, null);		
		}
		tpanel.setFrame(frame);
		frame.init();
		frame.setDividers(INITIAL_WD, INITIAL_HI);
		return frame;
	}
	
	public static TreeFrame loadTree(InputStream strm) throws IOException, ClassNotFoundException {
		ObjectLoadStream open = new ObjectLoadStream(strm);
		ClusStatManager manager = (ClusStatManager)open.readObject();
		Integer type = (Integer)open.readObject();
		ClusNode root = (ClusNode)open.readObject();
		open.close();	
		//		HierStatistic tr_def = (HierStatistic)root.getTotalStat();
		//		return TreeFrame.createFrame(schema.getRelationName(), root, tr_def.getHier());
		return TreeFrame.createFrame(manager, root, null);
	}		
	
	public static TreeFrame showTree(InputStream strm) throws IOException, ClassNotFoundException {
		TreeFrame frame = loadTree(strm);
		frame.addWindowListener(new WindowClosingListener(frame, WindowClosingListener.TYPE_EXIT));
		frame.setVisible(true);
		return frame;
	}	
	
	public static TreeFrame showTree(ClusStatManager manager, ClusNode root, ClassHierarchy hier) {		
		TreeFrame frame = createFrame(manager, root, hier);
		frame.addWindowListener(new WindowClosingListener(frame, WindowClosingListener.TYPE_EXIT));		
		frame.setVisible(true);
		return frame;
	}
	
	public static TreeFrame start(ClusStatManager manager, String opendir) throws IOException {
		TreeFrame frame = createFrame(manager, null, null);
		frame.addWindowListener(new WindowClosingListener(frame, WindowClosingListener.TYPE_EXIT));
		frame.openDir(opendir, null);
		frame.setVisible(true);
		return frame;
	}
}
