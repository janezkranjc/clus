package clus.model.pmml.tildepmml;
import java.io.*;

public class DataFieldContinuous extends DataField {

	public DataFieldContinuous(String $name) {
	
	name=$name;
	opType="continuous";
	
	}
	
	public void print(PrintWriter outStream) {
	
	outStream.write("<DataField name=\""+name+"\" optype=\"continuous\"/>\n");
	
	
	}
	


}
