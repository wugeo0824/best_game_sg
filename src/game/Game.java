package game;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Vector;

import model.Address;
import tracker.Tracker;
import utilities.Constants;

// entry point for the game
// Here, the player will be asked for the USER NAME
// Then, the program will contact tracker on the given ip & port
// Tracker will provide a list of current players, N and K
// This program will use that name list and start a new game

public class Game {

	static String userName;

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Game start failed. please type java Game [ip-address] [port-number] [player-id]");
			System.exit(0);
		}
		// Parse all arguments
		String ip = args[0];
		int port = Integer.parseInt(args[1]);
		userName = args[2];
		
		if (userName.length() != 2){
			System.out.println("Game start failed. user name must be only 2 charaters long");
			System.exit(0);
		}

		// contact tracker
		Tracker tracker = contactTracker(ip, port);

		// create the game
		createNewGame(tracker);
	}

	private static Tracker contactTracker(String ipAddress, int port) {
		Registry registry = null;

		try {
			registry = LocateRegistry.getRegistry(ipAddress, port);
			Tracker tracker = (Tracker) registry.lookup(Constants.TRACKER_NAME);

			return tracker;

		} catch (RemoteException e) {
			System.out.println("Tracker finding failed: " + e.getLocalizedMessage());
			System.exit(0);
		} catch (NotBoundException e) {
			e.printStackTrace();
			System.out.println("Please start tracker first");
			System.exit(0);
		}

		return null;
	}

	private static void createNewGame(Tracker tracker) {

		Vector<Address> players = null;

		try {
			players = tracker.getNodes();
			Address address = new Address(tracker.getIP(), tracker.getPort(), userName);
			
			for (Address existingPlayer:players){
				if (existingPlayer.sameAs(address)){
					System.out.println("Player ID " + userName + " has already exsited");
					return;
				}
			}

			System.out.println("Game starting for player: " + userName + " players already in game: " + players.size());
			GameNode gameNode = new GameNodeImpl(tracker, players, tracker.getN(), tracker.getK(), address);
			int currentSize = tracker.addNode(address, gameNode);
			if (currentSize == 1){
				gameNode.startAsPrimary();
			}else if (currentSize == 2){
				gameNode.startAsBackUp();
			}else {
				gameNode.startNormally();
			}
			

		} catch (RemoteException e) {
			System.out.println("Tracker finding failed: " + e.getLocalizedMessage());
			e.printStackTrace();
			System.exit(0);
		}

	}
}
