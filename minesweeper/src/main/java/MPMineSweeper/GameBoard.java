package MPMineSweeper;

import java.util.Random;

public class GameBoard {
    private Cell[][] cells;
    private int width;
    private int height;
    private int mineCount;
    private boolean gameStarted;
    private boolean gameOver;
    private int bombRevealedCount;

    /**
     * Constructs a GameBoard with specified dimensions and mine count.
     *
     * @param width     The width of the game board.
     * @param height    The height of the game board.
     * @param mineCount The number of mines on the board.
     */
    public GameBoard(int width, int height, int mineCount) {
        this.width = width;
        this.height = height;
        this.mineCount = mineCount;
        this.gameStarted = false;
        this.gameOver = false;
        this.bombRevealedCount = 0;
        cells = new Cell[height][width];
        initializeCells();
        placeMines();
        calculateNeighboringMines();
    }

    /**
     * Initializes the cells on the board.
     */
    private void initializeCells() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                cells[i][j] = new Cell();
            }
        }
    }

    /**
     * Places mines randomly on the board.
     */
    private void placeMines() {
        Random random = new Random();
        int minesPlaced = 0;
        while (minesPlaced < mineCount) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            if (!cells[y][x].isMine()) {
                cells[y][x].setMine(true);
                minesPlaced++;
            }
        }
    }

    /**
     * Toggles the flag on a specific cell.
     *
     * @param x         X-coordinate of the cell.
     * @param y         Y-coordinate of the cell.
     * @param isFlagged Flag indicating whether the cell is flagged.
     */
    public void toggleFlag(int x, int y, boolean isFlagged) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            Cell cell = cells[y][x];
            cell.setFlagged(isFlagged);
        }
    }

    /**
     * Calculates the number of neighboring mines for each cell.
     */
    private void calculateNeighboringMines() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (!cells[i][j].isMine()) {
                    int mines = countAdjacentMines(j, i);
                    cells[i][j].setNeighboringMines(mines);
                }
            }
        }
    }

    /**
     * Counts the number of mines adjacent to a given cell.
     *
     * @param x X-coordinate of the cell.
     * @param y Y-coordinate of the cell.
     * @return The number of adjacent mines.
     */
    private int countAdjacentMines(int x, int y) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int nx = x + j;
                int ny = y + i;
                if (nx >= 0 && ny >= 0 && nx < width && ny < height) {
                    if (cells[ny][nx].isMine()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        gameStarted = true;
    }

    /**
     * Checks if the game has started.
     *
     * @return true if the game has started, false otherwise.
     */
    public boolean isGameStarted() {
        return gameStarted;
    }

    /**
     * Sets the game over state.
     *
     * @param gameOver true to set the game as over, false otherwise.
     */
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    /**
     * Checks if the game is over.
     *
     * @return true if the game is over, false otherwise.
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Resets the game board to its initial state.
     */
    public void reset() {
        initializeCells();
        placeMines();
        calculateNeighboringMines();
        gameStarted = false;
        gameOver = false;
        bombRevealedCount = 0;
    }

    /**
     * Retrieves a specific cell from the board.
     *
     * @param x X-coordinate of the cell.
     * @param y Y-coordinate of the cell.
     * @return The cell at the specified coordinates.
     */
    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return cells[y][x];
    }

    /**
     * Reveals a specific cell and handles the game logic accordingly.
     *
     * @param x      X-coordinate of the cell.
     * @param y      Y-coordinate of the cell.
     * @param player The player revealing the cell.
     * @return true if a mine is revealed, false otherwise.
     */
    public boolean revealCell(int x, int y, Player player) {
        if (x < 0 || x >= width || y < 0 || y >= height || cells[y][x].isRevealed()) {
            return false;
        }

        cells[y][x].setRevealed(true);
        if (cells[y][x].isMine()) {
            bombRevealedCount++;
            if (bombRevealedCount >= 5) {
                gameOver = true;
            }
            return true;
        }

        if (cells[y][x].getNeighboringMines() == 0) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int nx = x + j;
                    int ny = y + i;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && (i != 0 || j != 0)) {
                        revealCell(nx, ny, player);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Increments the count of revealed bombs.
     *
     * @return The updated count of revealed bombs.
     */
    public int incrementBombCount() {
        bombRevealedCount++;
        return bombRevealedCount;
    }

    /**
     * Gets the count of revealed bombs.
     *
     * @return The count of revealed bombs.
     */
    public int getBombRevealedCount() {
        return bombRevealedCount;
    }

    /**
     * Checks if all non-mine cells have been revealed.
     *
     * @return true if all non-mine cells are revealed, false otherwise.
     */
    public boolean allNonMineCellsRevealed() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Cell cell = cells[i][j];
                if (!cell.isMine() && !cell.isRevealed()) {
                    return false;
                }
            }
        }
        return true;
    }
}
