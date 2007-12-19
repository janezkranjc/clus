package clus.main;

import java.io.*;
import java.text.*;
import java.util.*;

import jeans.resource.ResourceInfo;
import jeans.util.*;

import clus.statistic.StatisticPrintInfo;
import clus.util.*;
import clus.data.type.*;
import clus.error.*;
import clus.data.rows.*;

public class ClusOutput {

	protected ClusSchema m_Schema;
	protected Settings m_Sett;
	protected PrintWriter m_Writer;
	protected String m_Fname;
	protected Settings m_Sett2;
	protected StringWriter m_StrWrt;

	public ClusOutput(String fname, ClusSchema schema, Settings sett) throws IOException {
		m_Schema = schema;
		m_Sett = sett;
		m_Sett2 = sett;
		m_Fname = fname;
		m_Writer = sett.getFileAbsoluteWriter(fname);
	}
	
	public ClusOutput(ClusSchema schema, Settings sett) throws IOException {
		m_Schema = schema;
		m_Sett = sett;
		m_Sett2 = sett;		
		m_StrWrt = new StringWriter();
		m_Writer = new PrintWriter(m_StrWrt);
	}
	
	public void print(String str) {
		m_Writer.print(str);
	}

	public String getString() {
		return m_StrWrt.toString();
	}
	
	public Settings getSettings() {
		return m_Sett;
	}
	
	public void writeHeader() throws IOException {
		String relname = m_Schema.getRelationName();
		m_Writer.println("Clus run "+relname);
		m_Writer.println(StringUtils.makeString('*', 9+relname.length()));
		m_Writer.println();
		Date date = m_Schema.getSettings().getDate();
		m_Writer.println("Date: "+DateFormat.getInstance().format(date));
		m_Writer.println("File: "+m_Fname);
		int a_tot = m_Schema.getNbAttributes();
		int a_in = m_Schema.getNbDescriptiveAttributes();
		int a_out = m_Schema.getNbTargetAttributes();
		m_Writer.println("Attributes: "+a_tot+" (input: "+a_in+", output: "+a_out+")");
		m_Writer.println("Missing values: "+(m_Schema.hasMissing() ? "Yes" : "No"));
		m_Writer.println("Rows: "+m_Schema.getNbRows());
		m_Writer.println();
		m_Sett.show(m_Writer);
		m_Writer.flush();
	}
	
	public void writeBrief(ClusRun cr) throws IOException {
		String ridx = cr.getIndexString();
		m_Writer.println("Run: "+ridx);
		ClusErrorParent te_err = cr.getTestError();
		if (te_err != null) {
			te_err.showErrorBrief(cr, ClusModelInfo.TEST_ERR, m_Writer);
		}
		ClusErrorParent tr_err = cr.getTrainError();
		if (m_Sett.isOutTrainError() && tr_err != null) {
			tr_err.showErrorBrief(cr, ClusModelInfo.TRAIN_ERR, m_Writer);			
		}
		m_Writer.println();
	}
	
	public void writeOutput(ClusRun cr, boolean detail) throws IOException, ClusException {
		writeOutput(cr, detail, false);
	}
	
	public boolean shouldShowModel(int model) {
		Settings sett = getSettings();
		boolean others = sett.getShowModel(Settings.SHOW_MODELS_OTHERS);
		if (model == ClusModels.DEFAULT && sett.getShowModel(Settings.SHOW_MODELS_DEFAULT)) return true;
		else if (model == ClusModels.ORIGINAL && sett.getShowModel(Settings.SHOW_MODELS_ORIGINAL)) return true;
		else if (model == ClusModels.PRUNED && (sett.getShowModel(Settings.SHOW_MODELS_PRUNED) || others)) return true;
		else if (others) return true;
		return false;
	}

