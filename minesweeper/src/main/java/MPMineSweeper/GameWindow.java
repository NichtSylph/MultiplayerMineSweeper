package MPMineSweeper;

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
    private Integer playerNumber;

    public GameWindow(GameClient client) {
        this.gameClient = client;
        this.playerNumber = client.getPlayerNumber(); // Assume the player is initialized in the client now
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

        scoreLabel = new JLabel("Score: " + gameClient.getScore()); // Dynamically update the score
        controlPanel.add(scoreLabel);

        readyButton = new JButton("Ready");
        readyButton.addActionListener(e -> {
            gameClient.sendReady();
            readyButton.setEnabled(false);
        });
        controlPanel.add(readyButton);

        add(controlPanel, BorderLayout.SOUTH);
        setSize(800, 800);
        setLocationRelativeTo(null); // Center the window
        setVisible(true);
    }

    public void updateCellState(int x, int y, boolean isRevealed) {
        SwingUtilities.invokeLater(() -> {
            CellButton cellButton = cellButtons[y][x];
            cellButton.getCell().setRevealed(isRevealed);
            cellButton.revealCell(cellButton.getCell().isMine(), cellButton.getCell().getNeighboringMines());
            if (isRevealed) {
                gameClient.incrementScore(10);
                updateScore(gameClient.getScore());
            }
        });
    }

    public void updateFlagState(int x, int y, boolean isFlagged) {
        SwingUtilities.invokeLater(() -> {
            CellButton cellButton = cellButtons[y][x];
            cellButton.getCell().setFlagged(isFlagged);
            if (isFlagged) {
                gameClient.incrementScore(2);
            } else {
                gameClient.decrementScore(2);
            }
            updateScore(gameClient.getScore());
        });
    }

    public void updatePlayerCount(int count) {
        SwingUtilities.invokeLater(() -> playerCountLabel.setText("Players Connected: " + count));
    }

    public void updateScore(int newScore) {
        SwingUtilities.invokeLater(() -> scoreLabel.setText("Your Score: " + newScore));
    }

    public void handleTurnChange(int currentPlayerNumber) {
        SwingUtilities.invokeLater(() -> {
            boolean isCurrentTurn = playerNumber == currentPlayerNumber;
            updateTurnStatus(isCurrentTurn);
        });
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
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                cellButtons[y][x].setEnabled(enabled);
            }
        }
    }

    public JButton getReadyButton() {
        return readyButton;
    }
}
