
package addon.hmc.HMCConvertToSC;

import java.util.*;
import jeans.util.*;

import clus.*;
import clus.main.*;
import clus.data.rows.*;
import clus.data.io.*;
import clus.ext.hierarchical.*;

public class HMCConvertToSC {

	public final static boolean CREATE_TRAIN_TUNE_TEST_SPLIT = true;
	
	public void convert(String input, String output) throws Exception {
		Clus clus = new Clus();
		String appname = FileUtil.getName(input)+".s";
		clus.initializeAddOn(appname);
		ClusStatManager mgr = clus.getStatManager();
		Settings sett = clus.getSettings();

		ClassHierarchy hier = mgr.getHier();
		int sidx = hier.getType().getArrayIndex();
		String[] classterms = new String[hier.getTotal()];
		for (int i=0; i < hier.getTotal(); i++) {
			ClassTerm term = hier.getTermAt(i);
			classterms[i] = term.toStringHuman(hier);
		}		
		

		boolean[][] classes;
		if (CREATE_TRAIN_TUNE_TEST_SPLIT) {
			ClusRun run = clus.partitionData();

			RowData train = (RowData)run.getTrainingSet();
			classes = new boolean[train.getNbRows()][hier.getTotal()];
			for (int i = 0; i < train.getNbRows(); i++) {
	   			DataTuple tuple = train.getTuple(i);
	   			ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
	   			Arrays.fill(classes[i], false);
	   			tp.fillBoolArrayNodeAndAncestors(classes[i]);
	   		}		
			ARFFFile.writeArffToSC(output+".train.arff", train, classterms, classes);

			if (!sett.isNullTestFile()) {
				RowData test  = (RowData)run.getTestSet();
				classes = new boolean[test.getNbRows()][hier.getTotal()];
				for (int i = 0; i < test.getNbRows(); i++) {
		   			DataTuple tuple = test.getTuple(i);
		   			ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
		   			Arrays.fill(classes[i], false);
		   			tp.fillBoolArrayNodeAndAncestors(classes[i]);
		   		}		
				ARFFFile.writeArffToSC(output+".test.arff", test, classterms, classes);
			}
			if (!sett.isNullPruneFile()) {
				RowData tune  = (RowData)run.getPruneSet();	
				classes = new boolean[tune.getNbRows()][hier.getTotal()];
				for (int i = 0; i < tune.getNbRows(); i++) {
		   			DataTuple tuple = tune.getTuple(i);
		   			ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
		   			Arrays.fill(classes[i], false);
		   			tp.fillBoolArrayNodeAndAncestors(classes[i]);
		   		}		
				ARFFFile.writeArffToSC(output+".valid.arff", tune, classterms, classes);			
			}		
		} else {
			RowData data = (RowData)clus.getData();		
			classes = new boolean[data.getNbRows()][hier.getTotal()];
			for (int i = 0; i < data.getNbRows(); i++) {
	   			DataTuple tuple = data.getTuple(i);
	   			ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
	   			Arrays.fill(classes[i], false);
	   			tp.fillBoolArrayNodeAndAncestors(classes[i]);
	   		}		
			ARFFFile.writeArffToSC(output+".arff", data, classterms, classes);
		}
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: HMCConvertToSC input.arff output.arff");
			System.exit(0);
		}
		String input = args[0];
		String output = args[1];
		HMCConvertToSC cnv = new HMCConvertToSC();
		try {
			cnv.convert(input, output);
		} catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace();
		}
	}	
}
