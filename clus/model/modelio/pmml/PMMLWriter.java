
package clus.model.modelio.pmml;

import java.io.*;

public class PMMLWriter {

	protected String pmmlcode;

	public PMMLWriter(String pmml) {
		pmmlcode = pmml;
	}
	
	public void write(String fname) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out));
//		m_Node.printTree(writer, "");
		writer.flush();
	}
}
