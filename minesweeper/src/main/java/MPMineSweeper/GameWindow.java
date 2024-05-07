package main.java.MPMineSweeper;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

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
                Cell cell = new Cell(); // Create a new Cell object
                CellButton button = new CellButton(x, y, gameClient, cell); // Pass the Cell object to the CellButton
                button.setBorder(new LineBorder(Color.BLACK));
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

    public void updateCellState(int x, int y, boolean isRevealed) {
        CellButton cellButton = cellButtons[x][y];
        cellButton.getCell().setRevealed(isRevealed);
        cellButton.revealCell(cellButton.getCell().isMine(), cellButton.getCell().getNeighboringMines());
    }

    public void updateCellWithMinesCount(int x, int y, int count) {
        CellButton cellButton = cellButtons[y][x];
        if (cellButton != null) {
            cellButton.updateWithMinesCount(count);
        }
    }

    public void updatePlayerCount(int count) {
        playerCountLabel.setText("Players Connected: " + count);
    }

    public void updateScore(int newScore) {
        score = newScore;
        scoreLabel.setText("Your Score: " + score);
    }

    public void handleTurnChange(int currentPlayer) {
        this.currentPlayerNumber = currentPlayer;
        updateTurnStatus(this.playerNumber == this.currentPlayerNumber);
    }

    public void updateTurnStatus(boolean isMyTurn) {
        enableCellButtons(isMyTurn);
        if (!isMyTurn) {
            JOptionPane.showMessageDialog(this, "Wait for your turn.", "Turn Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void displayGameOver() {
        JOptionPane.showMessageDialog(this, "Game Over, the game has ended!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    public void notifyGameStarted() {
        JOptionPane.showMessageDialog(this, "The game has started!", "Game Start", JOptionPane.INFORMATION_MESSAGE);
        readyButton.setEnabled(false); // Disable the Ready button when the game starts
    }

    public void notifyServerClosing() {
        JOptionPane.showMessageDialog(this, "Server is closing, the game will end now.", "Server Closing", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public void enableCellButtons(boolean enabled) {
        for (CellButton[] row : cellButtons) {
            for (CellButton button : row) {
                button.setEnabled(enabled);
            }
        }
    }

    public JButton getReadyButton() {
        return readyButton;
    }
}