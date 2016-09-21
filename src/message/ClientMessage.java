package message;

import java.io.Serializable;
import model.Address;

//public interface ClientMessage extends Remote {
//
//	public PlayerAction getPlayerAction();
//
//	public Address getTargetAddress();
//}
public final class ClientMessage implements Serializable{

	/**
	 * Unique ID for serialization
	 */
	private static final long serialVersionUID = 1990824L;

	private PlayerAction playerAction;
	private Address targetAddress;

	public ClientMessage(Address address, PlayerAction playerAction) {
		this.targetAddress = address;
		this.playerAction = playerAction;
	}

	public PlayerAction getPlayerAction() {
		return playerAction;
	}

	public Address getTargetAddress() {
		return targetAddress;
	}

	@Override
	public String toString() {
		return "player " + targetAddress.getUserName() + " want to " + playerAction.toString();
		
	}
}