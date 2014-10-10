package jp.ac.tuat.cs.wifidirectkurogo.message;

import java.io.Serializable;
import java.util.HashMap;

public class HelloContent implements Serializable {
	private HashMap<String, String> addressTable; // MAC address -> Host address

	public HelloContent() {
		super();
		addressTable = new HashMap<String, String>();
	}
	
	public void add(String macAddress, String hostAddress){
		addressTable.put(macAddress, hostAddress);
	}

	public HashMap<String, String> getAddressTable() {
		return addressTable;
	}

}
