package clus.io;

import java.io.*;

import clus.data.rows.*;
import clus.data.type.*;

public abstract class ClusSerializable {	
	
	public abstract void read(ClusReader data, int row) throws IOException;

	public void term(ClusSchema schema) {
	}
	
	public void read(ClusReader data, DataTuple tuple) throws IOException {
		throw new IOException("Attribute does not support tuple wise reading");
	}	
}
