package clus.util;

import clus.main.Settings;

public class ClusException extends Exception {
	
	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	public ClusException(String msg) {
		super(msg);
	}
	
	public String toString() {
		return getMessage();
	}

}
