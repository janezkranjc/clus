package clus.ext.optxval;

import java.util.*;
import jeans.tree.*;

import clus.main.Settings;
import clus.model.test.*;

public class OptXValSplit extends MyNode {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected int[] m_Folds;
	protected NodeTest m_Test;

	public int init(int[] folds, NodeTest test) {
		m_Test = test;	
		int mnb = folds.length;
		m_Folds = new int[mnb];
		System.arraycopy(folds, 0, m_Folds, 0, mnb);
		int arity = test.getNbChildren();
		setNbChildren(arity);
		return arity;
	}
	
	public int[] getFolds() {
		return m_Folds;
	}

	public void setTest(NodeTest test) {
		m_Test = test;
	}
	
	public NodeTest getTest() {
		return m_Test;
	}
	
	public boolean contains(int fold) {
		return Arrays.binarySearch(m_Folds, fold) >= 0;
	}
}
