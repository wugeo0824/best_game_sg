package message;

import java.io.Serializable;

import model.Maze;

public class ServerMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8997183143974943698L;

	private Maze theMaze;

	public ServerMessage(Maze theMaze) {
		super();
		this.theMaze = theMaze;
	}

	public Maze getTheMaze() {
		return theMaze;
	}

}
