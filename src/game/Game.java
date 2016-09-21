package game;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
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
		
		// we are ignoring user's input, since we use the local host for tracker
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
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
		} catch (NotBoundException e) {
			e.printStackTrace();
			System.out.println("Please start tracker first");
		}
		
		return null;
	}
	
	private static void createNewGame(Tracker tracker) {
		
		Vector<Address> players = null;
		
		try {
			players = tracker.getNodes();
			String ip = InetAddress.getLocalHost().getHostAddress();
			int port = tracker.getPort();
			Address address = new Address(ip, port, userName);
			
			System.out.println("Game starting for player: " + userName + " players in game: " + players.size());
			GameNode gameNode = new GameNodeImpl(tracker, players, tracker.getN(), tracker.getK(), address);
			bindGameNodeToRmi(address, gameNode);
			gameNode.init();
		
		} catch (RemoteException | UnknownHostException e) {
			System.out.println("Tracker finding failed: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
	}
	
	private static void bindGameNodeToRmi(Address address, GameNode gameNode){
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(address.getHost(), address.getPort());
			registry.bind(address.getKey(), gameNode);
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("bind game failed: " + e.getLocalizedMessage());
		} catch (AlreadyBoundException e) {
			e.printStackTrace();
			System.out.println("Player with such user name has already existed in game");
		}
		
	}
}
