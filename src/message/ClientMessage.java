package message;

import java.io.Serializable;

import model.Address;

public class ClientMessage implements Serializable {

	/**
	 * Unique ID for serialization
	 */
	private static final long serialVersionUID = 1990824L;

	private PlayerAction playerAction;
	private Address targetAddress;

	public ClientMessage(Address address, PlayerAction playerAction) {
		super();
		this.playerAction = playerAction;
	}

	public PlayerAction getPlayerAction() {
		return playerAction;
	}

	public Address getTargetAddress() {
		return targetAddress;
	}

}
