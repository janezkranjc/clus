package clus.gui;

import javax.swing.tree.*;

import clus.ext.hierarchical.*;
import clus.main.Settings;

public class JTHierTreeNode extends DefaultMutableTreeNode {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public JTHierTreeNode(ClassTerm val) {
		super(val);
	}
	
	public JTHierTreeNode() {
		super();
	}	
}
