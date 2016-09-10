package game;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import message.ClientMessage;
import message.PlayerAction;
import message.ServerMessage;
import model.Address;
import model.Maze;

public class GameServerImpl implements GameServer {

	private final static int MESSAGE_SIZE_LIMIT = 20;

	private boolean isPrimary = false;
	private boolean isBackUp = false;

	private ConcurrentHashMap<String, GameNode> playerNodes;

	private Maze theMaze;

	private LinkedBlockingQueue<ClientMessage> incomingMessages;
	private Thread workerThread;

	public GameServerImpl(int size, int numberOfTreasures) {
		theMaze = new Maze(size, numberOfTreasures);
		incomingMessages = new LinkedBlockingQueue<ClientMessage>(MESSAGE_SIZE_LIMIT);
		playerNodes = new ConcurrentHashMap<String, GameNode>();
		workerThread = new Thread(processMessageRunnable);
	}

	@Override
	public void startNewGame() {
		workerThread.start();
	}

	public void createFromBackUp(Maze backUpMaze) {
		theMaze = null;
		theMaze = new Maze(backUpMaze.getSize(), backUpMaze.getNumberOfTreasures());
		theMaze.copyDataFrom(backUpMaze);
	}

	@Override
	public boolean enqueueNewMessage(ClientMessage message) {
		return incomingMessages.offer(message);
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
				ClientMessage nextMessage = incomingMessages.poll();
				if (nextMessage != null) {
					processMessage(nextMessage);
				}
			}
		}

	};

	private void processMessage(ClientMessage message) {
		// TODO
		Address target = message.getTargetAddress();
		PlayerAction action = message.getPlayerAction();
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

	@Override
	public void ping() throws RemoteException {

	}

	@Override
	public boolean joinGame(Address newPlayerAddress) throws RemoteException {
		// look up the corresponding player in RMI, and add to the current game
		// return true if succeed
		GameNode playerNode = findGameNode(newPlayerAddress);
		if (playerNode == null)
			return false;

		playerNodes.put(playerNode.getAddress().getKey(), playerNode);
		theMaze.addPlayer(playerNode.getAddress().getKey(), playerNode.getPlayer());
		playerNode.updateGame(new ServerMessage(theMaze));

		return true;
	}

	private GameNode findGameNode(Address address) {
		try {
			return (GameNode) LocateRegistry.getRegistry(address.getHost(), address.getPort()).lookup(address.getKey());
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
			// in case the node is not there, we remove it from server
			if (playerNodes.get(address.getKey()) != null) {
				theMaze.removePlayer(address.getKey());
				playerNodes.remove(address.getKey());
				// TODO notify the tracker of player leaving
			}
			return null;
		}
	}

}
