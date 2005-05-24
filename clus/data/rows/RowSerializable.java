package clus.data.rows;

import java.io.*;

import clus.io.*;

public abstract class RowSerializable extends ClusSerializable {	

	protected RowData m_Data;

	public RowSerializable(RowData data) {
		m_Data = data;
	}

	public void read(ClusReader data, int row) throws IOException {
		DataTuple tuple = m_Data.getTuple(row);
		read(data, tuple);
	}
}
