package model;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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

	private ConcurrentHashMap<String, Player> players;
	private ConcurrentHashMap<String, Treasure> treasures;

	private Random rand;

	public Maze(int size, int numberOfTreasures) {
		this.size = size;
		this.numberOfTreasures = numberOfTreasures;

		rand = new Random();

		initialize();
	}

	private void initialize() {
		players = new ConcurrentHashMap<String, Player>();
		treasures = new ConcurrentHashMap<String, Treasure>(numberOfTreasures);
		initializeTreasures();
	}

	public void copyDataFrom(Maze copy) {
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

	public void addPlayer(String playerKey, Player player) {
		int trials = size * size;
		while (trials >= 0) {
			Location random = nextRandomLocation();
			if (!isPlayerHere(random)) {
				player.setCurrentLocation(random);
				players.put(playerKey, player);
				trials = -1;
			}
			trials --;
		}
	}

	public void removePlayer(String playerKey) {
		players.remove(playerKey);
	}

	private boolean isPlayerHere(Location location) {
		for (Player existingPlayer : players.values()) {
			if (existingPlayer.getCurrentLocation().sameAs(location))
				return true;
		}

		return false;
	}
	
	public boolean movePlayer(String playerKey, PlayerAction action) {
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
			destination = currentLocation;
			return true;
		}
		
		if (isLocationValid(destination)){
			player.setCurrentLocation(destination);
			if (isTreasureHere(destination)){
				player.increaseScore();
			}
			return true;
		}
		
		return false;
	} 

	/**
	 * TREASURES
	 */

	private void initializeTreasures() {
		for (int i = 0; i < numberOfTreasures; i++) {
			generateNewTreasure();
		}
	}

	private boolean isTreasureHere(Location location) {

		for (Treasure existingTreasure : treasures.values()) {
			if (existingTreasure.getLocation().sameAs(location))
				return true;
		}

		return false;
	}

	private void generateNewTreasure() {
		int trials = size * size;
		while (trials >= 0) {
			Location random = nextRandomLocation();
			if (!isTreasureHere(random)) {
				Treasure newTreasure = new Treasure(random);
				treasures.put(random.getLocationId(), newTreasure);
				trials = -1;
			}
			trials --;
		}
	}

	/**
	 * TREASURES END
	 */

	private Location nextRandomLocation() {
		int x = rand.nextInt(size);
		int y = rand.nextInt(size);

		return new Location(x, y);
	}

	public int getSize() {
		return size;
	}

	public int getNumberOfTreasures() {
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
	private boolean isLocationValid(Location location) {
		if (location.getLocationX() >= size || location.getLocationY() >= size)
			return false;
		
		if (location.getLocationX() <0 || location.getLocationY() < 0)
			return false;
		
		if (isPlayerHere(location))
			return false;
		
		return true;
		
	}

}
