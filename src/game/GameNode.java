package game;

import java.rmi.Remote;
import java.rmi.RemoteException;

import message.ClientMessage;
import message.ServerMessage;
import model.Address;
import model.Player;

public interface GameNode extends Remote {

	void startNewGame() throws RemoteException;

	boolean enqueueNewMessage(ClientMessage message) throws RemoteException;

	boolean isPrimary() throws RemoteException;

	boolean isBackUp() throws RemoteException;

	void setPrimary(boolean isPrimary) throws RemoteException;

	void setBackUp(boolean isBackUp) throws RemoteException;

	void ping() throws RemoteException;
	
	boolean updateGame(ServerMessage message) throws RemoteException;

	Address getAddress() throws RemoteException;

	Player getPlayer() throws RemoteException;

	//boolean joinGame(Address newPlayerAddress) throws RemoteException;

}
