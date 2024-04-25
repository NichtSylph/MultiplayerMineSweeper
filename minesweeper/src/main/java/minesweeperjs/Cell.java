package minesweeperjs;

public class Cell {
    private boolean isMine; // Indicates if the cell is a mine
    private boolean flagged; // Indicates if the cell is flagged
    private int neighboringMines; // Number of neighboring mines
    private boolean isRevealed; // Indicates if the cell is revealed
    private Player revealedBy; // Player who revealed this cell

    public Cell() {
        this.isMine = false;
        this.flagged = false;
        this.neighboringMines = 0;
        this.isRevealed = false;
        this.revealedBy = null;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setMine(boolean mine) {
        isMine = mine;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public int getNeighboringMines() {
        return neighboringMines;
    }

    public void setNeighboringMines(int neighboringMines) {
        this.neighboringMines = neighboringMines;
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    public void setRevealed(boolean revealed) {
        isRevealed = revealed;
    }

    public Player getRevealedBy() {
        return revealedBy;
    }

    public void setRevealedBy(Player player) {
        this.revealedBy = player;
    }
}
