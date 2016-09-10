package game;

import java.rmi.Remote;
import java.rmi.RemoteException;

import message.ClientMessage;
import model.Address;

public interface GameServer extends Remote {

	void startNewGame() throws RemoteException;

	boolean enqueueNewMessage(ClientMessage message) throws RemoteException;

	boolean isPrimary() throws RemoteException;

	boolean isBackUp() throws RemoteException;

	void setPrimary(boolean isPrimary) throws RemoteException;

	void setBackUp(boolean isBackUp) throws RemoteException;

	void ping() throws RemoteException;

	boolean joinGame(Address newPlayerAddress) throws RemoteException;

}
