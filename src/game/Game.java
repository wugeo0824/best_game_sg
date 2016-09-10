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

	public static void main(String[] args) {
		
		// ask for User Name
		
		String ip = "";
		int port = 0;
		
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
			
			GameNodeImpl gameNode = new GameNodeImpl(players, tracker.getN(), tracker.getK());
			
			//TODO start the actual game
			
		} catch (RemoteException e) {
			System.out.println("Tracker finding failed: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
	}
}
