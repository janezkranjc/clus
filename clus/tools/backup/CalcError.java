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

public class CalcError {

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
			// Read default model
			tokens = new MStreamTokenizer(args[1]);
			String defs = tokens.readTillEol();
			ClassesTuple deft = new ClassesTuple(defs, stab);
			deft.addHierarchyIndices(hier);
			tokens.close();
			// Calc errors
			MyFile result = new MyFile(args[0]+".error");
			tokens = new MStreamTokenizer(args[0]);
			token = tokens.readTillEol();
			double dist = 0.0;
			double defdist = 0.0;
			double nb = 0.0;
			while (token != null) {
				StringTokenizer stok = new StringTokenizer(token, ",");
				String reals = stok.nextToken().trim();
				String preds = stok.nextToken().trim();
				ClassesTuple realt = new ClassesTuple(reals, stab);
				ClassesTuple predt = new ClassesTuple(preds, stab);
				realt.addHierarchyIndices(hier);
				predt.addHierarchyIndices(hier);
				dist += calcSquaredDistance(hier.getKMatrix(), realt, predt);
				defdist += calcSquaredDistance(hier.getKMatrix(), realt, deft);
				if ((nb % 100) == 0) {
					System.out.print(".");
					System.out.flush();
				}
				token = tokens.readTillEol();
				nb++;
			}	
			System.out.println();			
			double mean = dist/nb;	
			double defmean = defdist/nb;			
			result.log("Error: "+mean);
			result.log("Default: "+defmean);
			System.out.println("Error: "+mean+" "+nb);
			System.out.println("Default: "+defmean+" "+nb);
			tokens.close();
			result.close();	
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		} catch (ClusException e) {
			System.out.println("Clus Error: "+e.getMessage());		
		}
	}	
}

