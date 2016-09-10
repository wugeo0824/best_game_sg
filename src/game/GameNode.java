package game;

import java.rmi.Remote;
import java.rmi.RemoteException;

import message.ServerMessage;
import model.Address;
import model.Player;

public interface GameNode extends Remote {

	// starts the game
	void start() throws RemoteException;

	boolean updateGame(ServerMessage message) throws RemoteException;

	void ping() throws RemoteException;

	Address getAddress() throws RemoteException;

	Player getPlayer() throws RemoteException;
}
