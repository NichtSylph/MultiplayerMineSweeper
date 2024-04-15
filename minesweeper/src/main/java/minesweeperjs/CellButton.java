package minesweeperjs;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CellButton extends JButton {
    private final int x; // X-coordinate of the cell
    private final int y; // Y-coordinate of the cell
    private GameClient gameClient; // Reference to the game client
    private static final ImageIcon mineIcon = new ImageIcon(CellButton.class.getResource("/mineicon.png"));
    private static final ImageIcon flagIcon = new ImageIcon(CellButton.class.getResource("/flagicon.png"));
    private static final ImageIcon[] numberIcons = {
            new ImageIcon(CellButton.class.getResource("/iconNumber1.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber2.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber3.png")),
    };
    private boolean isMarked; // Flag for marking the cell
    private boolean isRevealed; // Flag for revealing the cell

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

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isRevealed)
                    return; // Ignore clicks if the cell is revealed

                if (e.getButton() == MouseEvent.BUTTON3) { // Right-click
                    toggleFlag();
                } else if (e.getButton() == MouseEvent.BUTTON1) { // Left-click
                    revealCellAction();
                }
            }
        });
    }

    private void revealCellAction() {
        if (gameClient.getCurrentPlayerNumber() == gameClient.getPlayerNumber() && !isRevealed) {
            gameClient.sendPlayerMove(x, y);
        }
    }

    private void toggleFlag() {
        if (isRevealed)
            return; // Ignore flagging if the cell is revealed

        isMarked = !isMarked;
        setIcon(isMarked ? flagIcon : null);
        setBackground(isMarked ? Color.YELLOW : Color.LIGHT_GRAY);
        gameClient.sendFlagChange(x, y, isMarked);
    }

    public void revealCell(boolean isMine, int neighboringMines) {
        setEnabled(false); // Disable the button
        setBackground(Color.WHITE); // Set the background to indicate reveal
        this.isRevealed = true; // Set the revealed flag

        if (isMine) {
            setIcon(mineIcon);
        } else if (neighboringMines > 0) {
            setIcon(getNumberIcon(neighboringMines));
        }
    }

    public void setMarked(boolean isMarked) {
        this.isMarked = isMarked;
        // Update the icon and background based on the new flag status
        setIcon(isMarked ? flagIcon : null);
        setBackground(isMarked ? Color.YELLOW : Color.LIGHT_GRAY);
    }

    public boolean isMarked() {
        return isMarked;
    }

    public void setCellRevealed(boolean isRevealed) {
        this.isRevealed = isRevealed;
        setEnabled(!isRevealed);
    }

    public boolean isCellRevealed() {
        return isRevealed;
    }

    public void resetState() {
        setIcon(null);
        setBackground(Color.LIGHT_GRAY);
        setEnabled(true);
        isRevealed = false;
    }

    public void revealWithoutMine() {
        setIcon(null);
        setBackground(Color.WHITE);
        setEnabled(false);
        isRevealed = true;
    }

    public void revealWithMine() {
        setIcon(mineIcon);
        setBackground(Color.RED);
        setEnabled(false);
        isRevealed = true;
    }

    public void updateWithMinesCount(int count) {
        // Check if the cell is already revealed
        if (!isRevealed) {
            if (count > 0 && count <= numberIcons.length) {
                setIcon(numberIcons[count - 1]); // Arrays are 0-indexed, so subtract 1
            } else {
                // If count is 0 or beyond range, clear the icon
                setIcon(null);
            }
        }
    }

    private ImageIcon getNumberIcon(int number) {
        if (number > 0 && number <= numberIcons.length) {
            return numberIcons[number - 1]; // Arrays are 0-indexed, so subtract 1
        }
        return null;
    }
}
