package tracker;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

import model.Address;

public interface Tracker extends Remote {
	
	int getN() throws RemoteException;
	
	int getK() throws RemoteException;
	
	Vector<Address> getNodes() throws RemoteException;

	void addNodeToRMIRegistry(Address node);

	void updateNodesList(Vector<Address> updatedNodes);

}
