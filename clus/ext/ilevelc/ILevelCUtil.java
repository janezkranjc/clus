package clus.ext.ilevelc;

import java.util.ArrayList;

public class ILevelCUtil {

	public static int[][] createConstraintsIndex(int nbtrain, ArrayList constr) {
		/* create index as array lists */
		ArrayList[] crIndex = new ArrayList[nbtrain];
		for (int i = 0; i < constr.size(); i++) {
			ILevelConstraint ic = (ILevelConstraint)constr.get(i);			
			int t1 = ic.getT1().getIndex();
			int t2 = ic.getT2().getIndex();
			if (crIndex[t1] == null) crIndex[t1] = new ArrayList();
			if (crIndex[t2] == null) crIndex[t2] = new ArrayList();
			crIndex[t1].add(new Integer(i));
			crIndex[t2].add(new Integer(i));
		}
		/* copy it to final int matrix */
		int[][] index = new int[nbtrain][];
		for (int i = 0; i < nbtrain; i++) {
			if (crIndex[i] != null) {
				int nb = crIndex[i].size();
				index[i] = new int[nb];
				for (int j = 0; j < nb; j++) {
					Integer value = (Integer)crIndex[i].get(j);
					index[i][j] = value;
				}				
			}
		}		
		return index;
	}
	
}
