package model;

import java.io.Serializable;

public final class Player implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8336238011993053688L;
	private int currentScore = 0;
	private Location currentLocation;
	private Address rmiAddress;
	private String name;

	public Player(String name, int currentScore, Location currentLocation, Address rmiAddress) {
		super();
		this.name = name;
		this.currentScore = currentScore;
		this.currentLocation = currentLocation;
		this.rmiAddress = rmiAddress;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}

	public Address getRmiAddress() {
		return rmiAddress;
	}

	public int getCurrentScore() {
		return currentScore;
	}

	public void setCurrentScore(int currentScore) {
		this.currentScore = currentScore;
	}

	public int increaseScore() {
		currentScore++;
		return currentScore;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
