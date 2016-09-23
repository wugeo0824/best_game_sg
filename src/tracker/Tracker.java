package tracker;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

import model.Address;

public interface Tracker extends Remote {
	
	int getN() throws RemoteException;
	
	int getK() throws RemoteException;
	
	Vector<Address> getNodes() throws RemoteException;

	void addNode(Address node) throws RemoteException;
	
	void deleteNode(Address node) throws RemoteException;

	//void updateNodesList(Vector<Address> updatedNodes) throws RemoteException;
	
	int getPort() throws RemoteException;

}
