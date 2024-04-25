package minesweeperjs;

import java.util.Random;

public class GameBoard {
    private Cell[][] cells;
    private int width;
    private int height;
    private int mineCount;
    private boolean gameStarted;
    private boolean gameOver;
    private int bombRevealedCount;
    private MoveEvaluator moveEvaluator;

    public GameBoard(int width, int height, int mineCount, Player[] players) {
        this.width = width;
        this.height = height;
        this.mineCount = mineCount;
        this.gameStarted = false;
        this.gameOver = false;
        this.bombRevealedCount = 0;
        this.cells = new Cell[height][width];
        initializeCells();
        placeMines();
        calculateNeighboringMines();
        this.moveEvaluator = new MoveEvaluator(this, players);
    }

    private void initializeCells() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                cells[i][j] = new Cell();
            }
        }
    }

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

    public void toggleFlag(int x, int y, boolean isFlagged) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            Cell cell = cells[y][x];
            if (!cell.isRevealed()) { // Prevent flagging of already revealed cells
                cell.setFlagged(isFlagged);
            }
        }
    }

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

    public boolean revealCell(int x, int y, Player player) {
        if (x < 0 || x >= width || y < 0 || y >= height || gameOver) {
            return false; // Cell is out of bounds or game is over
        }

        Cell cell = cells[y][x];
        if (cell.isRevealed()) {
            return false; // Cell is already revealed
        }

        // Check if it's the player's turn
        if (player != null && !player.isCurrentTurn()) {
            return false; // It's not the player's turn
        }

        cell.setRevealed(true);
        if (cell.isMine()) {
            bombRevealedCount++;
            if (bombRevealedCount >= mineCount) {
                gameOver = true;
            }
            return true;
        }

        if (cell.getNeighboringMines() == 0) {
            revealSurroundingCells(x, y);
        }

        return false;
    }

    private void revealSurroundingCells(int x, int y) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0)
                    continue; // Skip the current cell
                int nx = x + j;
                int ny = y + i;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    revealCell(nx, ny, null);
                }
            }
        }
    }

    public MoveEvaluator.MoveResult evaluateMove(int x, int y, Player player) {
        return moveEvaluator.evaluateMove(x, y, player); // Direct call without threading
    }

    public void startGame() {
        gameStarted = true;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void reset() {
        initializeCells();
        placeMines();
        calculateNeighboringMines();
        gameStarted = false;
        gameOver = false;
        bombRevealedCount = 0;
    }

    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return cells[y][x];
    }

    public int incrementBombCount() {
        bombRevealedCount++;
        return bombRevealedCount;
    }

    public int getBombRevealedCount() {
        return bombRevealedCount;
    }

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

    public int getNeighboringMinesCount(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 0; // Out of bounds
        }

        int minesCount = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int adjX = x + j;
                int adjY = y + i;
                if (adjX >= 0 && adjX < width && adjY >= 0 && adjY < height && cells[adjY][adjX].isMine()) {
                    minesCount++;
                }
            }
        }

        return minesCount;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setCells(Cell[][] cells) {
        this.cells = cells;
    }
}
