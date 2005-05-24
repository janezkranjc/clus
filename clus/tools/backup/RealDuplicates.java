import clus.tools.debug.Debug;

import jeans.util.*;
import clus.util.*;

import java.io.*;

public class RealDuplicates {

	MyArray m_Files = new MyArray();
	MyArray m_FNames = new MyArray();
	int m_NbDups;

	public String loadFile(String id) throws IOException {
		StringBuffer buffer = new StringBuffer();
		RunProcess proc = new RunProcess();
		proc.run("/home/jan/cache/dataset/nbex/myparse "+id, ".");
		MStreamTokenizer tokens = proc.getOutputTokenizer();
		String token = tokens.getToken();
		while (token != null) {
			buffer.append(token);			
			token = tokens.getToken();
		}
		return buffer.toString();
	}
	
	public void doall() throws IOException {
		MStreamTokenizer tokens = new MStreamTokenizer("../nbex/possibledups.txt");
		String token = tokens.getToken();
		while (token != null) {
			String buffer = loadFile(token);
			int len = buffer.length();
			System.out.println("File: "+token+" ("+len+")");
			m_FNames.addElement(token);
			m_Files.addElement(buffer);
			token = tokens.getToken();			
		}
		MyFile output = new MyFile("realduplicates.csv");
		for (int i = 0; i < m_Files.size(); i++) {
			String buf1 = (String)m_Files.elementAt(i);
			String fname1 = (String)m_FNames.elementAt(i);
			for (int j = i+1; j < m_Files.size(); j++) {
				String buf2 = (String)m_Files.elementAt(j);
				if (buf1.equals(buf2)) {
					String fname2 = (String)m_FNames.elementAt(j);
					System.out.println("Duplicates: "+fname1+" "+fname2);
					output.log(fname1+"\t"+fname2);
					m_NbDups++;
				}
			}
		}	
		output.close();
		System.out.println("Number of duplicates: "+m_NbDups);
	}

	public static void main(String[] args) {
		try {
			RealDuplicates dup = new RealDuplicates();
			dup.doall();
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		}
	}
}
