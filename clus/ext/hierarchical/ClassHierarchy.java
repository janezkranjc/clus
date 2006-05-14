package clus.ext.hierarchical;

import jeans.tree.*;
import jeans.math.*;

import java.io.*;
import java.util.*;

import clus.main.*;
import clus.util.*;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.*;

public class ClassHierarchy implements Serializable {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	public final static int TEST = 0;
	public final static int ERROR = 1;
	
	public final static int TREE = 0;	
	public final static int DAG = 1;
	
	protected int m_HierType = TREE;
	protected ClassTerm m_Root;
	protected ClassesTuple m_Eval;
	protected ArrayList m_ClassList = new ArrayList();
	protected HashMap m_ClassMap = new HashMap();
	protected NumericAttrType[] m_DummyTypes;
	protected boolean m_IsLocked;
	protected transient double[] m_Weights;
	protected transient Hashtable m_ErrorWeights = new Hashtable();
	protected transient ClassesAttrType m_Type;
	
	public ClassHierarchy() {		
	}
	
	public ClassHierarchy(ClassesAttrType type) {
		this(new ClassTerm());
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
		if (!isLocked()) m_Root.addClass(val, 0, this);
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
	
	public final void initClassListRecursiveTree(ClassTerm term) {
		m_ClassList.add(term);
		term.sortChildrenByID();
		for (int i = 0; i < term.getNbChildren(); i++) {
			initClassListRecursiveTree((ClassTerm)term.getChild(i));			
		}
	}
	
	public final void initClassListRecursiveDAG(ClassTerm term, HashSet set) {
		if (!set.contains(term.getID())) {
			// This is the first time we see this term
			m_ClassList.add(term);
			term.sortChildrenByID();
			for (int i = 0; i < term.getNbChildren(); i++) {
				initClassListRecursiveDAG((ClassTerm)term.getChild(i), set);			
			}
			set.add(term.getID());
		}		
	}
	
	public final void numberHierarchy() {
		m_Root.setIndex(-1);
		m_Root.sortChildrenByID();
		m_ClassList.clear();
		if (isDAG()) {
			// make sure each ID only appears once!
			HashSet set = new HashSet();
			for (int i = 0; i < m_Root.getNbChildren(); i++) {
				initClassListRecursiveDAG((ClassTerm)m_Root.getChild(i), set);			
			}
		} else {
			for (int i = 0; i < m_Root.getNbChildren(); i++) {
				initClassListRecursiveTree((ClassTerm)m_Root.getChild(i));			
			}		
		}
		for (int i = 0; i < getTotal(); i++) {
			ClassTerm term = getTermAt(i);
			term.setIndex(i);
		}
		System.out.println("Hierarchy initialized: "+getTotal()+" nodes");	
		// after this, the hierarchy must not change anymore
		setLocked(true);
	}
	
	void getAllPathsRecursive(ClassTerm node, String crpath, boolean[] visited, ArrayList paths) {
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClassTerm child = (ClassTerm)node.getChild(i);
			String new_path = node.getIndex() == -1 ? "" : crpath + "/";
			new_path = new_path + child.getID();
			paths.add(new_path);
			if (!visited[child.getIndex()]) {
				// If visited, then all paths for subtree below child are already included
				visited[child.getIndex()] = true;
				getAllPathsRecursive(child, new_path, visited, paths);
			}			
		}
	}
	
	public ArrayList getAllPaths() {
		ArrayList paths = new ArrayList();
		boolean[] visited = new boolean[getTotal()];
		getAllPathsRecursive(m_Root, "", visited, paths);
		return paths;
	}
		
	public void addAllClasses(ClassesTuple tuple, boolean[] matrix) {
		int idx = 0;
		tuple.setSize(countOnes(matrix));
		for (int i = 0; i < getTotal(); i++) {
			if (matrix[i]) tuple.setItemAt(new ClassesValue(getTermAt(i), 1.0), idx++);
		}
	}
		
	public void fillBooleanMatrixMaj(double[] mean, boolean[] matrix, double treshold) {
		for (int i = 0; i < getTotal(); i++) {
			ClassTerm term = getTermAt(i);
			if (mean[term.getIndex()] >= treshold/100.0) matrix[term.getIndex()] = true;
		}
	}
		
	public static void removeParentNodesRec(ClassTerm node, boolean[] matrix) {
		if (matrix[node.getIndex()]) {
			ClassTerm parent = node.getCTParent();
			while (parent.getIndex() != -1 && matrix[parent.getIndex()]) {
				matrix[parent.getIndex()] = false;
				parent = parent.getCTParent();
			}
		}
		for (int i = 0; i < node.getNbChildren(); i++) {
			removeParentNodesRec((ClassTerm)node.getChild(i), matrix);
		}
	}
	
	public static void removeParentNodes(ClassTerm node, boolean[] matrix) {
		for (int i = 0; i < node.getNbChildren(); i++) {
			removeParentNodesRec((ClassTerm)node.getChild(i), matrix);
		}
	}
	
	public static int countOnes(boolean[] matrix) {
		int count = 0;
		for (int i = 0; i < matrix.length; i++) {
			if (matrix[i]) count++;
		}
		return count;
	}
		
