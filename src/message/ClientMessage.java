package message;

import java.io.Serializable;

import model.Address;

public class ClientMessage implements Serializable {

	/**
	 * Unique ID for serialization
	 */
	private static final long serialVersionUID = 1990824L;

	private String targetId;
	private PlayerAction playerAction;
	private Address targetAddress;

	public ClientMessage(Address address, String targetId, PlayerAction playerAction) {
		super();
		this.targetId = targetId;
		this.playerAction = playerAction;
	}

	public String getTargetId() {
		return targetId;
	}

	public PlayerAction getPlayerAction() {
		return playerAction;
	}

	public Address getTargetAddress() {
		return targetAddress;
	}

}
