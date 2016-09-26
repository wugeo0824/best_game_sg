package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

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
	private String username; // part of the title TODO change into exactly two
								// characters
	private String server; // part of the title, identify primary/backup server
	private String status = "[waiting]"; // part of the title, waiting is the
											// default value of game status

	private Object[][] table;
	private ArrayList<Player> playerList;
	private ArrayList<Treasure> treasureList;
	private String[][] btnName;
	private JButton[][] buttons;
	private GameNodeImpl localGame;
	private DefaultTableModel tableModel;
	private static String tableTitle[] = { "Player", "Score" };
	private JPanel p1;
	private JPanel p2;
	private PlayerAction action = null;
	private int key;
	private MyKeyListener myKeylistener;

	public Window(GameNodeImpl localGame) throws RemoteException {
		Maze maze = localGame.getMaze();
		this.N = maze.getSize();
		this.K = maze.getNumberOfTreasures();
		this.setResizable(true);
		this.setSize(910, 750);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
		this.localGame = localGame;
		this.username = localGame.getPlayer().getName();
		this.setFocusable(true);

		myKeylistener = new MyKeyListener();

		// Initialize the variables
		btnName = new String[N][N];
		buttons = new JButton[N][N];

		playerList = new ArrayList<Player>();
		treasureList = new ArrayList<Treasure>();

		// setTableData();
		// setAllPositions();
		//
		// // add the two panels to the frame
		//

		//addKeyListener();
		
		addKeyListener(myKeylistener);
		
		// update title
		setWTitle();
	}

	private ArrayList<Player> getPlayerList(HashMap<String, Player> players) {
		ArrayList<Player> playerList = new ArrayList<Player>();

		if (players.isEmpty()) {
			return playerList;
		}

		Set<Map.Entry<String, Player>> playerInfoSet = players.entrySet();
		Iterator<Map.Entry<String, Player>> pl = playerInfoSet.iterator();
		while (pl.hasNext()) {
			playerList.add(pl.next().getValue());
		}
		return playerList;
	}

	private ArrayList<Treasure> getTreasureList(HashMap<String, Treasure> treasures) {
		ArrayList<Treasure> treasureList = new ArrayList<Treasure>();

		if (treasures.isEmpty())
			return treasureList;

		Set<Map.Entry<String, Treasure>> treasureSet = treasures.entrySet();
		Iterator<Map.Entry<String, Treasure>> tr = treasureSet.iterator();
		while (tr.hasNext()) {
			treasureList.add(tr.next().getValue());
		}
		return treasureList;
	}

//	private void addKeyListener() {
//		if (keylistenrAdded)
//			return;
//
//		addKeyListener(myKeylistener);
//		keylistenrAdded = true;
//	}

	/**
	 * WINDOW TITLE PART
	 */

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
		if (playerList.size() > 1) {
			status = "[game started]";
		}
	}

	public String getWTitle() {
		// getUsername();
		getStatus();
		getServer();

		title = "Player: " + username + " " + server + " " + status;
		return title;
	}

	public void setWTitle() {
		this.setTitle(getWTitle());
	}

	/**
	 * WINDOW LEFT PART
	 */
	public Object[][] getTableData() {
		table = new Object[playerList.size()][2];
		// set players id and score in JTable
		for (int k = 0; k < playerList.size(); k++) {
			table[k][0] = playerList.get(k).getName();
			table[k][1] = playerList.get(k).getCurrentScore();
		}
		return table;
	}

	public JPanel setTableData() {
		// Create panel p1 for the scores at the left part of the window
		if (p1 == null) {
			p1 = new JPanel();
			add(p1, BorderLayout.WEST);
		}

		p1.removeAll();
		table = getTableData();
		tableModel = new DefaultTableModel(table, tableTitle);
		JTable playerTable = new JTable(tableModel);
		JScrollPane scrollPane = new JScrollPane(playerTable);
		scrollPane.setPreferredSize(new Dimension(150, 700));
		p1.add(scrollPane);

		return p1;

	}

	public void updateTableData() {
		setTableData();
	}

	/**
	 * WINDOW RIGHT PART
	 * 
	 * @return
	 */
	public String[][] getAllPositions() {
		for (int m = 0; m < N; m++) {
			for (int n = 0; n < N; n++) {
				btnName[m][n] = "";
			}
		}

		// set players positions as players names
		for (int k = 0; k < playerList.size(); k++) {
			Location locP = playerList.get(k).getCurrentLocation();
			btnName[locP.getLocationY()][locP.getLocationX()] = playerList.get(k).getName();
		}

		// set treasures positions as "*"
		for (int k = 0; k < treasureList.size(); k++) {
			Location locT = treasureList.get(k).getLocation();
			btnName[locT.getLocationY()][locT.getLocationX()] = "*";
		}
		
		return btnName;
	}

	public JPanel setAllPositions() {

		btnName = getAllPositions();

		// Create panel p2 for the positions at the right of the window
		if (p2 == null) {
			p2 = new JPanel();
			p2.setLayout(new GridLayout(N, N));

			for (int r = 0; r < N; r++) {
				for (int c = 0; c < N; c++) {
					JButton btn = new JButton();
					btn.setFocusable(false);
					btn.setPreferredSize(new Dimension(50, 50));
					btn.setBorder(BorderFactory.createEtchedBorder());
					buttons[r][c] = btn;
					p2.add(btn);
				}
			}
			add(p2, BorderLayout.EAST);
		}

		for (int r = 0; r < N; r++) {
			for (int c = 0; c < N; c++) {
				buttons[r][c].setText(btnName[r][c]);
			}
		}

		return p2;
	}

	public void updateAllPositions() {
		setAllPositions();
	}

	/**
	 * KEYEVENT
	 */
	class MyKeyListener extends KeyAdapter {
		public void keyReleased(KeyEvent e) {
			key = e.getKeyCode();
			if (key == KeyEvent.VK_1) {
				action = PlayerAction.MOVE_LEFT;
			} else if (key == KeyEvent.VK_2) {
				action = PlayerAction.MOVE_DOWN;
			} else if (key == KeyEvent.VK_3) {
				action = PlayerAction.MOVE_RIGHT;
			} else if (key == KeyEvent.VK_4) {
				action = PlayerAction.MOVE_UP;
			} else if (key == KeyEvent.VK_9) {
				action = PlayerAction.QUIT;
			} else if (key == KeyEvent.VK_0) {
				action = PlayerAction.STAY;
			}
			System.out.println("Key " + e.toString() + "pressed");
			processKeyInput(action);
			action = null;
			return;
		}
	}

	private void processKeyInput(PlayerAction action) {
		if (action == null) {
			return;
		}

		if (action == PlayerAction.QUIT) {
			localGame.closeGame();
			close();
			System.exit(0);
			return;
		}

		localGame.playerMadeAMove(action);
	}

	public void updateMaze(Maze newMaze) {
		// update the GUI according to the maze
		playerList.clear();
		treasureList.clear();
		playerList.addAll(getPlayerList(newMaze.getPlayers()));
		treasureList.addAll(getTreasureList(newMaze.getTreasures()));

		updateTableData();
		updateAllPositions();
		revalidate();
		setWTitle();
	}

	public void close() {
		// when user request to quit the game
		super.dispose();
	}

}
