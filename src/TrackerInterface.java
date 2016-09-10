package muo;

import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

public interface TrackerInterface extends Remote {
	
	int getN() throws RemoteException;
	
	int getK() throws RemoteException;
	
	Vector<Node> getNodes() throws RemoteException;

	void addNodeToRMIRegistry(GameNodeInterface node) throws RemoteException, AlreadyBoundException;

	void removeInactiveNodes(Vector<String> deleteNodeName) throws RemoteException;

}
