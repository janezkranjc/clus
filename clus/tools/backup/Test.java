import clus.tools.debug.Debug;

import jeans.resource.*;

public class Test {

	public static void main(String[] args) {
		System.out.println("Prop: "+System.getProperty("java.library.path"));
	
if (Debug.debug == 1) {
		System.out.println("CPU: "+ResourceInfo.getCPUTime());
}

		for (int i = 0; i < 1000000; i++) {
			Test t = new Test();		
		}	
if (Debug.debug == 1) {
		System.out.println("CPU: "+ResourceInfo.getCPUTime());
}

if (Debug.debug == 1) {
		System.out.println("CPU: "+ResourceInfo.getMemorySize());	
}

		while(true) {}
	}
}
