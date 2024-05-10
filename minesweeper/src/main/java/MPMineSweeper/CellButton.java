package MPMineSweeper;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CellButton extends JButton {
    private final int x; // X-coordinate of the cell on the game board
    private final int y; // Y-coordinate of the cell on the game board
    private GameClient gameClient; // Reference to the game client
    private static final ImageIcon bombIcon = new ImageIcon(CellButton.class.getResource("/mineicon.png"));
    private static final ImageIcon flagIcon = new ImageIcon(CellButton.class.getResource("/flagicon.png"));
    static final ImageIcon[] numberIcons = new ImageIcon[8];
    static {
        for (int i = 0; i < 8; i++) {
            numberIcons[i] = new ImageIcon(CellButton.class.getResource("/iconNumber" + (i + 1) + ".png"));
        }
    }
    private boolean isMarked; // Flag to check if the cell is marked with a flag
    private boolean isRevealed; // Tracks whether the cell has been revealed

    public CellButton(int x, int y, GameClient gameClient) {
        this.x = x;
        this.y = y;
        this.gameClient = gameClient;
        this.isMarked = false;
        this.isRevealed = false;
        setPreferredSize(new Dimension(32, 32));
        setBackground(Color.LIGHT_GRAY);
        setOpaque(true);
        setBorderPainted(true);
        setBorder(new LineBorder(Color.BLACK));

        // Add mouse listener for right-click to toggle flag and left-click to reveal
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { // Right-click
                    toggleFlag();
                } else if (e.getButton() == MouseEvent.BUTTON1 && !isRevealed && !isMarked) { // Left-click
                    gameClient.sendPlayerMove(CellButton.this.x, CellButton.this.y);
                }
            }
        });
    }

    private void toggleFlag() {
        if (!isEnabled() || isRevealed) // Check if the cell is revealed
            return;

        isMarked = !isMarked;
        setIcon(isMarked ? flagIcon : null);
        setBackground(isMarked ? Color.YELLOW : Color.LIGHT_GRAY);
        gameClient.sendFlagChange(x, y, isMarked);
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    public void revealCell(boolean isMine, int neighboringMines) {
        SwingUtilities.invokeLater(() -> {
            isRevealed = true; // Mark the cell as revealed
            setEnabled(false);
            if (isMine) {
                setBackground(Color.RED);
                setIcon(bombIcon);
            } else {
                setBackground(Color.WHITE);
                if (neighboringMines > 0) {
                    setIcon(numberIcons[neighboringMines - 1]);
                } else {
                    setIcon(null);
                }
            }
            repaint();
        });
    }
}
