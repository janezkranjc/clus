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

package clus.algo.tdidt;

import jeans.tree.*;
import jeans.util.*;
import jeans.util.compound.*;

import java.util.*;
import java.io.*;

import weka.classifiers.trees.j48.NoSplit;
import weka.core.Utils;

import clus.util.*;
import clus.main.ClusModel;
import clus.main.ClusModelInfo;
import clus.main.ClusModelProcessor;
import clus.main.ClusRun;
import clus.main.ClusStatManager;
import clus.main.Global;
import clus.main.Settings;
import clus.model.test.*;
import clus.statistic.*;
import clus.data.rows.*;
import clus.data.attweights.*;
import clus.error.multiscore.*;

public class ClusNode extends MyNode implements ClusModel {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public final static int YES = 0;
	public final static int NO = 1;
	public final static int UNK = 2;
	
	public int m_ID;
	public NodeTest m_Test;
	public ClusStatistic m_ClusteringStat;
	public ClusStatistic m_TargetStat;
	public transient Object m_Visitor;	
	public long m_Time;	
	
	public MyNode cloneNode() {
		ClusNode clone = new ClusNode();
		clone.m_Test = m_Test;
		clone.m_ClusteringStat = m_ClusteringStat;
		clone.m_TargetStat = m_TargetStat;
		return clone;
	}
	
	
	public ClusNode cloneNodeWithVisitor() {
		ClusNode clone = (ClusNode)cloneNode();
		clone.setVisitor(getVisitor());
		return clone;
	}
	
	public final ClusNode cloneTreeWithVisitors(ClusNode n1, ClusNode n2) {
		if (n1 == this) {
			return n2;
		} else {
			ClusNode clone = (ClusNode)cloneNode();
			clone.setVisitor(getVisitor());
			int arity = getNbChildren();
			clone.setNbChildren(arity);
			for (int i = 0; i < arity; i++) {
				ClusNode node = (ClusNode)getChild(i);
				clone.setChild(node.cloneTreeWithVisitors(n1, n2), i);
			}		
			return clone;
		}
	}
	
	public final ClusNode cloneTreeWithVisitors() {
		ClusNode clone = (ClusNode)cloneNode();
		clone.setVisitor(getVisitor());
		int arity = getNbChildren();
		clone.setNbChildren(arity);
		for (int i = 0; i < arity; i++) {
			ClusNode node = (ClusNode)getChild(i);
			clone.setChild(node.cloneTreeWithVisitors(), i);
		}		
		return clone;
	}
	
	public void inverseTests() {		
		if (getNbChildren() == 2) {
			setTest(getTest().getBranchTest(ClusNode.NO));
			ClusNode ch1 = (ClusNode)getChild(0);
			ClusNode ch2 = (ClusNode)getChild(1);
			ch1.inverseTests();
			ch2.inverseTests();
			setChild(ch2, 0);
			setChild(ch1, 1);
		} else {
			for (int i = 0; i < getNbChildren(); i++) {
				ClusNode node = (ClusNode)getChild(i);
				node.inverseTests();
			}
		}		
	}
	
	public ClusNode[] getChildren(){
		ClusNode[] temp = new ClusNode[m_Children.size()];
		for(int i=0; i<m_Children.size(); i++)
			temp[i] = (ClusNode)getChild(i);
		return temp;
	}
	
	public double checkTotalWeight() {
		if (atBottomLevel()) {
			return getClusteringStat().getTotalWeight();
		} else {
			double sum = 0.0;
			for (int i = 0; i < getNbChildren(); i++) {
				ClusNode child = (ClusNode)getChild(i);
				sum += child.checkTotalWeight();
			}
			if (Math.abs(getClusteringStat().getTotalWeight() - sum) > 1e-6) {
				System.err.println("ClusNode::checkTotalWeight() error: "+getClusteringStat().getTotalWeight()+" <> "+sum);
			}
			return sum;
		}
	}
	
	public final void setVisitor(Object visitor) {
		m_Visitor = visitor;
	}
	
	public final Object getVisitor() {
		return m_Visitor;
	}
	
