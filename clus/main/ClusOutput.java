package clus.main;

import java.io.*;
import java.text.*;
import java.util.*;
import jeans.util.*;

import clus.util.*;
import clus.error.*;

public class ClusOutput {

	protected ClusSchema m_Schema;
	protected Settings m_Sett;
	protected PrintWriter m_Writer;
	protected String m_Fname;

	public ClusOutput(String fname, ClusSchema schema, Settings sett) throws IOException {
		m_Schema = schema;
		m_Sett = sett;
		m_Fname = fname;
		m_Writer = sett.getFileAbsoluteWriter(fname);
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
		int a_in = m_Schema.getNbInput();
		int a_out = m_Schema.getNbOutput();
		m_Writer.println("Attributes: "+a_tot+" (input: "+a_in+", output: "+a_out+")");
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

	public void writeOutput(ClusRun cr, boolean detail) throws IOException {
		String ridx = cr.getIndexString();
		m_Writer.println("Run: "+ridx);
		m_Writer.println(StringUtils.makeString('*', 5+ridx.length()));
		m_Writer.println();
		m_Writer.println("Statistics");
		m_Writer.println("----------");
		m_Writer.println();
		m_Writer.println("FTValue (FTest): "+m_Sett.getFTest());
/*		if (cr.getModel(ClusModels.ORIGINAL) instanceof ClusNode) {
			OptXVal.showFoldsInfo(m_Writer, (ClusNode)cr.getModel(ClusModels.ORIGINAL));
			m_Writer.println();
		}*/
		double tsec = (double)cr.getInductionTime()/1000.0;
		m_Writer.println("Time: "+ClusFormat.FOUR_AFTER_DOT.format(tsec)+" sec");
		m_Writer.println("Model information");
		for (int i = 0; i < cr.getNbModels(); i++) {
			ClusModelInfo mi = cr.getModelInfo(i);
			m_Writer.print("     "+mi.getName()+": ");
			String[] info = mi.getModelInfo().split("\\s*\\,\\s*");
			for (int j = 0; j < info.length; j++) {
				if (j > 0) m_Writer.print(StringUtils.makeString(' ', mi.getName().length()+7));
				m_Writer.println(info[j]);
			}
		}
		m_Writer.println();
		if (m_Sett.isOutFoldError() || detail) {
			ClusErrorParent te_err = cr.getTestError();
			if (m_Sett.isOutTrainError() || te_err == null) {
				ClusErrorParent tr_err = cr.getTrainError();
				if (tr_err != null) {
					m_Writer.println("Training error");
					m_Writer.println("--------------");
					m_Writer.println();
					tr_err.showError(cr, ClusModelInfo.TRAIN_ERR, m_Writer);
					m_Writer.println();
				}
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
		//for (int i = ClusModels.DEFAULT; i <= ClusModels.PRUNED; i++) {
		for (int i = 0; i < cr.getNbModels(); i++) {
			if (i != ClusModels.ORIGINAL) {
				ClusModelInfo mi = cr.getModelInfo(i);
				ClusModel root = mi.getModel();
				if (root != null) {
					String modelname = mi.getName() + " Model";
					m_Writer.println(modelname);
					m_Writer.println(StringUtils.makeString('*', modelname.length()));
					m_Writer.println();
					root.printModel(m_Writer);
					m_Writer.println();
				}
			}
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
		for (int i = ClusModels.ORIGINAL; i <= ClusModels.PRUNED; i++) {
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
		System.out.println("Output written to: "+m_Fname);
		m_Writer.close();
	}

	public static void printHeader() {
		System.out.println("Clus v1.0");
		System.out.println("---------");
		System.out.println();
	}

	public static void showHelp() {
		System.out.println("Written by: Jan Struyf <Jan.Struyf@cs.kuleuven.ac.be>");
		System.out.println("            Hendrik Blockeel <Hendrik Blockeel@cs.kuleuven.ac.be>");
		System.out.println();
		System.out.println("Usage: Clus appname");
		System.out.println("Database: appname.arff");
		System.out.println("Settings: appname.s");
		System.out.println("Output: appname.out");
		System.out.println();
		System.out.println("More information on:");
		System.out.println("http:://www.cs.kuleuven.ac.be/~ml");
	}
}
