package game;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import gui.Window;
import message.ClientMessage;
import message.PlayerAction;
import message.ServerMessage;
import model.Address;
import model.Maze;
import model.Player;
import tracker.Tracker;

public class GameNodeImpl extends UnicastRemoteObject implements GameNode {

	/**
	 * Generated ID
	 */
	private static final long serialVersionUID = -206482596196783156L;

	// private final static int MESSAGE_SIZE_LIMIT = 200;

	private Address primaryServer;
	private Address backUpServer;

	private boolean isPrimary = false;
	private boolean isBackUp = false;

	private Tracker tracker;

	private Maze theMaze;
	private Player me;
	private Address here;

	private LinkedBlockingQueue<ClientMessage> messagesFromClient;
	private ExecutorService serverExecutor;

	private LinkedBlockingQueue<ServerMessage> messagesFromServer;
	private ExecutorService clientExecutor;
	private Thread pinThread;

	private boolean isGamePlaying = false;

	private Window gameWindow;

	public GameNodeImpl(Tracker tracker, Vector<Address> playerNameList, int size, int numberOfTreasures, Address here)
			throws RemoteException {

		this.tracker = tracker;
		this.here = here;
		me = new Player(here.getUserName(), 0, null, here);
		theMaze = new Maze(size, numberOfTreasures);

		messagesFromClient = new LinkedBlockingQueue<ClientMessage>();
		serverExecutor = Executors.newSingleThreadExecutor();

		messagesFromServer = new LinkedBlockingQueue<ServerMessage>();
		clientExecutor = Executors.newSingleThreadExecutor();

		pinThread = new Thread(pinPlayersRunnable);
	}

	private void attachShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (isGamePlaying)
					closeGame();

