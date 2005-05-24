package clus.model.pmml.tildepmml;
import java.util.*;
import java.io.*;

public class DataFieldCategorical extends DataField {

private Vector values;

	public DataFieldCategorical(String $name, Vector $values) {
	
	name=$name;
	opType="categorical";	
	values=$values;
	
	}
	
	public void addValue(String value) {
	
	values.add(value);
	
	}

	public int getNbOfValues() {
	
	return values.size();
	
	}
	
	public void print(PrintWriter outStream) {
	
	int counter;
	int pointer=0;
	
	outStream.write("<DataField name=\""+name+"\" optype=\"categorical\"/>\n");
	
	counter = values.size();
		
	while (counter>0) {

	outStream.write(" <Value value=\""+values.elementAt(pointer)+"\"/>\n");
	
	pointer++;
	counter--;
	}
		
	outStream.write("</DataField>");
	
	}
	
	
}
