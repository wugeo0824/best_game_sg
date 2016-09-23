package game;

import java.rmi.Remote;
import java.rmi.RemoteException;

import message.ClientMessage;
import message.ServerMessage;
import model.Address;
import model.Maze;
import model.Player;

public interface GameNode extends Remote {

	//void startProcessingMessages() throws RemoteException;
	
	void init() throws RemoteException;

	boolean enqueueNewMessage(ClientMessage message) throws RemoteException;

	boolean isPrimary() throws RemoteException;

	boolean isBackUp() throws RemoteException;
	
	void becomePrimary() throws RemoteException;
	
	void becomeBackUp() throws RemoteException;

	void ping() throws RemoteException;
	
	boolean updateGame(ServerMessage message) throws RemoteException;

	Address getAddress() throws RemoteException;

	Player getPlayer() throws RemoteException;
	
	Maze getMaze() throws RemoteException;

	//boolean joinGame(Address newPlayerAddress) throws RemoteException;

}
