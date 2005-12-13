/*
 * Created on Jul 22, 2005
 */
package clus.error;

import clus.data.type.*;
import clus.main.Settings;

public class MisclassificationError extends Accuracy {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public MisclassificationError(ClusErrorParent par, NominalAttrType[] nom) {
		super(par, nom);
	}
	
	public boolean shouldBeLow() {
		return true;
	}	
	
	public double getModelErrorComponent(int i) {
		return 1.0 - ((double)m_NbCorrect[i]) / getNbExamples();
	}
		
	public String getName() {
		return "Misclassification error";
	}
	
	public ClusError getErrorClone(ClusErrorParent par) {
		return new MisclassificationError(par, m_Attrs);
	}
}