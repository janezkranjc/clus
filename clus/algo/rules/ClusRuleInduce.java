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

/* Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

import clus.algo.*;
import clus.algo.split.*;
import clus.algo.tdidt.*;
import clus.main.*;
import clus.model.*;
import clus.model.test.*;
import clus.heuristic.*;
import clus.selection.*;
import clus.statistic.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.ext.beamsearch.*;
import clus.util.*;
import clus.tools.optimization.de.*;

public class ClusRuleInduce extends ClusInductionAlgorithm {
	
	protected boolean m_BeamChanged;
	protected FindBestTestRules m_FindBestTest;
	protected ClusHeuristic m_Heuristic;
	
	public ClusRuleInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		super(schema, sett);
		m_FindBestTest = new FindBestTestRules(getStatManager());
	}
	
	void resetAll() {
		m_BeamChanged = false;
	}	
	
	public void setHeuristic(ClusHeuristic heur) {
		m_Heuristic = heur;
	}
		
	public double estimateBeamMeasure(ClusRule rule) {
		return m_Heuristic.calcHeuristic(null, rule.getClusteringStat(), null);
	}
	
	public boolean isBeamChanged() {
		return m_BeamChanged;
	}
	
	public void setBeamChanged(boolean change) {
		m_BeamChanged = change;
	}
	
	ClusBeam initializeBeam(RowData data) {
		Settings sett = getSettings();
		ClusBeam beam = new ClusBeam(sett.getBeamWidth(), sett.getBeamRemoveEqualHeur());
		ClusStatistic stat = createTotalClusteringStat(data);
		ClusRule rule = new ClusRule(getStatManager());
		rule.setClusteringStat(stat);
		rule.setVisitor(data);
		double value = estimateBeamMeasure(rule);
		beam.addModel(new ClusBeamModel(value, rule));		
		return beam;
	}
	
	public void refineModel(ClusBeamModel model, ClusBeam beam, int model_idx) {
		ClusRule rule = (ClusRule)model.getModel();
		RowData data = (RowData)rule.getVisitor();
		if (m_FindBestTest.initSelectorAndStopCrit(rule.getClusteringStat(), data)) {
			model.setFinished(true);
			return;			
		}
		CurrentBestTestAndHeuristic sel = m_FindBestTest.getBestTest();
		ClusAttrType[] attrs = data.getSchema().getDescriptiveAttributes();
		for (int i = 0; i < attrs.length; i++) {
			// if (Settings.VERBOSE > 1) System.out.print("\n    Ref.Attribute: " + attrs[i].getName() + ": ");
			sel.resetBestTest();
			double beam_min_value = beam.getMinValue();
			sel.setBestHeur(beam_min_value);
			ClusAttrType at = attrs[i];
			if (at instanceof NominalAttrType) m_FindBestTest.findNominal((NominalAttrType)at, data);
			else m_FindBestTest.findNumeric((NumericAttrType)at, data);
			if (sel.hasBestTest()) {
				NodeTest test = sel.updateTest();
				if (Settings.VERBOSE > 0) System.out.println("  Test: "+test.getString()+" -> "+sel.m_BestHeur);
				// RowData subset = data.applyWeighted(test, ClusNode.YES);
				RowData subset = data.apply(test, ClusNode.YES);
				ClusRule ref_rule = rule.cloneRule();
				ref_rule.addTest(test);
				ref_rule.setVisitor(subset);
				ref_rule.setClusteringStat(createTotalClusteringStat(subset));
				// if (Settings.VERBOSE > 0) System.out.println("  Sanity.check.val: " + sel.m_BestHeur);
				if (getSettings().isCompHeurRuleDist()) {
					int[] subset_idx = new int[subset.getNbRows()];
					for (int j = 0; j < subset_idx.length; j++) {
						subset_idx[j] = subset.getTuple(j).getIndex();
					}
					((ClusRuleHeuristicDispersion)m_Heuristic).setDataIndexes(subset_idx);
				}
				double new_heur = sanityCheck(sel.m_BestHeur, ref_rule);
				// if (Settings.VERBOSE > 0) System.out.println("  Sanity.check.exp: " + new_heur);
				// Check for sure if _strictly_ better!
				if (new_heur > beam_min_value) {
					ClusBeamModel new_model = new ClusBeamModel(new_heur, ref_rule);
					new_model.setParentModelIndex(model_idx);
					beam.addModel(new_model);
					setBeamChanged(true);
				}
			}
		}
	}
	
	public void refineBeam(ClusBeam beam) {
		setBeamChanged(false);
		ArrayList models = beam.toArray();
		for (int i = 0; i < models.size(); i++) {
			ClusBeamModel model = (ClusBeamModel)models.get(i);
			if (!(model.isRefined() || model.isFinished())) {
				// if (Settings.VERBOSE > 0) System.out.println("  Refine: model " + i);
				refineModel(model, beam, i);
				model.setRefined(true);
				model.setParentModelIndex(-1);
			}
		}	
	}
	
	public ClusRule learnOneRule(RowData data) {
		ClusBeam beam = initializeBeam(data);
		int i = 0;
		System.out.print("Step: ");
		while (true) {
			if (Settings.VERBOSE > 0) {
				System.out.println("Step: " + i);
			} else {
				if (i != 0) {
					System.out.print(",");
				}
				System.out.print(i);
			}
			System.out.flush();
			refineBeam(beam);
			if (!isBeamChanged()) {
				break;
			}
			i++;
		}
		System.out.println();
		double best = beam.getBestModel().getValue();
		double worst = beam.getWorstModel().getValue();		
		System.out.println("Worst = "+worst+" Best = "+best);
		ClusRule result = (ClusRule)beam.getBestAndSmallestModel().getModel();
		// Create target statistic for rule
		RowData rule_data = (RowData)result.getVisitor();
		result.setTargetStat(createTotalTargetStat(rule_data));
		result.setVisitor(null);
		return result;
	}
	
	public ClusRule learnEmptyRule(RowData data) {
		ClusRule result = new ClusRule(getStatManager());
		// Create target statistic for rule
		// RowData rule_data = (RowData)result.getVisitor();
		// result.setTargetStat(m_Induce.createTotalTargetStat(rule_data));
		// result.setVisitor(null);
		return result;
	}
	
	/**
	 * Returns all the rules in the beam, not just the best one.
	 * @param data
	 * @return array of rules
	 */
	public ClusRule[] learnBeamOfRules(RowData data) {
		ClusBeam beam = initializeBeam(data);
		int i = 0;
		System.out.print("Step: ");
		while (true) {
			if (Settings.VERBOSE > 0) {
				System.out.println("Step: " + i);
			} else {
				if (i != 0) {
					System.out.print(",");
				}
				System.out.print(i);
			}
			System.out.flush();
			refineBeam(beam);
			if (!isBeamChanged()) {
				break;
			}
			i++;
		}
		System.out.println();
		double best = beam.getBestModel().getValue();
		double worst = beam.getWorstModel().getValue();		
		System.out.println("Worst = "+worst+" Best = "+best);
		ArrayList beam_models = beam.toArray();
		ClusRule[] result = new ClusRule[beam_models.size()];
		for (int j = 0; j < beam_models.size(); j++) {
			// Put better models first
			int k = beam_models.size()-j-1;
			ClusRule rule = (ClusRule)((ClusBeamModel)beam_models.get(k)).getModel();
			// Create target statistic for this rule
			RowData rule_data = (RowData)rule.getVisitor();
			rule.setTargetStat(createTotalTargetStat(rule_data));
			rule.setVisitor(null);
			rule.simplify();
			result[j] = rule;
		}
		return result;
	}
	
	public void separateAndConquor(ClusRuleSet rset, RowData data) {
		while (data.getNbRows() > 0) {
			ClusRule rule = learnOneRule(data);
			if (rule.isEmpty()) {
				break;
			} else {
				rule.computePrediction();
				rule.printModel();
				System.out.println();
				rset.add(rule);
				data = rule.removeCovered(data);
			}
		}
		ClusStatistic left_over = createTotalTargetStat(data);
		left_over.calcMean();
		System.out.println("Left Over: "+left_over);
		rset.setTargetStat(left_over);
	}
	
	/**
	 * separateAndConquor method which uses reweighting
	 * @param rset
	 * @param data
	 * @throws ClusException 
	 */
	public void separateAndConquorWeighted(ClusRuleSet rset, RowData data) throws ClusException {
		int max_rules = getSettings().getMaxRulesNb();
		int i = 0;
		RowData data_copy = (RowData)data.deepCloneData(); // Probably not nice
		ArrayList bit_vect_array = new ArrayList();
		while ((data.getNbRows() > 0) && (i < max_rules)) {
			ClusRule rule = learnOneRule(data);
			if (rule.isEmpty()) {
				break;
			} else {
				rule.computePrediction();
				rule.printModel();
				System.out.println();
				rset.add(rule);
				data = rule.reweighCovered(data);
				i++;
				if (getSettings().isCompHeurRuleDist()) {
					boolean[] bit_vect = new boolean[data_copy.getNbRows()];
					for (int j = 0; j < bit_vect.length; j++) {
						if (!bit_vect[j]) {
							for (int k = 0; k < rset.getModelSize(); k++) {
								if (rset.getRule(k).covers(data_copy.getTuple(j))) {
									bit_vect[j] = true;
									break;									
								}
							}
						}
					}
					bit_vect_array.add(bit_vect);
					((ClusRuleHeuristicDispersion)m_Heuristic).setCoveredBitVectArray(bit_vect_array);
				}
			}
		}
		ClusStatistic left_over = createTotalTargetStat(data);
		left_over.calcMean();
		System.out.println("Left Over: "+left_over);
		rset.setTargetStat(left_over);
	}
	
	/** Modified separateAndConquorWeighted method: Adds a rule to the rule set
	 * only if it improves the rule set performance
	 * @param rset
	 * @param data
	 */ 
	public void separateAndConquorAddRulesIfBetter(ClusRuleSet rset, RowData data) throws ClusException {
		int max_rules = getSettings().getMaxRulesNb();
		int i = 0;
		RowData data_copy = (RowData)data.deepCloneData();
		ArrayList bit_vect_array = new ArrayList();
		ClusStatistic left_over = createTotalTargetStat(data_copy);
		left_over.calcMean();
		ClusStatistic new_left_over = left_over;
		rset.setTargetStat(left_over);
		double err_score = rset.computeErrorScore(data_copy);
		while ((data.getNbRows() > 0) && (i < max_rules)) {
			ClusRule rule = learnOneRule(data);
			if (rule.isEmpty()) {
				break;
			} else {
				rule.computePrediction();
				ClusRuleSet new_rset = rset.cloneRuleSet();
				new_rset.add(rule);
				data = rule.reweighCovered(data);
				left_over = new_left_over;
				new_left_over = createTotalTargetStat(data);
				new_left_over.calcMean();
				new_rset.setTargetStat(new_left_over);
				double new_err_score = new_rset.computeErrorScore(data_copy);
				if ((err_score - new_err_score) > 1e-6) {
					i++;
					rule.printModel();
					System.out.println();
					err_score = new_err_score;
					rset.add(rule);
					if (getSettings().isCompHeurRuleDist()) {
						boolean[] bit_vect = new boolean[data_copy.getNbRows()];
						for (int j = 0; j < bit_vect.length; j++) {
							if (!bit_vect[j]) {
								for (int k = 0; k < rset.getModelSize(); k++) {
									if (rset.getRule(k).covers(data_copy.getTuple(j))) {
										bit_vect[j] = true;
										break;									
									}
								}
							}
						}
						bit_vect_array.add(bit_vect);
						((ClusRuleHeuristicDispersion)m_Heuristic).setCoveredBitVectArray(bit_vect_array);
					}
				} else {
					break;
				}
			}
		}
		System.out.println("Left Over: "+left_over);
		rset.setTargetStat(left_over);
	}

	/** Modified separateAndConquorWeighted method: Adds a rule to the rule set
	 * only if it improves the rule set performance or if they cover default class.
	 * If not, it checks other rules in the beam.
	 * @param rset
	 * @param data
	 */ 
	public void separateAndConquorAddRulesIfBetterFromBeam(ClusRuleSet rset, RowData data) throws ClusException {
		int max_rules = getSettings().getMaxRulesNb();
		int i = 0;
		RowData data_copy = (RowData)data.deepCloneData();
		ArrayList bit_vect_array = new ArrayList();
		ClusStatistic left_over = createTotalTargetStat(data);
		ClusStatistic new_left_over = left_over;
		left_over.calcMean();
		rset.setTargetStat(left_over);
		int nb_tar = left_over.getNbAttributes();
		boolean cls_task = false;
		if (left_over instanceof ClassificationStat) {
			cls_task = true;
		}
		int[] def_maj_class = new int[nb_tar];
		if (cls_task) {
			for (int t = 0; t < nb_tar; t++) {
				def_maj_class[t] = left_over.getNominalPred()[t];
			}
		}
		double err_score = rset.computeErrorScore(data);
		while ((data_copy.getNbRows() > 0) && (i < max_rules)) {
			ClusRule[] rules = learnBeamOfRules(data_copy);
			left_over = new_left_over;
			int rule_added = -1;
			// Check all rules in the beam
			for (int j = 0; j < rules.length; j++) {
				if (rules[j].isEmpty()) {
					continue;
				} else {
					rules[j].computePrediction();
					ClusRuleSet new_rset = rset.cloneRuleSet();
					new_rset.add(rules[j]);
					RowData data_copy2 = (RowData)data_copy.deepCloneData();
					data_copy2 = rules[j].reweighCovered(data_copy2);
					ClusStatistic new_left_over2 = createTotalTargetStat(data_copy2);
					new_left_over2.calcMean();
					new_rset.setTargetStat(new_left_over2);
					double new_err_score = new_rset.computeErrorScore(data);
					// Add the rule anyway if classifies to the default class
					boolean add_anyway = false;
					if (cls_task) {
						for (int t = 0; t < nb_tar; t++) {
							if (def_maj_class[t] == rules[j].getTargetStat().getNominalPred()[t]) {
								add_anyway = true;
							}
						}
					}
					if (((err_score - new_err_score) > 1e-6) || add_anyway) {
						err_score = new_err_score;
						rule_added = j;
						data_copy = data_copy2;
						new_left_over = new_left_over2;
					}
				}
			}
			if (rule_added != -1) {
				i++;
				rules[rule_added].printModel();
				System.out.println();
				rset.add(rules[rule_added]);
				if (getSettings().isCompHeurRuleDist()) {
					boolean[] bit_vect = new boolean[data.getNbRows()];
					for (int j = 0; j < bit_vect.length; j++) {
						if (!bit_vect[j]) {
							for (int k = 0; k < rset.getModelSize(); k++) {
								if (rset.getRule(k).covers(data.getTuple(j))) {
									bit_vect[j] = true;
									break;									
								}
							}
						}
					}
					bit_vect_array.add(bit_vect);
					((ClusRuleHeuristicDispersion)m_Heuristic).setCoveredBitVectArray(bit_vect_array);
				}
			} else {
				break;
			}
		}
		System.out.println("Left Over: "+left_over);
		rset.setTargetStat(left_over);
	}
	
	/**
	 * separateAndConquor method that induces rules on several bootstraped data subsets
	 * @param rset
	 * @param data
	 * @throws ClusException 
	 */
	public void separateAndConquorBootstraped(ClusRuleSet rset, RowData data) throws ClusException {
		int nb_sets = 10; // TODO: parameter?
		int nb_rows = data.getNbRows();
		int max_rules = getSettings().getMaxRulesNb();
		max_rules /= nb_sets;
		RowData data_not_covered = (RowData)data.cloneData();
		for (int z = 0; z < nb_sets; z++) {
			// Select the data using bootstrap
			RowData data_sel = (RowData)data.cloneData();
			BaggingSelection msel = new BaggingSelection(nb_rows);
			data_sel.update(msel);
			// Reset tuple indexes used in heuristic
			if (getSettings().isCompHeurRuleDist()) {
				int[] data_idx = new int[data_sel.getNbRows()];
				for (int j = 0; j < data_sel.getNbRows(); j++) {
					data_sel.getTuple(j).setIndex(j);
					data_idx[j] = j;
				}
				((ClusRuleHeuristicDispersion)m_Heuristic).setDataIndexes(data_idx);
				((ClusRuleHeuristicDispersion)m_Heuristic).initCoveredBitVectArray(data_sel.getNbRows());
			}
			// Induce the rules
			int i = 0;
			RowData data_sel_copy = (RowData)data_sel.cloneData(); // No need for deep clone here
			ArrayList bit_vect_array = new ArrayList();
			while ((data_sel.getNbRows() > 0) && (i < max_rules)) {
				ClusRule rule = learnOneRule(data_sel);
				if (rule.isEmpty()) {
					break;
				} else {
					rule.computePrediction();
					rule.printModel();
					System.out.println();
					rset.addIfUnique(rule);
					data_sel = rule.removeCovered(data_sel);
					data_not_covered = rule.removeCovered(data_not_covered);
					i++;
					if (getSettings().isCompHeurRuleDist()) {
						boolean[] bit_vect = new boolean[data_sel_copy.getNbRows()];
						for (int j = 0; j < bit_vect.length; j++) {
							if (!bit_vect[j]) {
								for (int k = 0; k < rset.getModelSize(); k++) {
									if (rset.getRule(k).covers(data_sel_copy.getTuple(j))) {
										bit_vect[j] = true;
										break;									
									}
								}
							}
						}
						bit_vect_array.add(bit_vect);
						((ClusRuleHeuristicDispersion)m_Heuristic).setCoveredBitVectArray(bit_vect_array);
					}
				}
			}
		}
		ClusStatistic left_over = createTotalTargetStat(data_not_covered);
		left_over.calcMean();
		System.out.println("Left Over: "+left_over);
		rset.setTargetStat(left_over);
	}
	
	/** Evaluates each rule in the context of a complete rule set.
	 *  Individual rules are generated randomly.
	 * @param rset
	 * @param data
	 */ 
	public void separateAndConquorRandomly(ClusRuleSet rset, RowData data) throws ClusException {
    int nb_rules = 100; // TODO: parameter?
    int max_def_rules = 10; // TODO: parameter?
		ClusRule[] rules = new ClusRule[nb_rules];
		Random rn = new Random(42);
		for (int k = 0; k < nb_rules; k++) {
			ClusRule rule = generateOneRandomRule(data,rn);
			rule.computePrediction();
			rules[k] = rule;
		}
		int max_rules = getSettings().getMaxRulesNb();
		int i = 0;
		RowData data_copy = (RowData)data.deepCloneData();
		ClusStatistic left_over = createTotalTargetStat(data);
		ClusStatistic new_left_over = left_over;
		left_over.calcMean();
		rset.setTargetStat(left_over);
		int nb_tar = left_over.getNbAttributes();
		boolean cls_task = false;
		if (left_over instanceof ClassificationStat) {
			cls_task = true;
		}
		int[] def_maj_class = new int[nb_tar];
		if (cls_task) {
			for (int t = 0; t < nb_tar; t++) {
				def_maj_class[t] = left_over.getNominalPred()[t];
			}
		}
		double err_score = rset.computeErrorScore(data);
		int nb_def_rules = 0;
		boolean add_anyway = false;
		while (i < max_rules) {
			left_over = new_left_over;
			int rule_added = -1;
			// Check all random rules
			for (int j = 0; j < rules.length; j++) {
				if ((rules[j] == null) || (rules[j].isEmpty())) {
					continue;
				} else {
					rules[j].computePrediction();
					ClusRuleSet new_rset = rset.cloneRuleSet();
					new_rset.add(rules[j]);
					RowData data_copy2 = (RowData)data_copy.deepCloneData();
					data_copy2 = rules[j].reweighCovered(data_copy2);
					ClusStatistic new_left_over2 = createTotalTargetStat(data_copy2);
					new_left_over2.calcMean();
					new_rset.setTargetStat(new_left_over2);
					double new_err_score = new_rset.computeErrorScore(data);
					// Add the rule anyway if classifies to the default class
					add_anyway = false;
					if (cls_task) {
						for (int t = 0; t < nb_tar; t++) {
							if (def_maj_class[t] == rules[j].getTargetStat().getNominalPred()[t]) {
								add_anyway = true;
							}
						}
					}
					double err_d = err_score - new_err_score;
					if ((err_d > 1e-6) || (nb_def_rules < max_def_rules)) {
						if (add_anyway) {
							nb_def_rules++;
						}
						err_score = new_err_score;
						rule_added = j;
//						System.err.println(err_score + " " + add_anyway + " " + j + " " + err_d);
						data_copy = data_copy2;
						new_left_over = new_left_over2;
					}
				}
			}
			if (rule_added != -1) {
				i++;
				rules[rule_added].printModel();
				System.out.println();
				rset.addIfUnique(rules[rule_added]);
				rules[rule_added] = null;
			} else {
				break;
			}
		}
		System.out.println("Left Over: "+left_over);
		rset.setTargetStat(left_over);
	}
	
	/** Modified separateAndConquorWeighted method: evaluates each rule in
	 *  the context of a complete rule set, it builds the default rule first.
	 *  If first learned rule is no good, it checks next rules in the beam.
	 * @param rset
	 * @param data
	 */ 
	public void separateAndConquorAddRulesIfBetterFromBeam2(ClusRuleSet rset, RowData data) throws ClusException {
		int max_rules = getSettings().getMaxRulesNb();
		int i = 0;
		RowData data_copy = (RowData)data.deepCloneData();
		ClusStatistic left_over = createTotalTargetStat(data);
		// ClusStatistic new_left_over = left_over;
		left_over.calcMean();
		rset.setTargetStat(left_over);
		ClusRule empty_rule = learnEmptyRule(data_copy);
		empty_rule.setTargetStat(left_over);
		data_copy = empty_rule.reweighCovered(data_copy);
		double err_score = rset.computeErrorScore(data);
		while ((data.getNbRows() > 0) && (i < max_rules)) {
			ClusRule[] rules = learnBeamOfRules(data_copy);
			// left_over = new_left_over;
			int rule_added = -1;
			// Check all rules in the beam
			for (int j = 0; j < rules.length; j++) {
				if (rules[j].isEmpty()) {
					continue;
				} else {
					rules[j].computePrediction();
					ClusRuleSet new_rset = rset.cloneRuleSet();
					new_rset.add(rules[j]);
					RowData data_copy2 = (RowData)data_copy.deepCloneData();
					data_copy2 = rules[j].reweighCovered(data_copy2);
					// ClusStatistic new_left_over2 = m_Induce.createTotalTargetStat(data_copy2);
					// new_left_over2.calcMean();
					// new_rset.setTargetStat(new_left_over2);
					new_rset.setTargetStat(left_over);
					double new_err_score = new_rset.computeErrorScore(data);
					if ((err_score - new_err_score) > 1e-6) {
						err_score = new_err_score;
						rule_added = j;
						data_copy = data_copy2;
						// new_left_over = new_left_over2;
					}
				}
			}
			if (rule_added != -1) {
				i++;
				rules[rule_added].printModel();
				System.out.println();
				rset.add(rules[rule_added]);
			} else {
				break;
			}
		}
		System.out.println("Left Over: "+left_over);
		// rset.setTargetStat(left_over);
	}

	public void separateAndConquorWithHeuristic(ClusRuleSet rset, RowData data) {
		int max_rules = getSettings().getMaxRulesNb();
		ArrayList bit_vect_array = new ArrayList();
		int i = 0;
		/*
		getSettings().setCompHeurRuleDistPar(0.0);
		while (i < max_rules) {
			ClusRule rule = learnOneRule(data);
			if (rule.isEmpty()) {
				break;
			} else if (!rset.unique(rule)) {
				i++;
				double val = getSettings().getCompHeurRuleDistPar();
				val += 1;
				getSettings().setCompHeurRuleDistPar(val);
				continue;
			} else {
				getSettings().setCompHeurRuleDistPar(1.0); */
		while (i < max_rules) {
			/*
			ClusRule rule = learnOneRule(data);
			if (rule.isEmpty() || !rset.unique(rule)) {
				break; */
			ClusRule[] rules = learnBeamOfRules(data);
			ClusRule rule = rules[0];
			for (int l = 0; l < rules.length-1; l++) {
				rule = rules[l+1]; 
				if (rset.unique(rule)) {
					break;
				}
			}
			if (rule.isEmpty() || !rset.unique(rule)) {
				break;
			} else {
				rule.computePrediction();
				rule.printModel();
				System.out.println();
				rset.add(rule);
				i++;
				boolean[] bit_vect = new boolean[data.getNbRows()];
				for (int j = 0; j < bit_vect.length; j++) {
					if (!bit_vect[j]) {
						for (int k = 0; k < rset.getModelSize(); k++) {
							if (rset.getRule(k).covers(data.getTuple(j))) {
								bit_vect[j] = true;
								break;									
							}
						}
					}
				}
  			bit_vect_array.add(bit_vect);
	  		((ClusRuleHeuristicDispersion)m_Heuristic).setCoveredBitVectArray(bit_vect_array);
		  }
		}
		updateDefaultRule(rset, data);
	}
	
	public double sanityCheck(double value, ClusRule rule) {
		double expected = estimateBeamMeasure(rule);
		if (Math.abs(value-expected) > 1e-6) {
			System.out.println("Bug in heurisitc: "+value+" <> "+expected);
			PrintWriter wrt = new PrintWriter(System.out);
			rule.printModel(wrt);
			wrt.close();
			System.out.flush();
			System.exit(1);
		}
		return expected;
	}
	
	public ClusModel induce(ClusRun run) throws ClusException, IOException {
		int method = getSettings().getCoveringMethod();
		int add_method = getSettings().getRuleAddingMethod();
		RowData data = (RowData)run.getTrainingSet();
		ClusStatistic stat = createTotalClusteringStat(data);
		m_FindBestTest.initSelectorAndSplit(stat);
		setHeuristic(m_FindBestTest.getBestTest().getHeuristic());
		if (getSettings().isCompHeurRuleDist()) {
			int[] data_idx = new int[data.getNbRows()];
			for (int i = 0; i < data.getNbRows(); i++) {
				data.getTuple(i).setIndex(i);
				data_idx[i] = i;
			}
			((ClusRuleHeuristicDispersion)m_Heuristic).setDataIndexes(data_idx);
			((ClusRuleHeuristicDispersion)m_Heuristic).initCoveredBitVectArray(data.getNbRows());
		}
		ClusRuleSet rset = new ClusRuleSet(getStatManager());
		if (method == Settings.COVERING_METHOD_STANDARD) {
			separateAndConquor(rset, data);
		} else if (method == Settings.COVERING_METHOD_BEAM_RULE_DEF_SET) { // Obsolete!
			separateAndConquorAddRulesIfBetterFromBeam2(rset, data);
		} else if (method == Settings.COVERING_METHOD_RANDOM_RULE_SET) {
			separateAndConquorRandomly(rset, data);
		} else if (method == Settings.COVERING_METHOD_STANDARD_BOOTSTRAP) {
			separateAndConquorBootstraped(rset, data);
		} else if (method == Settings.COVERING_METHOD_HEURISTIC_ONLY) {
			separateAndConquorWithHeuristic(rset, data);
		} else if (add_method == Settings.RULE_ADDING_METHOD_IF_BETTER) {
			separateAndConquorAddRulesIfBetter(rset, data);
		} else if (add_method == Settings.RULE_ADDING_METHOD_IF_BETTER_BEAM) {
			separateAndConquorAddRulesIfBetterFromBeam(rset, data);
		} else {
			separateAndConquorWeighted(rset, data);
		}
		rset.postProc();
		// Optimizing rule set
		if (getSettings().getRulePredictionMethod() == Settings.RULE_PREDICTION_METHOD_OPTIMIZED) {
			rset = optimizeRuleSet(rset, data);
		}
		// Computing compactness
		if (getSettings().computeCompactness()) {
			rset.addDataToRules(data);
			rset.computeCompactness(ClusModel.TRAIN);
			rset.removeDataFromRules();
			if (run.getTestIter() != null) {
				RowData testdata = (RowData)run.getTestSet();
				rset.addDataToRules(testdata);
				rset.computeCompactness(ClusModel.TEST);
				rset.removeDataFromRules();
			}
		}
		// Number rules (for output prupose in WritePredictions)
		rset.numberRules();
		return rset;
	}

	public ClusRuleSet optimizeRuleSet(ClusRuleSet rset, RowData data) throws ClusException, IOException {
		String fname = getSettings().getDataFile();
		PrintWriter wrt_pred = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname+".r-pred")));
		DecimalFormat mf = new DecimalFormat("###.000");
		// Generate optimization input
		//ClusStatistic tar_stat = rset.m_StatManager.getStatistic(ClusAttrType.ATTR_USE_TARGET);
		DeAlg deAlg;
		//int nb_tar = tar_stat.getNbAttributes();
		int nb_rules = rset.getModelSize();
		int nb_rows = data.getNbRows();
		boolean classification = false;
		if (rset.m_TargetStat instanceof ClassificationStat) {
			classification = true;
		}
		// TODO: more target atts
		if (classification) {
			ClusSchema schema = data.getSchema();
			NominalAttrType[] target = schema.getNominalAttrUse(ClusAttrType.ATTR_USE_TARGET);
			int nb_values = ((ClassificationStat)rset.m_TargetStat).getAttribute(0).getNbValues(); 
			double[][][] rule_pred = new double[nb_rows][nb_rules][nb_values]; // [instance][rule][class_value]
			double[] true_val = new double[nb_rows];
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				for (int j = 0; j < nb_rules; j++) {
					ClusRule rule = rset.getRule(j);
					if (rule.covers(tuple)) {
						rule_pred[i][j] = (double[])((ClassificationStat)rule.predictWeighted(tuple)).
						getClassCounts(0); // TODO: 
					} else {
						for (int k = 0; k < nb_values; k++) {
							rule_pred[i][j][k] = Double.NaN;
						}
					}
					wrt_pred.print(", [");
					for (int k = 0; k < nb_values; k++) {
						wrt_pred.print(", " + mf.format(rule_pred[i][j][k]));
					}
					wrt_pred.print("]");
				}
				//true_val[i] = (double)tuple.getIntVal(0); // TODO: 
				true_val[i] = target[0].getNominal(tuple);
				wrt_pred.print(" :: " + mf.format(true_val[i]) + "\n");
			}
			wrt_pred.close();
			deAlg = new DeAlg(getStatManager(), rule_pred, true_val);
		} else { // regression
			double[][] rule_pred = new double[nb_rows][nb_rules]; // [instance][rule]
			double[] true_val = new double[nb_rows];
			for (int i = 0; i < nb_rows; i++) {
				DataTuple tuple = data.getTuple(i);
				for (int j = 0; j < nb_rules; j++) {
					ClusRule rule = rset.getRule(j);
					if (rule.covers(tuple)) {
						rule_pred[i][j] = ((RegressionStat)rule.predictWeighted(tuple)).getNumericPred()[0]; // TODO:
					} else {
						rule_pred[i][j] = Double.NaN;
					}
					wrt_pred.print(", " + mf.format(rule_pred[i][j]));
				}
				true_val[i] = tuple.getDoubleVal(0);
				wrt_pred.print(" :: " + mf.format(true_val[i]) + "\n");
			}
			wrt_pred.close();
			deAlg = new DeAlg(getStatManager(), rule_pred, true_val);
		}
		ArrayList weights = deAlg.evolution();
		for (int j = 0; j < nb_rules; j++) {
			rset.getRule(j).setOptWeight(((Double)weights.get(j)).doubleValue());
		}
		rset.removeLowWeightRules();
		RowData data_copy = (RowData)data.cloneData(); 
		updateDefaultRule(rset, data_copy);
		// TODO: Should I update all the rules also, rerun the optimization?
		return rset;
	}
	
