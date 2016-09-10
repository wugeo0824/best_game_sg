package game;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.InvalidParameterException;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import message.ClientMessage;
import message.PlayerAction;
import message.ServerMessage;
import model.Address;
import model.Maze;
import model.Player;

public class GameNodeImpl implements GameNode {

	private final static int MESSAGE_SIZE_LIMIT = 20;

	private Address primaryServer;
	private Address backUpServer;

	private boolean isPrimary = false;
	private boolean isBackUp = false;

	private ConcurrentHashMap<String, Address> playersInGame;

	private Maze theMaze;
	private Player me;
	private Address here;

	private LinkedBlockingQueue<ClientMessage> messagesFromClient;
	private Thread workerThread;
	private LinkedBlockingQueue<ServerMessage> messagesFromServer;
	private Thread gameThread;

	public GameNodeImpl(Vector<Address> playerNameList, int size, int numberOfTreasures) {
		if (playerNameList.isEmpty()){
			primaryServer = here;
		}
		
		if (playerNameList.size() == 1){
			backUpServer = here;
		}
		
		if (playerNameList.size() >= 2){
			primaryServer = playerNameList.get(0);
			backUpServer = playerNameList.get(1);
		}
		
		theMaze = new Maze(size, numberOfTreasures);
		messagesFromClient = new LinkedBlockingQueue<ClientMessage>(MESSAGE_SIZE_LIMIT);

		playersInGame = new ConcurrentHashMap<String, Address>();
		workerThread = new Thread(processMessageRunnable);

		messagesFromServer = new LinkedBlockingQueue<ServerMessage>();
		gameThread = new Thread(updateMazeRunnable);
	}

	@Override
	public void startNewGame() {
		if (primaryServer == null || backUpServer == null){
			throw new InvalidParameterException("Primary server address is " + (primaryServer == null));
		}
		
		workerThread.start();
		gameThread.start();
	}
	
	/**
	 * SERVER SIDE
	 * STARTS
	 */

	public void createFromBackUp(Maze backUpMaze) {
		theMaze = null;
		theMaze = new Maze(backUpMaze.getSize(), backUpMaze.getNumberOfTreasures());
		theMaze.copyDataFrom(backUpMaze);
	}

	@Override
	public boolean enqueueNewMessage(ClientMessage message) throws RemoteException {
		return messagesFromClient.offer(message);
	}

	private Runnable processMessageRunnable = new Runnable() {

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ClientMessage nextMessage = messagesFromClient.poll();
				if (nextMessage != null) {
					processMessage(nextMessage);
				}
			}
		}

	};

	private void processMessage(ClientMessage message) {
		Address target = message.getTargetAddress();
		PlayerAction action = message.getPlayerAction();

		// look up the corresponding player in RMI, and add to the current game
		// return true if succeed
		GameNode playerNode = findGameNode(target);

		if (playerNode == null)
			return;

		switch (action) {
		case JOIN:
			try {
				joinGame(playerNode);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				removePlayer(target.getKey());
			}
			break;
		case QUIT:
			removePlayer(target.getKey());
			break;
		default:
			// MOVE_UP, LEFT, RIGHT, DOWN, STAY
			theMaze.movePlayer(target.getKey(), action);
			break;
		}

		// update the calling player node
		try {
			playerNode.updateGame(new ServerMessage(theMaze));
		} catch (RemoteException e) {
			e.printStackTrace();
			removePlayer(target.getKey());
		}

		// update the back up server
	}

	public boolean isPrimary() {
		return isPrimary;
	}

	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}

	public boolean isBackUp() {
		return isBackUp;
	}

	public void setBackUp(boolean isBackUp) {
		this.isBackUp = isBackUp;
	}
	
	/**
	 * SERVER SIDE
	 * ENDS
	 */
	
	/**
	 * LOCAL
	 * STARTS
	 */
	
	public void updatePrimaryAddress(Address primary) {
		primaryServer = primary;
	}
	
	public void updateBackUp(Address backUp) {
		backUpServer = backUp;
	}

	@Override
	public void ping() throws RemoteException {

	}

	private void joinGame(GameNode playerNode) throws RemoteException {
		playersInGame.put(playerNode.getAddress().getKey(), playerNode.getAddress());
		theMaze.addPlayer(playerNode.getAddress().getKey(), playerNode.getPlayer());
		// tracker should have already known that this player joined game
	}

	private GameNode findGameNode(Address address) {
		try {
			return (GameNode) LocateRegistry.getRegistry(address.getHost(), address.getPort()).lookup(address.getKey());
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
			// in case the node is not there,
			// (may have crashed,)
			// we remove it from server
			if (playersInGame.get(address.getKey()) != null) {
				removePlayer(address.getKey());
			}
		}
		return null;
	}

	private void removePlayer(String playerKey) {
		theMaze.removePlayer(playerKey);
		playersInGame.remove(playerKey);
		// TODO notify the tracker of player leaving
	}

	@Override
	public boolean updateGame(ServerMessage message) {
		return messagesFromServer.offer(message);
	}

	private Runnable updateMazeRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			ServerMessage nextMessage = messagesFromServer.poll();
			if (nextMessage != null) {
				updateMazeMessage(nextMessage);
			}
		}
	};
	
	private void updateMazeMessage(ServerMessage nextMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public Address getAddress() throws RemoteException {
		return here;
	}

	@Override
	public Player getPlayer() throws RemoteException {
		return me;
	}
	
	/**
	 * LOCAL
	 * ENDS
	 */

}
