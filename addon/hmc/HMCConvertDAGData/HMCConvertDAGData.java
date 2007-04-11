
import jeans.util.*;
import clus.*;
import clus.main.*;
import clus.statistic.ClusStatistic;
import clus.data.rows.*;
import clus.data.type.ClusAttrType;
import clus.ext.hierarchical.ClassHierarchy;
import clus.ext.hierarchical.WHTDStatistic;
import clus.io.*;

public class HMCConvertDAGData {

	public final static boolean CREATE_TRAIN_TUNE_TEST_SPLIT = true;
	
	public void convert(String input, String output) throws Exception {
		Clus clus = new Clus();
		String appname = FileUtil.getName(input)+".s";
		clus.initializeAddOn(appname);
		ClusStatManager mgr = clus.getStatManager();
		ClusNode root = new ClusNode();
		Settings sett = clus.getSettings();
		RowData data = (RowData)clus.getData();		
		if (CREATE_TRAIN_TUNE_TEST_SPLIT) {
			ClusRun run = clus.partitionData();
			ClusStatistic[] stats = new ClusStatistic[1];
			stats[0] = mgr.createClusteringStat();
			data.calcTotalStats(stats);
			if (!sett.isNullTestFile()) {
				System.out.println("Loading: " + sett.getTestFile());
				clus.updateStatistic(sett.getTestFile(), stats);
			}
			if (!sett.isNullPruneFile()) {
				System.out.println("Loading: " + sett.getPruneFile());
				clus.updateStatistic(sett.getPruneFile(), stats);
			}
			ClusStatistic.calcMeans(stats);
			WHTDStatistic stat = (WHTDStatistic)stats[0];
			stat.showRootInfo();
			ClassHierarchy hier = mgr.getHier();
			hier.removeZeroClasses(stat);
			RowData train = (RowData)run.getTrainingSet();
			RowData tune  = (RowData)run.getPruneSet();
			RowData test  = (RowData)run.getTestSet();
			ARFFFile.writeArff(output+"-train.arff", train);
			ARFFFile.writeArff(output+"-tune.arff", tune);			
			ARFFFile.writeArff(output+"-test.arff", test);			
		} else {
			ARFFFile.writeArff(output, data);
		}
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: HMCConvertDAGData input.arff output.arff");
			System.exit(0);
		}
		String input = args[0];
		String output = args[1];
		HMCConvertDAGData cnv = new HMCConvertDAGData();
		try {
			cnv.convert(input, output);
		} catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace();
		}
	}	
}
