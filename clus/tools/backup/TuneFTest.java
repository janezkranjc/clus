import clus.tools.debug.Debug;

import java.io.*;
import java.util.*;
import jeans.util.*;

import clus.main.*;
import clus.util.*;
import clus.data.type.*;
import clus.error.*;
import clus.error.multiscore.*;
import clus.data.cols.*;
import clus.data.cols.attribute.*;
import clus.algo.induce.*;
import clus.heuristic.*;
import clus.selection.*;

public class TuneFTest {

	public int m_Folds = 10;
	public Clus m_Clus;
	public int[] m_Freq = new int[6];

	public TuneFTest(Clus clus) {
		m_Clus = clus;
	}
	
	// for each possible value of ftest, do cross-validation
	public int tuneFTest(ClusRun p_cr) throws IOException, ClusException {
		int bestf = -1;
		double besterr = Double.POSITIVE_INFINITY;
		ClusSchema schema = m_Clus.getSchema();		
		boolean prevTalking = Settings.enableVerbose(false);
		
		PredictionWriter.getInstance().setMainEnabled(false);		
		
		for (int ftest = 1; ftest <= 5; ftest++) {
			Settings.FTEST_LEVEL = ftest;
			// Do cross-validation
			ClusSummary csum = makeSummary();
			ClusData data = p_cr.getTrainingSet();
			int nbrows = data.getNbRows();
			System.err.println("Internal ftest: "+FTest.FTEST_LEVEL[ftest]+" on: "+nbrows);
			System.err.print("   ");
			XValMainSelection sel = schema.getXValSelection(data);			
			for (int i = 0; i < m_Folds; i++) {
				System.err.print(i+" ");
				System.err.flush();
				XValSelection msel = new XValSelection(sel, i);
				ClusRun m_cr = m_Clus.partitionData(data, msel, false, csum, i+1);
				m_cr.prepare();			 // Sort numeric attrs
				m_Clus.induce(m_cr);		 // Induce tree
				m_cr.unprepare();		 // Reset numeric data			
				m_Clus.calcError(m_cr, csum); 	 // Calc error
				m_cr.unPartitionData();    	 // Re-join partition
			}	
			System.err.println();
			csum.calculate();
			ClusErrorParent testerr = csum.getTestError();
			SquaredAbsoluteError sqabserr = (SquaredAbsoluteError)testerr.getError(0);
			double value = sqabserr.getSummaryError();
			if (value < besterr) {
				besterr = value;
				bestf = ftest;
			}
			System.err.println("   Error: "+value);	
		}
		Settings.enableVerbose(prevTalking);		
		System.err.println("Best error: "+besterr+" -> "+FTest.FTEST_LEVEL[bestf]);
		return bestf;		
	}
	
	public ClusSummary makeSummary() {
		ClusSummary summ = null;
/*		ClusSummary summ = new ClusSummary();
		ClusErrorParent train = new ClusErrorParent(m_Clus.getTargetSchema());
		train.addError(new SquaredAbsoluteError(train));		
		summ.setTrainTestError(train);*/
		return summ;
	}
	
	public void outputParams(String appname) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(appname+".param")));	
		int bidx = -1;
		int bfreq = 0;
		for (int i = 1; i <= 5; i++) {
			int idx = FTest.FTEST_INDEX[i];
			writer.print("Value: ");
			String st = String.valueOf(idx);
			writer.print(st+StringUtils.makeString(' ', 3-st.length()));
			writer.print(": ");			
			st = String.valueOf(FTest.FTEST_LEVEL[idx]);
			writer.print(st+StringUtils.makeString(' ', 7-st.length()));
			writer.print(" freq: ");
			st = String.valueOf(m_Freq[idx]);
			writer.println(st+StringUtils.makeString(' ', 4-st.length()));			
			if (m_Freq[idx] >= bfreq) {
				bfreq = m_Freq[idx];
				bidx = idx;
			}			
		}
		writer.println();
		writer.println("Best: "+bidx+": "+FTest.FTEST_LEVEL[bidx]);
		writer.close();
	}
	
	public void tune() throws IOException, ClusException {	
		ClusRun cr = m_Clus.partitionData();
		int bestf = tuneFTest(cr);
		double value = FTest.FTEST_LEVEL[bestf];
		System.out.println("Best FTest: "+value);
		Settings.setFTest(value);		
	}
	
	public void run(String appname, Date date) throws IOException, ClusException {
		ClusSchema schema = m_Clus.getSchema();
		ClusOutput output = new ClusOutput(appname+".xval", schema, m_Clus.getSettings());
		output.writeHeader(date);
		int nbrows = m_Clus.getNbRows();		
		XValMainSelection sel = schema.getXValSelection(m_Clus.getData());
		ClusSummary summary = m_Clus.getSummary();
		for (int i = 0; i < m_Folds; i++) {
			System.err.println("Cross-validation fold: "+i);		
			XValSelection msel = new XValSelection(sel, i);
			ClusRun cr = m_Clus.partitionData(msel, i+1);
			// Tune f-test
			Settings.FTEST_LEVEL = tuneFTest(cr);
			System.err.println("Running with F-test: "+Settings.FTEST_LEVEL);
			m_Freq[Settings.FTEST_LEVEL]++;
			// Run on this partition
			cr.prepare();			 // Sort numeric attrs
			m_Clus.induce(cr);		 // Induce tree
			cr.unprepare();			 // Reset numeric data			
			m_Clus.calcError(cr, summary);   // Calc error
			cr.unPartitionData();  		 // Re-join partition
		}
		summary.calculate();
		output.writeSummary(summary);
		output.close();	
		outputParams(appname);
	}
}
