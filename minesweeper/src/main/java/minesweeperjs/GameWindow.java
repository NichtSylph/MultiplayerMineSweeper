package minesweeperjs;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameWindow extends JFrame {
    private CellButton[][] cellButtons;
    private final int WIDTH = 16;
    private final int HEIGHT = 16;
    private GameClient gameClient;
    private JButton readyButton;
    private JLabel playerCountLabel;
    private JLabel scoreLabel;
    private int playerNumber;
    private int score;
    private int currentPlayerNumber;

    public GameWindow(GameClient client) {
        this.gameClient = client;
        this.score = 0;
        cellButtons = new CellButton[HEIGHT][WIDTH];
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Multiplayer Minesweeper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel gamePanel = new JPanel(new GridLayout(HEIGHT, WIDTH));
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                CellButton button = new CellButton(x, y, gameClient);
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

        scoreLabel = new JLabel("Your Score: " + score);
        controlPanel.add(scoreLabel);

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

    public CellButton getCellButton(int x, int y) {
        return cellButtons[y][x];
    }

    private void updateCellStateBasedOnPlayerAction(int x, int y) {
        gameClient.requestCellState(x, y);
    }

    public void updateCellWithMinesCount(int x, int y, int count) {
        CellButton cellButton = cellButtons[y][x];
        // Update cellButton based on the count
        if (cellButton != null) {
            cellButton.updateWithMinesCount(count);
        }
    }

    public void updatePlayerCount(int count) {
        playerCountLabel.setText("Players Connected: " + count);
    }

    public void updatePlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
        playerCountLabel.setText("You are Player " + playerNumber);
    }

    public void updateScore(int newScore) {
        score = newScore;
        scoreLabel.setText("Your Score: " + score);
    }

    private void handleCellClick(int x, int y) {
        if (this.currentPlayerNumber == this.playerNumber) {
            gameClient.sendPlayerMove(x, y);
            updateCellStateBasedOnPlayerAction(x, y);
        } else {
            JOptionPane.showMessageDialog(this, "It's not your turn!", "Turn Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void handlePlayerQuit(String playerName) {
        JOptionPane.showMessageDialog(this, playerName + " has quit the game", "Player Quit",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void handleTurnChange(String currentPlayerName) {
        this.currentPlayerNumber = extractPlayerNumber(currentPlayerName);
        boolean isMyTurn = this.playerNumber == this.currentPlayerNumber;
        enableCellButtons(isMyTurn); // Make sure this call is correct
    }

    private int extractPlayerNumber(String playerName) {
        try {
            String[] parts = playerName.split(" ");
            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            System.err.println("Error extracting player number: " + e.getMessage());
        }
        return -1;
    }

    public void enableReadyButton(boolean enabled) {
        readyButton.setEnabled(enabled);
    }

    public void showNotYourTurnMessage() {
        JOptionPane.showMessageDialog(this, "It's not your turn!", "Turn Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void displayGameOver() {
        displayBestMoves(); // Call this when the game is over, especially after a loss
        JOptionPane.showMessageDialog(this, "Game Over, 5 mines were exploded!", "Game Over",
                JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    public void notifyGameStarted() {
        JOptionPane.showMessageDialog(this, "The game has started!", "Game Start", JOptionPane.INFORMATION_MESSAGE);
    }

    public void notifyServerClosing() {
        JOptionPane.showMessageDialog(this, "Server is closing, the game will end now.", "Server Closing",
                JOptionPane.INFORMATION_MESSAGE);
        // Close the game window and clean up resources
        dispose();
    }

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

    public void updateCell(int x, int y, int state) {
        CellButton button = cellButtons[y][x];
        switch (state) {
            case 0: // Not revealed
                button.resetState();
                break;
            case 1: // Revealed without a mine
                button.revealWithoutMine();
                break;
            case 2: // Revealed with a mine
                button.revealWithMine();
                break;
            case 3: // Flagged
                button.setMarked(true);
                break;
            default:
                System.out.println("Unknown cell state: " + state);
                break;
        }
    }

    public void enableCellButtons(boolean enabled) {
        for (CellButton[] cellRow : cellButtons) {
            for (CellButton cell : cellRow) {
                cell.setEnabled(enabled);
            }
        }
    }

    public void displayBestMoves() {
        // Begin analysis of each cell on the board
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                CellButton button = cellButtons[y][x];
                if (!button.isCellRevealed() && !button.isMarked()) {
                    // Request the neighboring mines count from the GameClient
                    gameClient.getNeighboringMinesCount(x, y, (responseX, responseY, count) -> {
                        // This is a callback that will be called when the count is available
                        if (count == 0) {
                            // If there are no neighboring mines, it's generally safer to reveal
                            SwingUtilities.invokeLater(() -> {
                                displayBestMovesDialog("Reveal Cell at (" + responseX + ", " + responseY + ")");
                            });
                        }
                    });
                }
            }
        }
        displayBestMovesDialog("Best moves calculated. Displaying...");
    }

    private void displayBestMovesDialog(String bestMove) {
        JOptionPane.showMessageDialog(this, bestMove, "Best Moves", JOptionPane.INFORMATION_MESSAGE);
    }

    public void handleNeighboringMinesCountResponse(int x, int y, int count) {
        // Update the cell with the count of neighboring mines
        CellButton cellButton = cellButtons[y][x];
        if (cellButton != null && !cellButton.isCellRevealed()) {
            cellButton.updateWithMinesCount(count);
        }

        // Logic to determine and display the best moves
        if (count == 0) {
            displayBestMovesDialog("Reveal Cell at (" + x + ", " + y + ")");
        }
    }

    private class CellActionListener implements ActionListener {
        private final int x;
        private final int y;

        public CellActionListener(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Check if the button has already been revealed or if it's not the player's
            // turn
            if (!cellButtons[y][x].isCellRevealed() && gameClient.getCurrentPlayerNumber() == playerNumber) {
                // Handle the cell click
                handleCellClick(x, y);
            } else {
                // Show a dialog if it's not the player's turn or the cell is already revealed
                if (gameClient.getCurrentPlayerNumber() != playerNumber) {
                    JOptionPane.showMessageDialog(
                            GameWindow.this,
                            "It's not your turn!",
                            "Wait",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }
}