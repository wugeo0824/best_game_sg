package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import message.PlayerAction;
import model.Address;
import model.Location;
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

	private Object[][] playerData = null; 
	private Player[] playerArray;
	private Treasure[] treasureArray;
	private String[][] btnName;
	public PlayerAction action;

	public Window(int nGrid, int kTreasure, ConcurrentHashMap<String, Player> playersInfo, ConcurrentHashMap<String, Treasure> treasuresInfo) {
		this.N = nGrid;
		this.K = kTreasure;
		this.setSize(910, 750);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);

		// Create panel p1 for the scores at the left part of the window
		JPanel p1 = new JPanel();
		String[] columnNames = { "Player", "Score" }; // title of the table
		
		getAllPlayerData(playersInfo);		
		JTable playerTable = new JTable(playerData, columnNames);
		JScrollPane scrollPane = new JScrollPane(playerTable);
		scrollPane.setPreferredSize(new Dimension(150, 700));
		p1.add(scrollPane);

		// Create panel p2 for the positions at the right of the window
		JPanel p2 = new JPanel();
		p2.setLayout(new GridLayout(N, N));
		
		getAllPositions(playersInfo, treasuresInfo);
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
	}


	/**
	 * WINDOW TITLE PART
	 */
	private void getUsername() {
		username = JOptionPane.showInputDialog(null, "Welcome!\n" + "Please setup an username first.", "Maze",
				JOptionPane.PLAIN_MESSAGE);
	}

	private void getServer(Vector<Address> playersList) {
		if (username == playersList.get(0).getUserName()) {
			server = "(primary server)";
		} else if (username == playersList.get(1).getUserName()) {
			server = "(backup server)";
		} else {
			server = null;
		}

	}

	private void getStatus(Vector<Address> playersList) {
		if (playersList.size() > 1) {
			status = "[started]";
		}
	}

	public String getWTitle(Vector<Address> playersList) {
		getUsername();
		getStatus(playersList);
		getServer(playersList);

		title = username + server + status;
		return title;
	}
	
	public void setWTitle(Vector<Address> playersList){
		this.setTitle(getWTitle(playersList));
	}
	

	/**
	 * WINDOW LEFT PART
	 */
	public void getAllPlayerData(ConcurrentHashMap<String, Player> playersInfo) {
		// set players id and score in JTable
		int size = 0;

		for (Map.Entry<String, Player> list : playersInfo.entrySet()) {
			playerArray[size] = list.getValue();
			size++;
		}

		for (int k = 0; k < size; k++) {
			playerData[k][0] = playerArray[k].getName();
			playerData[k][1] = playerArray[k].getCurrentScore();
		}
	}


	/**
	 * WINDOW RIGHT PART
	 */
	public void getAllPositions(ConcurrentHashMap<String, Player> playersInfo, ConcurrentHashMap<String, Treasure> treasuresInfo){
		// set players positions as players names
		int sizeP = 0;
		for (Map.Entry<String, Player> listP : playersInfo.entrySet()) {
			playerArray[sizeP] = listP.getValue();
			sizeP++;
		}
		for (int k = 0; k < sizeP; k++) {
			Location locP = playerArray[k].getCurrentLocation();
			btnName[locP.getLocationX()][locP.getLocationY()] = playerArray[k].getName();
		}
		
		// set treasures positions as "*"
		int sizeT = 0;
		for (Map.Entry<String, Treasure> listT : treasuresInfo.entrySet()) {
			treasureArray[sizeT] = listT.getValue();
			sizeT++;
		}
		for (int k = 0; k < sizeT; k++) {
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
			return;			
		}
		
		
	}
	


}
