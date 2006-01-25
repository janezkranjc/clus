package clus.ext.hierarchical;

import jeans.tree.*;
import jeans.util.*;
import jeans.math.*;
import jeans.util.compound.*;
import jeans.util.array.*;
import jeans.math.matrix.*;

import java.io.*;
import java.util.*;
import Jama.*;

import clus.main.*;
import clus.util.*;
import clus.data.type.*;

// FIXME -- This file really needs some cleaning up :-) :-)

public class ClassHierarchy implements Serializable {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public final static int TEST = 0;
	public final static int ERROR = 1;
	
	protected int m_Number;
	protected ClassTerm m_Root;
	protected ClassesTuple m_Ignore;
	protected NumericAttrType[] m_DummyTypes;
	protected transient double[] m_Weights;
	protected transient Hashtable m_ErrorWeights = new Hashtable();
	protected transient MSymMatrix m_KMatrix;
	protected transient ClassesAttrType m_Type;
	protected transient HierBasicDistance m_NodeSelDist;
	
	public ClassHierarchy() {
		m_Root = new ClassTerm();
	}
	
	public ClassHierarchy(ClassesAttrType type) {
		m_Root = new ClassTerm();
		setType(type);
	}
	
	public ClassHierarchy(ClassTerm root) {
		m_Root = root;
	} 
	
	public Settings getSettings() {
		return m_Type.getSettings();
	}
	
	public final void setType(ClassesAttrType type) {
		m_Type = type;
	}
	
	public final ClassesAttrType getType() {
		return m_Type;
	}
	
	public final void addClass(ClassesValue val) {
		m_Root.addClass(val, 0);
	}
	
	public final void print(PrintWriter wrt) {
		m_Root.print(0, wrt, null, null);
	}

	public final void print(PrintWriter wrt, double[] counts, double[] weights) {
		m_Root.print(0, wrt, counts, weights);
	}	
	
	public final void print(PrintWriter wrt, double[] counts) {
		m_Root.print(0, wrt, counts, m_Weights);
	}
	
	public final int getMaxDepth() {
		return m_Root.getMaxDepth();
	}
	
	public final ClassTerm getRoot() {
		return m_Root;
	}
	
	public final void setNodeSelDist(HierBasicDistance dist) {
		m_NodeSelDist = dist;
	}
	
