package model;

import java.io.Serializable;

public final class Treasure implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1461217285859255536L;
	private Location location;

	public Treasure(Location location) {
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}

}
