package clus.ext;

import jeans.util.cmdline.*;

import clus.main.*;
import clus.util.*;
import clus.algo.induce.*;

import java.io.*;

public class DefaultExtension extends ClusExtension {
	
	public ClusInduce createInduce(ClusSchema schema, Settings sett, CMDLineArgs cargs) throws ClusException, IOException {
		schema.addIndices(ClusSchema.ROWS);
		if (sett.hasConstraintFile()) {
			boolean fillin = cargs.hasOption("fillin");
			return new ConstraintDFInduce(schema, sett, fillin);
		} else {
			return new DepthFirstInduce(schema, sett);
		}
	}
}