	public final void numberHierarchy() {
		m_Number = -1;
		CompleteTreeIterator iter = getRootIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			node.setIndex(m_Number++);
		}
	}
	
	public double[] calcAccumulated(double[] weights, double sum) {
		double[] accu = new double[weights.length];
		m_Root.accuWeight(accu, weights);
		MDoubleArray.dotscalar(accu, 1.0/sum);
		return accu;
	}
	
	public void makeRelative(double[] weights) {
		m_Root.makeRelative(weights);
	}
	
	public String makePredictString(double[] weights, double sum, double thres) {
		MyArray res = new MyArray();
		double[] accu = calcAccumulated(weights, sum);
		CompleteTreeIterator iter = getNoRootIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			int idx = node.getIndex();
			if (accu[idx] >= 0.0) {
				DoubleObject ob = new DoubleObject(accu[idx], node.toString());
				res.addElement(ob);
			}
		}
		res.sort();
		StringBuffer buf = new StringBuffer();
		buf.append(ClusFormat.ONE_AFTER_DOT.format(sum));
		for (int i = 0; i < res.size() && i < 5; i++) {
			buf.append(" ");
			DoubleObject ob = (DoubleObject)res.elementAt(i);
			buf.append("["+ob.getObject());
			String str = ClusFormat.TWO_AFTER_DOT.format(ob.getValue());
			buf.append(" "+str+"]");
		}
		buf.append(" NODE: ");
		buf.append(getBestNode(weights));
		buf.append(" LEAF: ");
		buf.append(getBestLeaf(weights));
		return buf.toString();
	}
	
	/* This method returns all the leafterms which have a weightedaboundance larger than 0 */
	
	public ClassesTuple getBestTuple(double[] counts, double sum_Weight) {
		CompleteTreeIterator iter = getRootIter();
		ClassesTuple returnTuple = new ClassesTuple(counts.length);
		double weightedAbundance;
		int countA = 0;
		int countB = 0;
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm) iter.getNextNode();
			weightedAbundance = counts[countA++]/sum_Weight;
			// weightedAbundance will only be larger then 0 in a leaf (at the moment)
			if (weightedAbundance > 0) {
				ClassesValue val = new ClassesValue(node, weightedAbundance);
				returnTuple.setItemAt(val,countB++);
			}
		}
		returnTuple.setLength(countB);
		return returnTuple;
	}
	
	public static void addAllClasses(ClassTerm node, ClassesTuple tuple, boolean[] matrix) {
		for (int i = 0; i < node.getNbChildren(); i++) {
			addAllClassesRec((ClassTerm)node.getChild(i), tuple, matrix);
		}
	}
	
	public static void addAllClassesRec(ClassTerm node, ClassesTuple tuple, boolean[] matrix) {
		if (matrix[node.getIndex()]) {
			tuple.addItem(new ClassesValue(node, 1.0));
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			addAllClassesRec((ClassTerm)node.getChild(i), tuple, matrix);
		}
	}	
	
	public static void fillBooleanMatrixMaj(ClassTerm node, double[] mean, boolean[] matrix, double treshold) {
		for (int i = 0; i < node.getNbChildren(); i++) {
			fillBooleanMatrixMajRec((ClassTerm)node.getChild(i), mean, matrix, treshold);
		}
	}
	
	public static void fillBooleanMatrixMajRec(ClassTerm node, double[] mean, boolean[] matrix, double treshold) {
		if (mean[node.getIndex()] >= treshold/100.0) matrix[node.getIndex()] = true;
		for (int i = 0; i < node.getNbChildren(); i++) {
			fillBooleanMatrixMajRec((ClassTerm)node.getChild(i), mean, matrix, treshold);
		}
	}	
	
	public static void removeParentNodes(ClassTerm node, boolean[] matrix) {
		if (matrix[node.getIndex()]) {
			ClassTerm parent = node.getCTParent();
			while (parent != null && matrix[parent.getIndex()]) {
				matrix[parent.getIndex()] = false;
				parent = parent.getCTParent();
			}
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			removeParentNodes((ClassTerm)node.getChild(i), matrix);
		}
	}
	
	public static int countOnes(boolean[] matrix) {
		int count = 0;
		for (int i = 0; i < matrix.length; i++) {
			if (matrix[i]) count++;
		}
		return count;
	}
	
	public void removeIgnoredClasses(boolean[] classes) {
		if (m_Ignore != null) {
			int len = m_Ignore.getLength();
			for (int i = 0; i < len; i++) {
				classes[m_Ignore.getPosition(i)] = false;				
			}
		}		
	}
	
	public ClassesTuple getBestTupleMajNoParents(double[] mean, double treshold) {
		boolean[] classes = new boolean[getTotal()];
		fillBooleanMatrixMaj(getRoot(), mean, classes, treshold);
		removeParentNodes(getRoot(), classes);
		ClassesTuple tuple = new ClassesTuple(countOnes(classes));
		addAllClasses(getRoot(), tuple, classes);
		return tuple;
	}
	
	public ClassesTuple getBestTupleMaj(double[] mean, double treshold) {
		boolean[] classes = new boolean[getTotal()];
		fillBooleanMatrixMaj(getRoot(), mean, classes, treshold);
		removeIgnoredClasses(classes);
		ClassesTuple tuple = new ClassesTuple(countOnes(classes));
		addAllClasses(getRoot(), tuple, classes);
		return tuple;
	}		
	
	public ClassTerm getBestLeaf(double[] weights) {
		ClassTerm bestnode = null;
		double bestval = Double.NEGATIVE_INFINITY;
		CompleteTreeIterator iter = getNoRootIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			if (node.atBottomLevel()) {
				int idx = node.getIndex();
				if (weights[idx] >= bestval) {
					bestval = weights[idx];
					bestnode = node;
				}
			}
		}
		return bestnode;
	}
	
	public ClassTerm getBestNode(double[] weights) {
		ClassTerm bestnode = null;
		double bestval = Double.POSITIVE_INFINITY;
		CompleteTreeIterator iter = getNoRootIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			// Calculate shortest path to other examples
			double wdist = 0.0;
			CompleteTreeIterator sumiter = getNoRootIter();
			while (sumiter.hasMoreNodes()) {
				ClassTerm other = (ClassTerm)sumiter.getNextNode();
				double distance = m_NodeSelDist.calcDistance(node, other);
				double weight = weights[other.getIndex()];
				wdist += weight*distance;
			}
			// Check if better
			if (wdist < bestval) {
				bestval = wdist;
				bestnode = node;
			}
		}
		return bestnode;
	}
	
	public ClassesTuple getBestTupleOld(double[] counts, double sum) {
		double bestvalue = Double.POSITIVE_INFINITY;
		ClassesTuple besttuple = new ClassesTuple(0);
		// Try all 1-class tuples
		ClassesTuple current = new ClassesTuple(1);
		LeafTreeIterator iter = getLeavesIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			current.setItemAt(node, 0);
			double value = evaluateTuple(current, counts, sum);
			if (value < bestvalue) {
				besttuple.cloneFrom(current);
				bestvalue = value;
			}
		}
		// Try all 2-class tuples
		/*
		 current = new ClassesTuple(2);
		 System.out.println("Best: "+besttuple);
		 iter = getLeavesIter();
		 while (iter.hasMoreNodes()) {
		 ClassTerm node = (ClassTerm)iter.getNextNode();
		 current.setItemAt(node, 0);
		 LeafTreeIterator iter2 = new LeafTreeIterator(iter);
		 while (iter2.hasMoreNodes()) {
		 ClassTerm node2 = (ClassTerm)iter2.getNextNode();
		 current.setItemAt(node2, 1);
		 double value = evaluateTuple(current, counts, sum);
		 if (value < bestvalue) {
		 besttuple.cloneFrom(current);
		 bestvalue = value;
		 }
		 }
		 }
		 */
		// If best tuple has length 2, try to extend it further
		boolean try_add = true;
		while (try_add) {
			try_add = false;
			int oldsize = besttuple.size();
			current = new ClassesTuple(oldsize+1);
			current.cloneFirstN(oldsize, besttuple);
			iter = getLeavesIter();
			while (iter.hasMoreNodes()) {
				ClassTerm node = (ClassTerm)iter.getNextNode();
				if (!current.containsFirstN(oldsize, node)) {
					current.setItemAt(node, oldsize);
					double value = evaluateTuple(current, counts, sum);
					if (value < bestvalue) {
						besttuple.cloneFrom(current);
						bestvalue = value;
						try_add = true;
					}
				}
			}
		}
		return besttuple;
	}
	
	public double evaluateTuple(ClassesTuple pred, double[] counts, double sum) {
		// Initialize variables
		MSymMatrix km = getKMatrix();
		double aiAai = 0.0;
		double sumweight = 0.0;
		double[] Aai = new double[km.getRows()];
		ClassesTuple actual = new ClassesTuple(1);
		// Calc error over all leaves
		LeafTreeIterator iter = getLeavesIter();
		while (iter.hasMoreNodes()) {
			ClassTerm acutal_term = (ClassTerm)iter.getNextNode();
			actual.setItemAt(acutal_term, 0);
			int actual_idx = acutal_term.getIndex();
			double weight = counts[actual_idx] / sum;
			km.addRowWeighted(Aai, actual_idx, weight);
			aiAai += weight * km.xtAx(actual);
			sumweight += weight;
		}
		double piApi = km.xtAx(pred);
		return sumweight*piApi - 2 * MSymMatrix.dot(pred, Aai) + aiAai;
	}
	
	public final LeafTreeIterator getLeavesIter() {
		return new LeafTreeIterator(m_Root);
	}
	
	public final CompleteTreeIterator getNoRootIter() {
		CompleteTreeIterator iter = new CompleteTreeIterator(m_Root);
		if (iter.hasMoreNodes()) iter.getNextNode();
		return iter;
	}
	
	public final CompleteTreeIterator getRootIter() {
		return new CompleteTreeIterator(m_Root);
	}
	
	/*
	 1  1,9  1,9  2,7  2,7
	 1,9 3,61 1,99 2,07 2,07
	 1,9 1,99 3,61 5,13 5,13
	 2,7 2,07 5,13 7,29 6,01
	 2,7 2,07 5,13 6,01 7,29
	 
	 -0,5657; -0,1035; 1,28; 2,9899; 19,1993
	 */
	
	public final void setKMatrix(MSymMatrix mtrx) {
		m_KMatrix = mtrx;
	}
	
	public final MSymMatrix getKMatrix() {
		return m_KMatrix;
	}
	
	public final void showEigenValues() {
		System.out.println();
		MSymMatrix KM = getKMatrix();
		KM.print(ClusFormat.OUT_WRITER, ClusFormat.TWO_AFTER_DOT, 5);
		ClusFormat.OUT_WRITER.println();
		Matrix A = new Matrix(KM.toCPArray(), m_Number, m_Number);
		EigenvalueDecomposition dec = new EigenvalueDecomposition(A);
		MMatrix EigV = new MStoredMatrix(dec.getRealEigenvalues());
		EigV.print(ClusFormat.OUT_WRITER, ClusFormat.TWO_AFTER_DOT, 7);
		ClusFormat.OUT_WRITER.flush();
		System.out.println();
	}
	
	public final void calcErrorWeights(HierWeightSPath dist, double widec) {		
		double[] temp = new double[getTotal()];
		CompleteTreeIterator it_i = getRootIter();
		while (it_i.hasMoreNodes()) {
			ClassTerm ni = (ClassTerm)it_i.getNextNode();
			temp[ni.getIndex()] = dist.getWeight(ni.getLevel());
		}	
		m_ErrorWeights.put((new Double(widec)).toString(),temp);
	}
	
	public final void calcErrorWeights(double[] widecs){		
		int depth = getMaxDepth();
		for (int i = 0;i<widecs.length;i++){
			double widec = widecs[i];
			HierWeightSPath path = new HierWeightSPath(depth,widec);
			calcErrorWeights(path, widec);
		}		
	}    
	
	public final double[] getWeights() {
		return m_Weights;
	}
	
	public final void calcWeights() {
		HierNodeWeights ws = new HierNodeWeights();
		double widec = Settings.HIER_W_PARAM.getValue();
		ws.initExponentialDepthWeights(this, widec);
		m_Weights = ws.getWeights();
	}
	
	public final SingleStat getMeanBranch(boolean[] enabled) {
		SingleStat stat = new SingleStat();
		m_Root.getMeanBranch(enabled, stat);
		return stat;
	}
	
	public final MSymMatrix calcMatrix(HierBasicDistance dist) {
		MSymMatrix KM = new MSymMatrix(m_Number, true);
		double root_delta = dist.getVirtualRootWeight();
		CompleteTreeIterator it_i = getRootIter();
		while (it_i.hasMoreNodes()) {
			ClassTerm ni = (ClassTerm)it_i.getNextNode();
			int i = ni.getIndex();
			CompleteTreeIterator it_j = getRootIter();
			/*if (ni.atBottomLevel()) {
			 System.out.print("\n"+ i  + ":");
			 }*/
			while (it_j.hasMoreNodes()) {
				ClassTerm nj = (ClassTerm)it_j.getNextNode();
				int j = nj.getIndex();
				if (j <= i) {
					double d_0i = dist.calcDistance(m_Root, ni) + root_delta;
					double d_0j = dist.calcDistance(m_Root, nj) + root_delta;
					double d_ij = dist.calcDistance(ni, nj);
					double kij = 0.5*(d_0i*d_0i + d_0j*d_0j - d_ij*d_ij);
					/*if (j == i) {
					 System.out.println(d_0i+","+d_0j+","+d_ij+","+kij+"\n");
					 }*/
					//System.out.println(i + "," + j + ":" + d_0i + "/" + d_0j + "/" + d_ij + "->" + kij);
					
					KM.set(i, j, kij);
					/*if (ni.atBottomLevel()){
					 if (nj.atBottomLevel()){
					 System.out.print("->"+ j + ":" + kij+ " ");
					 }}*/
				}
			}
		}
		/*for (int i = 0; i < KM.getRows() ; i++) {
		 System.out.print(KM.toString(i));
		 }*/
		
		return KM;
	}
	
	public final void calcMatrix() {
		int depth = getMaxDepth();
		double widec = Settings.HIER_W_PARAM.getValue();
		System.out.println("W0 = "+widec);
		HierBasicDistance dist = new HierWeightSPath(depth, widec);
		setKMatrix(calcMatrix(dist));
	}
	
	public final void showAllNodes() {
		CompleteTreeIterator iter = new CompleteTreeIterator(m_Root);
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			System.out.println("Node: "+node);
		}
	}
	
	public final int getTotal() {
		return m_Number;
	}
	
	public final int getDepth() {
		return m_Root.getMaxDepth();
	}
	
	public final int[] getClassesByLevel() {
		int[] res = new int[getDepth()];
		countClassesRecursive(m_Root, 0, res);
		return res;
	}
	
	public final void countClassesRecursive(ClassTerm root, int depth, int[] cls) {
		cls[depth]++;
		for (int i = 0; i < root.getNbChildren(); i++) {
				countClassesRecursive((ClassTerm)root.getChild(i), depth+1, cls);
		}		
	}	
	
	public final void initialize() {
		numberHierarchy();
		int hiermode = getSettings().getHierMode();
		if (hiermode == Settings.HIERMODE_TREE_DIST_ABS_WEUCLID ||
			hiermode == Settings.HIERMODE_TREE_DIST_WEUCLID) {
			calcWeights();
		} else {
			calcMatrix();
		}
		double widec = Settings.HIER_W_PARAM.getValue();
		setNodeSelDist(new HierWeightSPath(getMaxDepth(), widec));
		ClusSchema schema = m_Type.getSchema();
		int maxIndex = schema.getNbAttributes();
		m_DummyTypes = new NumericAttrType[getTotal()];
		for (int i = 0; i < getTotal(); i++) {
			NumericAttrType type = new NumericAttrType("H"+i);
			type.setIndex(maxIndex++);
			type.setSchema(schema);
			m_DummyTypes[i] = type;
		}
	}
	
	public final NumericAttrType[] getDummyAttrs() {
		return m_DummyTypes;
	}
	
	public final void showSummary() {
		int leaves = 0;
		int depth = getMaxDepth();
		System.out.println("Depth: "+depth);
		System.out.println("Nodes: "+getTotal());
		ClassTerm root = getRoot();
		int nb = root.getNbChildren();
		for (int i = 0; i < nb; i++) {
			ClassTerm chi = (ClassTerm)root.getChild(i);
			int nbl = chi.getNbLeaves();
			System.out.println("Child "+i+": "+chi.getID()+" "+nbl);
			leaves += nbl;
		}
		System.out.println("Leaves: "+leaves);
	}
	
	public final ClassTerm getCheckClassTerm(ClassesValue vl) throws ClusException {
		int pos = 0;
		int nb_level = vl.getNbLevels();
		ClassTerm subterm = m_Root;
		while (true) {
			if (pos >= nb_level) return subterm;
			String lookup = vl.getClassID(pos);
			ClassTerm found = subterm.getByName(lookup);
			if (found == null)
				throw new ClusException("Value not an element of hierarchy: "+vl);
			subterm = found;
			pos++;
		}
	}
	
	public final ClassTerm getClassTerm(ClassesValue vl) throws ClusException {
		int pos = 0;
		int nb_level = vl.getNbLevels();
		ClassTerm subterm = m_Root;
		while (true) {
			if (pos >= nb_level) return subterm;
			String lookup = vl.getClassID(pos);
			if (lookup.equals("0")) {
				return subterm;
			} else {
				ClassTerm found = subterm.getByName(lookup);
				if (found == null) throw new ClusException("Classes value not in hierarchy: "+vl.toPathString());
				subterm = found;
			}			
			pos++;
		}
	}
	
	public final ClassesValue createValueByName(String name, StringTable table) throws ClusException {		
		ClassesValue value = new ClassesValue(name, table);
		value.addHierarchyIndices(this);
		return value;
	}
	
	public final ClassesValue createValueByName(String name) throws ClusException {		
		ClassesValue value = new ClassesValue(name, getType().getTable());
		value.addHierarchyIndices(this);
		return value;
	}
	
	public final int getClassIndex(ClassesValue vl) throws ClusException {
		return getClassTerm(vl).getIndex();
	}
	
	public final double getWeight(int idx) {
		return m_Weights[idx];
	}
	
	public final double getErrorWeight (int idx, double widec) {
		return ((double[])m_ErrorWeights.get(new Double(widec).toString()))[idx];
	}
	
	public final void setIgnoreClasses(ClassesTuple ignore) {
		m_Ignore = ignore;
	}
	
	public final ClassesTuple getIgnoreClasses() {
		return m_Ignore;
	}
}
