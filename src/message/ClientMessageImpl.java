//package message;
//
//import java.io.Serializable;
//import java.rmi.RemoteException;
//import java.rmi.server.UnicastRemoteObject;
//
//import model.Address;
//
//public final class ClientMessageImpl implements ClientMessage, Serializable{
//
//	/**
//	 * Unique ID for serialization
//	 */
//	private static final long serialVersionUID = 1990824L;
//
//	private PlayerAction playerAction;
//	private Address targetAddress;
//
//	public ClientMessageImpl(Address address, PlayerAction playerAction) {
//		this.targetAddress = address;
//		this.playerAction = playerAction;
//	}
//
//	public PlayerAction getPlayerAction() {
//		return playerAction;
//	}
//
//	public Address getTargetAddress() {
//		return targetAddress;
//	}
//
//	@Override
//	public String toString() {
//		return "player " + targetAddress.getUserName() + " want to " + playerAction.toString();
//		
//	}
//}
