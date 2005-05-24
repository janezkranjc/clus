package clus.ext.optxval;
import clus.tools.debug.Debug;


import java.io.*;
import java.util.*;
import jeans.util.array.*;
import jeans.util.cmdline.*;

import jeans.resource.*;

import clus.*;

import clus.main.*;
import clus.util.*;
import clus.ext.*;
import clus.data.rows.*;
import clus.algo.induce.*;
import clus.selection.*;
import clus.error.multiscore.*;

public class OptXVal extends ClusExtension {

	protected Clus m_Clus;
	
	public OptXVal(Clus clus) {
		m_Clus = clus;
	}

	public ClusInduce createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {
		schema.addIndices(ClusSchema.ROWS);
		int nb_num = schema.getNbNum();
		if (Settings.XVAL_OVERLAP && nb_num > 0) return new OptXValIndOV(schema, sett);
		else return new OptXValIndNO(schema, sett);
	}
	
	public final void addFoldNrs(RowData set, XValMainSelection sel) {
		int nb = set.getNbRows();
		for (int i = 0; i < nb; i++) {
			int fold = sel.getFold(i);
			DataTuple tuple = set.getTuple(i);
			tuple.setIndex(fold+1);
		}
	}
	
	public final static void showFoldsInfo(PrintWriter writer, Object root) {
		OptXValBinTree bintree = OptXValBinTree.convertTree(root);		
		
		double[] fis = bintree.getFIs();
		double[] nodes = bintree.getNodes();
		double[] times = bintree.getTimes();		
		MDoubleArray.divide(fis, nodes);
		
		writer.println("FoldsInfo");		
		writer.println("Nodes:  "+MDoubleArray.toString(nodes));
		writer.println("f(i-1): "+MDoubleArray.toString(fis));
		writer.println("Time:   "+MDoubleArray.toString(times));
	}
	
	public final static void showForest(PrintWriter writer, OptXValNode root) {
		writer.println("XVal Forest");
		writer.println("***********");
		writer.println();
		showFoldsInfo(writer, root);
		writer.println();
		root.printTree(writer, "");
		writer.println();		
	}
	
	public final void xvalRun(String appname, Date date) throws IOException, ClusException {
		Settings sett = m_Clus.getSettings();
		ClusSchema schema = m_Clus.getSchema();
		RowData set = m_Clus.getRowDataClone();		
		XValMainSelection sel = schema.getXValSelection(set);
		addFoldNrs(set, sel);		
		OptXValInduce induce = (OptXValInduce)m_Clus.getInduce();
		induce.initialize(sel.getNbFolds());
		long time;
if (Debug.debug == 1) {
		time = ResourceInfo.getCPUTime();
}

		OptXValNode root = null;
		int nbr = 0;		
		while (true) {
			root = induce.optXVal(set);
			nbr++;
if (Debug.debug == 1) {
			if ((ResourceInfo.getCPUTime() - time) > 5000.0) break;
}

		}		
		ClusSummary summary = m_Clus.getSummary();		
if (Debug.debug == 1) {
if (Debug.debug == 1) {
		summary.setInductionTime((long)ClusStat.addToTotal(ResourceInfo.getCPUTime() - time, nbr));
}

}

if (Debug.debug == 1) {
		ClusStat.addTimes(nbr);		
}

		// Output whole tree		
		MultiScore score = m_Clus.getMultiScore();		
		ClusOutput output = new ClusOutput(appname+".out", schema, sett);
		output.writeHeader();		
		ClusNode tree = root.getTree(0);
		ClusRun cr = m_Clus.partitionData();
		tree.postProc(score);		
		m_Clus.storeAndPruneModel(cr, tree);		
//		m_Clus.calcError(cr, null);
		output.writeOutput(cr, true);
		output.close();								
		// Output xval trees
		output = new ClusOutput(appname+".xval", schema, sett);
		output.writeHeader();		
		if (Settings.SHOW_XVAL_FOREST) showForest(output.getWriter(), root);
		for (int i = 0; i < sel.getNbFolds(); i++) {
			XValSelection msel = new XValSelection(sel, i);
			cr = m_Clus.partitionData(msel, i+1);
			tree = root.getTree(i+1);
			tree.postProc(score);			
			m_Clus.storeAndPruneModel(cr, tree);
//			m_Clus.calcError(cr, summary);
			if (sett.isOutFoldTree()) output.writeOutput(cr, false);	
		}		
		output.writeSummary(summary);
		output.close();	
	}
}
