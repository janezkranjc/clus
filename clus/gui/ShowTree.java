
package clus.gui;

import java.io.*;

public class ShowTree {


	public static void main(String[] args) {
		try {
		    	SimpleTreeFrame.showTree(args[0]);
		} catch (IOException e) {
			System.out.println("IO Error: "+e.getMessage());
		} catch (ClassNotFoundException e) {
			System.out.println("Class Not Found: "+e.getMessage());
		}
	}
}
