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

	public void attachShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
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
		//addNewPlayer(this);
		enqueueNewMessage(new ClientMessage(here, PlayerAction.JOIN));

		// start the actual game GUI
		gameWindow = new Window(this);
		
		// update the game GUI
		gameWindow.updateMaze(theMaze);
		
		// primary should constantly check whether there is dead node
		pinThread.start();

		System.out.println("Successfully started player at [" + here.getKey() + "]");
	}

	@Override
	public void startAsBackUp() throws RemoteException {
		startProcessingMessages();

		primaryServer = getNodesListFromTracker().get(0);
		backUpServer = here;
		isBackUp = true;
		System.out.println("Here is BackUp. Primary server at " + primaryServer.getKey());

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
		serverExecutor.submit(new Runnable() {
			@Override
			public void run() {
				ClientMessage nextMessage = messagesFromClient.poll();
				if (nextMessage != null) {
					processMessageFromClient(nextMessage);
				}
			}

		});
		return true;
	}

	private synchronized boolean processMessageFromClient(ClientMessage message) {
		Address target = message.getTargetAddress();
		PlayerAction action = message.getPlayerAction();

		if (target.sameAs(here)) {
			// when primary making a move
			// primary is never making request quit
			if (action == PlayerAction.JOIN){
				theMaze.addPlayer(here.getKey(), me);
			}else{
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
			try {
				addNewPlayer(playerNode);
				tellBackUpAndCallingNodesTheNewGameState(target, playerNode);
			} catch (RemoteException e) {
				System.out.println("Player " + target.getUserName() + " can not be located. Removing from game");
				removePlayer(target);
			}
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

		Address callingAddress = null;
		// update the calling player node
		try {
			callingNode.updateGame(new ServerMessage(theMaze));
			callingAddress = callingNode.getAddress();
		} catch (RemoteException e) {
			e.printStackTrace();
			removePlayer(target);
		}

		// if backUpServer has not set up yet
		if (backUpServer == null) {
			findNewBackUp();
		} else {
			// update the back up server
			if (callingAddress != null && !callingAddress.sameAs(backUpServer))
				updateBackUpServer();
		}

		// update the local GUI (this is for primary server)
		gameWindow.updateMaze(theMaze);
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
		findNewBackUp();

		if (!pinThread.isAlive())
			pinThread.start();
	}

	@Override
	public synchronized void becomeBackUp() {
		isPrimary = false;
		isBackUp = true;
		backUpServer = here;
	}

	/**
	 * Update the back up server with the most up to date game maze This should
	 * only be called by primary server
	 */
	private synchronized void updateBackUpServer() {
		GameNode backUpNode = null;
		if (backUpServer == null)
			return;
		
		try {
			backUpNode = (GameNode) LocateRegistry.getRegistry(backUpServer.getHost(), backUpServer.getPort())
					.lookup(backUpServer.getKey());
			backUpNode.updateGame(new ServerMessage(theMaze));
			System.out.println("BackUp Server has updated with new state");
		} catch (RemoteException | NotBoundException e) {
			// in the event that back up server is no where to be found
			System.out.println("BackUp Server has stopped working");
			// remove the old dead back up node
			removePlayer(backUpServer);
			// find a new one
			findNewBackUp();
		}
	}

	/**
	 * Original back up server stopped working, find a new one. only be called
	 * by primary server
	 */
	private synchronized void findNewBackUp() {

		// find new one
		GameNode backUpNode = null;

		try {
			// find the next available node
			// start from second one, since first one can be primary and we dont
			// know (when primary quits game)
			Vector<Address> nodes = getNodesListFromTracker();
			for (int i = 1; i < nodes.size(); i++) {
				Address address = nodes.get(i);
				if (!address.sameAs(here) && !address.sameAs(primaryServer) && !address.sameAs(backUpServer)) {
					backUpServer = address;
					backUpNode = (GameNode) LocateRegistry.getRegistry(backUpServer.getHost(), backUpServer.getPort())
							.lookup(backUpServer.getKey());
					backUpNode.updateGame(new ServerMessage(theMaze));
					backUpNode.becomeBackUp();
					System.out.println(backUpNode.getAddress().getUserName() + " is now back up");
					break;
				}
			}
		} catch (NotBoundException | RemoteException e) {
			e.printStackTrace();
			System.out.println("Cannot locate the node, this should not happen if the list is up to date");
		}
	}

	private synchronized Vector<Address> getNodesListFromTracker() {
		try {
			Vector<Address> nodes = tracker.getNodes();
			return nodes;
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Tracker has stopped working");
		}

		return null;

	}

	/**
	 * This is called from processing client message. At this point of time,
	 * tracker should have the address of this new player already
	 * 
	 * @param playerNode
	 * @throws RemoteException
	 */
	private synchronized boolean addNewPlayer(GameNode playerNode) throws RemoteException {
		// tell the local maze
		boolean added = theMaze.addPlayer(playerNode.getAddress().getKey(), playerNode.getPlayer());
		System.out.println("Player " + playerNode.getPlayer().getName() + " has been added to maze " + added);

		return added;
	}

	/**
	 * Regularly checks whether the players are still in game The checking
	 * interval has been set to 5000ms => 5s
	 */
	static int pinCounter = 2;

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

				System.out.println("Pin Thread pins");

				Vector<Address> nodes = getNodesListFromTracker();
				if (nodes.size() > 2) {
					synchronized (this) {
						// we are skipping 0 and 1, since we are not checking
						// primary and back up
						if (pinCounter >= nodes.size()) {
							pinCounter = 2;
						}

						findGameNode(nodes.get(pinCounter));
						pinCounter++;
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

		// notify the tracker of player leaving
		try {
			tracker.deleteNode(playerAddress);
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Tracker has stopped working");
			System.exit(0);
		}

		if (!backUpServer.sameAs(playerAddress)) {
			// update backUpServer
			updateBackUpServer();
		}

		// update the local GUI (this is for primary server)
		gameWindow.updateMaze(theMaze);
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

		// notify the GUI of the new game state (this is for nodes other than
		// primary server)
		if (gameWindow == null) {
			// start the actual game GUI
			try {
				gameWindow = new Window(this);
			} catch (RemoteException e) {
				// error connecting to the local node
				e.printStackTrace();
				System.out.println("error connecting to the local node");
			}
		}
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

	public synchronized void playerMadeAMove(PlayerAction action) {

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
			return;
		}

		// tell the primary server
		try {
			GameNode primary = (GameNode) LocateRegistry.getRegistry(primaryServer.getHost(), primaryServer.getPort())
					.lookup(primaryServer.getKey());
			primary.enqueueNewMessage(message);
			// if (!primary.isPrimary()) {
			// primary.becomePrimary();
			// }
		} catch (RemoteException | NotBoundException e) {
			// in the event that primary is down
			System.out.println("playerMadeAMove error, primary is down");
			try {
				GameNode backUp = (GameNode) LocateRegistry.getRegistry(backUpServer.getHost(), backUpServer.getPort())
						.lookup(backUpServer.getKey());
				backUp.becomePrimary();
				primaryServer = backUpServer;
				playerMadeAMove(message.getPlayerAction());

			} catch (RemoteException | NotBoundException e1) {
				// and the backUp also failed
				System.out.println("playerMadeAMove error, backUp also failed");
				Vector<Address> nodes = getNodesListFromTracker();
				for (int i = 0; i < nodes.size(); i++) {
					Address address = nodes.get(i);
					if (!address.sameAs(here) && !address.sameAs(primaryServer) && !address.sameAs(backUpServer)) {
						primaryServer = address;
					}
				}

				playerMadeAMove(message.getPlayerAction());
			}
		}
	}

	/**
	 * LOCAL ENDS
	 */

	public void terminateProcess() {
		System.exit(0);
	}

	public void closeGame() {

		Vector<Address> nodes = getNodesListFromTracker();

		if (nodes == null || nodes.size() < 3) {
			// less than 2 players in game
			isPrimary = false;
			// notify GUI
			gameWindow.close();
			return;
		}

		// there will always be at least 2 players
		if (isPrimary) {
			// when there are enough players, we let the backUp to be
			// primary, and tell it to remove here from the game
			try {
				GameNode backUp = (GameNode) LocateRegistry.getRegistry(backUpServer.getHost(), backUpServer.getPort())
						.lookup(backUpServer.getKey());
				backUp.becomePrimary();
				primaryServer = backUpServer;
				// backUpServer = nodesInGame.get(2);
				isPrimary = false;

			} catch (RemoteException | NotBoundException e) {
				e.printStackTrace();
			}

		}

		playerMadeAMove(PlayerAction.QUIT);
		isGamePlaying = false;

		// notify GUI
		serverExecutor.shutdown();
		clientExecutor.shutdown();
		gameWindow.close();
	}

	@Override
	public Maze getMaze() throws RemoteException {
		return theMaze;
	}

	@Override
	public void ping() throws RemoteException {
		// just to check whether this node is still running
	}

}
