package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import game.GameNodeImpl;
import message.PlayerAction;
import model.Location;
import model.Maze;
import model.Player;
import model.Treasure;

public class Window extends JFrame {
	private static final long serialVersionUID = 1509090869849539876L;
	
	public int N; // the maze is an N-by-N grid
	public int K; // the number of treasures

	private String title; // showed in the top of the window
	private String username; // part of the title TODO change into exactly two characters
	private String server; // part of the title, identify primary/backup server
	private String status = "[waiting]"; // part of the title, waiting is the default value of game status

	private Object[][] playerData; 
	private Player[] playerArray;
	private Treasure[] treasureArray;
	private String[][] btnName;
	private GameNodeImpl localGame;

	public Window(GameNodeImpl localGame) throws RemoteException {
		Maze maze = localGame.getMaze();
		this.N = maze.getSize();
		this.K = maze.getNumberOfTreasures();
		this.setSize(910, 750);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
		this.localGame = localGame;
		this.username = localGame.getPlayer().getName();

		// Initialize the variables
		btnName = new String[N][N];
		
		Set<Map.Entry<String, Player>> playerInfoSet = maze.getPlayers().entrySet();
		playerArray = new Player[playerInfoSet.size()];
		int i = 0;
		Iterator<Map.Entry<String, Player>> pl = playerInfoSet.iterator();
		while (pl.hasNext()){
			playerArray[i] = pl.next().getValue();
			i++;
		}
		
		Set<Map.Entry<String, Treasure>> treasureSet = maze.getTreasures().entrySet();
		treasureArray = new Treasure[treasureSet.size()];
		i = 0;
		Iterator<Map.Entry<String, Treasure>> tr = treasureSet.iterator();
		while (tr.hasNext()){
			treasureArray[i] = tr.next().getValue();
			i++;
		}
		
		playerData = new Object[playerArray.length][2];
		
		// Create panel p1 for the scores at the left part of the window
		JPanel p1 = new JPanel();
		String[] columnNames = { "Player", "Score" }; // title of the table
		
		getAllPlayerData();		
		JTable playerTable = new JTable(playerData, columnNames);
		JScrollPane scrollPane = new JScrollPane(playerTable);
		scrollPane.setPreferredSize(new Dimension(150, 700));
		p1.add(scrollPane);

		// Create panel p2 for the positions at the right of the window
		JPanel p2 = new JPanel();
		p2.setLayout(new GridLayout(N, N));
		
		getAllPositions();
		for (int r = 0; r < N; r++) {
			for (int c = 0; c < N; c++) {
				JButton btn = new JButton();
				btn.setText(btnName[r][c]);
				btn.setPreferredSize(new Dimension(50, 50));
				btn.setBorder(BorderFactory.createEtchedBorder());
				p2.add(btn);
			}
		}

		// add the two panels to the frame
		add(p1, BorderLayout.WEST);
		add(p2, BorderLayout.EAST);
		
		// add key listener
		setFocusable(true);
		addKeyListener(new MyKeyListener ());
		
		// update title
		setWTitle();
	}


	/**
	 * WINDOW TITLE PART
	 */
//	private void getUsername() {
//		username = JOptionPane.showInputDialog(null, "Welcome!\n" + "Please setup an username first.", "Maze",
//				JOptionPane.PLAIN_MESSAGE);
//	}

	private void getServer() {
		if (localGame.isPrimary()) {
			server = "(primary server)";
		} else if (localGame.isBackUp()) {
			server = "(backup server)";
		} else {
			server = "";
		}

	}

	private void getStatus() {
		if (playerArray.length > 1) {
			status = "[started]";
		}
	}

	public String getWTitle() {
		//getUsername();
		getStatus();
		getServer();

		title = username + server + status;
		return title;
	}
	
	public void setWTitle(){
		this.setTitle(getWTitle());
	}
	

	/**
	 * WINDOW LEFT PART
	 */
	public void getAllPlayerData() {
		// set players id and score in JTable
		for (int k = 0; k < playerArray.length; k++) {
			playerData[k][0] = playerArray[k].getName();
			playerData[k][1] = playerArray[k].getCurrentScore();
		}
	}


	/**
	 * WINDOW RIGHT PART
	 */
	public void getAllPositions(){
		// set players positions as players names
		for (int k = 0; k < playerArray.length; k++) {
			Location locP = playerArray[k].getCurrentLocation();
			btnName[locP.getLocationX()][locP.getLocationY()] = playerArray[k].getName();
		}
		
		// set treasures positions as "*"	
		for (int k = 0; k < treasureArray.length; k++) {
			Location locT = treasureArray[k].getLocation();
			btnName[locT.getLocationX()][locT.getLocationY()] = "*";
		}
	}

	/**
	 * KEYEVENT
	 */
	class MyKeyListener extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			int key = e.getKeyCode();
			PlayerAction action = null;
			if (key == KeyEvent.VK_1){
				action = PlayerAction.MOVE_LEFT;
			} else if (key == KeyEvent.VK_2){
				action = PlayerAction.MOVE_DOWN;
			} else if (key == KeyEvent.VK_3){
				action = PlayerAction.MOVE_RIGHT;
			} else if (key == KeyEvent.VK_4){
				action = PlayerAction.MOVE_UP;
			} else if (key == KeyEvent.VK_9){
				action = PlayerAction.QUIT;
			} else if (key == KeyEvent.VK_0){
				action = PlayerAction.STAY;
			}
			processKeyInput(action);
			return;			
		}
	}
	
	private void processKeyInput(PlayerAction action) {
		if (action == null){
			return;
		}
		
		if (action == PlayerAction.QUIT){
			localGame.closeGame();
			return;
		}
			
		localGame.playerMadeAMove(action);
	}
	
	public void updateGame(Maze newMaze){
		//TODO update the GUI according to the maze
	}
	
	public void close(){
		//TODO when user request to quit the game
	}


}
