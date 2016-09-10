package muo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
//import muo.Node;

/**
 * Assuming that we have a RMI registry running on the same machine with Tracker
 * 
 */
public class Tracker extends UnicastRemoteObject implements TrackerInterface {
	private static final long serialVersionUID = 4262700063105105025L;
	
	public int port, N, K;
	/*
	 * List of RMI naming of all GameNode
	 * We use Player name as RMI naming for each GameNode
	 */
	Vector<Node> nodes;

	public Tracker(int port, int N, int K) throws RemoteException {
		this.port = port;
		this.N = N;
		this.K = K;
		nodes = new Vector<Node>();
	}

	public static void main(String[] args) throws AccessException, RemoteException, NotBoundException, AlreadyBoundException, UnknownHostException {
		if (args.length != 3) {
			printHelp();
			System.exit(0);
		}
		// Parse all arguments
		int port = Integer.parseInt(args[0]);
		int N = Integer.parseInt(args[1]);
		int K = Integer.parseInt(args[2]);

		// Get IP address of current machine
		String ip = InetAddress.getLocalHost().getHostAddress();
		String trackerNaming = "Tracker";
		String address = "rmi://" + ip + ":" + port + "/"+trackerNaming;

		// Register the tracker to rmiregistry
		TrackerInterface stub = new Tracker(port, N, K);
		Registry registry = null;
		try {
			/*
			 * By default, port is 1099. Otherwise, port is as arguments.
			 */
			registry = LocateRegistry.createRegistry(port); 
			registry.bind(trackerNaming, stub);

			System.out.println("Successfully register Tracker at [" + address +"]");
			
		} catch (AlreadyBoundException e) {
			System.out.println("AlreadyBoundException: Retry to register the Tracker");

			registry.unbind(trackerNaming);
			registry.bind(trackerNaming, stub);

			System.out.println("Successfully register Tracker at [" + address +"]");

		}
		
		// This part is for debugging
		// Every 10s print out all node names stored in Tracker
		TimerTask task = (new TimerTask(){
			@Override
			public void run() {
				try {
					for(Node printNode : stub.getNodes()){
						System.out.println("Nodes: "+printNode.getNodeName());
					}
					System.out.println("N: "+N+"   K: "+K);
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
	public Vector<Node> getNodes() throws RemoteException {
		return nodes;
	}

	@Override
	public synchronized void addNodeToRMIRegistry(GameNodeInterface node) throws RemoteException, AlreadyBoundException {

		String nodeName = node.getPlayerId();
		String nodeIP=node.getPlayerIPAddress();
		int nodePort=node.getPlayerPort();
		String nodeInfo=node.getPlayerId()+node.getPlayerIPAddress()+node.getPlayerPort();
		
		Node addnode=new Node();
		addnode.setNodeName(nodeName);
		addnode.setNodeIP(nodeIP);
		addnode.setNodePort(nodePort);

		Registry registry = LocateRegistry.getRegistry(port);
		registry.bind(nodeName, node);

		System.out.println("Successfully register node [" + nodeName +nodeIP+nodePort+ "]");

		// Add to list of game node names kept by Tracker
		nodes.add(addnode);
	}

	@Override
	public void removeInactiveNodes(Vector<String> deleteNodeName) throws RemoteException {
		
		int count = 0;
		System.out.print("Remove inactive node names: ");

		try {
			Registry registry = LocateRegistry.getRegistry(port);

			for (Iterator<Node> iterator = nodes.iterator(); iterator.hasNext();) {
				Node node = iterator.next();
				if (deleteNodeName.contains(node.getNodeName())) {

					// Unbind from RMI
					registry.unbind(node.getNodeName());

					// Remove from the list
					iterator.remove();
					
					System.out.print("["+deleteNodeName+"] ");
					count++;
				}
			}
			System.out.println("Total: "+count+" nodes removed");
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

	private static void printHelp() {
		System.out.println("java Tracker [port-number] [N] [K]");
	}
}
