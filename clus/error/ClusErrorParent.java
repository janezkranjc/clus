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

package clus.error;

import jeans.util.*;

import java.text.*;
import java.util.*;
import java.io.*;

import clus.main.*;
import clus.data.rows.*;
import clus.data.attweights.*;
import clus.util.*;
import clus.statistic.*;

// FIXME : replace nbexamples by sumweight (not?) !

public class ClusErrorParent implements Serializable {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;	
	
	protected int m_NbTotal;
	protected int m_NbExamples;
	protected int m_NbCover;
	protected ClusStatManager m_StatManager;
	protected Vector m_Error = new Vector();	
		
	public ClusErrorParent(ClusStatManager smanager) {
		m_StatManager = smanager;
		m_NbTotal = -1;
	}
			
	public void setNbTotal(int nb) {
		m_NbTotal = nb;
	}
	
	public int getNbTotal() {
		return m_NbTotal == -1 ? m_NbExamples : m_NbTotal;
	}
	
	public void setNbExamples(int nb) {
		m_NbExamples = nb;
	}
	
	public void setNbExamples(int nb, int cover) {
		m_NbExamples = nb;
		m_NbCover = cover;
	}	
	
	public void setWeights(ClusAttributeWeights weights) {
		for (int i = 0; i < m_Error.size(); i++) {
			ClusError err = (ClusError)m_Error.elementAt(i);
			err.setWeights(weights);
		}		
	}
	
	public void checkChildren() {
		int nb_e = m_Error.size();
		for (int i = 0; i < nb_e; i++) {
			ClusError err = (ClusError)m_Error.elementAt(i);
			if (err.getParent() != this) System.out.println("Child: "+err+" has incorrect parent: "+err.getParent()+" "+this);
		}		
	}
	
	public void calcError(TupleIterator iter, ClusModel model) throws ClusException, IOException {
		iter.init();
		DataTuple tuple = iter.readTuple();
		while (tuple != null) {		
			ClusStatistic pred = model.predictWeighted(tuple);				
			addExample(tuple, pred);
			tuple = iter.readTuple();			
		}
		iter.close();
	}
	
	public ClusErrorParent getErrorClone() {	
		ClusErrorParent res = new ClusErrorParent(m_StatManager);
		int nb = m_Error.size();
		for (int i = 0; i < nb; i++) {
			ClusError err = (ClusError)m_Error.elementAt(i);
			res.addError(err.getErrorClone(res));
		}
		return res;
	}
	
	public void addError(ClusError err) {
		m_Error.addElement(err);
	}
	
	public int getNbErrors() {
		return m_Error.size();
	}	
	
	public ClusError getFirstError() {
		return getError(0);
	}
	
	public ClusError getError(int idx) {
		return (ClusError)m_Error.elementAt(idx);
	}	
	
	public ClusError getErrorByName(String name) {
		int nb_e = m_Error.size();
		for (int i = 0; i < nb_e; i++) {
			ClusError err = (ClusError)m_Error.elementAt(i);
			if (err.getName().equals(name)) return err;
		}		
		return null;
	}	

	public void compute(RowData data, ClusModel model) {
		int nb = m_Error.size();
		for (int i = 0; i < nb; i++) {
			ClusError err = (ClusError)m_Error.elementAt(i);
			err.compute(data, model);
		}	
		m_NbExamples = data.getNbRows();
		m_NbCover = m_NbExamples;
	}

	public void reset() {
		int nb = m_Error.size();
		for (int i = 0; i < nb; i++) {
			ClusError err = (ClusError)m_Error.elementAt(i);
			err.reset();
		}	
		m_NbExamples = 0;
		m_NbCover = 0;
	}
	
	public void addExample(DataTuple tuple, ClusStatistic stat) {
		m_NbExamples++;
		int nb = m_Error.size();		
		if (stat != null && stat.isValidPrediction()) {
			m_NbCover++;
			for (int i = 0; i < nb; i++) {
				ClusError err = (ClusError)m_Error.elementAt(i);
				err.addExample(tuple, stat);
			}
		} else {
			for (int i = 0; i < nb; i++) {
				ClusError err = (ClusError)m_Error.elementAt(i);
				err.addInvalid(tuple);
			}
		}
	}
	
	public void addExample() {
		m_NbExamples++;
		m_NbCover++;			
	}		
	
	public void add(ClusErrorParent par) {
		int nb = m_Error.size();
		for (int i = 0; i < nb; i++) {
			ClusError err = (ClusError)m_Error.elementAt(i);
			err.add(par.getError(i));
		}
		m_NbExamples += par.getNbExamples();
		m_NbCover += par.getNbCover();
	}
	
	public void updateFromGlobalMeasure(ClusErrorParent par) {
		int nb = m_Error.size();
		for (int i = 0; i < nb; i++) {
			ClusError err = (ClusError)m_Error.elementAt(i);
			err.updateFromGlobalMeasure(par.getError(i));
		}
		setNbTotal(par.getNbExamples());
	}	
	
