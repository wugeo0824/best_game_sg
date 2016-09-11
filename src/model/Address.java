package model;

import java.io.Serializable;

public class Address implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2665715409510172137L;

	private String host;
	private int port;
	private String key;
	private String userName;

	public Address(String host, int port, String userName) {
		this.host = host;
		this.port = port;
		this.userName = userName;
		key = "host" + host + "port" + port + "user "+ userName;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getKey() {
		return key;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public boolean sameAs(Address comparing) {
		
		if (getHost().equals(comparing.getHost()) && getPort() == comparing.getPort() && getUserName().equals(comparing.getUserName()))
			return true;
		
		return false;
	}
}
