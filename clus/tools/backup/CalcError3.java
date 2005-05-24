import clus.tools.debug.Debug;

import jeans.util.*;
import jeans.tree.*;

import java.io.*;
import java.util.*;
import jeans.util.array.*;
import jeans.math.matrix.*;

import clus.util.*;
import clus.main.*;
import clus.ext.hierarchical.*;

public class CalcError3 {

	public static double calcSquaredDistance(MSymMatrix km, ClassesTuple actual, ClassesTuple predicted) {
		double[] error = new double[km.getRows()];
		for (int i = 0; i < predicted.size(); i++) error[predicted.getPosition(i)] += 1.0;
		for (int i = 0; i < actual.size(); i++) error[actual.getPosition(i)] -= 1.0;
		return km.xtAx(error);
	}

	public static void main(String[] args) {
		try {
			Settings sett = new Settings();
			sett.create();
			StringTable stab = new StringTable();
			ClassHierarchy hier = new ClassHierarchy(null);
			// Read hierarcy
			MStreamTokenizer tokens = new MStreamTokenizer("../hierarchy.hier");
			String token = tokens.getToken();
			while (token != null) {
				ClassesTuple tuple = new ClassesTuple(token, stab);
				tuple.addToHierarchy(hier);
				token = tokens.getToken();
			}
			tokens.close();
			hier.initialize();
			// Calc errors
			tokens = new MStreamTokenizer(args[0]);
			token = tokens.readTillEol();
			double dist = 0.0;
			double nb = 0.0;
			ClassesTuple predt = new ClassesTuple("comp.graphics", stab);			
			HierSSPCalc myc = new HierSSPCalc(hier.getKMatrix(), 0);
			while (token != null) {
				StringTokenizer stok = new StringTokenizer(token, ",");
				String reals = stok.nextToken().trim();
				String preds = stok.nextToken().trim();
				ClassesTuple realt = new ClassesTuple(reals, stab);
				realt.addHierarchyIndices(hier);
				predt.addHierarchyIndices(hier);
				double mydist = calcSquaredDistance(hier.getKMatrix(), realt, predt);	
				System.out.println("d("+realt+","+predt+") = "+mydist);
				myc.addTarget(1.0, realt);
				dist += mydist;
				nb++;
				token = tokens.readTillEol();				
			}	
			System.out.println();			
			double mean = dist/nb;	
			double mean2 = myc.getValue(predt)/nb;
			System.out.println("Error: "+mean+" "+nb);
			System.out.println("Error: "+mean2+" "+nb);			
			tokens.close();
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		} catch (ClusException e) {
			System.out.println("Clus Error: "+e.getMessage());		
		}
	}	
}

