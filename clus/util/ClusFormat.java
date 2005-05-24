package clus.util;

import java.text.*;
import java.io.*;

public class ClusFormat {

	public final static NumberFormat ONE_AFTER_DOT = makeNAfterDot(1);
	public final static NumberFormat TWO_AFTER_DOT = makeNAfterDot(2);	
	public final static NumberFormat THREE_AFTER_DOT = makeNAfterDot(3);	
	public final static NumberFormat MM3_AFTER_DOT = makeNAfterDot2(3);	
	public final static NumberFormat SIX_AFTER_DOT = makeNAfterDot(6);	
	public final static NumberFormat FOUR_AFTER_DOT = makeNAfterDot(4);
	public final static PrintWriter OUT_WRITER = new PrintWriter(new OutputStreamWriter(System.out));
	
	public static NumberFormat makeNAfterDot(int n) {
		NumberFormat fr = NumberFormat.getInstance();
		fr.setMaximumFractionDigits(n);
		try {
			DecimalFormat df = (DecimalFormat)fr;
			DecimalFormatSymbols sym = df.getDecimalFormatSymbols();
			sym.setDecimalSeparator('.');
			df.setGroupingUsed(false);
			df.setDecimalFormatSymbols(sym);
		} catch (ClassCastException e) {}
		return fr;
	}
	
	public static NumberFormat makeNAfterDot2(int n) {
		NumberFormat fr = makeNAfterDot(n);
		fr.setMinimumFractionDigits(n);
		return fr;
	}	
	
	public static void printArray(PrintWriter out, double[] a1, double[] a2, NumberFormat nf) {
		for (int i = 0; i < a1.length; i++) {
			if (i != 0) out.print(", ");
			if (a2[i] == 0.0) out.print(nf.format(0.0));
			else out.print(nf.format(a1[i]/a2[i]));
		}	
	}
	
	public static void printArray(PrintWriter out, double[] a1, NumberFormat nf) {
		for (int i = 0; i < a1.length; i++) {
			if (i != 0) out.print(", ");
			out.print(nf.format(a1[i]));
		}	
	}	
}