/*			try {
				// Generate pathseeker input
				ClusStatistic tar_stat = rset.m_StatManager.getStatistic(ClusAttrType.ATTR_USE_TARGET);
				int nb_tar = tar_stat.getNbNominalAttributes();
				boolean classification = false;
				if (rset.m_TargetStat instanceof ClassificationStat) {
					classification = true;
				}
				String fname = getSettings().getDataFile();
				PrintWriter wrt_pred = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname+".pred.dat")));
				PrintWriter wrt_resp = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname+".resp.dat")));
				PrintWriter wrt_train = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname+".train.txt")));
				for (int i = 0; i < data.getNbRows(); i++) {
					DataTuple tuple = data.getTuple(i);
					for (int j = 0; j < rset.getModelSize(); j++) {
						ClusRule rule = rset.getRule(j);
						if (rule.covers(tuple)) {
							if (classification) {
								// TODO: Don't look just at the first target attribute!
								wrt_pred.write("" + ((ClassificationStat)rule.predictWeighted(tuple)).
										                 getNominalPred()[0]);
							} else {
								// TODO: Don't look just at the first target attribute!
								wrt_pred.write("" + ((RegressionStat)rule.predictWeighted(tuple)).
										                 getNumericPred()[0]);
							}
						} 
						if ((j+1) < rset.getModelSize()) {
							wrt_pred.write(",");
						}
					}
					wrt_pred.println();
					if (classification) {
						// TODO: Don't look just at the first target attribute!
						wrt_resp.print("" + tuple.getIntVal(0));
						if ((i+1) < data.getNbRows()) {
							wrt_resp.write(",");
						}
					} else {
						// TODO: Don't look just at the first target attribute!
						wrt_resp.print("" + tuple.getDoubleVal(0));
						if ((i+1) < data.getNbRows()) {
							wrt_resp.write(",");
						}
					}
				}
				wrt_resp.println();
				if (classification) {
					wrt_train.println("@mode=class");
				} else {
					wrt_train.println("@mode=regres");
				}
				wrt_train.println("@model_file=" + fname + ".model.pth");
				wrt_train.println("@coeffs_file=" + fname + ".coeffs.pth");
				wrt_train.println("@nvar=" + rset.getModelSize());
				wrt_train.println("@nobs=" + data.getNbRows());
				wrt_train.println("@format=csv");
				wrt_train.println("@response_data=" + fname + ".resp.dat");
				wrt_train.println("@pred_data=" + fname + ".pred.dat");
				wrt_train.println("@org=by_obs");
				wrt_train.println("@missing=9.9e35");
				wrt_train.println("@obs_weights=equal");
				wrt_train.println("@var_weights=equal");
				wrt_train.println("@quantile=0.025 ");
				wrt_train.println("@numspect=0");
				wrt_train.println("@constraints=all");
				wrt_train.println("@nfold=3");
				wrt_train.println("@start=0.0");
				wrt_train.println("@end=1.0");
				wrt_train.println("@numval=6");
				wrt_train.println("@alpha=0.8");
				wrt_train.println("@modsel=a_roc");
				wrt_train.println("@delnu=0.01");
				wrt_train.println("@maxstep=20000");
				wrt_train.println("@kfreq=100");
				wrt_train.println("@convfac=1.1");
				wrt_train.println("@fast=no");
				wrt_train.println("@impl=auto");
				wrt_train.println();
				wrt_pred.close();
				wrt_resp.close();
				wrt_train.close();
				// Run pathseeker
				// Read pathseeker weights
			} catch (Exception e) {
				// TODO: handle exception
			}
*/

	public void updateDefaultRule(ClusRuleSet rset, RowData data) {
		for (int i = 0; i < rset.getModelSize(); i++) {
			data = rset.getRule(i).removeCovered(data);
		}
		ClusStatistic left_over = createTotalTargetStat(data);
		left_over.calcMean();
		System.out.println("Left Over: "+left_over);
		rset.setTargetStat(left_over);
	}
	
	/**
	 * Method that induces a specified number of random rules.
	 * @param cr ClusRun
	 * @return RuleSet
	 */
	public ClusModel induceRandomly(ClusRun run) throws ClusException, IOException {
		int number = getSettings().nbRandomRules();
		RowData data = (RowData)run.getTrainingSet();
		ClusStatistic stat = createTotalClusteringStat(data);
		m_FindBestTest.initSelectorAndSplit(stat);
		setHeuristic(m_FindBestTest.getBestTest().getHeuristic()); // ??? 
		ClusRuleSet rset = new ClusRuleSet(getStatManager());
		Random rn = new Random(42);
		for (int i = 0; i < number; i++) {
			ClusRule rule = generateOneRandomRule(data,rn);
			rule.computePrediction();
			rule.printModel();
			System.out.println();
			if (!rset.addIfUnique(rule)) {
				i--;
			}
		}
		ClusStatistic left_over = createTotalTargetStat(data);
		left_over.calcMean();
		System.out.println("Left Over: "+left_over);
		rset.setTargetStat(left_over);
		rset.postProc();
		// Computing compactness
		if (getSettings().computeCompactness()) {
			rset.addDataToRules(data);
			rset.computeCompactness(ClusModel.TRAIN);
			rset.removeDataFromRules();
			if (run.getTestIter() != null) {
				RowData testdata = (RowData)run.getTestSet();
				rset.addDataToRules(testdata);
				rset.computeCompactness(ClusModel.TEST);
				rset.removeDataFromRules();
			}
		}
		return rset;
	}
	
	/** 
	 * Generates one random rule.
	 * @param data
	 * @param rn
	 * @return
	 */
	private ClusRule generateOneRandomRule(RowData data, Random rn) {
		// TODO: Remove/change the beam stuff!!! 
		// Jans: Removed beam stuff (because was more difficult to debug)
		ClusStatManager mgr = getStatManager();    
		ClusRule result = new ClusRule(mgr);
		ClusAttrType[] attrs = data.getSchema().getDescriptiveAttributes();
		// Pointer to the complete data set
		RowData orig_data = data;
		// Generate number of tests
		int nb_tests;
		if (attrs.length > 1) {
			nb_tests = rn.nextInt(attrs.length - 1) + 1;
		} else {
			nb_tests = 1;
		}
		// Generate attributes in these tests
		int[] test_atts = new int[nb_tests];
		for (int i = 0; i < nb_tests; i++) {
			while (true) {
				int att_idx = rn.nextInt(attrs.length);
				boolean unique = true;
				for (int j = 0; j < i; j++) {
					if (att_idx == test_atts[j]) {
						unique = false;
					}
				}
				if (unique) {
					test_atts[i] = att_idx;
					break;
				}
			}
		}
		CurrentBestTestAndHeuristic sel = m_FindBestTest.getBestTest();
		for (int i = 0; i < test_atts.length; i++) {
			result.setClusteringStat(createTotalClusteringStat(data));
			if (m_FindBestTest.initSelectorAndStopCrit(result.getClusteringStat(), data)) {
				// Do not add test if stop criterion succeeds (???)
				break;			
			}
			sel.resetBestTest();
			sel.setBestHeur(Double.NEGATIVE_INFINITY);
			ClusAttrType at = attrs[test_atts[i]];
			if (at instanceof NominalAttrType) {
				m_FindBestTest.findNominalRandom((NominalAttrType)at, data, rn);
			} else {
				m_FindBestTest.findNumericRandom((NumericAttrType)at, data, orig_data, rn);
			}
			if (sel.hasBestTest()) {
				NodeTest test = sel.updateTest();
				if (Settings.VERBOSE > 0) System.out.println("  Test: "+test.getString()+" -> "+sel.m_BestHeur);
				result.addTest(test);
				// data = data.applyWeighted(test, ClusNode.YES);
				data = data.apply(test, ClusNode.YES);           // ???
			}
		}
		// Create target and clustering statistic for rule
		result.setTargetStat(createTotalTargetStat(data));
		result.setClusteringStat(createTotalClusteringStat(data));
		return result;
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		// ClusRulesForAttrs rfa = new ClusRulesForAttrs();
		// return rfa.constructRules(cr);
		resetAll();
		if (!getSettings().isRandomRules()) {
			return induce(cr);
		} else {
			return induceRandomly(cr);
		}
	}
	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		ClusModel model = induceSingleUnpruned(cr);
		// FIXME: implement cloneModel();
		// cr.getModelInfo(ClusModels.ORIGINAL).setModel(model);
		// ClusModel pruned = model.cloneModel();
		ClusModel pruned = model;
		ClusModelInfo pruned_model = cr.addModelInfo(ClusModel.PRUNED);
		pruned_model.setModel(pruned);
		pruned_model.setName("Pruned");
	}
}