	public void writeOutput(ClusRun cr, boolean detail, boolean outputtrain) throws IOException, ClusException {
		ArrayList models = new ArrayList();
		String ridx = cr.getIndexString();
		m_Writer.println("Run: "+ridx);
		m_Writer.println(StringUtils.makeString('*', 5+ridx.length()));
		m_Writer.println();
		m_Writer.println("Statistics");
		m_Writer.println("----------");
		m_Writer.println();
		m_Writer.println("FTValue (FTest): "+m_Sett.getFTest());
		double tsec = (double)cr.getInductionTime()/1000.0;
		double tpru = (double)cr.getPruneTime()/1000.0;
		// Prepare models for printing if required
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModelInfo mi = cr.getModelInfo(i);
			ClusModel root = mi.getModel();
			if (root != null) {
				if (mi.shouldPruneInvalid()) {
					root = root.prune(ClusModel.PRUNE_INVALID);
				}
			}
			models.add(root);			
		}
		// Compute statistics
	    String cpu = ResourceInfo.isLibLoaded() ? " (CPU)" : "";
		m_Writer.println("Induction Time: "+ClusFormat.FOUR_AFTER_DOT.format(tsec)+" sec"+cpu);
		m_Writer.println("Pruning Time: "+ClusFormat.FOUR_AFTER_DOT.format(tpru)+" sec"+cpu);		
		m_Writer.println("Model information");
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModelInfo mi = cr.getModelInfo(i);
			m_Writer.print("     "+mi.getName()+": ");
			ClusModel model = (ClusModel)models.get(i);
			String info_str = model == null ? "No model available" : model.getModelInfo();			
			String[] info = info_str.split("\\s*\\,\\s*");
			for (int j = 0; j < info.length; j++) {
				if (j > 0) m_Writer.print(StringUtils.makeString(' ', mi.getName().length()+7));
				m_Writer.println(info[j]);
			}
		}
		m_Writer.println();
		ClusErrorParent te_err = cr.getTestError();
		if (m_Sett.isOutFoldError() || detail) {
			if (outputtrain) {
				ClusErrorParent tr_err = cr.getTrainError();
				if (tr_err != null) {
					m_Writer.println("Training error");
					m_Writer.println("--------------");
					m_Writer.println();
					tr_err.showError(cr, ClusModelInfo.TRAIN_ERR, m_Writer);
					m_Writer.println();
				}
				ClusErrorParent.printExtraError(cr, ClusModelInfo.TRAIN_ERR, m_Writer);
			}
			ClusErrorParent va_err = cr.getValidationError();
			if (va_err != null) {
				m_Writer.println("Validation error");
				m_Writer.println("----------------");
				m_Writer.println();
				va_err.showError(cr, ClusModelInfo.VALID_ERR, m_Writer);
				m_Writer.println();
			}			
			if (te_err != null) {
				m_Writer.println("Testing error");
				m_Writer.println("-------------");
				m_Writer.println();
				te_err.showError(cr, ClusModelInfo.TEST_ERR, m_Writer);
				m_Writer.println();				
			}
		}
		StatisticPrintInfo info = m_Sett.getStatisticPrintInfo();
		
		for (int i = 0; i < cr.getNbModels(); i++) {
			if (shouldShowModel(i)) {
				ClusModelInfo mi = cr.getModelInfo(i);
				ClusModel root = (ClusModel)models.get(i);
				if (root != null) {
					String modelname = mi.getName() + " Model";
					m_Writer.println(modelname);
					m_Writer.println(StringUtils.makeString('*', modelname.length()));
					m_Writer.println();
					if (m_Sett.isPrintModelAndExamples()) {
						RowData pex = null; //(RowData)cr.getTrainingSet();
						if (te_err != null) pex = (RowData)cr.getTestSet();
						root.printModelAndExamples(m_Writer, info, pex);
					} else {
						root.printModel(m_Writer, info);
					}
					m_Writer.println();
					if (getSettings().isOutputPythonModel()) {
						// use following lines for getting tree as Python function 
						m_Writer.print("def clus_tree( ");
						ClusAttrType[] cat = ClusSchema.vectorToAttrArray(m_Schema.collectAttributes(ClusAttrType.ATTR_USE_DESCRIPTIVE, ClusAttrType.THIS_TYPE));
						for (int ii=0;ii<cat.length-1;ii++){
							m_Writer.print(cat[ii].getName()+",");
						}
						m_Writer.println(cat[cat.length-1].getName()+" ):");
						root.printModelToPythonScript(m_Writer);
						m_Writer.println();
					}
					
				}//end if (root != null)				
			}//end if (shouldShowModel(i))
		}// end for
		if (getSettings().isOutputDatabaseQueries()) {
			int starttree = getSettings().getStartTreeCpt();
			int startitem = getSettings().getStartItemCpt();
			ClusModel root = (ClusModel)models.get(cr.getNbModels()-1);
			// use the following lines for creating a SQL file that will put the tree into a database
			String out_database_name =  m_Sett2.getAppName()+".txt";
			PrintWriter database_writer = m_Sett2.getFileAbsoluteWriter(out_database_name);
			root.printModelToQuery(database_writer,cr,starttree,startitem);
			database_writer.close();
			System.out.println("the queries are in "+out_database_name);
		}
		
		m_Writer.flush();
	}

	public String getQuotient(int a, int b) {
		double val = b == 0 ? 0.0 : (double)a/b;
		return ClusFormat.ONE_AFTER_DOT.format(val);
	}

	public void writeSummary(ClusSummary summary) {
		m_Writer.println("Summary");
		m_Writer.println("*******");
		m_Writer.println();
		int runs = summary.getNbRuns();
		m_Writer.println("Runs: "+runs);
		double tsec = (double)summary.getInductionTime()/1000.0;
		m_Writer.println("Induction time: "+ClusFormat.FOUR_AFTER_DOT.format(tsec)+" sec");
		double psec = (double)summary.getPrepareTime()/1000.0;
		m_Writer.println("Preprocessing time: "+ClusFormat.ONE_AFTER_DOT.format(psec)+" sec");
		m_Writer.println("Mean number of tests");
		
		//Currently implemented ensemble methods don't have pruned models
		int end_model;
		if (!Settings.m_EnsembleMode)end_model = ClusModels.PRUNED;
		else end_model = ClusModels.ORIGINAL;
		
//		for (int i = ClusModels.ORIGINAL; i <= ClusModels.PRUNED; i++) {
		for (int i = ClusModels.ORIGINAL; i <= end_model; i++) {
			ClusModelInfo mi = summary.getModelInfo(i);
			m_Writer.println("     "+mi.getName()+": "+getQuotient(mi.getModelSize(), runs));
		}
		m_Writer.println();
		ClusErrorParent tr_err = summary.getTrainError();
		if (m_Sett.isOutTrainError() && tr_err != null) {
			m_Writer.println("Training error");
			m_Writer.println("--------------");
			m_Writer.println();
			tr_err.showError(summary, ClusModelInfo.TRAIN_ERR, m_Writer);
			m_Writer.println();
		}
		ClusErrorParent va_err = summary.getValidationError();
		if (va_err != null) {
			m_Writer.println("Validation error");
			m_Writer.println("----------------");
			m_Writer.println();
			va_err.showError(summary, ClusModelInfo.VALID_ERR, m_Writer);
			m_Writer.println();
		}		
		ClusErrorParent te_err = summary.getTestError();
		if (te_err != null) {
			m_Writer.println("Testing error");
			m_Writer.println("-------------");
			m_Writer.println();
			te_err.showError(summary, ClusModelInfo.TEST_ERR, m_Writer);
		}
		m_Writer.println();
		m_Writer.flush();
	}

	public PrintWriter getWriter() {
		return m_Writer;
	}

	public void close() {
		if (m_Fname != null) System.out.println("Output written to: "+m_Fname);
		m_Writer.close();
	}

	public static void printHeader() {
		System.out.println("Clus v2.0");
		System.out.println("---------");
		System.out.println();
	}

	public static void showHelp() {
		System.out.println("Co-developed by:");
		System.out.println("   Katholieke Universiteit Leuven, Leuven, Belgium");
		System.out.println("   Jozef Stefan Institute, Ljubljana, Slovenia"); 
		System.out.println();
		System.out.println("Usage: clus appname");
		System.out.println("Database: appname.arff");
		System.out.println("Settings: appname.s");
		System.out.println("Output:   appname.out");
		System.out.println();
		System.out.println("More information on:");
		System.out.println("http://www.cs.kuleuven.be/~dtai/clus");
	}
}
