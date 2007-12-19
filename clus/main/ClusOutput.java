/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

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
		System.out.println("Clus v2.1 - Software for Predictive Clustering");
		System.out.println();
		System.out.println("Copyright (C) 2007");
		System.out.println("   Katholieke Universiteit Leuven, Leuven, Belgium");
		System.out.println("   Jozef Stefan Institute, Ljubljana, Slovenia");
		System.out.println();
		System.out.println("This program comes with ABSOLUTELY NO WARRANTY; for details");
		System.out.println("type 'clus -warranty'. This is free software, and you are");
		System.out.println("welcome to redistribute it under certain conditions; type");
		System.out.println("'clus -copying' for details.");
		System.out.println();
	}
	
	public static void printWarranty() {
		System.out.println("Disclaimer of Warranty.");
		System.out.println();
		System.out.println("  THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY");
		System.out.println("APPLICABLE LAW.  EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT");
		System.out.println("HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM \"AS IS\" WITHOUT WARRANTY");
		System.out.println("OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,");
		System.out.println("THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR");
		System.out.println("PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM");
		System.out.println("IS WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF");
		System.out.println("ALL NECESSARY SERVICING, REPAIR OR CORRECTION.");
		System.out.println();
		System.out.println("Limitation of Liability.");
		System.out.println();
		System.out.println("IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING");
		System.out.println("WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS");
		System.out.println("THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY");
		System.out.println("GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE");
		System.out.println("USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF");
		System.out.println("DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD");
		System.out.println("PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS),");
		System.out.println("EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF");
		System.out.println("SUCH DAMAGES.");
		System.out.println();
		System.out.println("Type 'clus -copying' for more details.");
	}

	public static void showHelp() {	    
		System.out.println("Usage: clus appname");
		System.out.println("Database: appname.arff");
		System.out.println("Settings: appname.s");
		System.out.println("Output:   appname.out");
		System.out.println();
		System.out.println("More information on:");
		System.out.println("http://www.cs.kuleuven.be/~dtai/clus");
	}
	
	public static void printGPL() {
		
	}	
}
