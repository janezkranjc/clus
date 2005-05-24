package clus.error.multiscore;

import java.util.*;

import clus.main.*;
import clus.util.*;
import clus.data.type.*;

public class MultiScore {

	protected double[] m_Thresholds;	
	protected int m_NbValues;
	protected TargetSchema m_TarSchema;

	public MultiScore(ClusSchema schema, Settings sett) throws ClusException {
		String val = sett.getMultiScore();
		int len = val.length();
		int nb_wanted = schema.getNbTarNum();
		try {
			if (len > 2 && val.charAt(0) == '{' && val.charAt(len-1) == '}') {
				StringTokenizer tokens = new StringTokenizer(val.substring(1,len-1), ", ");
				m_NbValues = tokens.countTokens();
				if (m_NbValues != nb_wanted) 
					throw new ClusException("Not enough ("+m_NbValues+" < "+nb_wanted+") thresholds given for multi-score");
				m_Thresholds = new double[m_NbValues];
				for (int i = 0; i < m_NbValues; i++) m_Thresholds[i] = Double.parseDouble(tokens.nextToken());
			} else {
				double thr = Double.parseDouble(val);
				m_Thresholds = new double[m_NbValues = nb_wanted];
				for (int i = 0; i < m_NbValues; i++) m_Thresholds[i] = thr;
			}
			m_TarSchema = createTarSchema(schema);			
		} catch (NumberFormatException e) {
			throw new ClusException("Parse error reading multi-score values");
		}
	}
	
	public TargetSchema createTarSchema(ClusSchema schema) {
		int i = 0;
		int nb = schema.getNbAttributes();		
		TargetSchema ntschema = new TargetSchema(m_NbValues, 0);
		for (int j = 0; j < nb; j++) {
			ClusAttrType at = schema.getAttrType(j);
			if (at.getStatus() == ClusAttrType.STATUS_TARGET && at.getTypeIndex() == NumericAttrType.THIS_TYPE) {
				ClusAttrType ntype = new NominalAttrType(at.getName());
				ntschema.setType(NominalAttrType.THIS_TYPE, i++, ntype);
			}
		}
		return ntschema;
	}
	
	public TargetSchema getTarSchema() {
		return m_TarSchema;	
	}
	
	public int getNbTarget() {
		return m_NbValues;
	}

	// Class index 0 = positive, 1 = negative (!)
	public int[] multiScore(double[] input) {
		int[] res = new int[input.length];
		for (int i = 0; i < m_NbValues; i++) 
			res[i] = (input[i] > m_Thresholds[i]) ? 0 : 1;
		return res;
	}
	
	// Class index 0 = positive, 1 = negative (!)	
	public void multiScore(double[] input, int[] res) {
		for (int i = 0; i < m_NbValues; i++) 
			res[i] = (input[i] > m_Thresholds[i]) ? 0 : 1;
	}	
}