	/*
	public double get_errorclassif(){
		System.out.println("le nomre d'exemple est"+getNbExamples());
		System.out.println("le nomre d'exemple couvert est"+getNbCover());
			return (getNbExamples()-getNbCover());
	}
	*/
	public double getErrorClassif(){
		ClusError err = getError(0);
		return err.get_error_classif();
	}
	
	public double getErrorAccuracy() {
		ClusError err = getError(0);
		return err.get_accuracy();
	}
	
	public double getErrorPrecision() {
		ClusError err = getError(0);
		return err.get_precision();
	}
	
	public double getErrorRecall() {
		ClusError err = getError(0);
		return err.get_recall();
	}
	
	public double getErrorAuc() {
		ClusError err = getError(0);
		return err.get_auc();
	}
	
	
	public void showError(PrintWriter out) {
		int nb = m_Error.size();
		out.println("Number of examples: "+getNbTotal()+" (covered: "+getNbCover()+")");
		for (int i = 0; i < nb; i++) {
			ClusError err1 = getError(i);
			out.print(err1.getName()+": ");
			err1.showModelError(out, ClusError.DETAIL_SMALL);
		}			
	}
	
	public static boolean checkCoverage(CRParent models, int type, int nb) {
		int nb_models = models.getNbModels();
		for (int j = 0; j < nb_models; j++) {
			ClusErrorParent parent = models.getModelInfo(j).getError(type);
			if (parent.getNbCover() != nb) return false;
		}
		return true;
	}
	
	public void showError(CRParent models, int type, PrintWriter out) {
		int nb = m_Error.size();
		ClusModelInfo definf = models.getModelInfo(ClusModels.DEFAULT);
		ClusErrorParent defpar = definf.getError(type);
		out.println("Number of examples: "+defpar.getNbExamples());
		int nb_models = models.getNbModels();
		if (!checkCoverage(models, type, defpar.getNbExamples())) {
			out.println("Coverage:");
			for (int j = 0; j < nb_models; j++) {
				ClusModelInfo inf = models.getModelInfo(j);
				if (inf.getModel() != null) {
					ClusErrorParent parent = inf.getError(type);		
					out.println("  "+inf.getName()+": "+parent.getNbCover());
				}
			}
		}
		for (int i = 0; i < nb; i++) {
			ClusError err1 = getError(i);
			out.println(err1.getName());			
			for (int j = 0; j < nb_models; j++) {
				ClusModelInfo inf = models.getModelInfo(j);
				ClusErrorParent parent = inf.getError(type);		
				ClusError err2 = parent.getError(i);
				if (inf.hasModel()) {
					if (err2.isMultiLine()) {
						out.print("   "+inf.getName()+": ");
					} else {
						out.print("   "+StringUtils.printStr(inf.getName(),15)+": ");
					}
					err2.showModelError(out, ClusError.DETAIL_SMALL);
				}
			}
		}			
	}
	
	public static void printExtraError(CRParent models, int type, PrintWriter out) {
		int ctr = 0;
		int nb_models = models.getNbModels();		
		for (int j = 0; j < nb_models; j++) {
			ClusModelInfo inf = models.getModelInfo(j);
			ClusErrorParent parent = inf.getExtraError(type);		
			if (parent != null && inf.hasModel()) {
				int nb_err = parent.getNbErrors();
				for (int i = 0; i < nb_err; i++) {
					out.print("   "+StringUtils.printStr(inf.getName(),15)+": ");
					ClusError err = parent.getError(i);
					err.showModelError(out, ClusError.DETAIL_SMALL);
					ctr++;
				}
			}
		}
		if (ctr != 0) out.println();
	}		
	
	public void showErrorBrief(CRParent models, int type, PrintWriter out) {
		int nb = m_Error.size();
		for (int i = 0; i < nb; i++) {
			ClusError err1 = getError(i);
			if (type == ClusModelInfo.TRAIN_ERR) out.print("Train ");
			else out.print("Test ");
			out.println(err1.getName());
			int nb_models = models.getNbModels();
			for (int j = 0; j < nb_models; j++) {
				ClusModelInfo inf = models.getModelInfo(j);
				ClusErrorParent parent = inf.getError(type);		
				ClusError err2 = parent.getError(i);
				out.print("   "+StringUtils.printStr(inf.getName(),15)+": ");
				err2.showModelError(out, ClusError.DETAIL_VERY_SMALL);
			}
		}			
	}
	
	public String getPrefix() {
		return "   ";
	}
	
	public int getNbExamples() {
		return m_NbExamples;
	}
	
	public int getNbCover() {
		return m_NbCover;
	}
	
	public NumberFormat getFormat() {
		return ClusFormat.FOUR_AFTER_DOT;
//		return NumberFormat.getInstance();
	}
}
