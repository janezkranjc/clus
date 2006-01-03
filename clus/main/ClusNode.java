package clus.main;

import jeans.tree.*;
import jeans.util.*;
import jeans.util.compound.*;

import java.util.*;
import java.io.*;

import clus.util.*;
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
	
	// Safe prune this tree (using predictions in leaves)
	public final void safePrune() {
		int nb_c = getNbChildren();
		for (int i = 0; i < nb_c; i++) {
			ClusNode info = (ClusNode)getChild(i);
			info.safePrune();
		}
		if (allSameLeaves()) makeLeaf();
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
	
	/*
	 public final void attachData(ClusData data) {
	 int nb_c = getNbChildren();
	 if (nb_c > 0) data.attach(this);
	 for (int i = 0; i < nb_c; i++) {
	 ClusNode info = (ClusNode)getChild(i);
	 info.attachData(data);
	 }
	 }
	 
	 public final void detachData() {
	 m_SplitAttr = null;
	 int nb_c = getNbChildren();
	 for (int i = 0; i < nb_c; i++) {
	 ClusNode info = (ClusNode)getChild(i);
	 info.detachData();
	 }
	 }
	 */
	
	/***************************************************************************
	 * Code for making predictions
	 ***************************************************************************/
	
	/*
	 public final ClusNode predict(int idx) {
	 if (m_Children != null) {
	 int n_idx = m_Test.predict((ClusAttribute)m_SplitAttr, idx);
	 ClusNode info = (ClusNode)getChild(n_idx);
	 return info.predict(idx);
	 } else {
	 return this;
	 }
	 }
	 */
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
				stat.calcMean();
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
		m_TargetStat.setSDataSize(subset.getNbRows());
		subset.calcTotalStat(m_TargetStat);
		m_TargetStat.optimizePreCalc(subset);
	}
	
	public final void initClusteringStat(ClusStatManager smgr, RowData subset) {
		m_ClusteringStat = smgr.createClusteringStat();
		m_ClusteringStat.setSDataSize(subset.getNbRows());
		subset.calcTotalStat(m_ClusteringStat);
		m_ClusteringStat.optimizePreCalc(subset);
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
			ClusStatistic total = tree.getClusteringStat();
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
	
	public final void printTree() {
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(System.out));
		printTree(wrt, StatisticPrintInfo.getInstance(), "");
		wrt.flush();
	}
	
	public final void printTree(PrintWriter writer, StatisticPrintInfo info, String prefix) {
		int arity = getNbChildren();
		if (arity > 0) {
			int delta = hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2) {
				writer.println(m_Test.getTestString());
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
			} else {
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
	
}
