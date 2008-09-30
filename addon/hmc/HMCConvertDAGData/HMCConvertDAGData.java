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

package addon.hmc.HMCConvertDAGData;

import jeans.util.*;
import clus.*;
import clus.main.*;
import clus.statistic.ClusStatistic;
import clus.data.io.*;
import clus.data.rows.*;
import clus.ext.hierarchical.ClassHierarchy;
import clus.ext.hierarchical.WHTDStatistic;

public class HMCConvertDAGData {

	public final static boolean CREATE_TRAIN_TUNE_TEST_SPLIT = true;

	public void convert(String input, String output) throws Exception {
		Clus clus = new Clus();
		String appname = FileUtil.getName(input)+".s";
		clus.initializeAddOn(appname);
		ClusStatManager mgr = clus.getStatManager();
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
			hier.showSummary();
			RowData train = (RowData)run.getTrainingSet();
			ARFFFile.writeArff(output+".train.arff", train);
			if (!sett.isNullTestFile()) {
				RowData test  = (RowData)run.getTestSet();
				ARFFFile.writeArff(output+".test.arff", test);
			}
			if (!sett.isNullPruneFile()) {
				RowData tune  = (RowData)run.getPruneSet();
				ARFFFile.writeArff(output+".valid.arff", tune);
			}
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
