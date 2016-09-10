package model;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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
		boolean success = false;
		while (!success) {
			Location random = nextRandomLocation();
			if (!isPlayerHere(random)) {
				player.setCurrentLocation(random);
				players.put(playerKey, player);
				success = true;
			}
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
		boolean success = false;
		while (!success) {
			Location random = nextRandomLocation();
			if (!isTreasureHere(random)) {
				Treasure newTreasure = new Treasure(random);
				treasures.put(random.getLocationId(), newTreasure);
				success = true;
			}
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

}
