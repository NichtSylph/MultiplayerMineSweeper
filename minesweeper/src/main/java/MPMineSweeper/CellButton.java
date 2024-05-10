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

    /**
     * Constructor for creating a cell button.
     *
     * @param x          X-coordinate of the cell.
     * @param y          Y-coordinate of the cell.
     * @param gameClient Reference to the game client handling the game logic.
     */
    public CellButton(int x, int y, GameClient gameClient) {
        this.x = x;
        this.y = y;
        this.gameClient = gameClient;
        this.isMarked = false;
        setPreferredSize(new Dimension(32, 32)); // Set the preferred size of the button
        setBackground(Color.LIGHT_GRAY);
        setOpaque(true);
        setBorderPainted(true);
        setBorder(new LineBorder(Color.BLACK));

        // Add mouse listener for right-click to toggle flag
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { // Right-click
                    toggleFlag();
                }
            }
        });
    }

    /**
     * Toggles the flag on the cell when right-clicked.
     */
    private void toggleFlag() {
        if (!isEnabled())
            return; // Can't mark/unmark revealed cells

        isMarked = !isMarked;
        setIcon(isMarked ? flagIcon : null);
        setBackground(isMarked ? Color.YELLOW : Color.LIGHT_GRAY);
        gameClient.sendFlagChange(getXCoordinate(), getYCoordinate(), isMarked);
    }

    /**
     * Gets the X-coordinate of this cell.
     *
     * @return X-coordinate of the cell.
     */
    public int getXCoordinate() {
        return x;
    }

    /**
     * Gets the Y-coordinate of this cell.
     *
     * @return Y-coordinate of the cell.
     */
    public int getYCoordinate() {
        return y;
    }

    /**
     * Reveals the cell and updates its appearance based on whether it is a mine and
     * the number of neighboring mines.
     *
     * @param isMine           Indicates whether this cell is a mine.
     * @param neighboringMines The number of neighboring mines.
     */
    public void revealCell(boolean isMine, int neighboringMines) {
        setEnabled(false);
        if (isMine) {
            setBackground(Color.RED);
            setIcon(bombIcon); // Set bomb icon
            System.out.println("Mine revealed");
        } else {
            setBackground(Color.WHITE);
            if (neighboringMines > 0) {
                setIcon(numberIcons[neighboringMines - 1]); // Set number icon
                System.out.println("Setting number icon for mines: " + neighboringMines);
            } else {
                setIcon(null); // Clear icon if no neighboring mines
                System.out.println("No neighboring mines");
            }
        }
    }

}
