package clus.tools.textproc;

import java.io.*;
import java.util.*;

public class StopList extends Hashtable {

	public StopList() {
		super();
	}

	public StopList(String filename) throws IOException {
		String dummy = "";
		LineNumberReader reader = new LineNumberReader(new FileReader(filename));
		while (true) {
			String word = reader.readLine();
			if (word == null) break;
			put(word, dummy);
		}
		reader.close();
	}

	public boolean hasWord(String word) {
		return containsKey(word);
	}
}