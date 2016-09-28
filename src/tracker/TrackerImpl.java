package tracker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import game.GameNode;
import model.Address;
import utilities.Constants;

/**
 * Assuming that we have a RMI registry running on the same machine with Tracker
 * 
 * Keeps a list of current players
 * 
 * This also handles the binding and un-binding of game nodes from RMI
 */
public class TrackerImpl extends UnicastRemoteObject implements Tracker {
	private static final long serialVersionUID = 4262700063105105025L;

	private int port, N, K;
	/*
	 * List of RMI naming of all GameNode We use Player name as RMI naming for
	 * each GameNode
	 */
	Vector<Address> nodes;
	private static String ip;

	public TrackerImpl(int port, int N, int K) throws RemoteException {
		this.port = port;
		this.N = N;
		this.K = K;
		nodes = new Vector<Address>();
	}

	public static void main(String[] args)
			throws AccessException, RemoteException, NotBoundException, AlreadyBoundException, UnknownHostException {
		if (args.length != 3) {
			printHelp();
			System.exit(0);
		}
		// Parse all arguments
		int port = Integer.parseInt(args[0]);
		int N = Integer.parseInt(args[1]);
		int K = Integer.parseInt(args[2]);

		// Get IP address of current machine
		ip = InetAddress.getLocalHost().getHostAddress();
		String trackerNaming = Constants.TRACKER_NAME;
		String address = "rmi://" + ip + ":" + port + "/" + trackerNaming;

		// Register the tracker to rmi registry
		Tracker stub = new TrackerImpl(port, N, K);
		Registry registry = null;
		try {
			/*
			 * By default, port is 1099. Otherwise, port is as arguments.
			 */
			registry = LocateRegistry.createRegistry(port);
			registry.bind(trackerNaming, stub);

			System.out.println("Successfully register Tracker at [" + address + "]");

		} catch (AlreadyBoundException e) {
			System.out.println("AlreadyBoundException: Retry to register the Tracker");

			registry.unbind(trackerNaming);
			registry.bind(trackerNaming, stub);

			System.out.println("Successfully register Tracker at [" + address + "]");

		}

		// This part is for debugging
		// Every 10s print out all node names stored in Tracker
		TimerTask task = (new TimerTask() {
			@Override
			public void run() {
				try {
					for (Address printNode : stub.getNodes()) {
						System.out.println("Nodes: " + printNode.getKey());
					}
					System.out.println("N: " + N + "   K: " + K);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(task, 0, 10000);
	}

	@Override
	public int getN() throws RemoteException {
		return N;
	}

	@Override
	public int getK() throws RemoteException {
		return K;
	}

	@Override
	public synchronized Vector<Address> getNodes() throws RemoteException {
		return nodes;
	}

	@Override
	public synchronized int addNode(Address address, GameNode gameNode) throws RemoteException {
		// Add to list of game node names kept by Tracker
		nodes.add(address);
		bindGameNodeToRmi(address, gameNode);
		System.out.println("Successfully register node [" + address.getKey() + "]");
		return nodes.size();
	}
	
	private static void bindGameNodeToRmi(Address address, GameNode gameNode) {
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

	private static void printHelp() {
		System.out.println("java Tracker [port-number] [N] [K]");
	}

	@Override
	public int getPort() throws RemoteException {
		return port;
	}

	@Override
	public synchronized boolean deleteNode(Address node) throws RemoteException {
		boolean result = false;;
		Address target = null;
		
		for (Address address:nodes){
			if (address.sameAs(node)){
				target = address;
				break;
			}
		}
		
		if (target != null){
			result = nodes.remove(target);
		}
		
		System.out.println("Player " + node.getUserName() + " is removed: " +result);
		
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(node.getHost(), node.getPort());
			registry.unbind(node.getKey());
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("unbind game failed: " + e.getLocalizedMessage());
		} catch (NotBoundException e) {
			//e.printStackTrace();
			System.out.println("Player with such user name does not exist in game, or has already quit");
		}
		
		return true;
	}

	@Override
	public String getIP() throws RemoteException {
		return ip;
	}
}
