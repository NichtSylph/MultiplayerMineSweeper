package MPMineSweeper;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class GameWindow extends JFrame {
    private CellButton[][] cellButtons;
    private final int WIDTH = 16;
    private final int HEIGHT = 16;
    private GameClient gameClient;
    private JPanel controlPanel;
    private JButton readyButton;
    private JLabel playerCountLabel;
    private JLabel scoreLabel;
    private Integer playerNumber;

    public GameWindow(GameClient client) {
        this.gameClient = client;
        this.playerNumber = client.getPlayerNumber();
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

        this.controlPanel = new JPanel();
        this.playerCountLabel = new JLabel("Players Connected: 0");
        this.controlPanel.add(this.playerCountLabel);

        this.scoreLabel = new JLabel("Score: " + gameClient.getScore()); // Dynamically update the score
        this.controlPanel.add(this.scoreLabel);

        this.readyButton = new JButton("Ready");
        this.readyButton.addActionListener(e -> {
            this.gameClient.sendReady();
            this.readyButton.setEnabled(false);
        });
        this.controlPanel.add(this.readyButton);

        add(this.controlPanel, BorderLayout.SOUTH);
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

    public void updatePlayerCount(Integer count) {
        System.out.println("KKM in panel update: " + count);
        this.playerCountLabel = new JLabel("Players Connected: " + count);
        System.out.println("KKM in panel this.playerCountLabel: " + this.playerCountLabel);
        this.controlPanel.remove(0);
        this.controlPanel.add(this.playerCountLabel, 0);

        SwingUtilities.invokeLater(() -> {
            this.controlPanel.validate();
            this.controlPanel.repaint();
        });

        // Boolean existingPanel = false;
        // if (this.controlPanel != null) {
        //     System.out.println("KKM remove panel: " + count);
        //     this.controlPanel.removeAll();
        //     existingPanel = true;
        // }
        // this.controlPanel = (this.controlPanel == null) ? new JPanel() : this.controlPanel;
        // this.playerCountLabel = new JLabel("Players Connected: " + count);
        // this.controlPanel.add(playerCountLabel);

        // this.scoreLabel = (this.scoreLabel == null) ? new JLabel("Score: " + gameClient.getScore()) : this.scoreLabel;
        // this.controlPanel.add(this.scoreLabel);
        // this.readyButton = (this.readyButton == null) ? new JButton("Ready") : this.readyButton;

        // if (this.gameClient.isPlayerReady()) {
        //     this.readyButton.setEnabled(false);
        //     this.controlPanel.add(this.readyButton);
        // } else {
        //     this.readyButton.addActionListener(e -> {
        //         this.gameClient.sendReady();
        //         this.readyButton.setEnabled(false);
        //     });
        //     this.controlPanel.add(this.readyButton);
        // }

        // System.out.println("KKM in panel update");

        // if (!existingPanel) {
        //     add(this.controlPanel, BorderLayout.SOUTH);
        // } else {
        //     this.controlPanel.repaint();
        // }
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
        this.enableCellButtons(isMyTurn);
        if (!isMyTurn) {
            JOptionPane.showMessageDialog(this, "Wait for your turn.", "Turn Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            this.showMyTurnMessage();
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

    public void showMyTurnMessage() {
        JOptionPane.showMessageDialog(this, "Its your turn...", "Turn Active", JOptionPane.INFORMATION_MESSAGE);
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
        return this.readyButton;
    }
}
