package message;

import java.io.Serializable;

public enum PlayerAction implements Serializable{
	JOIN, MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, STAY, QUIT
}
