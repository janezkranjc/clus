import clus.tools.debug.Debug;

import jeans.util.*;
import jeans.tree.*;

import java.io.*;
import java.util.*;
import jeans.util.array.*;

import clus.util.*;
import clus.main.*;
import clus.ext.hierarchical.*;

public class ClassHUtil {

	public static void main(String[] args) {
		try {
			double weightedpenalty = 0.0;
			double spathlength = 0.0;
			double count = 0.0;
			double correct = 0.0;
			StringTable stab = new StringTable();
			MyArray examples = new MyArray();
			ClassHierarchy hier = new ClassHierarchy(null);
			boolean leaf = (args.length == 3 && args[2].equals("leaf"));
			// Read hierarcy
			MStreamTokenizer tokens = new MStreamTokenizer(args[0]);
			String token = tokens.getToken();
			while (token != null) {
				ClassesTuple tuple = new ClassesTuple(token, stab);
				tuple.addToHierarchy(hier);
				examples.addElement(tuple);
				token = tokens.getToken();
			}
			tokens.close();
			int depth = hier.getMaxDepth();


		HierWPenalty wpend = new HierWPenalty(depth, 0.5);		
		
/*		
		CompleteTreeIterator i1 = hier.getNoRootIter();
		while (i1.hasMoreNodes()) {
			ClassTerm n1 = (ClassTerm)i1.getNextNode();
			CompleteTreeIterator i2 = hier.getNoRootIter();
			while (i2.hasMoreNodes()) {
				ClassTerm n2 = (ClassTerm)i2.getNextNode();
				double d12 = wpend.calcDistance(n1, n2);
				CompleteTreeIterator i3 = hier.getNoRootIter();
				while (i3.hasMoreNodes()) {
					ClassTerm n3 = (ClassTerm)i3.getNextNode();
					double d13 = wpend.calcDistance(n1, n3);
					double d32 = wpend.calcDistance(n3, n2);
					if (d13 + d32 < d12) {
						System.out.println("Error:");
						System.out.println("	"+n1);
						System.out.println("	"+n2);
						System.out.println("	"+n3);
						System.out.println("	"+d13+" + "+d32+" > "+d12);						
					}									
				}
			}
		}
*/		
			// Print hierarchy
//			hier.print(ClusFormat.OUT_WRITER);
//			ClusFormat.OUT_WRITER.flush();			
			// Calculate error

			int[] errors = new int[2*depth+1];			
			int[] hlevel = new int[2*depth+1];
			HierWeightSPath	spath = new HierWeightSPath(depth, 1.0);
			HierLevelDistance hleveld = new HierLevelDistance();
			tokens = new MStreamTokenizer(args[1]);
			token = tokens.readTillEol();
			MyFile sphist = new MyFile("sp_hist.csv");
			while (token != null) {
				StringTokenizer stok = new StringTokenizer(token, ",");
				String reals = stok.nextToken().trim();
				String pred1 = stok.nextToken().trim();
				String preds = leaf ? stok.nextToken().trim() : pred1;
				ClassesTuple realt = new ClassesTuple(reals, stab);
				ClassesTuple predt = new ClassesTuple(preds, stab);
				count += 1.0;
				if (realt.equalsTuple(predt)) {
					correct += 1.0;
				}
				ClassTerm termr = hier.getClassTerm(realt.elementAt(0));
				ClassTerm termp = hier.getClassTerm(predt.elementAt(0));				
				
				// Calculate shortest path distance
				double spathd = spath.calcDistance(termr, termp);
				spathlength += spathd;
				sphist.log(""+spathd);				
				
				// Calculate histogram
				int mdist = (int)Math.floor(spathd+0.5);			
				errors[mdist]++;
				
				// Calculate penalty based distance
				double wpvalue = wpend.calcDistance(termr, termp);
				weightedpenalty += wpvalue;
				
				// Calculate error
				int hlvlval = (int)Math.floor(hleveld.calcDistance(termr, termp)+0.5);
				hlevel[depth+hlvlval]++;
				
//				System.out.println("T1="+termr+" T2="+termp+" -> "+wpvalue+" "+spathd+" "+hlvlval);
				
				token = tokens.readTillEol();
			}			
			tokens.close();
			sphist.close();	
			double accuracy = correct/count*100.0;
			System.out.println("Count: "+count);
			System.out.println("Correct: "+correct+" "+ClusFormat.TWO_AFTER_DOT.format(accuracy)+"%");
			double nb_err = 0.0;
			for (int i = 1; i <= depth*2; i++) {
				nb_err += errors[i];
			}
			System.out.println("Mean Shortest Path Length: "+spathlength/count);
			System.out.println("Mean Weighted Penalty: "+weightedpenalty/count);
			for (int i = 0; i <= depth*2; i++) {
				double perc1 = (double)errors[i]/count*100.0;
				double perc2 = (double)errors[i]/nb_err*100.0;
				System.out.print(String.valueOf(i)+"\t"+errors[i]);
				System.out.print("\t"+ClusFormat.TWO_AFTER_DOT.format(perc1)+"%");
				System.out.print("\t"+ClusFormat.TWO_AFTER_DOT.format(perc2)+"%");
				System.out.println();
			}
			System.out.println("Prediction error vs level");
			for (int i = 0; i <= depth*2; i++) {
				if (i != 0) System.out.print("\t");
				System.out.print(String.valueOf(i-depth));
			}
			System.out.println();			
			for (int i = 0; i <= depth*2; i++) {
				if (i != 0) System.out.print("\t");
				System.out.print(hlevel[i]);
			}
			System.out.println();
			MyFile sperror = new MyFile("sperror.csv");
			for (int i = 0; i <= depth*2; i++) {
				double perc2 = (double)errors[i]/count*100.0;
				sperror.log(String.valueOf(perc2));
			}			
			sperror.close();			
			MyFile lverror = new MyFile("lverror.csv");
			for (int i = 0; i <= depth*2; i++) {
				lverror.log(String.valueOf(hlevel[i]));
			}
			lverror.close();
			System.out.println();			
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		} catch (ClusException e) {
			System.out.println("Clus Error: "+e.getMessage());		
		}
	}	
}

