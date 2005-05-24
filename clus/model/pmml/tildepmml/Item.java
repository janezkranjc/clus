package clus.model.pmml.tildepmml;

import java.io.*;

public class Item{

private int id;
private boolean fieldOrValue; //true for field, false for value
private String field;
private String value;

	public Item(int $id, boolean $fieldOrValue, String $fieldOrValueString) {
	
	id=$id;

	if ($fieldOrValue) { field=""; value=$fieldOrValueString; }
	else { value=$fieldOrValueString; field=""; }

	}
	
	public int getId() {
	
	return id;
	
	}

	public boolean isField() {
	
	return fieldOrValue;
	
	}
	
	public String getField() {
	
	return field;
	
	}
	
	public String getValue() {
	
	return value;
	
	}
	
	public void print(PrintWriter outStream) {
	
	if (fieldOrValue) 
	outStream.write("<Item id=\""+id+"\" field=\""+field+"\"/>\n");
	else outStream.write("<Item id=\""+id+"\" value=\""+value+"\"/>\n");
	
	
	}
}
