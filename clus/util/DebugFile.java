package clus.util;

import java.io.*;

public class DebugFile {

	protected static PrintWriter m_Writer = makeWriter();
	
	public static void log(String strg) {
		if (m_Writer != null) m_Writer.println(strg);
	}

	public static void exit() {
		close();
		System.exit(-1);
	}	
	
	public static void close() {
		if (m_Writer != null) m_Writer.close();
	}
	
	protected static PrintWriter makeWriter() {
		try {
			return new PrintWriter(new OutputStreamWriter(new FileOutputStream("debug.txt")));
		} catch (IOException e) {
			System.err.println("Error creating debug writer");
			return null;
		}
	}	
}
