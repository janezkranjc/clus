package clus.util;

public class ClusException extends Exception {

	public ClusException(String msg) {
		super(msg);
	}
	
	public String toString() {
		return getMessage();
	}

}
