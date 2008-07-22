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

package hmcnwmodels;

import java.io.*;
import java.util.*;

import jeans.util.array.StringTable;
import jeans.util.cmdline.*;

import clus.Clus;
import clus.algo.*;
import clus.algo.tdidt.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.ext.hierarchical.*;
import clus.main.*;
import clus.model.*;
import clus.model.modelio.*;
import clus.statistic.*;
import clus.util.*;

public class HMCNodeWiseModels implements CMDLineArgsProvider {

	private static String[] g_Options = {};
	private static int[] g_OptionArities = {};

	protected Clus m_Clus;
	protected CMDLineArgs m_Cargs;
	protected StringTable m_Table = new StringTable();
	protected Hashtable m_Mappings;

	public void run(String[] args) throws IOException, ClusException, ClassNotFoundException {
			m_Clus = new Clus();
			Settings sett = m_Clus.getSettings();
			m_Cargs = new CMDLineArgs(this);

			String[] newargs = new String[args.length-1];
			for (int i=0; i<newargs.length; i++)
			{
				newargs[i] = args[i];
			}
			readFtests(args[args.length-1]);
			m_Cargs.process(newargs);

//			m_Cargs.process(args);

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

	private void readFtests(String filename) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String s;
			s = in.readLine();
			String[] parts;
			m_Mappings = new Hashtable();
			while (s!=null)
			{
				parts = s.split("\t");
				m_Mappings.put(parts[0], parts[1]);
				s = in.readLine();
			}
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
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

	public void doOneNode(ClassTerm node, ClassHierarchy hier, RowData train, RowData valid) throws ClusException, IOException {
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
			ClusInductionAlgorithmType clss = new ClusDecisionTree(m_Clus);
			m_Clus.recreateInduce(m_Cargs, clss, cschema, childData);
			String name = m_Clus.getSettings().getAppName() + "-" + nodeName + "-" + childName;
			ClusRun cr = new ClusRun(childData.cloneData(), m_Clus.getSummary());
			cr.createTrainIter();

			if (valid!=null) {
				RowData validNodeData = getNodeData(valid, node.getIndex());
				RowData validChildData = createChildData(validNodeData, ctype, child.getIndex());
				TupleIterator iter = validChildData.getIterator();
				cr.setTestSet(iter);
				m_Clus.initializeSummary(clss);
			}

/*			String fstr = (String) m_Mappings.get(parentChildName);
			if (fstr==null) {
				System.out.println("geen ftest gevonden voor "+ parentChildName);
			}
			else {
				System.out.println("fstr: "+ fstr);
				float ft = Float.valueOf(fstr);
				m_Clus.getSettings().setFTest(ft);
			}
*/
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
			io.addModel(cr.addModelInfo(ClusModel.ORIGINAL));
			io.save("nodewise/model/" + name + ".model");

		}
	}

	public void computeRecursive(ClassTerm node, ClassHierarchy hier, RowData train, RowData valid, boolean[] computed) throws ClusException, IOException {
		if (!computed[node.getIndex()]) {
			// remember that we did this one
			computed[node.getIndex()] = true;
			doOneNode(node, hier, train, valid);
			// recursively do children
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClassTerm child = (ClassTerm)node.getChild(i);
				computeRecursive(child, hier, train, valid, computed);
			}
		}
	}

	public void computeRecursiveRoot(ClassTerm node, ClassHierarchy hier, RowData train, RowData valid, boolean[] computed) throws ClusException, IOException {
		doOneNode(node, hier, train, valid);
		for (int i = 0; i < node.getNbChildren(); i++) {
			ClassTerm child = (ClassTerm)node.getChild(i);
			computeRecursive(child, hier, train, valid, computed);
		}
	}

	public void doRun() throws IOException, ClusException, ClassNotFoundException {
		ClusRun cr = m_Clus.partitionData();
		RowData train = (RowData)cr.getTrainingSet();
		RowData valid = (RowData)cr.getTestSet();
		ClusStatManager mgr = m_Clus.getStatManager();
		ClassHierarchy hier = mgr.getHier();
		ClassTerm root = hier.getRoot();
		boolean[] computed = new boolean[hier.getTotal()];
		computeRecursiveRoot(root, hier, train, valid, computed);
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
