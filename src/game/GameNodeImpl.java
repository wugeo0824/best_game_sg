package game;

import java.rmi.RemoteException;
import java.util.concurrent.LinkedBlockingQueue;

import message.ClientMessage;
import message.ServerMessage;
import model.Address;
import model.Maze;
import model.Player;

public class GameNodeImpl implements GameNode {

	private GameServer primaryServer;
	private GameServer backUpServer;

	// local copy of data
	private GameServerImpl localSimulation;
	private Player me;
	private Maze localMaze;
	private Address here;

	private LinkedBlockingQueue<ServerMessage> incomingMessages;
	private Thread gameThread;

	public GameNodeImpl(Address primaryServerAddress, int size, int numberOfTreasures) {
		// TODO get primary server and initialization
		localMaze = new Maze(size, numberOfTreasures);
		incomingMessages = new LinkedBlockingQueue<ServerMessage>();
		gameThread = new Thread(updateMazeRunnable);

		initGame();
	}

	private void initGame() {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() {
		gameThread.start();
	}

	@Override
	public boolean updateGame(ServerMessage message) {
		return incomingMessages.offer(message);
	}

	private Runnable updateMazeRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			ServerMessage nextMessage = incomingMessages.poll();
			if (nextMessage != null) {
				processMessage(nextMessage);
			}
		}

		private void processMessage(ServerMessage nextMessage) {
			// TODO Auto-generated method stub

		}
	};

	@Override
	public void ping() throws RemoteException {
	}

	@Override
	public Address getAddress() throws RemoteException {
		return here;
	}

	@Override
	public Player getPlayer() throws RemoteException {
		return me;
	}
}
