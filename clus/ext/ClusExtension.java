package clus.ext;

import jeans.util.cmdline.*;

import clus.main.*;
import clus.util.*;
import clus.algo.induce.*;

import java.io.*;

public abstract class ClusExtension {
	
	public final static int REGULAR_TREE = 0;
	public final static int SET_OF_TREES = 1;
	public final static int BEAM = 2;
	public final static int RULE_SET = 3;	
	public final static int RULE = 4;	

	public abstract ClusInduce createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException;

}
