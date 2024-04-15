package minesweeperjs;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class GameBoard {
    private Cell[][] cells;
    private Player[] players;
    private int width;
    private int height;
    private int mineCount;
    private boolean gameStarted;
    private boolean gameOver;
    private int bombRevealedCount;
    private MoveEvaluator moveEvaluator;
    private ExecutorService executorService;

    public GameBoard(int width, int height, int mineCount, Player[] players) {
        this.width = width;
        this.height = height;
        this.mineCount = mineCount;
        this.players = players;
        this.gameStarted = false;
        this.gameOver = false;
        this.bombRevealedCount = 0;
        this.cells = new Cell[height][width];
        initializeCells();
        placeMines();
        calculateNeighboringMines();
        this.moveEvaluator = new MoveEvaluator(this, players);
        this.executorService = Executors.newFixedThreadPool(4); // Parallelism level set
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
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false; // Cell is out of bounds
        }

        Cell cell = cells[y][x];
        if (cell.isRevealed()) {
            return false; // Cell is already revealed
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
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0)
                        continue; // Skip the current cell
                    revealCell(x + j, y + i, player);
                }
            }
        }

        return false;
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
            // Out of bounds, return 0 or an appropriate error value
            return 0;
        }

        int minesCount = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int adjX = x + j;
                int adjY = y + i;

                // Check boundaries
                if (adjX < 0 || adjX >= width || adjY < 0 || adjY >= height) {
                    continue;
                }

                // Check if the adjacent cell is a mine
                if (cells[adjY][adjX].isMine()) {
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

    public GameBoard simulateReveal(int x, int y, Player player) {
        GameBoard simulatedBoard = new GameBoard(this.width, this.height, this.mineCount, this.players);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                simulatedBoard.cells[i][j] = new Cell(this.cells[i][j]);
            }
        }

        simulatedBoard.revealCell(x, y, player);
        return simulatedBoard;
    }

    // Update this method to pass an integer for parallelism
    public MoveEvaluator.MoveResult evaluateMoveWithDepth(int x, int y, Player player) {
        int depth = 3; // Depth level for move evaluation
        int parallelism = 4; // Parallelism level
        return moveEvaluator.evaluateMoveWithDepthAndParallelism(x, y, player, depth, parallelism);
    }

    // Ensure to properly shutdown the executor service
    public void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
