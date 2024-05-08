package MPMineSweeper;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CellButton extends JButton {
    private final int x; // X-coordinate of the cell
    private final int y; // Y-coordinate of the cell
    private GameClient gameClient; // Reference to the game client
    private Cell cell; // The cell this button represents
    private static final ImageIcon mineIcon = new ImageIcon(CellButton.class.getResource("/mineicon.png"));
    private static final ImageIcon flagIcon = new ImageIcon(CellButton.class.getResource("/flagicon.png"));
    private static final ImageIcon[] numberIcons = {
            new ImageIcon(CellButton.class.getResource("/iconNumber1.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber2.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber3.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber4.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber5.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber6.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber7.png")),
            new ImageIcon(CellButton.class.getResource("/iconNumber8.png"))
    };

    public CellButton(int x, int y, GameClient gameClient, Cell cell) {
        if (gameClient == null) {
            throw new IllegalArgumentException("gameClient cannot be null");
        }
        this.x = x;
        this.y = y;
        this.gameClient = gameClient;
        this.cell = cell;
        setPreferredSize(new Dimension(32, 32));
        setBackground(Color.LIGHT_GRAY);
        setOpaque(true);
        setBorderPainted(true);
        setBorder(new LineBorder(Color.BLACK));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (cell.isRevealed())
                    return; // Ignore clicks if the cell is revealed

                if (e.getButton() == MouseEvent.BUTTON3) { // Right-click
                    toggleFlag();
                } else if (e.getButton() == MouseEvent.BUTTON1) { // Left-click
                    revealCellAction();
                }
            }
        });
    }

    public void revealCellAction() {
        if (!gameClient.isGameStarted()) {
            JOptionPane.showMessageDialog(this, "The game has not started yet. Please press the Ready button.", "IDLE", JOptionPane.WARNING_MESSAGE);
            return;
        }
    
        Player currentPlayer = gameClient.getCurrentPlayer();
        System.out.println("Attempting to reveal cell: Player " + currentPlayer.getPlayerNumber() +
                ", Current player: " + gameClient.getCurrentPlayer().getPlayerNumber());
        if (currentPlayer.isCurrentTurn() && !cell.isRevealed() && !cell.isFlagged()) {
            gameClient.sendPlayerMove(x, y);
            currentPlayer.incrementScore(10);
            // Update the cell's state
            cell.setRevealed(true);
            // Reveal the cell in the UI
            revealCell(cell.isMine(), cell.getNeighboringMines());
        } else if (!currentPlayer.isCurrentTurn()) {
            JOptionPane.showMessageDialog(this, "Wait for your turn, Player " + currentPlayer.getPlayerNumber() + "!", "Turn Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void toggleFlag() {
        if (!gameClient.isGameStarted()) {
            JOptionPane.showMessageDialog(this, "The game has not started yet. Please press the Ready button.", "IDLE", JOptionPane.WARNING_MESSAGE);
            return;
        }
    
        if (cell.isRevealed())
            return; // Ignore flagging if the cell is revealed
    
        cell.setFlagged(!cell.isFlagged());
        setIcon(cell.isFlagged() ? flagIcon : null);
        setBackground(cell.isFlagged() ? Color.YELLOW : Color.LIGHT_GRAY);
        gameClient.sendFlagChange(x, y, cell.isFlagged());
        if (cell.isFlagged()) {
            gameClient.getCurrentPlayer().incrementScore(2); // Increment score when a flag is placed
        } else {
            gameClient.getCurrentPlayer().incrementScore(-2); // Decrement score when a flag is removed
        }
    }

    public void revealCell(boolean isMine, int neighboringMines) {
        cell.setRevealed(true);
        setEnabled(false); // Disable the button
        setBackground(Color.WHITE); // Set the background to indicate reveal
    
        if (isMine) {
            setIcon(mineIcon);
        } else {
            updateWithMinesCount(neighboringMines);
        }
    }

    public void updateWithMinesCount(int count) {
        if (!cell.isRevealed()) {
            if (count > 0 && count <= numberIcons.length) {
                setIcon(numberIcons[count - 1]); // Arrays are 0-indexed, so subtract 1
            } else {
                setIcon(null);
            }
        }
    }

    public Cell getCell() {
        return this.cell;
    }
}