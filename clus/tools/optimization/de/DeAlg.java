/*
 * Created on 2006.3.29
 */
package clus.tools.optimization.de;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import clus.main.*;
import clus.data.type.*;
import clus.data.rows.*;
import clus.statistic.*;
import clus.util.ClusFormat;
import clus.algo.rules.*;


/**
 * Differential evolution algorithm.
 * 
 * @author Tea Tusar
 */
public class DeAlg {
	
	// private DeParams m_Params;                        
	private DeProbl m_Probl;    
	private DePop m_Pop;
	private DeInd m_Best;
	private ClusStatManager m_StatMgr;

  /**
   * Constructor for classification optimization
   * @param stat_mgr
   * @param rule_pred
   * @param true_val
   */
	public DeAlg(ClusStatManager stat_mgr, double[][][] rule_pred, double[] true_val) {
		m_StatMgr = stat_mgr;
		m_Probl = new DeProbl(stat_mgr, rule_pred, true_val);
		m_Pop = new DePop(stat_mgr, m_Probl);
		ClusStatistic tar_stat = m_StatMgr.getStatistic(ClusAttrType.ATTR_USE_TARGET);
		try {
			if (tar_stat.getNbAttributes() > 1) {
				throw new Exception("Not yet implemented: More than one target attribute!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

  /**
   * Constructor for regression optimization
   * @param stat_mgr
   * @param rule_pred
   * @param true_val
   */
	public DeAlg(ClusStatManager stat_mgr, double[][] rule_pred, double[] true_val) {
		m_StatMgr = stat_mgr;
		m_Probl = new DeProbl(stat_mgr, rule_pred, true_val);
		m_Pop = new DePop(stat_mgr, m_Probl);
		ClusStatistic tar_stat = m_StatMgr.getStatistic(ClusAttrType.ATTR_USE_TARGET);
		try {
			if (tar_stat.getNbAttributes() > 1) {
				throw new Exception("Not yet implemented: More than one target attribute!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList evolution() {
		int num_eval;
		System.out.print("\nOptimizing rule weights (" + getSettings().getOptDENumEval() + ") ");
		try {
			PrintWriter wrt_log = new PrintWriter(new OutputStreamWriter
                                (new FileOutputStream("evol.log")));
			// PrintWriter wrt_pop = new PrintWriter(new OutputStreamWriter
      //                           (new FileOutputStream("evol.pop")));
			m_Pop.createFirstPop();
			num_eval = m_Pop.evaluatePop(0);
			m_Best = new DeInd();
			m_Best.copy((DeInd)m_Pop.m_Inds.get(0));
			for (int i = 0; i < getSettings().getOptDEPopSize(); i++) {
				OutputLog((DeInd)m_Pop.m_Inds.get(i), i, wrt_log);
			}
			OutputPop();
			while (num_eval < getSettings().getOptDENumEval()) {
				System.out.print(".");
				m_Pop.sortPopRandom();
				DeInd candidate = new DeInd();
				for (int i = 0; i < getSettings().getOptDEPopSize(); i++) {
					candidate.setGenes(m_Pop.getCandidate(i));
					num_eval = candidate.evaluate(m_Probl, num_eval);
					OutputLog(candidate, num_eval, wrt_log);
					if (candidate.m_Fitness < ((DeInd)m_Pop.m_Inds.get(i)).m_Fitness) {
						((DeInd)m_Pop.m_Inds.get(i)).copy(candidate);
					}
				}
			}     
			wrt_log.close();
			// wrt_pop.close();
			System.out.println(" done!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return m_Best.getGenes();
	}
	
	public void OutputPop() throws IOException {
		/*for (int i = 0; i < m_params.m_pop_size; i++) {
			String tmp = ((DeInd)m_pop.m_inds.elementAt(i)).GetIndString();
			m_file_pop.write(tmp);
			m_file_pop.write('\n');
		}*/		
	}
	
	public void OutputLog(DeInd ind, int index, PrintWriter wrt) {
		NumberFormat fr = ClusFormat.SIX_AFTER_DOT;
		if (m_Best.m_Fitness > ind.m_Fitness) {
			m_Best.copy(ind);
		}
		wrt.print("" + fr.format(index));
		wrt.print("\t");
		wrt.print("" + fr.format(m_Best.m_Fitness));
		wrt.print("\t");
		wrt.print(ind.getIndString());
		wrt.print("\n");
	}

	public Settings getSettings() {
		return m_StatMgr.getSettings();
	}
	
}
