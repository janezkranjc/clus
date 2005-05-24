package clus.error;

import java.io.*;
import jeans.util.*;

import clus.main.*;
import clus.util.*;

public class ContingencyTable extends ClusNominalError {

	protected final static String REAL_PRED = "REAL\\PRED";

	protected int[][][] m_ContTable;
	protected TargetSchema m_Schema;

	public ContingencyTable(ClusErrorParent par, TargetSchema schema) {
		super(par, schema.getNbNomAndNum());
		m_Schema = schema;
		m_ContTable = new int[m_Dim][][];
		for (int i = 0; i < m_Dim; i++) {
			int size = schema.getNbNomValues(i);
			m_ContTable[i] = new int[size][size];
		}
	}
	
	public int calcNbCorrect(int[][] table) {
		int sum = 0;
		int size = table.length;
		for (int j = 0; j < size; j++) {
			sum += table[j][j];
		}
		return sum;
	}
	
	public double calcXSquare(int[][] table) {
		int size = table.length;
		int[] ri = new int[size];
		int[] cj = new int[size];		
		for (int j = 0; j < size; j++) {
			ri[j] = sumRow(table, j);
			cj[j] = sumColumn(table, j);
		}
		double xsquare = 0.0;
		int nb = getNbExamples();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				double eij = (double)ri[i]*cj[j]/nb;
				double err = (double)table[i][j] - eij;
				if (err != 0.0)	xsquare += err*err/eij;
			}
		}
		return xsquare;		
	}
	
	public double calcCramerV(int[][] table) {
		int q = table.length;
		int n = getNbExamples();
		double div = (double)n*(q-1);
		return Math.sqrt(calcXSquare(table)/div);
	}
	
	public double calcAccuracy(int[][] table) {
		return (double)calcNbCorrect(table)/getNbExamples();
	}
	
	public double calcDefaultAccuracy(int i) {
		return 0.0;
	}	
	
	public void showAccuracy(PrintWriter out, int i) {
		double acc = calcAccuracy(m_ContTable[i]);
		out.print("Accuracy: "+ClusFormat.SIX_AFTER_DOT.format(acc));
		out.println();
	}

	public void add(ClusError other) {
		ContingencyTable cont = (ContingencyTable)other;
		for (int i = 0; i < m_Dim; i++) {
			int t1[][] = m_ContTable[i];
			int t2[][] = cont.m_ContTable[i];
			int size = t1.length;
			for (int j = 0; j < size; j++) {
				for (int k = 0; k < size; k++) {
					t1[j][k] += t2[j][k];
				}
			}
		}
	}	
	
	public void showModelError(PrintWriter out, int detail) {
		if (detail == DETAIL_VERY_SMALL) {
			out.print(getPrefix()+"[");
			for (int i = 0; i < m_Dim; i++) {
				if (i != 0) out.print(",");
				double acc = calcAccuracy(m_ContTable[i]);
				out.print(ClusFormat.SIX_AFTER_DOT.format(acc));
			}
			out.println("]");
		} else {
			for (int i = 0; i < m_Dim; i++) {
				out.println(getPrefix()+"Attribute: "+m_Schema.getNomName(i));
				out.println();
				showContTable(out, i, m_Schema);
			}
		}
	}	
	
	public int sumColumn(int[][] table, int j) {
		int sum = 0;
		int size = table.length;
		for (int i = 0; i < size; i++)
			sum += table[i][j];
		return sum;
	}
	
	public int sumRow(int[][] table, int i) {
		int sum = 0;
		int size = table.length;
		for (int j = 0; j < size; j++)
			sum += table[i][j];
		return sum;
	}	
	
	public void showContTable(PrintWriter out, int i, TargetSchema schema) {
		int[][] table = m_ContTable[i];
		int size = schema.getNbNomValues(i);
		// Calculate sizes
		int[] wds = new int[size+2];
		// First column
		wds[0] = REAL_PRED.length();
		for (int j = 0; j < size; j++) {
			wds[j+1] = schema.getIntVal(i,j).length()+1;
		}
		// Middle columns
		for (int j = 0; j < size; j++) {
			wds[0] = Math.max(wds[0], schema.getIntVal(i,j).length());
			for (int k = 0; k < size; k++) {
				String str = String.valueOf(table[j][k]);
				wds[k+1] = Math.max(wds[k+1], str.length()+1);
			}
			String str = String.valueOf(sumRow(table, j));
			wds[size+1] = Math.max(wds[size+1], str.length()+1);
		}
		// Bottom row
		for (int k = 0; k < size; k++) {
			String str = String.valueOf(sumColumn(table, k));
			wds[k+1] = Math.max(wds[k+1], str.length()+1);
		}					
		// Total sum
		wds[size+1] = Math.max(wds[size+1], String.valueOf(getNbExamples()).length()+1);
		// Calculate line width
		int s = 0;
		for (int j = 0; j < size+2; j++) s += wds[j];
		String horiz = getPrefix()+"  "+StringUtils.makeString('-', s+(size+1)*2);
		// Header		
		out.print(getPrefix()+"  ");
		printString(out, wds[0], REAL_PRED);
		out.print(" |");
		for (int j = 0; j < size; j++) {
			printString(out, wds[j+1], schema.getIntVal(i,j));
			out.print(" |");
		}
		out.println();
		out.println(horiz);
		// Data rows
		for (int j = 0; j < size; j++) {
			out.print(getPrefix()+"  ");
			printString(out, wds[0], schema.getIntVal(i,j));	
			out.print(" |");			
			for (int k = 0; k < size; k++) {
				printString(out, wds[k+1], String.valueOf(table[j][k]));
				out.print(" |");
			}
			printString(out, wds[size+1], String.valueOf(sumRow(table, j)));
			out.println();
		}						
		out.println(horiz);
		out.print(getPrefix()+"  ");
		out.print(StringUtils.makeString(' ', wds[0]));
		out.print(" |");
		for (int k = 0; k < size; k++) {
			printString(out, wds[k+1], String.valueOf(sumColumn(table, k)));
			out.print(" |");
		}
		printString(out, wds[size+1], String.valueOf(getNbExamples()));
		out.println();
		out.println();		
		out.print(getPrefix()+"  ");		
		showAccuracy(out, i);
		out.print(getPrefix()+"  ");		
		double cramer = calcCramerV(table);
		out.println("Cramer's coefficient: "+ClusFormat.SIX_AFTER_DOT.format(cramer));		
		out.println();
	}
	
	public void showSummaryError(PrintWriter out, boolean detail) {		
		if (!detail) {	
			for (int i = 0; i < m_Dim; i++) {
				out.print(getPrefix()+"Attribute: "+m_Schema.getNomName(i)+" - ");
				showAccuracy(out, i);
			}			
		}
	}	
	
	public void printString(PrintWriter out, int wd, String str) {
		out.print(StringUtils.makeString(' ', wd-str.length()));
		out.print(str);
	}
	
	public String getName() {
		return "Classification Error";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new ContingencyTable(par, m_Schema);
	}

	public void addExample(int[] real, int[] predicted) {
		for (int i = 0; i < m_Dim; i++) {
			m_ContTable[i][real[i]][predicted[i]]++;
		}
	}
}
