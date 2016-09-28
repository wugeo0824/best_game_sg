package model;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Random;

import message.PlayerAction;

/**
 * Maze object. Coordinates: top left cell is (0, 0), bottom right cell is
 * (size-1, size-1) This object actually carries the "Current Game State"
 * 
 * @author Xijun
 *
 */

public class Maze implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7862423925849738140L;

	private int size;
	private int numberOfTreasures;

	private HashMap<String, Player> players;
	private HashMap<String, Treasure> treasures;

	private Random rand;
	private boolean isReady = false;

	public Maze(int size, int numberOfTreasures) {
		this.size = size;
		this.numberOfTreasures = numberOfTreasures;

		rand = new Random();
		players = new HashMap<String, Player>();
		treasures = new HashMap<String, Treasure>(numberOfTreasures);
	}

	public void initialize() {
		initializeTreasures();
		isReady = true;
	}

	public synchronized void copyDataFrom(Maze copy) {
		if (size != copy.size || numberOfTreasures != copy.numberOfTreasures)
			throw new InvalidParameterException("Maze params are not consistent");

		players.clear();
		players.putAll(copy.players);
		treasures.clear();
		treasures.putAll(copy.treasures);
	}

	/**
	 * PLAYERS
	 */

	
	public synchronized boolean addPlayer(String playerKey, Player player) {
		// we only do N*2 times of trials
		// there wont be empty spaces more than that amount
		int trials = size * size;
		while (trials >= 0) {
			Location random = nextRandomLocation();
			if (!isPlayerHere(random) && !isTreasureHere(random)) {
				player.setCurrentLocation(random);
				players.put(playerKey, player);
				trials = -1;
				return true;
			}
			trials --;
		}
		
		return false;
	}

	public synchronized HashMap<String, Player> getPlayers() {
		return players;
	}

	public synchronized void removePlayer(String playerKey) {
		players.remove(playerKey);
	}

	private synchronized boolean isPlayerHere(Location location) {
		for (Player existingPlayer : players.values()) {
			if (existingPlayer.getCurrentLocation().sameAs(location))
				return true;
		}

		return false;
	}
	
	public synchronized boolean movePlayer(String playerKey, PlayerAction action) {
		if (players.get(playerKey) == null)
			return false;
		
		Player player = players.get(playerKey);
		Location currentLocation = player.getCurrentLocation();
		Location destination;
		
		switch(action){
		case MOVE_UP:
			destination = new Location(currentLocation.getLocationX(), currentLocation.getLocationY() - 1);
			break;
		case MOVE_LEFT:
			destination = new Location(currentLocation.getLocationX() - 1, currentLocation.getLocationY());
			break;
		case MOVE_RIGHT:
			destination = new Location(currentLocation.getLocationX() + 1, currentLocation.getLocationY());
			break;
		case MOVE_DOWN:
			destination = new Location(currentLocation.getLocationX(), currentLocation.getLocationY() + 1);
			break;
		default:
			// STAY
			destination = currentLocation;
			return true;
		}
		
		if (isLocationValid(destination)){
			player.setCurrentLocation(destination);
			if (isTreasureHere(destination)){
				consumeTreasure(destination);
				player.increaseScore();
				generateNewTreasure();
			}
			return true;
		}
		
		return false;
	} 

	/**
	 * TREASURES
	 */

	private synchronized void initializeTreasures() {
		for (int i = 0; i < numberOfTreasures; i++) {
			generateNewTreasure();
		}
	}

	private synchronized boolean isTreasureHere(Location location) {

		for (Treasure existingTreasure : treasures.values()) {
			if (existingTreasure.getLocation().sameAs(location))
				return true;
		}

		return false;
	}

	private synchronized void generateNewTreasure() {
		int trials = size * size;
		while (trials >= 0) {
			Location random = nextRandomLocation();
			if (!isTreasureHere(random) && !isPlayerHere(random)) {
				Treasure newTreasure = new Treasure(random);
				treasures.put(random.getLocationId(), newTreasure);
				trials = -1;
			}
			trials --;
		}
	}
	
	private synchronized void consumeTreasure(Location location){
		treasures.remove(location.getLocationId());
	}
	
	public synchronized HashMap<String, Treasure> getTreasures() {
		return treasures;
	}

	/**
	 * TREASURES END
	 */

	private synchronized Location nextRandomLocation() {
		int x = rand.nextInt(size);
		int y = rand.nextInt(size);

		return new Location(x, y);
	}

	public synchronized int getSize() {
		return size;
	}

	public synchronized int getNumberOfTreasures() {
		return numberOfTreasures;
	}
	
	/**
	 * The location is not valid, if
	 * 1. outside the x boundary
	 * 2. outside the y boundary
	 * 3. another player is already there
	 * 
	 * @param location the location to be checked
	 * @return true if valid
	 */
	private synchronized boolean isLocationValid(Location location) {
		if (location.getLocationX() >= size || location.getLocationY() >= size)
			return false;
		
		if (location.getLocationX() <0 || location.getLocationY() < 0)
			return false;
		
		if (isPlayerHere(location))
			return false;
		
		return true;
	}

	public synchronized boolean isReady() {
		return isReady;
	}
}
