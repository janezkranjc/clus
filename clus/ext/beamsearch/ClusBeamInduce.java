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

package clus.ext.beamsearch;

import clus.Clus;
import clus.algo.*;
import clus.algo.split.NominalSplit;
import clus.algo.tdidt.*;
import clus.data.rows.RowData;
import clus.data.type.*;
import clus.ext.ensembles.ClusForest;
import clus.main.*;
import clus.util.*;
import clus.nominal.split.*;
import clus.pruning.PruneTree;
import clus.model.ClusModel;
import clus.model.ClusModelInfo;
import clus.model.modelio.*;

import java.io.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;


public class ClusBeamInduce extends ClusInductionAlgorithm {
	
	protected NominalSplit m_Split;
	protected ClusBeamSearch m_Search;
	protected ArrayList m_BeamSimTrain;
	protected ArrayList m_BeamSimTest;
	static boolean isHeaderWritten = false;
	
	public ClusBeamInduce(ClusSchema schema, Settings sett, ClusBeamSearch search) throws ClusException, IOException {
		super(schema, sett);
		m_Search = search;
		m_BeamSimTrain = new ArrayList();
		m_BeamSimTest = new ArrayList();
	}
	
	public void initializeHeuristic() {
		m_Search.initializeHeuristic();
	}
		
	public boolean isModelWriter() {
		return true;
	}
	
