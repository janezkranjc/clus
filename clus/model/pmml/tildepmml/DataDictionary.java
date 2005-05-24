package clus.model.pmml.tildepmml;
import java.util.*;

public class DataDictionary{

private int nbOfFields;
private Vector dataFields;

	public DataDictionary(Vector $dataFields) {
	
	nbOfFields=$dataFields.size();
	dataFields=$dataFields;
	
	}


	public int getNbOfFields() {
	
	return nbOfFields;
	
	}
		
	public void addDataField(DataField $dataField) {
	
	dataFields.add($dataField);
	nbOfFields++;
	
	}	
	
	public DataField getDataFieldAt(int index) {
	
	return (DataField)dataFields.elementAt(index);	
	
	}
			

}
