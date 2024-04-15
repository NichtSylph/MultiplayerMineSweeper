package minesweeperjs;

/**
 * Represents a cell in the Minesweeper game.
 * Each cell can be a mine or safe, can be flagged or unflagged,
 * and can keep track of the number of neighboring mines.
 * 
 * @author Joel Santos
 * @version 1.0
 * @since 12/01/2023
 */
public class Cell {
    private boolean isMine; // Indicates if the cell is a mine
    private boolean flagged; // Indicates if the cell is flagged
    private int neighboringMines; // Number of neighboring mines
    private boolean isRevealed; // Indicates if the cell is revealed
    private Player revealedBy; // Player who revealed this cell

    /**
     * Constructor for Cell. Initializes the cell as not a mine, not flagged,
     * with zero neighboring mines, not revealed, and with no player who revealed
     * it.
     */
    public Cell() {
        this.isMine = false;
        this.flagged = false;
        this.neighboringMines = 0;
        this.isRevealed = false;
        this.revealedBy = null;
    }

    /**
     * Copy constructor for Cell. Creates a new Cell instance by copying the state
     * of another Cell.
     * 
     * @param otherCell The Cell to copy.
     */
    public Cell(Cell otherCell) {
        this.isMine = otherCell.isMine;
        this.flagged = otherCell.flagged;
        this.neighboringMines = otherCell.neighboringMines;
        this.isRevealed = otherCell.isRevealed;
        this.revealedBy = otherCell.revealedBy;
    }

    /**
     * Checks if the cell is a mine.
     * 
     * @return true if the cell is a mine, false otherwise.
     */
    public boolean isMine() {
        return isMine;
    }

    /**
     * Sets the cell as a mine or not.
     * 
     * @param mine true to set the cell as a mine, false otherwise.
     */
    public void setMine(boolean mine) {
        isMine = mine;
    }

    /**
     * Checks if the cell is flagged.
     * 
     * @return true if the cell is flagged, false otherwise.
     */
    public boolean isFlagged() {
        return flagged;
    }

    /**
     * Sets the cell as flagged or unflagged.
     * 
     * @param flagged true to flag the cell, false to unflag it.
     */
    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    /**
     * Gets the number of neighboring mines.
     * 
     * @return the number of neighboring mines.
     */
    public int getNeighboringMines() {
        return neighboringMines;
    }

    /**
     * Sets the number of neighboring mines for this cell.
     * 
     * @param neighboringMines the number of neighboring mines.
     */
    public void setNeighboringMines(int neighboringMines) {
        this.neighboringMines = neighboringMines;
    }

    /**
     * Checks if the cell is revealed.
     * 
     * @return true if the cell is revealed, false otherwise.
     */
    public boolean isRevealed() {
        return isRevealed;
    }

    /**
     * Sets the cell as revealed or unrevealed.
     * 
     * @param revealed true to reveal the cell, false to hide it.
     */
    public void setRevealed(boolean revealed) {
        isRevealed = revealed;
    }

    /**
     * Gets the player who revealed the cell.
     * 
     * @return the player who revealed the cell, or null if the cell hasn't been
     *         revealed yet.
     */
    public Player getRevealedBy() {
        return revealedBy;
    }

    /**
     * Sets the player who revealed the cell.
     * 
     * @param player the player who revealed the cell.
     */
    public void setRevealedBy(Player player) {
        this.revealedBy = player;
    }
}