	public final void clearVisitors() {
		m_Visitor = null;
		int arity = getNbChildren();
		for (int i = 0; i < arity; i++) {
			ClusNode child = (ClusNode)getChild(i);
			child.clearVisitors();
		}
	}
	
	public final int getID() {
		return m_ID;
	}

	
	public boolean equals(Object other) {
		ClusNode o = (ClusNode)other;
		if (m_Test != null && o.m_Test != null) {
			if (!m_Test.equals(o.m_Test)) return false;
		} else {
			if (m_Test != null || o.m_Test != null) return false;
		}
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			if (!getChild(i).equals(o.getChild(i))) return false;
		}
		return true;
	}
	
	public int hashCode() {
		int hashCode = 1234;
		if (m_Test != null) {
			hashCode += m_Test.hashCode();
		} else {
			hashCode += 4567;
		}
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			hashCode += getChild(i).hashCode();
		}
		return hashCode;
	}
	
	/***************************************************************************
	 * Insprectors concenring test
	 ***************************************************************************/
	
	public final boolean hasBestTest() {
		return m_Test != null;
	}
	
	public final NodeTest getTest() {
		return m_Test;
	}
	
	public final void setTest(NodeTest test) {
		m_Test = test;
	}
	
	public final String getTestString() {
		return m_Test != null ? m_Test.getString() : "None";
	}
	
	public int getModelSize() {
		return getNbNodes();
	}
	
	public String getModelInfo() {
		return "Nodes = "+getNbNodes()+" (Leaves: "+getNbLeaves()+")";
	}
	
	public final boolean hasUnknownBranch() {
		return m_Test.hasUnknownBranch();
	}
	
	/***************************************************************************
	 * Insprectors concenring statistics
	 ***************************************************************************/
	
	public final ClusStatistic getClusteringStat() {
		return m_ClusteringStat;
	}
	
	public final ClusStatistic getTargetStat() {
		return m_TargetStat;
	}	
	
	public final double getTotWeight() {
		return m_ClusteringStat.m_SumWeight;
	}
	
	// Weight of unknown examples over total weight
	public final double getUnknownFreq() {
		return m_Test.getUnknownFreq();
	}
	
	/***************************************************************************
	 * Mutators
	 ***************************************************************************/
	
	public final void setClusteringStat(ClusStatistic stat) {
		m_ClusteringStat = stat;
	}
	
	public final void setTargetStat(ClusStatistic stat) {
		m_TargetStat = stat;
	}	
	
	public final void computePrediction() {
		if (getClusteringStat() != null) getClusteringStat().calcMean();
		if (getTargetStat() != null) getTargetStat().calcMean();
	}
	
	public final int updateArity() {
		int arity = m_Test.updateArity();
		setNbChildren(arity);
		return arity;
	}
	
	public final ClusNode postProc(MultiScore score) {
		updateTree();
		if (Settings.IS_MULTISCORE) multiScore(score);
		safePrune();
		return this;
	}
	
	public final void cleanup() {
		if (m_ClusteringStat != null) m_ClusteringStat.setSDataSize(0);
		if (m_TargetStat != null) m_TargetStat.setSDataSize(0);
	}
	
	public void makeLeaf() {
		m_Test = null;
		cleanup();
		removeAllChildren();
	}
	
	public final void updateTree() {
		cleanup();
		computePrediction();
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.updateTree();
		}
	}
	
	/***************************************************************************
	 * Code for safe package clus.pruning the tree
	 ***************************************************************************/
	
	// Test if two nodes predict the same
	public final boolean samePrediction(ClusNode other) {
		return m_TargetStat.samePrediction(other.m_TargetStat);
	}
	
	// Test if all children are leaves that predict the same
	public final boolean allSameLeaves() {
		int nb_c = getNbChildren();
		if (nb_c == 0) return false;
		ClusNode cr = (ClusNode)getChild(0);
		if (!cr.atBottomLevel()) return false;
		for (int i = 1; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			if (!info.atBottomLevel()) return false;
			if (!info.samePrediction(cr)) return false;
		}
		return true;
	}
	
	public void pruneByTrainErr(ClusAttributeWeights scale) {
		if (!atBottomLevel()){
			double errorsOfSubtree = estimateErrorAbsolute(scale);
			double errorsOfLeaf = getTargetStat().getError(scale);
			if (errorsOfSubtree >= errorsOfLeaf-1E-3) {
				makeLeaf();
			} else {
				for (int i = 0; i < getNbChildren(); i++) {
					ClusNode child = (ClusNode)getChild(i);
					child.pruneByTrainErr(scale);
			    }
			}
		}
	}	
	
	// Safe prune this tree (using predictions in leaves)
	public final void safePrune() {
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.safePrune();
		}
		if (allSameLeaves()) makeLeaf();
	}

	public final boolean allInvalidLeaves() {
		int nb_c = getNbChildren();
		if (nb_c == 0) return false;
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			if (!info.atBottomLevel()) return false;
			if (info.getTargetStat().isValidPrediction()) return false;
		}
		return true;
	}
	
	public final void pruneInvalid() {
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.pruneInvalid();
		}
		if (allInvalidLeaves()) makeLeaf();
	}
	
    public ClusModel prune(int prunetype) {
    	if (prunetype == PRUNE_INVALID) {
    		ClusNode pruned = (ClusNode)cloneTree();
    		pruned.pruneInvalid();
    		return pruned;
    	}
		return this;
	}
	
	/***************************************************************************
	 * Multi score code - this should be made more general!
	 ***************************************************************************/
	
	public final void multiScore(MultiScore score) {
		m_ClusteringStat = new MultiScoreStat(m_ClusteringStat, score);
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.multiScore(score);
		}
	}
	
	/***************************************************************************
	 * Code to attach another dataset to the tree
	 ***************************************************************************/
	
	public final void attachModel(Hashtable table) throws ClusException {
		int nb_c = getNbChildren();
		if (nb_c > 0) m_Test.attachModel(table);
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.attachModel(table);
		}
	}
		
	/***************************************************************************
	 * Code for making predictions
	 ***************************************************************************/
	
	public ClusStatistic predictWeighted(DataTuple tuple) {
		if (atBottomLevel()) {
			return getTargetStat();
		} else {
			int n_idx = m_Test.predictWeighted(tuple);
			if (n_idx != -1) {
				ClusNode info = (ClusNode)getChild(n_idx);
				return info.predictWeighted(tuple);
			} else {
				int nb_c = getNbChildren();
				ClusStatistic stat = getTargetStat().cloneSimple();
				for (int i = 0; i < nb_c; i++) {
					ClusNode node = (ClusNode)getChild(i);
					ClusStatistic nodes = node.predictWeighted(tuple);
					stat.addPrediction(nodes, m_Test.getProportion(i));
				}
				stat.computePrediction();
				return stat;
			}
		}
	}
	
	public final void applyModelProcessor(DataTuple tuple, ClusModelProcessor proc) throws IOException {
		int nb_c = getNbChildren();
		if (nb_c == 0 || proc.needsInternalNodes()) proc.modelUpdate(tuple, this);
		if (nb_c != 0) {
			int n_idx = m_Test.predictWeighted(tuple);
			if (n_idx != -1) {
				ClusNode info = (ClusNode)getChild(n_idx);
				info.applyModelProcessor(tuple, proc);
			} else {
				for (int i = 0; i < nb_c; i++) {
					ClusNode node = (ClusNode)getChild(i);
					double prop = m_Test.getProportion(i);
					node.applyModelProcessor(tuple.multiplyWeight(prop), proc);
				}
			}
		}
	}
	
	public final void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
		int nb_c = getNbChildren();
		for (int i = 0; i < mproc.size(); i++) {
			ClusModelProcessor proc = (ClusModelProcessor)mproc.elementAt(i);
			if (nb_c == 0 || proc.needsInternalNodes()) proc.modelUpdate(tuple, this);
		}
		if (nb_c != 0) {
			int n_idx = m_Test.predictWeighted(tuple);
			if (n_idx != -1) {
				ClusNode info = (ClusNode)getChild(n_idx);
				info.applyModelProcessors(tuple, mproc);
			} else {
				for (int i = 0; i < nb_c; i++) {
					ClusNode node = (ClusNode)getChild(i);
					double prop = m_Test.getProportion(i);
					node.applyModelProcessors(tuple.multiplyWeight(prop), mproc);
				}
			}
		}
	}
	
	/***************************************************************************
	 * Change the total statistic of the tree?
	 ***************************************************************************/
	
	public final void initTargetStat(ClusStatManager smgr, RowData subset) {
		m_TargetStat = smgr.createTargetStat();
		subset.calcTotalStatBitVector(m_TargetStat);
	}
	
	public final void initClusteringStat(ClusStatManager smgr, RowData subset) {
		m_ClusteringStat = smgr.createClusteringStat();
		subset.calcTotalStatBitVector(m_ClusteringStat);
	}
	
	public final void reInitTargetStat(RowData subset) {
		if (m_TargetStat != null) {
			m_TargetStat.reset();
			subset.calcTotalStatBitVector(m_TargetStat);			
		}
	}
	
	public final void reInitClusteringStat(RowData subset) {
		if (m_ClusteringStat != null) {
			m_ClusteringStat.reset();
			subset.calcTotalStatBitVector(m_ClusteringStat);
		}
	}	
		
	public final void initTotStats(ClusStatistic stat) {
		m_ClusteringStat = stat.cloneStat();
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode node = (ClusNode)getChild(i);
			node.initTotStats(stat);
		}
	}
	
	public final void numberTree() {
		numberTree(new IntObject(1,null));
	}
	
	public final void numberTree(IntObject count) {
		int arity = getNbChildren();
		if (arity > 0) {
			m_ID = 0;
			for (int i = 0; i < arity; i++) {
				ClusNode child = (ClusNode)getChild(i);
				child.numberTree(count);
			}
		} else {
			m_ID = count.getValue();
			count.incValue();
		}
	}
	
	public final void addChildStats() {
		int nb_c = getNbChildren();
		if (nb_c > 0) {
			ClusNode ch0 = (ClusNode)getChild(0);
			ch0.addChildStats();
			ClusStatistic stat = ch0.getClusteringStat();
			ClusStatistic root = stat.cloneSimple();
			root.addPrediction(stat, 1.0);
			for (int i = 1; i < nb_c; i++) {
				ClusNode node = (ClusNode)getChild(i);
				node.addChildStats();
				root.addPrediction(node.getClusteringStat(), 1.0);
			}
			root.calcMean();
			setClusteringStat(root);
		}
	}
	
	public double estimateErrorAbsolute(ClusAttributeWeights scale) {
		return estimateErrorRecursive(this, scale);
	}

	public double estimateError(ClusAttributeWeights scale) {
		return estimateErrorRecursive(this, scale) / getClusteringStat().getTotalWeight();
	}
	
	public double estimateSS(ClusAttributeWeights scale) {
		return estimateSSRecursive(this, scale);
	}
	
	public double estimateVariance(ClusAttributeWeights scale) {
		return estimateSSRecursive(this, scale) / getClusteringStat().getTotalWeight();
	}	
	
	public static double estimateSSRecursive(ClusNode tree, ClusAttributeWeights scale) {
		if (tree.atBottomLevel()) {
			ClusStatistic total = tree.getClusteringStat();
			return total.getSS(scale);
		} else {
			double result = 0.0;
			for (int i = 0; i < tree.getNbChildren(); i++) {
				ClusNode child = (ClusNode)tree.getChild(i);
				result += estimateSSRecursive(child, scale);
			}
			return result;
		}
	}
	
	public static double estimateErrorRecursive(ClusNode tree, ClusAttributeWeights scale) {
		if (tree.atBottomLevel()) {
			ClusStatistic total = tree.getTargetStat();
			return total.getError(scale);
		} else {
			double result = 0.0;
			for (int i = 0; i < tree.getNbChildren(); i++) {
				ClusNode child = (ClusNode)tree.getChild(i);
				result += estimateErrorRecursive(child, scale);
			}
			return result;
		}
	}	
	
	//if all the weight are equal to one
	// cpt count the number of leaf
	public static double estimateErrorRecursive(ClusNode tree) {
		if (tree.atBottomLevel()) {
			ClusStatistic total = tree.getTargetStat();
			//System.out.println("CLUSNODE error at leaf is "+total.getErrorRel());
			return total.getError();
			
		} else {
			double result = 0.0;
			for (int i = 0; i < tree.getNbChildren(); i++) {
				ClusNode child = (ClusNode)tree.getChild(i);
				result += estimateErrorRecursive(child);
			}
			return result;
		}
	}	
	
	public int getNbLeaf(){
		int nbleaf =0;
		if (atBottomLevel()) {nbleaf++;}
		else {for (int i = 0; i < getNbChildren(); i++) {
			ClusNode child = (ClusNode)getChild(i);
			nbleaf += child.getNbLeaf();
		}}
		return nbleaf;
	}
	
	/***************************************************************************
	 * Printing the tree ?
	 ***************************************************************************/
	
	// FIXME - what for NominalTests with only two possible outcomes?
	
	public void printModel(PrintWriter wrt) {
		printTree(wrt, StatisticPrintInfo.getInstance(), "");
	}

	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		printTree(wrt, info, "");
	}	
	
	public void printModelAndExamples(PrintWriter wrt, StatisticPrintInfo info, RowData examples) {
		printModel(wrt, info);
	}		
	
	public void printModelToPythonScript(PrintWriter wrt){
		printTreeToPythonScript(wrt, "\t");
	}
	
	public void printModelToQuery(PrintWriter wrt, ClusRun cr, int starttree, int startitem){
		int lastmodel = cr.getNbModels()-1;
		System.out.print("The number of models to print is:"+lastmodel+", the first one is :");
		String [][] tabitem = new String[lastmodel+1][10000]; //table of item
		int [][] tabexist = new int[lastmodel+1][10000]; //table of booleen for each item 
		Global.set_treecpt(starttree);
		Global.set_itemsetcpt(startitem);
		ClusModelInfo m = cr.getModelInfo(0);//cr.getModelInfo(lastmodel);
		String firstmodelname = m.getName();
		System.out.println(firstmodelname);
		
		if(firstmodelname != "Default"){ // exhaustive search
		for (int i = 0; i < cr.getNbModels(); i++) {
		ClusModelInfo mod = cr.getModelInfo(i);
		ClusNode tree = (ClusNode)cr.getModel(i);
		tabitem[i][0] = "null";
		tabexist[i][0] = 1;
		wrt.println("INSERT INTO trees_sets VALUES("+Global.get_itemsetcpt()+", '"+tabitem[i][0]+"', "+tabexist[i][0]+")");
		wrt.println("INSERT INTO all_trees VALUES("+Global.get_treecpt()+", "+Global.get_itemsetcpt()+",1)");
		Global.inc_itemsetcpt();
		if(tree.getNbChildren() != 0){
		printTreeInDatabase(wrt,tabitem[i],tabexist[i], 1,"all_trees");
		}
		if(tree.getNbNodes() == 1){ //we look for the majority class in the data
		double error_rate = (tree.m_ClusteringStat).getErrorRel();
		wrt.println("INSERT INTO trees_charac VALUES("+Global.get_treecpt()+", "+mod.getModelSize()+", "+error_rate+", "+(1-error_rate)+", NULL)");		
		}
		else{
		//print the statitistics here (the format depend on the needs of the plsql program)
		//writer.println("INSERT INTO trees_charac VALUES(T1,"+size+error+accuracy+constraint);
		wrt.println("INSERT INTO trees_charac VALUES("+Global.get_treecpt()+", "+mod.getModelSize()+", "+(mod.m_TrainErr).getErrorClassif()+", "+(mod.m_TrainErr).getErrorAccuracy()+", NULL)");
		}
		Global.inc_treecpt();
		}
		}
		else { //greedy search 
		ClusModelInfo mod = cr.getModelInfo(lastmodel);
		ClusNode tree = (ClusNode)cr.getModel(lastmodel);
		tabitem[lastmodel][0] = "null";
		tabexist[lastmodel][0] = 1;
		wrt.println("INSERT INTO trees_sets VALUES("+Global.get_itemsetcpt()+", '"+tabitem[lastmodel][0]+"', "+tabexist[lastmodel][0]+")");
		wrt.println("INSERT INTO greedy_trees VALUES("+Global.get_treecpt()+", "+Global.get_itemsetcpt()+",1)");
		Global.inc_itemsetcpt();
		if(tree.getNbChildren() != 0){
		printTreeInDatabase(wrt,tabitem[lastmodel],tabexist[lastmodel], 1,"greedy_trees");
		}
		wrt.println("INSERT INTO trees_charac VALUES("+Global.get_treecpt()+", "+mod.getModelSize()+", "+(mod.m_TrainErr).getErrorClassif()+", "+(mod.m_TrainErr).getErrorAccuracy()+", NULL)");
		Global.inc_treecpt();
		}
	}
	
	public final void printTree() {
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(System.out));
		printTree(wrt, StatisticPrintInfo.getInstance(), "");
		wrt.flush();
	}
	
	public final void writeDistributionForInternalNode(PrintWriter writer, StatisticPrintInfo info) {
		if (info.INTERNAL_DISTR) {
			if (m_TargetStat != null) {
				writer.print(": "+m_TargetStat.getString(info));
			}			
		}
		writer.println();
	}
	
	public final void printTree(PrintWriter writer, StatisticPrintInfo info, String prefix) {
		int arity = getNbChildren();
		if (arity > 0) {
			int delta = hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2) {
				writer.print(m_Test.getTestString());
				writeDistributionForInternalNode(writer, info);
				writer.print(prefix + "+--yes: ");
				((ClusNode)getChild(YES)).printTree(writer, info, prefix+"|       ");
				writer.print(prefix + "+--no:  ");
				if (hasUnknownBranch()) {
					((ClusNode)getChild(NO)).printTree(writer, info, prefix+"|       ");
					writer.print(prefix + "+--unk: ");
					((ClusNode)getChild(UNK)).printTree(writer, info, prefix+"        ");
				} else {
					((ClusNode)getChild(NO)).printTree(writer, info, prefix+"        ");
				}
			} else {//on the leaves
				writer.println(m_Test.getTestString());				
				for (int i = 0; i < arity; i++) {
					ClusNode child = (ClusNode)getChild(i);
					String branchlabel = m_Test.getBranchLabel(i);
					writer.print(prefix + "+--" + branchlabel + ": ");
					String suffix = StringUtils.makeString(' ', branchlabel.length()+4);
					if (i != arity-1) {
						child.printTree(writer, info, prefix+"|"+suffix);						
					} else {
						child.printTree(writer, info, prefix+" "+suffix);
					}
				}
			}
		} else {
			if (m_TargetStat == null) {
				writer.print("?");
			} else {
				writer.print(m_TargetStat.getString(info));
			}
			if (getID() != 0 && info.SHOW_INDEX) writer.println(" ("+getID()+")");
			else writer.println();
		}
	}
	
	/*to print the tree directly into an IDB : Elisa Fromont 19/06/2006*/
