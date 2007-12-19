package clus.gui;

import java.awt.*;
import javax.swing.*;

import java.io.*;
import jeans.util.*;
import jeans.graph.*;
import jeans.io.*;

import clus.main.*;
import clus.data.type.*;
import clus.model.modelio.tilde.*;

public class SimpleTreeFrame extends JFrame {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public final static int INITIAL_WD = 800;
	public final static int INITIAL_HI = 600;	

	TreePanel m_TreePanel;

	public SimpleTreeFrame(String title, TreePanel tpanel) {
		super(title);
		m_TreePanel = tpanel;
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(3,3));
		panel.add(m_TreePanel, BorderLayout.CENTER);
		setContentPane(panel);
	}
		
	public void init() {
		pack();
		setSize(INITIAL_WD, INITIAL_HI);
	}
       
	public static SimpleTreeFrame createFrame(String title, ClusNode root) {
		String[] lines = new String[0];
		TreePanel tpanel = new TreePanel(root, lines);
		SimpleTreeFrame frame = new SimpleTreeFrame(title, tpanel);
		frame.init();
		return frame;
	}
	
	public static SimpleTreeFrame loadTree(InputStream strm) throws IOException, ClassNotFoundException {
		ObjectLoadStream open = new ObjectLoadStream(strm);
		ClusSchema schema = (ClusSchema)open.readObject();
		ClusNode root = (ClusNode)open.readObject();
		open.close();	
		return createFrame(schema.getRelationName(), root);
	}		
	
	public static SimpleTreeFrame loadTildeTree(InputStream strm) throws IOException, ClassNotFoundException {	
		TildeOutReader reader = new TildeOutReader(strm);
		reader.doParse();
		ClusNode root = reader.getTree();
		reader.close();	
		return createFrame("TildeTree", root);
	}		
		
	public static SimpleTreeFrame showTree(String fname) throws IOException, ClassNotFoundException {
		SimpleTreeFrame frame;
		if (FileUtil.getExtension(fname).equals("out")) {
			frame = loadTildeTree(new FileInputStream(fname));
		} else {
			frame = loadTree(new FileInputStream(fname));
		}
		frame.addWindowListener(new WindowClosingListener(frame, WindowClosingListener.TYPE_EXIT));
		frame.setVisible(true);
		return frame;
	}	
}
