
package clus.ext.hierarchical;

import java.util.*;
import java.io.*;

import clus.data.rows.*;

import jeans.util.array.*;


public class HierMatrixOutput {	

	public static void writeExamples(RowData data, ClassHierarchy hier) {
		try {
			PrintWriter wrt = data.getSchema().getSettings().getFileAbsoluteWriter("examples.matrix");
			writeHeader(hier, wrt);
			ClassesAttrType type = hier.getType();		
			int sidx = type.getArrayIndex();	
			double[] vector = new double[hier.getTotal()];
			for (int i = 0; i < data.getNbRows(); i++) {
				Arrays.fill(vector, 0.0);
				DataTuple tuple = data.getTuple(i);
				ClassesTuple tp = (ClassesTuple)tuple.getObjVal(sidx);
				for (int j = 0; j < tp.size(); j++) {
					ClassesValue val = tp.elementAt(j);
					vector[val.getIndex()] = 1.0;
				}
				wrt.println(MDoubleArray.toString(vector));
			}
			wrt.close();
		} catch (IOException e) {
			System.out.println("Error: "+e.getMessage());
		}
	}

	public static void writeHeader(ClassHierarchy hier, PrintWriter wrt) {		
		wrt.print("[");
		for (int i = 0; i < hier.getTotal(); i++) {
			if (i != 0) wrt.print(",");
			wrt.print(String.valueOf(hier.getWeight(i)));			
		}
		wrt.println("]");
	}
}
