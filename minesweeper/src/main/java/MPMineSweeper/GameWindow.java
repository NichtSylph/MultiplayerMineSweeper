package MPMineSweeper;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

public class GameWindow extends JFrame {
    private CellButton[][] cellButtons;
    private final int WIDTH = 16;
    private final int HEIGHT = 16;
    private GameClient gameClient;
    private JButton readyButton;
    private JLabel playerCountLabel;
    private int playerNumber;
    private static ImageIcon mineIcon;
    private static ImageIcon flagIcon;

    /**
     * Constructor for GameWindow.
     *
     * @param client The game client associated with this window.
     */
    public GameWindow(GameClient client) {
        this.gameClient = client;
        loadIcons();
        cellButtons = new CellButton[HEIGHT][WIDTH];
        initializeUI();
    }

    /**
     * Loads the icons used in the game.
     */
    private void loadIcons() {
        URL flagIconUrl = getClass().getResource("/flagicon.png");
        URL mineIconUrl = getClass().getResource("/mineicon.png");
        if (flagIconUrl != null && mineIconUrl != null) {
            flagIcon = new ImageIcon(flagIconUrl);
            mineIcon = new ImageIcon(mineIconUrl);
        } else {
            System.err.println("Icon files not found");
            // Handle error or set default icons
        }
    }

    /**
     * Initializes the user interface of the game window.
     */
    private void initializeUI() {
        setTitle("Multiplayer Minesweeper by Joel Santos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel gamePanel = new JPanel(new GridLayout(HEIGHT, WIDTH));
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                CellButton button = new CellButton(x, y, this.gameClient);
                button.setBorder(new LineBorder(Color.BLACK));
                button.addActionListener(new CellActionListener(x, y));
                cellButtons[y][x] = button;
                gamePanel.add(button);
            }
        }
        add(gamePanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        playerCountLabel = new JLabel("Players Connected: 0");
        controlPanel.add(playerCountLabel);

        readyButton = new JButton("Ready");
        readyButton.addActionListener(e -> {
            gameClient.sendReady();
            readyButton.setEnabled(false);
        });
        controlPanel.add(readyButton);

        add(controlPanel, BorderLayout.SOUTH);
        setSize(800, 800);
        setVisible(true);
    }

    /**
     * Requests the cell state from the server based on player action.
     *
     * @param x The x-coordinate of the cell.
     * @param y The y-coordinate of the cell.
     */
    private void updateCellStateBasedOnPlayerAction(int x, int y) {
        gameClient.requestCellState(x, y);
    }

    /**
     * Updates the player count displayed in the window.
     *
     * @param count The number of connected players.
     */
    public void updatePlayerCount(int count) {
        playerCountLabel.setText("Players Connected: " + count);
    }

    /**
     * Handles a click on a cell in the game.
     *
     * @param x The x-coordinate of the cell.
     * @param y The y-coordinate of the cell.
     */
    private void handleCellClick(int x, int y) {
        // Check if the game has started
        if (!gameClient.isGameStarted()) {
            JOptionPane.showMessageDialog(this, "The game hasn't started yet!", "Game Info", JOptionPane.WARNING_MESSAGE);
            return;
        }
    
        System.out.println("Current player number: " + gameClient.getCurrentPlayerNumber()); // Debug log
        System.out.println("This player's number: " + playerNumber); // Debug log
    
        if (gameClient.getCurrentPlayerNumber() != playerNumber) {
            // Notify the user that it's not their turn
            JOptionPane.showMessageDialog(this, "It's not your turn!", "Turn Info", JOptionPane.WARNING_MESSAGE);
            return;
        }
    
        // Proceed with making a move if it is the player's turn
        gameClient.sendPlayerMove(x, y);
        updateCellStateBasedOnPlayerAction(x, y);
    }

    /**
     * Enables or disables the 'Ready' button.
     *
     * @param enabled true to enable the button, false to disable it.
     */
    public void enableReadyButton(boolean enabled) {
        readyButton.setEnabled(enabled);
    }

