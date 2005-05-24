package clus.io;

import java.io.*;

import clus.data.rows.*;

public class DummySerializable extends ClusSerializable {

	public void read(ClusReader data, int row) throws IOException {
		data.skipTillComma();
	}
	
	public void read(ClusReader data, DataTuple tuple) throws IOException {
		data.skipTillComma();
	}	
}
