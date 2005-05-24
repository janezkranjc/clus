package clus.model.pmml.tildepmml;
import java.io.*;

public class MiningField{

private String name;
private String usageType; // predicted or active or empty

	public MiningField(String $name, String $usageType) {
	
	name=$name;
	usageType=$usageType;
	
	}
	
	public MiningField(String $name) {
	
	name=$name;
	usageType="";
	
	}
	
	public String getName() {
	
	return name;
	
	}
	
	public String getUsageType() {
	
	return usageType;
	
	}
	
	public void print(PrintWriter outStream) {
	
	if (usageType!="") outStream.write("<MiningField name=\""+name+"\" usageType=\""+usageType+"\"></MiningField>\n");
	else outStream.write("<MiningField name=\""+name+"\"></MiningField>\n");
		
	}
	
	
	
}
