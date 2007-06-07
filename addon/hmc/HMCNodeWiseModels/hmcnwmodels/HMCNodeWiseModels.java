/*package hmcnwmodels;*/

import java.io.*;
import java.util.*;

import jeans.util.array.StringTable;
import jeans.util.cmdline.*;

import clus.Clus;
import clus.algo.tdidt.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.error.ClusErrorParent;
import clus.ext.hierarchical.*;
import clus.main.*;
import clus.model.modelio.ClusModelCollectionIO;
import clus.statistic.ClusStatistic;
import clus.util.*;

public class HMCNodeWiseModels implements CMDLineArgsProvider {
	
	private static String[] g_Options = {};
	private static int[] g_OptionArities = {};
	
	protected Clus m_Clus;
	protected CMDLineArgs m_Cargs;
	protected StringTable m_Table = new StringTable();

	public void run(String[] args) throws IOException, ClusException, ClassNotFoundException {
			m_Clus = new Clus();
			Settings sett = m_Clus.getSettings();
			m_Cargs = new CMDLineArgs(this);
			m_Cargs.process(args);
			if (m_Cargs.allOK()) {
				(new File("nodewise")).mkdir();				
				(new File("nodewise/out")).mkdir();
				(new File("nodewise/model")).mkdir();
				sett.setDate(new Date());
				sett.setAppName(m_Cargs.getMainArg(0));
				m_Clus.initSettings(m_Cargs);
				ClusDecisionTree clss = new ClusDecisionTree(m_Clus);
				m_Clus.initialize(m_Cargs, clss);
				doRun();
			}
	}
	
	public RowData getNodeData(RowData train, int nodeid) {
		ArrayList selected = new ArrayList();
		for (int i = 0; i < train.getNbRows(); i++) {
			DataTuple tuple = train.getTuple(i);
			ClassesTuple target = (ClassesTuple)tuple.getObjVal(0);
			if (nodeid == -1 || target.hasClass(nodeid)) {
				selected.add(tuple);
			}
		}
		return new RowData(selected, train.getSchema());			
	}
	
	public RowData createChildData(RowData nodeData, ClassesAttrType ctype, int childid) throws ClusException {
		// Create hierarchy with just one class
		ClassHierarchy chier = ctype.getHier();
		ClassesValue one = new ClassesValue("1", ctype.getTable());
		chier.addClass(one);
		chier.initialize();
		one.addHierarchyIndices(chier);
		RowData childData = new RowData(ctype.getSchema(), nodeData.getNbRows());
		for (int j = 0; j < nodeData.getNbRows(); j++) {
			ClassesTuple clss = null;
			DataTuple tuple = nodeData.getTuple(j);
			ClassesTuple target = (ClassesTuple)tuple.getObjVal(0);
			if (target.hasClass(childid)) {
				clss = new ClassesTuple(1);
				clss.addItem(new ClassesValue(one.getTerm()));
			} else {
				clss = new ClassesTuple(0);
			}
			DataTuple new_tuple = tuple.deepCloneTuple();
			new_tuple.setObjectVal(clss, 0);
			childData.setTuple(new_tuple, j);
		}
		return childData;
	}	

	public ClusSchema createChildSchema(ClusSchema oschema, ClassesAttrType ctype, String name) throws ClusException {
		ClusSchema cschema = new ClusSchema(name);
		for (int j = 0; j < oschema.getNbAttributes(); j++) {
			ClusAttrType atype = oschema.getAttrType(j);
			if (!(atype instanceof ClassesAttrType)) {
				ClusAttrType copy_atype = atype.cloneType();
				cschema.addAttrType(copy_atype);
			}
		}
		cschema.addAttrType(ctype);
		cschema.initializeSettings(m_Clus.getSettings());
		return cschema;
	}
	
	public void doOneNode(ClassTerm node, ClassHierarchy hier, RowData train) throws ClusException, IOException {
		// get data relevant to node
		RowData nodeData = getNodeData(train, node.getIndex());
		String nodeName = node.toPathString("=");
		// for each child, create new tree
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClassTerm child = (ClassTerm)node.getChild(i);
			String childName = child.toPathString("=");
			ClassesAttrType ctype = new ClassesAttrType(nodeName+"-"+childName);
			ClusSchema cschema = createChildSchema(train.getSchema(), ctype, "REL-"+nodeName+"-"+childName);
			RowData childData = createChildData(nodeData, ctype, child.getIndex());
			ClusClassifier clss = new ClusDecisionTree(m_Clus);
			m_Clus.recreateInduce(m_Cargs, clss, cschema, childData);
			String name = m_Clus.getSettings().getAppName() + "-" + nodeName + "-" + childName;
			ClusRun cr = new ClusRun(childData.cloneData(), m_Clus.getSummary());
			cr.createTrainIter();
			ClusOutput output = new ClusOutput("nodewise/out/" + name + ".out", cschema, m_Clus.getSettings());
			ClusStatistic tr_stat = m_Clus.getStatManager().createStatistic(ClusAttrType.ATTR_USE_ALL);
			cr.getTrainingSet().calcTotalStat(tr_stat);
			m_Clus.getStatManager().setTrainSetStat(tr_stat);				
			m_Clus.induce(cr, clss); // Induce model
			m_Clus.calcError(cr, null); // Calc error
			output.writeHeader();
			output.writeOutput(cr, true, true);
			output.close();
			ClusModelCollectionIO io = new ClusModelCollectionIO();
			io.addModel(cr.addModelInfo(ClusModels.ORIGINAL));
			io.save("nodewise/model/" + name + ".model");
		}
	}

	public void computeRecursive(ClassTerm node, ClassHierarchy hier, RowData train, boolean[] computed) throws ClusException, IOException {
		if (!computed[node.getIndex()]) {
			// remember that we did this one
			computed[node.getIndex()] = true;
			doOneNode(node, hier, train);
			// recursively do children
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClassTerm child = (ClassTerm)node.getChild(i);
				computeRecursive(child, hier, train, computed);
			}			
		}
	}
	
	public void computeRecursiveRoot(ClassTerm node, ClassHierarchy hier, RowData train, boolean[] computed) throws ClusException, IOException {
		doOneNode(node, hier, train);
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClassTerm child = (ClassTerm)node.getChild(i);
			computeRecursive(child, hier, train, computed);
		}			
	}	
	
	public void doRun() throws IOException, ClusException, ClassNotFoundException {
		ClusRun cr = m_Clus.partitionData();
		RowData train = (RowData)cr.getTrainingSet();
		ClusStatManager mgr = m_Clus.getStatManager();
		ClassHierarchy hier = mgr.getHier();
		ClassTerm root = hier.getRoot();
		boolean[] computed = new boolean[hier.getTotal()];
		computeRecursiveRoot(root, hier, train, computed);
	}
		
	public String[] getOptionArgs() {
		return g_Options;
	}
	
	public int[] getOptionArgArities() {
		return g_OptionArities;
	}
	
	public int getNbMainArgs() {
		return 1;
	}

	public void showHelp() {
	}
		
	public static void main(String[] args) {
		try {
			HMCNodeWiseModels m = new HMCNodeWiseModels();
			m.run(args);
		} catch (IOException io) {
			System.out.println("IO Error: "+io.getMessage());
		} catch (ClusException cl) {
			System.out.println("Error: "+cl.getMessage());
		} catch (ClassNotFoundException cn) {
			System.out.println("Error: "+cn.getMessage());
		}
	}
	
}
