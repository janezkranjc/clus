import clus.tools.debug.Debug;


import jeans.io.*;
import jeans.io.ini.*;
import jeans.util.*;
import jeans.util.cmdline.*;

import java.io.*;
import java.util.*;
import java.text.*;

import clus.io.*;
import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.data.rows.*;
import clus.error.*;
import clus.error.multiscore.*;
import clus.statistic.*;
import clus.algo.induce.*;
import clus.selection.*;
import clus.ext.*;
import clus.ext.optxval.*;
import clus.ext.hierarchical.*;
import clus.pruning.*;

import clus.model.processor.*;

import clus.model.test.*;
import clus.statistic.*;

public class TestLoad {

	public static void main(String[] args) {
		ClusNode m_Root;	
		try {
			ObjectLoadStream open = new ObjectLoadStream(new FileInputStream("pruned.tree"));
			m_Root = (ClusNode)open.readObject();
			open.close();
			
			System.out.println("Tree:");
			m_Root.printTree(ClusFormat.OUT_WRITER, "");
			ClusFormat.OUT_WRITER.flush();
		} catch (Exception e) {
			System.out.println("Error loading tree: "+e.getMessage());
		}	
	}

}