public final void printTreeInDatabase(PrintWriter writer, String tabitem[], int tabexist[], int cpt, String typetree) {
	int arity = getNbChildren();
	if (arity > 0) {
		int delta = hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2) { //the tree is binary
				// in case the test is postive
				tabitem[cpt] = m_Test.getTestString();
				tabexist[cpt] = 1;
				for(int i =0; i <= cpt; i++){
					writer.println("INSERT INTO trees_sets VALUES("+Global.get_itemsetcpt()+", '"+tabitem[i]+"', "+tabexist[i]+")");
				}
				writer.println("INSERT INTO "+typetree+" VALUES("+Global.get_treecpt()+", "+Global.get_itemsetcpt()+",1)");
				Global.inc_itemsetcpt();
				cpt++;
				((ClusNode)getChild(YES)).printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);			
				cpt--;//to remove the last test on the father : now the test is negative		
				// in ca	se the test is negative
				tabitem[cpt]= m_Test.getTestString();
				tabexist[cpt] = 0;
				for(int i =0; i <= cpt; i++){
					writer.println("INSERT INTO trees_sets VALUES("+Global.get_itemsetcpt()+", '"+tabitem[i]+"', "+tabexist[i]+")");
				}
				writer.println("INSERT INTO "+typetree+" VALUES("+Global.get_treecpt()+", "+Global.get_itemsetcpt()+",1)");
				Global.inc_itemsetcpt();
				cpt++;
				if (hasUnknownBranch()) {
					((ClusNode)getChild(NO)).printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);
					
					((ClusNode)getChild(UNK)).printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);
					} 
				else {
				((ClusNode)getChild(NO)).printTreeInDatabase(writer, tabitem, tabexist, cpt, typetree);
				}
			}//end if arity- delta ==2
			
			else{ //arity -delta =/= 2	the tree is not binary
				//Has not beeen modified for databse purpose yet !!!!!!
				writer.println("arity-delta different 2");
				for (int i = 0; i < arity; i++) {
				ClusNode child = (ClusNode)getChild(i);
				String branchlabel = m_Test.getBranchLabel(i);
				writer.print("+--" + branchlabel + ": ");
				if (i != arity-1) {
					child.printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);						
				} else {
					child.printTreeInDatabase(writer,tabitem, tabexist, cpt, typetree);
				}
				}// end for
			}//end else arity -delta =/= 2	
			} //end if arity >0 0
			else {// if arity =0 : on a leaf
				if (m_TargetStat == null) {
					writer.print("?");
				} else {
					tabitem[cpt] = m_TargetStat.getPredictedClassName(0);
					tabexist[cpt] = 1;
					for(int i =0; i <= cpt; i++){
					writer.println("INSERT INTO trees_sets VALUES("+Global.get_itemsetcpt()+", '"+tabitem[i]+"', "+tabexist[i]+")");
					}
					writer.println("INSERT INTO "+typetree+" VALUES("+Global.get_treecpt()+", "+Global.get_itemsetcpt()+",0)");
					cpt++;	
					Global.inc_itemsetcpt();
				}
			}//end else if arity =0
			
	}
	

	public final void printTreeToPythonScript(PrintWriter writer, String prefix) {
		int arity = getNbChildren();
		if (arity > 0) {
			int delta = hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2) {
				writer.println(prefix+"if " +m_Test.getTestString()+":");
				((ClusNode)getChild(YES)).printTreeToPythonScript(writer, prefix+"\t");
				writer.println(prefix + "else: ");
				if (hasUnknownBranch()) {
					//TODO anything to do???
				} else {
					((ClusNode)getChild(NO)).printTreeToPythonScript(writer, prefix+"\t");
				}
			} else {
				//TODO what to do?
			}
		} else {
			if (m_TargetStat != null) {
				writer.println(prefix+"return "+m_TargetStat.getArrayOfStatistic());
				System.out.println(m_TargetStat.getClass());
			}
		}
	}
	
	public String toString() {
		try{
			if (hasBestTest()) return getTestString();
			else return m_TargetStat.getSimpleString();
		}
		catch(Exception e){return "null clusnode ";}
	}
	
	/**
	 * Returns the majority class for this node.(not so good name)
	 */
	public ClusStatistic predictWeightedLeaf(DataTuple tuple) {
		return getTargetStat();
	}
	
	public void retrieveStatistics(ArrayList list) {
		if (m_ClusteringStat != null) list.add(m_ClusteringStat);
		if (m_TargetStat != null) list.add(m_TargetStat);
		int arity = getNbChildren();
		for (int i = 0; i < arity; i++) {
			ClusNode child = (ClusNode)getChild(i);
			child.retrieveStatistics(list);
		}	   
	}

	public int getLargestBranchIndex() {
		double max = 0.0;
		int max_idx = -1;
		for (int i = 0; i < getNbChildren(); i++) {
			ClusNode child = (ClusNode)getChild(i);
			double child_w = child.getTotWeight();
			if (ClusUtil.grOrEq(child_w, max)) {
				max = child_w;
				max_idx = i;
			}
		}
		return max_idx;
	}

	public void adaptToData(RowData data) {
		// sort data into tree
		NodeTest tst = getTest();
		for (int i = 0; i < getNbChildren(); i++) {
			ClusNode child = (ClusNode)getChild(i);
			RowData subset = data.applyWeighted(tst, i);
			child.adaptToData(subset);
		}
		// recompute statistics
		reInitTargetStat(data);
		reInitClusteringStat(data);
	}
}