	public void writeModel(ClusModelCollectionIO strm) throws IOException {
		m_Search.writeModel(strm);
	}
	
	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		ClusNode root = m_Search.beamSearch(cr);
		root.updateTree();
		return root;
	}
	
	public void induceAll(ClusRun cr) throws ClusException, IOException {
		m_Search.beamSearch(cr);
		ClusModelInfo def_model = cr.addModelInfo(ClusModel.DEFAULT);
		def_model.setModel(ClusDecisionTree.induceDefault(cr));
		def_model.setName("Default");
		ArrayList lst = m_Search.getBeam().toArray();
		
		//the pruning is ON for all setings! This could be turned off when needed!
		if (cr.getStatManager().getSettings().getBeamTreeMaxSize() <= -1) postPruneBeamModels(cr, lst);
		
		if (cr.getStatManager().getSettings().getBeamSortOnTrainParameter())sortModels(cr, lst);
		if (!cr.getStatManager().getSettings().isFastBS())writeSimilarityFile(lst, cr);

		boolean toForest = cr.getStatManager().getSettings().isBeamToForest();
		ClusForest bForest = new ClusForest(getStatManager());
		
		for (int i = 0; i < lst.size(); i++) {
			ClusBeamModel mdl = (ClusBeamModel)lst.get(lst.size()-i-1);
			ClusNode tree = (ClusNode)mdl.getModel();			
			tree.updateTree();
			ClusModelInfo model_info = cr.addModelInfo(i+1);
			model_info.setModel(tree);
			model_info.setName("Beam "+(i+1));
			model_info.clearAll();
			if (toForest)bForest.addModelToForest((ClusModel)tree);
		}
		if (toForest){
			ClusModelInfo forest_info = cr.addModelInfo(lst.size()+1);
			forest_info.setModel(bForest);
			forest_info.setName("BeamToForest");
		}
	}
	
	/**Dragi, JSI
	 *Post Pruning of the models in the beam
	 * 
	 * @param cr - ClusRun
	 * @param arr - List with the beam
	 * @throws ClusException
	 */
	public void postPruneBeamModels(ClusRun cr, ArrayList arr) throws ClusException{
		for (int i=0; i<arr.size();i++){
			    PruneTree pruner = getStatManager().getTreePruner(null);
			    pruner.setTrainingData((RowData)cr.getTrainingSet());
			    pruner.prune((ClusNode)((ClusBeamModel)arr.get(i)).getModel());
		}
	}

	/**Dragi, JSI
	 * Sorts the beam according to train accuracy/correlation in descending order
	 * In case of equal train accuracy/correlation 
	 * then the tree with greater heuristic score are put higher
	 * @param cr - Clus Run
	 * @param arr	- List with the beam
	 * @throws ClusException 
	 * @throws ClusException
	 * @throws IOException
	 */
	public void sortModels(ClusRun cr, ArrayList arr) throws ClusException, IOException{
//		if (cr.getStatManager().getSettings().getBeamTreeMaxSize() <= -1) {
//			postPruneBeamModels(cr, arr);
//		}
		int size = arr.size();
		ClusBeamModel[] models = new ClusBeamModel[size];
		double[]err = new double [size];
		double[]heur = new double [size];
		for (int i = 0 ; i < size; i++){
			models[i] = (ClusBeamModel)arr.get(i);
			err[i] = Clus.calcModelError(cr.getStatManager(), (RowData)cr.getTrainingSet(), models[i].getModel());
			heur[i] = models[i].getValue();
		}
		ClusBeamModel cbm;
		double tmp;
		for (int j = 0; j < size -1; j++)
			for (int k = j+1; k < size; k++){
				if (err[j]>err[k]){
					cbm = models [j];
					models [j] = models [k];
					models [k] = cbm;
					tmp = err [j];
					err [j] = err [k];
					err [k] = tmp;
					tmp = heur [j];
					heur [j] = heur [k];
					heur [k] = tmp;
				}
				else if (err[j]==err[k]){
						if (heur[j]<heur[k]){
							cbm = models [j];
							models [j] = models [k];
							models [k] = cbm;
							tmp = err [j];
							err [j] = err [k];
							err [k] = tmp;
							tmp = heur [j];
							heur [j] = heur [k];
							heur [k] = tmp;
						} 
				}
			}
		arr.clear();
		for (int m = 0; m < size; m++)arr.add(models[m]);
	}
	
	public void writeSimilarityFile(ArrayList beam, ClusRun run) throws IOException, ClusException{
		String str = run.getStatManager().getSettings().getFileAbsolute(run.getStatManager().getSettings().getAppName())+".bsim";
		File output = new File(str);
		if (!isHeaderWritten)writeHeader(output, run.getStatManager().getSettings());
		FileWriter wrtr = new FileWriter(output, true);
		double[]sim = new double [2];
		//sim[0] - training
		//sim[1] - testing
		NumberFormat outF = ClusFormat.FOUR_AFTER_DOT;
		if ((run.getStatManager().getMode()!=1)&&(run.getStatManager().getMode()!=0)){
			System.err.println(getClass().getName()+"writeSimilarityFile(): Unhandled Type of Target Attribute");
			throw new ClusException("Unhandled Type of Target Attribute");
		}
		boolean isNum = (run.getStatManager().getMode()==1);
		sim[0] = ClusBeamModelDistance.calcBeamSimilarity(beam, (RowData)run.getTrainingSet(), isNum);
		m_BeamSimTrain.add(Double.valueOf(sim[0]));
		try {
		sim[1] = ClusBeamModelDistance.calcBeamSimilarity(beam, run.getTestSet(), isNum);
		m_BeamSimTest.add(Double.valueOf(sim[1]));	
			if (Settings.IS_XVAL){
				wrtr.write("Fold "+run.getIndexString()+":\t"+outF.format(sim[0])+"\t\t"+outF.format(sim[1])+"\n");
				if (run.getIndex() == run.getStatManager().getSettings().getXValFolds()){
					//we reached the last fold, so we write a summary
					wrtr.write("---------------------------------------\n");
					wrtr.write("Summary:\t"+outF.format(getAverage(m_BeamSimTrain))+"\t\t"+outF.format(getAverage(m_BeamSimTest))+"\n");
				}
			}else wrtr.append("\t\t"+outF.format(sim[0])+"\t\t"+outF.format(sim[1])+"\n");
		}catch(NullPointerException e){
			if (!Settings.IS_XVAL) wrtr.append("Summary:\t"+outF.format(sim[0])+"\t\t"+"N/A"+"\n");
		}
		wrtr.flush();
	}
	
	public void writeHeader(File output, Settings sett) throws IOException{
		FileWriter wrtr = new FileWriter(output);
		wrtr.write("Clus Beam-Search run\n");
		wrtr.write("----------------------\n");
		wrtr.write("Date:\t"+DateFormat.getInstance().format(sett.getDate())+"\n");
		wrtr.write("File:\t"+output+"\n");
		wrtr.write("\n");
		wrtr.write("Beam Similarity Output\n");
		wrtr.write("----------------------\n");
		wrtr.write("\t\tTraining\tTesting\n");
		wrtr.write("---------------------------------------\n");
		wrtr.flush();
		isHeaderWritten = true;
	}
	
	public static double getAverage(ArrayList arr){
	double result = 0.0;
	for (int i = 0; i < arr.size(); i++)
		result += ((Double)arr.get(i)).doubleValue();
	return result / arr.size();
	}
}
