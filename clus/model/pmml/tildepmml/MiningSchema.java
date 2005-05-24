package clus.model.pmml.tildepmml;

import java.util.*;
import java.io.*;

public class MiningSchema{

private int nbOfFields;
private Vector miningFields;

	public MiningSchema(Vector $miningFields) {
	
	nbOfFields=$miningFields.size();
	miningFields=$miningFields;
	
	}


	public int getNbOfFields() {
	
	return nbOfFields;
	
	}
		
	public void addMiningField(MiningField $miningField) {
	
	miningFields.add($miningField);
	nbOfFields++;
	
	}	
	
	public MiningField getMiningFieldAt(int index) {
	
	return (MiningField)miningFields.elementAt(index);	
	
	}
	
	public void print(PrintWriter outStream) {
	
	boolean empty=true;
	int counter=0;
	
		if (nbOfFields>0) {
		outStream.write("<MiningSchema>\n");
		empty=false;
		}
		
		while (nbOfFields>0) {
		
		((MiningField)miningFields.elementAt(counter)).print(outStream);
		
		nbOfFields--;
		counter++;
		}
		
		if (empty=false) outStream.write("</MiningSchema>\n");
	
		
	}		

}
