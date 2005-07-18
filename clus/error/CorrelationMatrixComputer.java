/*
 * Created on May 10, 2005
 */
package clus.error;

import clus.data.rows.*;
import clus.data.type.*;
import clus.data.type.ClusAttrType;
import clus.main.*;
import clus.util.*;

public class CorrelationMatrixComputer {

	PearsonCorrelation[][] m_Matrix;
	
	public void compute(RowData data) {
		ClusSchema schema = data.getSchema();
		TargetSchema target = schema.getTargetSchema();
		int nb_num = target.getNbNum();
		m_Matrix = new PearsonCorrelation[nb_num][nb_num];
		TargetSchema ts2 = new TargetSchema(0, 1);		
		ClusErrorParent par = new ClusErrorParent(null);
		NumericAttrType[] attrs = null;
		for (int i = 0; i < nb_num; i++) {
			for (int j = 0; j < nb_num; j++) {
				m_Matrix[i][j] = new PearsonCorrelation(par, attrs);
			}
		}
		double[] a1 = new double[1];
		double[] a2 = new double[1];		
		par.setNbExamples(data.getNbRows());
		ClusAttrType[] numtypes = target.getNumTypes();
		for (int i = 0; i < data.getNbRows(); i++) {
			DataTuple tuple = data.getTuple(i);			
			for (int j = 0; j < nb_num; j++) {
				for (int k = 0; k < nb_num; k++) {
					a1[0] = ((NumericAttrType)numtypes[j]).getNumeric(tuple);
					a2[0] = ((NumericAttrType)numtypes[k]).getNumeric(tuple);
					m_Matrix[j][k].addExample(a1, a2);
				}
			}				
		}
	}
	
	public void printMatrixTeX() {
		int nb_num = m_Matrix.length;
		System.out.println("Number of numeric: "+nb_num);
		System.out.println();
		System.out.print("\\begin{tabular}{");
		for (int i = 0; i < nb_num+1; i++) {
			System.out.print("l");
		}
		System.out.println("}");
		for (int i = 0; i < nb_num; i++) {
			System.out.print(" & "+(i+1));
		}
		System.out.println("\\\\");
		for (int i = 0; i < nb_num; i++) {
			System.out.print(i+1);
			for (int j = 0; j < nb_num; j++) {
				double corr = m_Matrix[i][j].getCorrelation(0);
				System.out.print(" & "+ClusFormat.THREE_AFTER_DOT.format(corr));
			}
			System.out.println("\\\\");
		}
		System.out.println("\\end{tabular}");
	}
	
}