    /**
     * Updates the display of the game board based on the current game state.
     *
     * @param mines            Array indicating mine locations.
     * @param revealed         Array indicating which cells have been revealed.
     * @param neighboringMines Array indicating the number of neighboring mines.
     */
    public void updateBoardDisplay(boolean[][] mines, boolean[][] revealed, int[][] neighboringMines) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                CellButton button = cellButtons[y][x];
                if (revealed[y][x]) {
                    button.revealCell(mines[y][x], neighboringMines[y][x]);
                }
            }
        }
    }

    /**
     * Displays a game over message and exits the application.
     */
    public void displayGameOver() {
        JOptionPane.showMessageDialog(this, "Game Over, 5 mines were exploded!", "Game Over",
                JOptionPane.INFORMATION_MESSAGE);
        System.exit(0); // Terminate the application
    }

    /**
     * Updates the game state and UI based on the current state of the game.
     *
     * @param state The current state of the game.
     */
    public void updateGameState(String state) {
        switch (state) {
            case "STARTED":
                JOptionPane.showMessageDialog(this, "Game has started!", "Game Start", JOptionPane.INFORMATION_MESSAGE);
                enableCellButtons(true);
                break;
            case "STOPPED":
            case "OVER":
                enableCellButtons(false);
                if (state.equals("OVER")) {
                    JOptionPane.showMessageDialog(this, "Game Over!", "Game Status", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            default:
                System.out.println("Unknown game state: " + state);
                break;
        }
    }

    /**
     * Updates the player number in the UI.
     *
     * @param playerNumber The number of the player.
     */
    public void updatePlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
        playerCountLabel.setText("You are Player " + playerNumber);
    }

    /**
     * Updates the state of a specific cell in the UI.
     *
     * @param x          The x-coordinate of the cell.
     * @param y          The y-coordinate of the cell.
     * @param cellState  The new state of the cell.
     * @param minesCount The number of mines around the cell, only relevant if no
     *                   mine at this cell.
     */
    public void updateCell(int x, int y, int cellState, int minesCount) {
        CellButton button = cellButtons[y][x];
        switch (cellState) {
            case 1: // Revealed cell with no mine
                button.setBackground(Color.WHITE); // Set background to white
                button.setEnabled(false); // Disable the button as it's revealed
                if (minesCount > 0) {
                    button.setIcon(CellButton.numberIcons[minesCount - 1]); // Set number icon
                } else {
                    button.setIcon(null); // No mines around, clear any icon
                }
                break;
            case 2: // Revealed cell with a mine
                button.setIcon(mineIcon); // Set mine icon
                button.setBackground(Color.RED); // Set background to red
                button.setEnabled(false); // Disable the button as it's revealed
                break;
            case 3: // Marked as a potential mine (flagged)
                button.setIcon(flagIcon); // Set flag icon
                button.setBackground(Color.YELLOW); // Set background to yellow to indicate flagging
                break;
            default: // Default state (hidden)
                button.setIcon(null); // Clear any icon
                button.setText(""); // Clear any text
                button.setBackground(Color.LIGHT_GRAY); // Set background to light gray
                button.setEnabled(true); // Enable the button
                break;
        }
    }

    /**
     * Displays a message when a player quits the game.
     *
     * @param playerName The name of the player who quit.
     */
    public void handlePlayerQuit(String playerName) {
        JOptionPane.showMessageDialog(this, playerName + " has quit the game", "Player Quit",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Handles the change of turn in the game.
     *
     * @param currentPlayerNumber The number of the player whose turn it is now.
     */
    public void handleTurnChange(int currentPlayerNumber) {
        playerCountLabel.setText("It's Player " + currentPlayerNumber + "'s turn");
    }

    /**
     * Enables or disables cell buttons on the game board.
     *
     * @param enabled true to enable the buttons, false to disable them.
     */
    private void enableCellButtons(boolean enabled) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                cellButtons[y][x].setEnabled(enabled);
            }
        }
    }

    public void displayPlayerQuit(int playerNumber) {
        JOptionPane.showMessageDialog(null, "Player " + playerNumber + " has quit the game.", "Notification",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Inner class for handling actions on cell buttons.
     */
    private class CellActionListener implements ActionListener {
        private final int x;
        private final int y;
    
        public CellActionListener(int x, int y) {
            this.x = x;
            this.y = y;
        }
    
        @Override
        public void actionPerformed(ActionEvent e) {
            handleCellClick(x, y);
        }
    }
}