	// Currently not used
	public ClassesTuple getBestTupleMajNoParents(double[] mean, double treshold) {
		boolean[] classes = new boolean[getTotal()];
		fillBooleanMatrixMaj(mean, classes, treshold);
		removeParentNodes(getRoot(), classes);
		ClassesTuple tuple = new ClassesTuple();
		addAllClasses(tuple, classes);
		return tuple;
	}
	
	public ClassesTuple getBestTupleMaj(double[] mean, double treshold) {
		boolean[] classes = new boolean[getTotal()];
		fillBooleanMatrixMaj(mean, classes, treshold);
		ClassesTuple tuple = new ClassesTuple();
		addAllClasses(tuple, classes);
		return tuple;
	}		
		
	public final CompleteTreeIterator getNoRootIter() {
		CompleteTreeIterator iter = new CompleteTreeIterator(m_Root);
		if (iter.hasMoreNodes()) iter.getNextNode();
		return iter;
	}
	
	public final CompleteTreeIterator getRootIter() {
		return new CompleteTreeIterator(m_Root);
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
	
	public final int getTotal() {
		return m_ClassList.size();
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
		calcWeights();
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
	
	public final ClassTerm getClassTermTree(ClassesValue vl) throws ClusException {
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
				if (found == null) throw new ClusException("Classes value not in tree hierarchy: "+vl.toPathString());
				subterm = found;
			}			
			pos++;
		}
	}
	
	public final ClassTerm getClassTermDAG(ClassesValue vl) throws ClusException {
		ClassTerm term = getClassTermByName(vl.getMostSpecificClass());
		if (term == null) throw new ClusException("Classes value not in DAG hierarchy: "+vl.toPathString());
		return term;
	}
	
	public final ClassTerm getClassTerm(ClassesValue vl) throws ClusException {
		if (isTree()) {
			return getClassTermTree(vl);
		} else {
			return getClassTermDAG(vl);
		}
	}
		
	public final int getClassIndex(ClassesValue vl) throws ClusException {
		return getClassTerm(vl).getIndex();
	}
	
	public final double getWeight(int idx) {
		return m_Weights[idx];
	}
		
	public final void setEvalClasses(ClassesTuple eval) {
		m_Eval = eval;
	}
	
	public final ClassesTuple getEvalClasses() {
		return m_Eval;
	}
	
	public final boolean[] getEvalClassesVector() {
		if (m_Eval == null) {
			boolean[] res = new boolean[getTotal()];
			Arrays.fill(res, true);
			return res;
		} else {
			return m_Eval.getVectorBoolean(this);
		}
	}
	
	public void writeTargets(RowData data, ClusSchema schema, String name) throws ClusException, IOException {
		double[] wis = getWeights();
		PrintWriter wrt = new PrintWriter(new FileWriter(name + ".weights"));	
		wrt.print("weights(X) :- X = [");
		for (int i = 0; i < wis.length; i++) {
			if (i != 0) wrt.print(",");
			wrt.print(wis[i]);				
		}
		wrt.println("].");
		wrt.println();
		ClassTerm[] terms = new ClassTerm[wis.length];
		CompleteTreeIterator iter = getRootIter();
		while (iter.hasMoreNodes()) {
			ClassTerm node = (ClassTerm)iter.getNextNode();
			if (node.getIndex() != -1) terms[node.getIndex()] = node;
		}
		for (int i = 0; i < wis.length; i++) {
			wrt.print("% class "+terms[i]+": ");
			wrt.println(wis[i]);				
		}
		wrt.close();
		ClusAttrType[] keys = schema.getAllAttrUse(ClusAttrType.ATTR_USE_KEY);
		int sidx = getType().getArrayIndex();
		wrt = new PrintWriter(new FileWriter(name + ".targets"));
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);
			int pos = 0;
			for (int j = 0; j < keys.length; j++) {
				if (pos != 0) wrt.print(",");
				wrt.print(keys[j].getString(tuple));
				pos++;
			}
			ClassesTuple target = (ClassesTuple)tuple.getObjVal(sidx);
			double[] vec = target.getVectorNodeAndAncestors(this);
			wrt.print(",");
			wrt.print(target.toString());
			wrt.print(",[");
			for (int j = 0; j < vec.length; j++) {
				if (j != 0) wrt.print(",");
				wrt.print(vec[j]);				
			}
			wrt.println("]");			
		}		
		wrt.close();
	}
	
	public void setLocked(boolean lock) {
		m_IsLocked = lock;
	}
	
	public boolean isLocked() {
		return m_IsLocked;
	}
	
	public void setHierType(int type) {
		m_HierType = type;
	}
	
	public boolean isTree() {
		return m_HierType == TREE;
	}
	
	public boolean isDAG() {
		return m_HierType == DAG;
	}	
	
	public ClassTerm getClassTermByName(String id) {
		return (ClassTerm)m_ClassMap.get(id);
	}
	
	public void addClassTerm(String id, ClassTerm term) {
		m_ClassMap.put(id, term);
	}
	
	public void addClassTerm(ClassTerm term) {
		m_ClassList.add(term);
	}
	
	public ClassTerm getTermAt(int i) {
		return (ClassTerm)m_ClassList.get(i);
	}
}
