package tracker;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

import game.GameNode;
import model.Address;

public interface Tracker extends Remote {
	
	int getN() throws RemoteException;
	
	int getK() throws RemoteException;
	
	Vector<Address> getNodes() throws RemoteException;

	int addNode(Address node, GameNode gameNode) throws RemoteException;
	
	boolean deleteNode(Address node) throws RemoteException;

	//void updateNodesList(Vector<Address> updatedNodes) throws RemoteException;
	
	int getPort() throws RemoteException;
	
	String getIP() throws RemoteException;

}
