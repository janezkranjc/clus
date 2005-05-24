package clus.model.pmml.tildepmml;
import java.io.*;

public class SimplePredicate{

private String field; 
private String operator; //greaterThan, lessThan, greaterOrEqual, lessOrEqual, equal
private String value; //string, yes, no, int


	public SimplePredicate(String $field, String $operator, String $value) {
	
	field=$field;
	operator=$operator;
	value=$value;
	
	}
	
	
	public String getField() {
	
	return field;
	
	}
	
	public String getOperator() { 
	
	return operator;
	
	}
	
	public String getValue() {
	
	return value;
	
	}

	public void print(PrintWriter outStream) {
	
	outStream.write("<SimplePredicate field=\""+field+"\" operator=\""+operator+"\" value=\""+value+"\"/>\n");
	
	}
	
}
