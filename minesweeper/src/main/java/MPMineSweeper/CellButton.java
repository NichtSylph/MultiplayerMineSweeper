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
    private static final ImageIcon mineIcon = loadIcon("resources/mineicon.png");
    private static final ImageIcon flagIcon = loadIcon("resources/flagicon.png");
    private static final ImageIcon[] numberIcons = {
        loadIcon("resources/iconNumber1.png"),
        loadIcon("resources/iconNumber2.png"),
        loadIcon("resources/iconNumber3.png"),
        loadIcon("resources/iconNumber4.png"),
        loadIcon("resources/iconNumber5.png"),
        loadIcon("resources/iconNumber6.png"),
        loadIcon("resources/iconNumber7.png"),
        loadIcon("resources/iconNumber8.png")
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

    private static ImageIcon loadIcon(String path) {
        java.net.URL imgURL = CellButton.class.getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path + " -- Full path attempted: " + CellButton.class.getClassLoader().getResource("").getPath());
            return null;
        }
    }

    public void revealCellAction() {
        if (!gameClient.isGameStarted()) {
            JOptionPane.showMessageDialog(this, "The game has not started yet. Please press the Ready button.", "IDLE",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
    
        Player currentPlayer = gameClient.getCurrentPlayer();
        System.out.println("Attempting to reveal cell: Player " + currentPlayer.getPlayerNumber() +
                ", Current player: " + gameClient.getCurrentPlayer().getPlayerNumber());
        if (!cell.isRevealed() && !cell.isFlagged()) {
            gameClient.sendPlayerMove(x, y);
            // Update the cell's state
            cell.setRevealed(true);
            // Reveal the cell in the UI
            revealCell(cell.isMine(), cell.getNeighboringMines());
        } else {
            JOptionPane.showMessageDialog(this, "Wait for your turn, Player " + currentPlayer.getPlayerNumber() + "!",
                    "Turn Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void toggleFlag() {
        if (!gameClient.isGameStarted()) {
            JOptionPane.showMessageDialog(this, "The game has not started yet. Please press the Ready button.", "IDLE",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (cell.isRevealed())
            return; // Ignore flagging if the cell is revealed

        cell.setFlagged(!cell.isFlagged());
        setIcon(cell.isFlagged() ? flagIcon : null);
        setBackground(cell.isFlagged() ? Color.YELLOW : Color.LIGHT_GRAY);
        gameClient.sendFlagChange(x, y, cell.isFlagged());
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