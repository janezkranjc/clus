package clus.data.cols.attribute;

import java.io.*;

import clus.io.*;
import clus.data.cols.*;
import clus.data.type.*;

public class NominalTarget extends NominalAttrBase {

	protected ColTarget m_Target;
	protected int m_Index;	

	public NominalTarget(ColTarget target, NominalAttrType type, int index) {
		super(type);
		m_Target = target;
		m_Index = index;
	}

	public void read(ClusReader data, int row) throws IOException {
		String value = data.readString();
		Integer i = (Integer)getNominalType().getValueIndex(value);
		if (i != null) {
			m_Target.setNominal(m_Index, row, i.intValue()); 
		} else {
			throw new IOException("Illegal value '"+value+"' for target "+getName()+" at row "+(row+1));
		}
	}
}
