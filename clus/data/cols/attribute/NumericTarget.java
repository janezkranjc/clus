package clus.data.cols.attribute;

import java.io.*;

import clus.io.*;
import clus.data.cols.*;
import clus.data.type.*;

public class NumericTarget extends NumericAttrBase {

	protected ColTarget m_Target;
	protected int m_Index;	

	public NumericTarget(ColTarget target, NumericAttrType type, int index) {
		super(type);
		m_Target = target;
		m_Index = index;
	}

	public void read(ClusReader data, int row) throws IOException {
		m_Target.setNumeric(m_Index, row, data.readFloat()); 
	}
}
