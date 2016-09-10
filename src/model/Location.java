package model;

import java.io.Serializable;

public final class Location implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1459974655785198125L;
	private int locationX;
	private int locationY;
	private String id;

	public Location(int X, int Y) {
		locationX = X;
		locationY = Y;
		id = "loc" + locationX + "ation" + locationY;
	}

	public int getLocationX() {
		return locationX;
	}

	public void setLocationX(int locationX) {
		this.locationX = locationX;
	}

	public int getLocationY() {
		return locationY;
	}

	public void setLocationY(int locationY) {
		this.locationY = locationY;
	}

	public boolean sameAs(Location theOtherLocation) {
		if (getLocationX() == theOtherLocation.getLocationX() && getLocationY() == theOtherLocation.getLocationY())
			return true;

		return false;
	}

	public String getLocationId() {
		return id;
	}

}