				System.out.println("Shut Down Hook Executed.");
			}
		});
		System.out.println("Shut Down Hook Attached.");
	}

	@Override
	public void startAsPrimary() throws RemoteException {
		startProcessingMessages();
		// this node has not been added to the tracker yet
		primaryServer = here;
		isPrimary = true;
		// only initialize maze at primary server
		theMaze.initialize();

		// add primary player into local game
		// addNewPlayer(this);
		enqueueNewMessage(new ClientMessage(here, PlayerAction.JOIN));

		// start the actual game GUI
		gameWindow = new Window(this);

		// update the game GUI
		gameWindow.updateMaze(theMaze);

		// primary should constantly check whether there is dead node
		pinThread.start();

		System.out.println("Here is Primary.");
		System.out.println("Successfully started player at [" + here.getKey() + "]");
	}

	@Override
	public void startAsBackUp() throws RemoteException {
		startProcessingMessages();

		primaryServer = getNodesListFromTracker().get(0);
		backUpServer = here;
		isBackUp = true;
		isPrimary = false;
		System.out.println("Here is BackUp. Primary server at " + primaryServer.getKey());

		// notify the GUI of the new game state (this is for nodes other than
		// primary server)
		gameWindow = new Window(this);

		// if this is not the primary server
		// tell the primary server that i have joined
		// and starts the game
		playerMadeAMove(PlayerAction.JOIN);
		System.out.println("Successfully started player at [" + here.getKey() + "]");
	}

	@Override
	public void startNormally() throws RemoteException {
		startProcessingMessages();

		primaryServer = getNodesListFromTracker().get(0);
		backUpServer = getNodesListFromTracker().get(1);
		System.out.println("Here is Normal. Primary server at " + primaryServer.getKey());
		System.out.println("BackUp server at " + backUpServer.getKey());

		isPrimary = false;
		isBackUp = false;
		
		gameWindow = new Window(this);
		// if this is not the primary server
		// tell the primary server that i have joined
		// and starts the game
		playerMadeAMove(PlayerAction.JOIN);
		System.out.println("Successfully started player at [" + here.getKey() + "]");
	}

	public void startProcessingMessages() {
		isGamePlaying = true;

		attachShutDownHook();
	}

	/**
	 * SERVER SIDE STARTS
	 */

	@Override
	public synchronized boolean enqueueNewMessage(ClientMessage message) throws RemoteException {
		System.out.println("Got message: " + message.toString());
		messagesFromClient.add(message);
		while (theMaze.isReady() && !messagesFromClient.isEmpty()){
			serverExecutor.submit(new Runnable() {
				@Override
				public void run() {
					ClientMessage nextMessage = messagesFromClient.poll();
					if (nextMessage != null) {
						processMessageFromClient(nextMessage);
					}
				}
			});
		}

		return false;
	}

	private synchronized boolean processMessageFromClient(ClientMessage message) {
		Address target = message.getTargetAddress();
		PlayerAction action = message.getPlayerAction();

		if (target.sameAs(here)) {
			// when primary making a move
			// primary is never making request quit
			if (action == PlayerAction.JOIN) {
				theMaze.addPlayer(here.getKey(), me);
			} else {
				theMaze.movePlayer(target.getKey(), action);
			}
			updateBackUpServer();

			// update the local GUI (this is for primary server)
			gameWindow.updateMaze(theMaze);
			return true;
		}

		// look up the corresponding player in RMI, and add to the current game
		// return true if succeed
		GameNode playerNode = findGameNode(target);

		if (playerNode == null)
			return false;

		switch (action) {
		case JOIN:
			addNewPlayer(playerNode, target);
			tellBackUpAndCallingNodesTheNewGameState(target, playerNode);
			break;
		case QUIT:
			removePlayer(target);
			break;
		default:
			// MOVE_UP, LEFT, RIGHT, DOWN, STAY
			theMaze.movePlayer(target.getKey(), action);
			tellBackUpAndCallingNodesTheNewGameState(target, playerNode);
			break;
		}

		System.out.println("Processed message: " + message.toString());
		return true;
	}

	private synchronized void tellBackUpAndCallingNodesTheNewGameState(Address target, GameNode callingNode) {
		// update the local GUI (this is for primary server)
		gameWindow.updateMaze(theMaze);

		Address callingAddress = null;
		// update the calling player node
		try {
			callingNode.updateGame(new ServerMessage(theMaze));
			callingAddress = callingNode.getAddress();

			// if backUpServer has not set up yet
			if (backUpServer == null || backUpServer.sameAs(here)) {
				findNewBackUpAndUpdate();
			} else if (!callingAddress.sameAs(backUpServer)) {
				// update the back up server
				updateBackUpServer();
			}

		} catch (RemoteException e) {
			removePlayer(target);
		}

	}

	public boolean isPrimary() {
		return isPrimary;
	}

	public boolean isBackUp() {
		return isBackUp;
	}

	@Override
	public synchronized void becomePrimary() {
		isPrimary = true;
		isBackUp = false;
		primaryServer = here;
		backUpServer = null;
		findNewBackUpAndUpdate();

		if (!pinThread.isAlive())
			pinThread.start();
	}

	@Override
	public synchronized void becomeBackUp(Address primary) {
		isPrimary = false;
		isBackUp = true;
		primaryServer = primary;
		backUpServer = here;
	}

	/**
	 * Update the back up server with the most up to date game maze This should
	 * only be called by primary server
	 */
	private synchronized boolean updateBackUpServer() {
		GameNode backUpNode = null;

		if (backUpServer == null || backUpServer.sameAs(here) || backUpServer.sameAs(primaryServer)) {
			Vector<Address> nodes = getNodesListFromTracker();
			if (nodes.size() < 2)
				return false;
			backUpServer = nodes.get(1);
		}

		try {
			backUpNode = (GameNode) LocateRegistry.getRegistry(backUpServer.getHost(), backUpServer.getPort())
					.lookup(backUpServer.getKey());
			backUpNode.updateGame(new ServerMessage(theMaze));

			if (!backUpNode.isBackUp())
				backUpNode.becomeBackUp(here);

			System.out
					.println("BackUp Server " + backUpNode.getAddress().getUserName() + " has updated with new state");

			return true;
		} catch (RemoteException | NotBoundException e) {
			// in the event that back up server is no where to be found
			System.out.println("BackUp Server has stopped working");
			// remove the old dead back up node
			removePlayer(backUpServer);
			// find a new one
			findNewBackUpAndUpdate();

			return false;
		}
	}

	/**
	 * Original back up server stopped working, find a new one. only be called
	 * by primary server
	 */
	private synchronized void findNewBackUpAndUpdate() {

		// find new one
		GameNode backUpNode = null;
		boolean done = false;

		// find the next available node
		// start from second one, since first one can be primary and we dont
		// know (when primary quits game)
		Vector<Address> nodes = getNodesListFromTracker();
		
		if (nodes == null || nodes.size() < 2)
			return;
		
		int i = 1;
		
		while (!done){
			Address address = nodes.get(i);
			i++;
			if (address != null && !address.sameAs(here) && !address.sameAs(primaryServer)
					&& !address.sameAs(backUpServer)) {
				try {
					backUpNode = (GameNode) LocateRegistry.getRegistry(address.getHost(), address.getPort())
							.lookup(address.getKey());
					backUpNode.updateGame(new ServerMessage(theMaze));
					backUpNode.becomeBackUp(here);
					backUpServer = address;

					System.out.println(backUpNode.getAddress().getUserName() + " is now back up");
					done = true;
					
				} catch (NotBoundException | RemoteException e) {
					// e.printStackTrace();
					backUpServer = null;
					done = false;
					System.out.println("Building new back up failed for player " + address.getUserName() + ", trying next node");
				}
			}
		}
	}

	private synchronized Vector<Address> getNodesListFromTracker() {
		try {
			Vector<Address> nodes = tracker.getNodes();
			return nodes;

		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Tracker has stopped working");
			return null;
		}
	}

	/**
	 * This is called from processing client message. At this point of time,
	 * tracker should have the address of this new player already
	 * 
	 * @param playerNode
	 * @throws RemoteException
	 */
	private synchronized boolean addNewPlayer(GameNode playerNode, Address address) {
		// tell the local maze
		boolean added = false;
		try {
			added = theMaze.addPlayer(address.getKey(), playerNode.getPlayer());
			System.out.println("Player " + playerNode.getPlayer().getName() + " has been added to maze " + added);
		} catch (RemoteException e) {
			// that node is longer there
			// e.printStackTrace();
			System.out.println("Player " + address.getUserName() + " can not be located. Removing from game");
			removePlayer(address);
		}

		return added;
	}

	/**
	 * Regularly checks whether the players are still in game The checking
	 * interval has been set to 5000ms => 5s
	 */
	static int pinCounter = -1;

	private Runnable pinPlayersRunnable = new Runnable() {

		@Override
		public void run() {
			while (isGamePlaying) {
				try {
					Thread.sleep(5000);

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Vector<Address> nodes = getNodesListFromTracker();

				if (nodes.size() > 2) {
					synchronized (this) {
						// we are skipping 0 and 1, since we are not checking
						// primary and back up
						if (pinCounter < 0) {
							pinCounter = nodes.size() - 1; // the last one of nodes list
						}
						
						System.out.println("Pin Thread pins");

						findGameNode(nodes.get(pinCounter));
						pinCounter--;
					}
				}
			}
		}

	};

	/**
	 * SERVER SIDE ENDS
	 */

	private GameNode findGameNode(Address address) {
		try {
			GameNode node = (GameNode) LocateRegistry.getRegistry(address.getHost(), address.getPort())
					.lookup(address.getKey());
			node.ping();
			return node;
		} catch (RemoteException | NotBoundException e) {
			System.out.println("Player " + address.getUserName() + "is not in game. Removing");
			// in case the node is not there, (may have crashed)
			// we remove it from server
			removePlayer(address);
		}
		return null;
	}

	private synchronized void removePlayer(Address playerAddress) {
		// remove from maze
		theMaze.removePlayer(playerAddress.getKey());

		// update the local GUI (this is for primary server)
		gameWindow.updateMaze(theMaze);

		// notify the tracker of player leaving
		try {
			
			tracker.deleteNode(playerAddress);
			
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Tracker has stopped working");
			isGamePlaying = false;
			System.exit(0);
			
		} finally {
			if (!backUpServer.sameAs(playerAddress)) {
				// update backUpServer
				updateBackUpServer();
			}else{
				backUpServer = null;
				findNewBackUpAndUpdate();
			}
		}
	}

	/**
	 * LOCAL STARTS
	 */

	@Override
	public boolean updateGame(ServerMessage message) throws RemoteException {
		System.out.println("Received message from primary");
		messagesFromServer.add(message);
		clientExecutor.submit(new Runnable() {
			@Override
			public void run() {
				ServerMessage nextMessage = messagesFromServer.poll();
				if (nextMessage != null) {
					processMessageFromServer(nextMessage);
				}
			}
		});

		return true;
	}

	private synchronized void processMessageFromServer(ServerMessage nextMessage) {
		// update the local maze
		Maze serverMaze = nextMessage.getTheMaze();
		theMaze.copyDataFrom(serverMaze);
		gameWindow.updateMaze(theMaze);
	}

	@Override
	public Address getAddress() throws RemoteException {
		return here;
	}

	@Override
	public Player getPlayer() throws RemoteException {
		return me;
	}

	public synchronized boolean playerMadeAMove(PlayerAction action) {

		ClientMessage message = new ClientMessage(here, action);
		System.out.println(message.toString());

		if (isPrimary && action != PlayerAction.QUIT) {
			try {
				enqueueNewMessage(message);
			} catch (RemoteException e) {
				// if primary is down, this is not possible since this is
				// primary
				e.printStackTrace();
			}
			return true;
		}

		try {
			// tell the primary server
			GameNode primary = (GameNode) LocateRegistry.getRegistry(primaryServer.getHost(), primaryServer.getPort())
					.lookup(primaryServer.getKey());

			primary.enqueueNewMessage(message);

			return true;

		} catch (RemoteException | NotBoundException e) {
			// in the event that primary is down
			System.out.println("playerMadeAMove error, primary is down");

			try {
				GameNode nextPrimary = (GameNode) LocateRegistry.getRegistry(backUpServer.getHost(), backUpServer.getPort())
						.lookup(backUpServer.getKey());

				if (!nextPrimary.isPrimary())
					nextPrimary.becomePrimary();

				primaryServer = nextPrimary.getAddress();
				backUpServer = nextPrimary.getBackUpServerAddress();

				if (backUpServer.sameAs(here) && !isBackUp) {
					becomeBackUp(primaryServer);
				}

				nextPrimary.enqueueNewMessage(message);

				return false;

			} catch (RemoteException | NotBoundException e1) {
				// and the backUp also failed

				System.out.println("playerMadeAMove error, backUp also failed");

				Vector<Address> nodes = getNodesListFromTracker();

				for (int i = 0; i < nodes.size(); i++) {
					Address address = nodes.get(i);
					if (!address.sameAs(here) && !address.sameAs(primaryServer) && !address.sameAs(backUpServer)) {
						try {
							GameNode newPrimary = (GameNode) LocateRegistry
									.getRegistry(address.getHost(), address.getPort()).lookup(address.getKey());

							if (newPrimary.isPrimary())
								newPrimary.becomePrimary();

							primaryServer = newPrimary.getAddress();
							backUpServer = newPrimary.getBackUpServerAddress();

							if (backUpServer.sameAs(here) && !isBackUp) {
								becomeBackUp(primaryServer);
							}

							newPrimary.enqueueNewMessage(message);

							return false;

						} catch (RemoteException | NotBoundException e2) {
							// No three nodes could fail together
							e2.printStackTrace();
						}
					}
				}
			}
		}

		return true;
	}

	/**
	 * LOCAL ENDS
	 */

	public void closeGame() {

		if (!isGamePlaying)
			return;

		isGamePlaying = false;

		Vector<Address> nodes = getNodesListFromTracker();

		if (nodes == null || nodes.size() < 3) {
			// less than 2 players in game
			isPrimary = false;
			return;
		}
		
		System.out.println("Game closing: " + here.getUserName() + " nodes in game " + nodes.size());

		// there will always be at least 2 players
		if (isPrimary) {
			// when there are enough players, we let the backUp to be
			// primary, and tell it to remove here from the game
			try {
				if (!messagesFromClient.isEmpty()){
					while(messagesFromClient.peek() != null){
						serverExecutor.submit(new Runnable() {
							@Override
							public void run() {
								ClientMessage nextMessage = messagesFromClient.poll();
								if (nextMessage != null) {
									processMessageFromClient(nextMessage);
								}
							}
							
						});
					}
				}
				serverExecutor.shutdown();
				
				theMaze.removePlayer(here.getKey());
				
				GameNode backUp = (GameNode) LocateRegistry.getRegistry(backUpServer.getHost(), backUpServer.getPort())
						.lookup(backUpServer.getKey());
				backUp.updateGame(new ServerMessage(theMaze));
				backUp.becomePrimary();

				primaryServer = backUp.getAddress();
				backUpServer = backUp.getBackUpServerAddress();

				isPrimary = false;
				tracker.deleteNode(here);
				
				//backUp.enqueueNewMessage(new ClientMessage(here, PlayerAction.QUIT));

			} catch (RemoteException | NotBoundException e) {
				e.printStackTrace();
			}

		} else {
			playerMadeAMove(PlayerAction.QUIT);
		}
	}

	@Override
	public Maze getMaze() throws RemoteException {
		return theMaze;
	}

	@Override
	public boolean ping() throws RemoteException {
		// just to check whether this node is still running
		return true;
	}

	@Override
	public Address getBackUpServerAddress() throws RemoteException {
		return backUpServer;
	}

}
