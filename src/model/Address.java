package model;

import java.io.Serializable;
import java.rmi.Remote;

public class Address implements Serializable, Remote {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2665715409510172137L;

	private String host;
	private int port;
	private String key;

	public Address(String host, int port) {
		this.host = host;
		this.port = port;
		key = "host" + host + "port" + port;
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
}
